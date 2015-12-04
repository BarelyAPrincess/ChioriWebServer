/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.event.server;

import com.chiorichan.event.EventRegistrar;

/**
 * Used for plugin enable and disable events
 */
public abstract class PluginEvent extends ServerEvent
{
	private final EventRegistrar plugin;
	
	public PluginEvent( final EventRegistrar plugin )
	{
		this.plugin = plugin;
	}
	
	/**
	 * Gets the plugin involved in this event
	 * 
	 * @return Plugin for this event
	 */
	public EventRegistrar getPlugin()
	{
		return plugin;
	}
}
