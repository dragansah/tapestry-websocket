package com.dragansah.tapestry.websocket.internal.services;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.apache.tapestry5.TrackableComponentEventCallback;
import org.apache.tapestry5.internal.services.ComponentResultProcessorWrapper;
import org.apache.tapestry5.internal.services.PageActivator;
import org.apache.tapestry5.internal.services.PageRenderQueue;
import org.apache.tapestry5.internal.services.RequestImpl;
import org.apache.tapestry5.internal.services.RequestPageCache;
import org.apache.tapestry5.internal.services.TapestrySessionFactory;
import org.apache.tapestry5.internal.structure.ComponentPageElement;
import org.apache.tapestry5.internal.structure.Page;
import org.apache.tapestry5.internal.util.Holder;
import org.apache.tapestry5.ioc.internal.util.TapestryException;
import org.apache.tapestry5.services.ComponentEventRequestParameters;
import org.apache.tapestry5.services.ComponentEventResultProcessor;
import org.apache.tapestry5.services.ComponentRequestFilter;
import org.apache.tapestry5.services.ComponentRequestHandler;
import org.apache.tapestry5.services.Environment;
import org.apache.tapestry5.services.PageRenderRequestParameters;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.RequestGlobals;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketFactory;

import com.dragansah.tapestry.websocket.services.WebSocketConnectionManager;

/**
 * A filter that intercepts WebSocketMarker-oriented requests, those that originate on the
 * client-side using the WebSocketMarker object. In these cases, the action processing occurs
 * normally, but the response is quite different.
 */
public class WebSocketFilter implements ComponentRequestFilter, WebSocketFactory.Acceptor
{
	private final RequestGlobals requestGlobals;
	private final WebSocketConnectionManager webSocketConnectionManager;
	private final Environment environment;
	private final WebSocketFactory webSocketFactory;
	private final WebSocketComponentEventLinkDecoder linkDecoder;
	private final RequestPageCache cache;
	private final PageRenderQueue queue;
	private final ComponentEventResultProcessor resultProcessor;
	private final PageActivator pageActivator;
	private final String applicationCharset;
	private final TapestrySessionFactory sessionFactory;

	public WebSocketFilter(RequestGlobals requestGlobals,
			WebSocketConnectionManager webSocketConnectionManager, Environment environment,
			WebSocketComponentEventLinkDecoder linkDecoder, RequestPageCache cache,
			PageRenderQueue queue, ComponentEventResultProcessor resultProcessor,
			PageActivator pageActivator, String applicationCharset,
			TapestrySessionFactory sessionFactory)
	{
		this.requestGlobals = requestGlobals;
		this.webSocketConnectionManager = webSocketConnectionManager;
		this.environment = environment;
		this.linkDecoder = linkDecoder;
		this.cache = cache;
		this.queue = queue;
		this.resultProcessor = resultProcessor;
		this.pageActivator = pageActivator;
		this.applicationCharset = applicationCharset;
		this.sessionFactory = sessionFactory;

		webSocketFactory = new WebSocketFactory(this);
	}

	// just a stop point for websocket requests
	@Override
	public void handleComponentEvent(ComponentEventRequestParameters parameters,
			ComponentRequestHandler handler) throws IOException
	{
		boolean websocket = false;
		if (webSocketFactory.acceptWebSocket(requestGlobals.getHTTPServletRequest(),
				requestGlobals.getHTTPServletResponse())
				|| requestGlobals.getResponse().isCommitted())
			websocket = true;

		if (websocket)
			return;

		handler.handleComponentEvent(parameters);
	}

	// just a stop point for websocket requests
	@Override
	public void handlePageRender(PageRenderRequestParameters parameters,
			ComponentRequestHandler handler) throws IOException
	{
		// WebSocket requests come as page render requests, so we handle these
		boolean websocket = false;
		if (webSocketFactory.acceptWebSocket(requestGlobals.getHTTPServletRequest(),
				requestGlobals.getHTTPServletResponse())
				|| requestGlobals.getHTTPServletResponse().isCommitted())
			websocket = true;

		if (websocket)
			return;

		handler.handlePageRender(parameters);
	}

	void processComponentEvent(ComponentEventRequestParameters parameters) throws IOException
	{
		Page activePage = cache.get(parameters.getActivePageName());

		final Holder<Boolean> resultProcessorInvoked = Holder.create();
		resultProcessorInvoked.put(false);

		ComponentEventResultProcessor interceptor = new ComponentEventResultProcessor()
		{
			public void processResultValue(Object value) throws IOException
			{
				resultProcessorInvoked.put(true);

				resultProcessor.processResultValue(value);
			}
		};

		// If we end up doing a partial render, the page render queue service needs to know the
		// page that will be rendered (for logging purposes, if nothing else).

		queue.setRenderingPage(activePage);

		if (pageActivator.activatePage(activePage.getRootElement().getComponentResources(),
				parameters.getPageActivationContext(), interceptor))
			return;

		Page containerPage = cache.get(parameters.getContainingPageName());

		ComponentPageElement element = containerPage.getComponentElementByNestedId(parameters
				.getNestedComponentId());

		// In many cases, the triggered element is a Form that needs to be able to
		// pass its event handler return values to the correct result processor.
		// This is certainly the case for forms.

		TrackableComponentEventCallback callback = new ComponentResultProcessorWrapper(interceptor);

		environment.push(ComponentEventResultProcessor.class, interceptor);
		environment.push(TrackableComponentEventCallback.class, callback);

		boolean handled = element.triggerContextEvent(parameters.getEventType(),
				parameters.getEventContext(), callback);

		if (!handled)
			throw new TapestryException(
					String.format(
							"Request event '%s' (on component %s) was not handled; you must provide a matching event handler method in the component or in one of its containers.",
							parameters.getEventType(), element.getCompleteId()), element, null);

		environment.pop(TrackableComponentEventCallback.class);
		environment.pop(ComponentEventResultProcessor.class);
	}

	@Override
	public WebSocket doWebSocketConnect(final HttpServletRequest request, String protocol)
	{
		return new WebSocket.OnTextMessage()
		{
			private Connection connection;

			@Override
			public void onOpen(Connection connection)
			{
				this.connection = connection;
				storeRequest(request);
				webSocketConnectionManager.saveConnection(getPath(), connection);
				triggerEvent("open");
			}

			@Override
			public void onMessage(String data)
			{
				storeRequest(request);
				triggerEvent(data);
			}

			@Override
			public void onClose(int closeCode, String message)
			{
				storeRequest(request);
				triggerEvent("close");
				webSocketConnectionManager.removeConnection(getPath(), connection);
			}

			private void storeRequest(final HttpServletRequest request)
			{
				requestGlobals.storeServletRequestResponse(request, null);
				Request req = new RequestImpl(request, applicationCharset, sessionFactory);
				requestGlobals.storeRequestResponse(req, null);
			}

			private void triggerEvent(String data)
			{
				try
				{
					// push the Connection to the Environment so that result processors and event
					// handlers can use it if needed. The connection may be exposed as a service
					// (best as a shadow service) in the future.
					environment.push(Connection.class, connection);
					final String requestPath = String.format("%s:%s", getPath(), data);
					environment.push(CurrentPathInfo.class, new CurrentPathInfo(requestPath));

					ComponentEventRequestParameters params = linkDecoder
							.decodeComponentEventRequest(requestPath);
					if (params != null)
						processComponentEvent(params);
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				finally
				{
					environment.pop(Connection.class);
					environment.pop(CurrentPathInfo.class);
				}
			}

			private String getPath()
			{
				String path = requestGlobals.getRequest().getPath();
				if (path.equals(""))
					path = "/";
				if (path.equals("/"))
					path = "/index"; // hack for index pages
				return path;
			}
		};
	}

	@Override
	public boolean checkOrigin(HttpServletRequest request, String origin)
	{
		return true;
	}

}
