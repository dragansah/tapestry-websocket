package com.dragansah.tapestry.websocket.pages;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

import org.apache.tapestry5.Asset;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.Log;
import org.apache.tapestry5.annotations.Path;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.json.JSONObject;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

import com.dragansah.tapestry.websocket.annotations.Broadcast;
import com.dragansah.tapestry.websocket.services.ChatService;

@Import(library = "chat.js", stylesheet = "chat.css")
public class Index
{
	@SuppressWarnings("unused")
	@Inject
	@Path("tapestry.png")
	@Property
	private Asset logo;

	@SuppressWarnings("unused")
	@Property
	private String user;

	@Inject
	private ChatService chatService;

	@Inject
	private JavaScriptSupport jss;

	@Inject
	private PageRenderLinkSource linkSource;

	void setupRender()
	{
		String webSocketURL = linkSource.createPageRenderLink(Index.class).toAbsoluteURI()
				.replace("http", "ws");
		jss.addInitializerCall("initChat", new JSONObject("url", webSocketURL));
	}

	public Collection<String> getUsers()
	{
		return chatService.getUsers();
	}

	/**
	 * Open WebSocket connection
	 */
	@Log
	void onOpen()
	{

	}

	/**
	 * Close WebSocket connection
	 */
	@Log
	void onClose()
	{
	}

	@Log
	@Broadcast
	Object onLogin(String username) throws IOException
	{
		chatService.login(username);
		return new JSONObject("loginUser", username).toCompactString();

	}

	@Log
	@Broadcast
	Object onLogout(String username) throws IOException
	{
		chatService.logout(username);
		return new JSONObject("logoutUser", username).toCompactString();
	}

	@Log
	@Broadcast
	Object onMessage(String message, String fromUser) throws IOException
	{
		String date = new SimpleDateFormat("hh:mm:ss").format(new Date());
		return new JSONObject("message", new JSONObject("timestamp", date, "username", fromUser,
				"content", message).toCompactString()).toCompactString();
	}
}
