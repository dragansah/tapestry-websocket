package com.dragansah.tapestry.websocket.services;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatServiceImpl implements ChatService
{
	private CopyOnWriteArrayList<String> users;

	public ChatServiceImpl()
	{
		users = new CopyOnWriteArrayList<String>();
	}

	@Override
	public Collection<String> getUsers()
	{
		return users;
	}

	@Override
	public void login(String username)
	{
		if (!users.contains(username))
			users.add(username);
	}

	@Override
	public void logout(String username)
	{
		if (users.contains(username))
			users.remove(username);
	}
}
