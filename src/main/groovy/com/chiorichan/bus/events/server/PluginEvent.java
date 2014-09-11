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

import com.chiorichan.plugin.Plugin;

/**
 * Used for plugin enable and disable events
 */
public abstract class PluginEvent extends ServerEvent
{
	private final Plugin plugin;
	
	public PluginEvent(final Plugin plugin)
	{
		this.plugin = plugin;
	}
	
	/**
	 * Gets the plugin involved in this event
	 * 
	 * @return Plugin for this event
	 */
	public Plugin getPlugin()
	{
		return plugin;
	}
}
