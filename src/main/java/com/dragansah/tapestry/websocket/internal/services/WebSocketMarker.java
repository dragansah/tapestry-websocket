package com.dragansah.tapestry.websocket.internal.services;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.tapestry5.services.ComponentEventRequestHandler;

/**
 * Marker annotation for services related to processing an WebSocketMarker request (rather than a
 * {@linkplain org.apache.tapestry5.services.Traditional traditional} or
 * {@linkplain org.apache.tapestry5.services.Ajax ajax} requests).
 * 
 * @see ComponentEventRequestHandler
 */
@Target(
{ ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WebSocketMarker
{
}
