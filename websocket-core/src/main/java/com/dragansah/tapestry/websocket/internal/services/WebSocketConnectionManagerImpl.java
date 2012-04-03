package com.dragansah.tapestry.websocket.internal.services;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;

import org.apache.tapestry5.ioc.internal.util.CollectionFactory;
import org.apache.tapestry5.services.Environment;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.Session;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocket.Connection;

import com.dragansah.tapestry.websocket.services.WebSocketConnectionManager;

public class WebSocketConnectionManagerImpl implements WebSocketConnectionManager
{
	private ConcurrentMap<String, Collection<Connection>> pathToConnections;

	private static final String SESSION_TOKEN = "websocket-session-token";

	@Inject
	private Request request;

	@Inject
	private Environment environment;

	public WebSocketConnectionManagerImpl()
	{
		pathToConnections = CollectionFactory.newConcurrentMap();
	}

	@Override
	public void saveConnection(String path, Connection connection)
	{
		String token = (String) getSession().getAttribute(SESSION_TOKEN);
		if (token == null)
		{
			token = UUID.randomUUID().toString();
			getSession().setAttribute(SESSION_TOKEN, token);
		}

		if (!pathToConnections.containsKey(path))
			pathToConnections.put(path, new ConcurrentLinkedQueue<WebSocket.Connection>());

		pathToConnections.get(path).add(connection);
	}

	private Session getSession()
	{
		return request.getSession(true);
	}

	@Override
	public void removeConnection(String path, Connection connection)
	{
		pathToConnections.get(path).remove(connection);

		if (pathToConnections.get(path).size() == 0)
			pathToConnections.remove(path);
	}

	@Override
	public Collection<Connection> getConnections()
	{
		if (environment.peek(Connection.class) == null)
			throw new IllegalStateException(
					"Attempting to access the WebSocket path, when a WebSocket request is not in progress.");

		return getConnections(request.getPath());
	}

	@Override
	public Collection<Connection> getConnections(String path)
	{
		return Collections.unmodifiableCollection(pathToConnections.get(path));
	}
}
