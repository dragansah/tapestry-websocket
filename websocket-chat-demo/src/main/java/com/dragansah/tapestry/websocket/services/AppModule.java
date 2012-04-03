package com.dragansah.tapestry.websocket.services;

import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.SubModule;

import com.dragansah.tapestry.websocket.WebSocketModule;

@SubModule(WebSocketModule.class)
public class AppModule
{
	public static void bind(ServiceBinder binder)
	{
		binder.bind(ChatService.class, ChatServiceImpl.class);
	}

	public static void contributeFactoryDefaults(MappedConfiguration<String, Object> configuration)
	{
		configuration.override(SymbolConstants.APPLICATION_VERSION, "1.0-SNAPSHOT");
	}

	public static void contributeApplicationDefaults(
			MappedConfiguration<String, Object> configuration)
	{
		configuration.add(SymbolConstants.SUPPORTED_LOCALES, "en");
		configuration.add(SymbolConstants.PRODUCTION_MODE, false);
	}

}
