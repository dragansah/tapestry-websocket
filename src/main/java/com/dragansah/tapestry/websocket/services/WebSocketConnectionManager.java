package com.dragansah.tapestry.websocket.services;

import java.util.Collection;

import org.eclipse.jetty.websocket.WebSocket.Connection;

public interface WebSocketConnectionManager
{
	/**
	 * Saves a {@link Connection} for the given request path in a cache.
	 */
	void saveConnection(String path, Connection connection);

	/**
	 * Removes a {@link Connection} for the given request path from the cache.
	 */
	void removeConnection(String path, Connection connection);

	/**
	 * Returns all connections for WebSocket path in the current WebSocket request. If this method
	 * is called while not in the middle of a WebSocket request, than an
	 * {@link IllegalStateException} is thrown.
	 * 
	 * @throws IllegalStateException
	 *             if called while not in the middle of a WebSocket request.
	 * 
	 * @see {@link WebSocketConnectionManager#getConnection(String)}
	 */
	Collection<Connection> getConnections();

	/**
	 * Returns all connections for the given WebSocket path. This method can be called while not in
	 * the middle of a WebSocket request.
	 * 
	 * @see {@link WebSocketConnectionManager#getConnections()}
	 */
	Collection<Connection> getConnections(String path);
}
