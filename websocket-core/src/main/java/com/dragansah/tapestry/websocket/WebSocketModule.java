package com.dragansah.tapestry.websocket;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.internal.services.ObjectComponentEventResultProcessor;
import org.apache.tapestry5.internal.services.PageActivator;
import org.apache.tapestry5.internal.services.PageRenderQueue;
import org.apache.tapestry5.internal.services.RequestPageCache;
import org.apache.tapestry5.internal.services.RequestSecurityManager;
import org.apache.tapestry5.internal.services.TapestrySessionFactory;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.Autobuild;
import org.apache.tapestry5.ioc.annotations.Contribute;
import org.apache.tapestry5.ioc.annotations.Marker;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.ioc.internal.util.CollectionFactory;
import org.apache.tapestry5.ioc.services.StrategyBuilder;
import org.apache.tapestry5.ioc.util.StrategyRegistry;
import org.apache.tapestry5.json.JSONObject;
import org.apache.tapestry5.services.ComponentEventResultProcessor;
import org.apache.tapestry5.services.ComponentRequestFilter;
import org.apache.tapestry5.services.Environment;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.RequestGlobals;
import org.apache.tapestry5.services.transform.ComponentClassTransformWorker2;
import org.eclipse.jetty.websocket.WebSocket.Connection;

import com.dragansah.tapestry.websocket.internal.services.BroadcastPathInfo;
import com.dragansah.tapestry.websocket.internal.services.BroadcastWorker;
import com.dragansah.tapestry.websocket.internal.services.WebSocketComponentEventLinkDecoder;
import com.dragansah.tapestry.websocket.internal.services.WebSocketConnectionManagerImpl;
import com.dragansah.tapestry.websocket.internal.services.WebSocketFilter;
import com.dragansah.tapestry.websocket.internal.services.WebSocketMarker;
import com.dragansah.tapestry.websocket.services.WebSocketConnectionManager;

/**
 * This module is automatically included as part of the Tapestry IoC Registry, it's a good place to
 * configure and extend Tapestry, or to place your own service definitions.
 */
public class WebSocketModule
{
	private final StrategyBuilder strategyBuilder;

	public WebSocketModule(StrategyBuilder strategyBuilder)
	{
		this.strategyBuilder = strategyBuilder;
	}

	public static void bind(ServiceBinder binder)
	{
		binder.bind(WebSocketConnectionManager.class, WebSocketConnectionManagerImpl.class);
	}

	/**
	 * The component event result processor used for Ajax-oriented component requests.
	 */
	@Marker(WebSocketMarker.class)
	public ComponentEventResultProcessor buildWebSocketComponentEventResultProcessor(
			Map<Class, ComponentEventResultProcessor> configuration)
	{
		Set<Class> handledTypes = CollectionFactory.newSet(configuration.keySet());

		// A slight hack!

		configuration.put(Object.class, new ObjectComponentEventResultProcessor(handledTypes));

		StrategyRegistry<ComponentEventResultProcessor> registry = StrategyRegistry.newInstance(
				ComponentEventResultProcessor.class, configuration);

		return strategyBuilder.build(registry);
	}

	public void contributeComponentRequestHandler(
			OrderedConfiguration<ComponentRequestFilter> configuration,
			final RequestSecurityManager requestSecurityManager, RequestGlobals requestGlobals,
			WebSocketConnectionManager webSocketConnectionManager, final Environment environment,
			@Autobuild WebSocketComponentEventLinkDecoder linkDecoder, RequestPageCache cache,
			PageRenderQueue queue, @WebSocketMarker ComponentEventResultProcessor resultProcessor,
			PageActivator pageActivator,
			@Symbol(SymbolConstants.CHARSET) String applicationCharset,
			TapestrySessionFactory sessionFactory)
	{
		configuration.add("WebSocket", new WebSocketFilter(requestGlobals,
				webSocketConnectionManager, environment, linkDecoder, cache, queue,
				resultProcessor, pageActivator, applicationCharset, sessionFactory), "before:Ajax");
	}

	@Contribute(ComponentClassTransformWorker2.class)
	public void setupWorkers(OrderedConfiguration<ComponentClassTransformWorker2> conf,
			Environment environment)
	{
		conf.add("BroadcastWorker", new BroadcastWorker(environment));
	}

	@WebSocketMarker
	@Contribute(ComponentEventResultProcessor.class)
	public static void provideWebSocketComponentEventResultProcessors(
			MappedConfiguration<Class, ComponentEventResultProcessor> configuration,
			final WebSocketConnectionManager connectionManager, final Request request,
			final Environment environment)
	{
		final ComponentEventResultProcessor<String> stringProcessor = new ComponentEventResultProcessor<String>()
		{
			@Override
			public void processResultValue(String value)
			{
				Connection conn = environment.peek(Connection.class);
				BroadcastPathInfo broadcast = environment.peek(BroadcastPathInfo.class);
				if (broadcast != null)
				{
					String path = broadcast.path.equals("") ? request.getPath()
							: broadcast.path;
					if (path.equals(""))
						path = "/";
					if (path.equals("/"))
						path = "/index"; // hack for index pages
					for (Connection connection : connectionManager.getConnections(path))
					{
						try
						{
							connection.sendMessage(value);
						}
						catch (IOException e)
						{
							e.printStackTrace();
						}
					}
					return;
				}

				if (conn != null)
					try
					{
						conn.sendMessage(value);
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
			}
		};

		configuration.add(String.class, stringProcessor);

		configuration.add(JSONObject.class, new ComponentEventResultProcessor<JSONObject>()
		{
			@Override
			public void processResultValue(JSONObject value) throws IOException
			{
				stringProcessor.processResultValue(value.toCompactString());
			}
		});

	}
}
