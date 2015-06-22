/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.event.server;

import com.chiorichan.event.EventCreator;
import com.chiorichan.event.HandlerList;

/**
 * Called when a plugin is disabled.
 */
public class PluginDisableEvent extends PluginEvent
{
	private static final HandlerList handlers = new HandlerList();
	
	public PluginDisableEvent( final EventCreator plugin )
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
