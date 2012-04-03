package com.dragansah.tapestry.websocket.internal.services;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tapestry5.EventConstants;
import org.apache.tapestry5.EventContext;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.internal.EmptyEventContext;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.services.ComponentClassResolver;
import org.apache.tapestry5.services.ComponentEventRequestParameters;
import org.apache.tapestry5.services.ContextPathEncoder;

public class WebSocketComponentEventLinkDecoder
{
	private final ComponentClassResolver componentClassResolver;

	private final ContextPathEncoder contextPathEncoder;

	private static final char SLASH = '/';

	// A beast that recognizes all the elements of a path in a single go.
	// We skip the leading slash, then take the next few terms (until a dot or a colon)
	// as the page name. Then there's a sequence that sees a dot
	// and recognizes the nested component id (which may be missing), which ends
	// at the colon, or at the slash (or the end of the string). The colon identifies
	// the event name (the event name is also optional). A valid path will always have
	// a nested component id or an event name (or both) ... when both are missing, then the
	// path is most likely a page render request. After the optional event name,
	// the next piece is the action context, which is the remainder of the path.

	private final Pattern COMPONENT_EVENT_REQUEST_PATH_PATTERN;

	// Constants for the match groups in the above pattern.
	private static final int LOGICAL_PAGE_NAME = 1;
	private static final int NESTED_ID = 6;
	private static final int EVENT_NAME = 9;
	private static final int CONTEXT = 11;

	public WebSocketComponentEventLinkDecoder(ComponentClassResolver componentClassResolver,
			ContextPathEncoder contextPathEncoder,
			@Symbol(SymbolConstants.APPLICATION_FOLDER) String applicationFolder)
	{
		this.componentClassResolver = componentClassResolver;
		this.contextPathEncoder = contextPathEncoder;

		boolean hasAppFolder = applicationFolder.equals("");

		String applicationFolderPattern = hasAppFolder ? "" : applicationFolder + SLASH;

		COMPONENT_EVENT_REQUEST_PATH_PATTERN = Pattern.compile(

		"^/" + // The leading slash is recognized but skipped
				applicationFolderPattern + // The folder containing the application (TAP5-743)
				"(((\\w(?:\\w|-)*)/)*(\\w+))" + // A series of folder names (which allow dashes)
												// leading up to the page name, forming
				// the logical page name (may include the locale name)
				"(\\.(\\w+(\\.\\w+)*))?" + // The first dot separates the page name from the nested
				// component id
				"(\\:(\\w+))?" + // A colon, then the event type
				"(/(.*))?", // A slash, then the action context
				Pattern.COMMENTS);
	}

	public ComponentEventRequestParameters decodeComponentEventRequest(String requestPath)
	{
		Matcher matcher = COMPONENT_EVENT_REQUEST_PATH_PATTERN.matcher(requestPath);

		if (!matcher.matches())
			return null;

		String nestedComponentId = matcher.group(NESTED_ID);

		String eventType = matcher.group(EVENT_NAME);

		if (nestedComponentId == null && eventType == null)
			return null;

		String activePageName = matcher.group(LOGICAL_PAGE_NAME);

		if (!componentClassResolver.isPageName(activePageName))
			return null;

		activePageName = componentClassResolver.canonicalizePageName(activePageName);

		EventContext eventContext = new EmptyEventContext();
		// we first encode the value because the link is not built by tapestry and can contain
		// white spaces and other characters that need to be encoded.
		String context = matcher.group(CONTEXT);
		if (context != null)
		{
			String[] contextSplit = context.split("/");
			StringBuilder encodedContext = new StringBuilder();
			for (String contextToEncode : contextSplit)
			{
				encodedContext.append(contextPathEncoder.encodeValue(contextToEncode));
				encodedContext.append("/");
			}
			eventContext = contextPathEncoder.decodePath(encodedContext.toString());
		}

		EventContext activationContext = new EmptyEventContext();
		// .decodePath(request.getParameter(InternalConstants.PAGE_CONTEXT_NAME));

		// The event type is often omitted, and defaults to "action".

		if (eventType == null)
			eventType = EventConstants.ACTION;

		if (nestedComponentId == null)
			nestedComponentId = "";

		String containingPageName = activePageName;

		return new ComponentEventRequestParameters(activePageName, containingPageName,
				nestedComponentId, eventType, activationContext, eventContext);
	}
}
