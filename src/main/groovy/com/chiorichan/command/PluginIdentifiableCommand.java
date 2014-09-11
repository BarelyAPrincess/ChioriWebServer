/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.command;

import com.chiorichan.plugin.Plugin;

/**
 * This interface is used by the help system to group commands into sub-indexes based on the {@link Plugin} they are a
 * part of. Custom command implementations will need to implement this interface to have a sub-index automatically
 * generated on the plugin's behalf.
 */
public interface PluginIdentifiableCommand
{
	/**
	 * Gets the owner of this PluginIdentifiableCommand.
	 * 
	 * @return Plugin that owns this PluginIdentifiableCommand.
	 */
	public Plugin getPlugin();
}
