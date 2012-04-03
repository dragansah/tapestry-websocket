package com.dragansah.tapestry.websocket.services;

import java.util.Collection;

public interface ChatService
{
	Collection<String> getUsers();

	void login(String username);

	void logout(String username);
}
