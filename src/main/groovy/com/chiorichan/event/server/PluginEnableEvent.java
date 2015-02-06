/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.event.server;

import com.chiorichan.event.EventCreator;
import com.chiorichan.event.HandlerList;

/**
 * Called when a plugin is enabled.
 */
public class PluginEnableEvent extends PluginEvent
{
	private static final HandlerList handlers = new HandlerList();
	
	public PluginEnableEvent( final EventCreator plugin )
	{
		super( plugin );
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
