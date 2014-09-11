/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.bus.events.server;

import com.chiorichan.bus.events.HandlerList;
import com.chiorichan.plugin.RegisteredServiceProvider;

/**
 * This event is called when a service is registered.<br>
 * <b>Warning:</b> The order in which register and unregister events are called should not be relied upon.
 */
public class ServiceRegisterEvent extends ServiceEvent
{
	private static final HandlerList handlers = new HandlerList();
	
	public ServiceRegisterEvent(RegisteredServiceProvider<?> registeredProvider)
	{
		super( registeredProvider );
	}
	
	@Override
	public HandlerList getHandlers()
	{
		return handlers;
	}
	
	public static HandlerList getHandlerList()
	{
		return handlers;
	}
}
