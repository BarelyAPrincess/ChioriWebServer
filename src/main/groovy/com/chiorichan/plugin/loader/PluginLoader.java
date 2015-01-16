/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.plugin.loader;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.chiorichan.event.Event;
import com.chiorichan.event.Listener;
import com.chiorichan.event.RegisteredListener;
import com.chiorichan.plugin.InvalidDescriptionException;
import com.chiorichan.plugin.InvalidPluginException;
import com.chiorichan.plugin.PluginDescriptionFile;
import com.chiorichan.plugin.UnknownDependencyException;

/**
 * Represents a plugin loader, which handles direct access to specific types
 * of plugins
 */
public interface PluginLoader
{
	/**
	 * Loads the plugin contained in the specified file
	 * 
	 * @param file File to attempt to load
	 * @return Plugin that was contained in the specified file, or null if
	 *         unsuccessful
	 * @throws InvalidPluginException Thrown when the specified file is not a
	 *              plugin
	 * @throws UnknownDependencyException If a required dependency could not
	 *              be found
	 */
	public Plugin loadPlugin( File file ) throws InvalidPluginException, UnknownDependencyException;
	
	/**
	 * Loads a PluginDescriptionFile from the specified file
	 * 
	 * @param file File to attempt to load from
	 * @return A new PluginDescriptionFile loaded from the plugin.yml in the
	 *         specified file
	 * @throws InvalidDescriptionException If the plugin description file
	 *              could not be created
	 */
	public PluginDescriptionFile getPluginDescription( File file ) throws InvalidDescriptionException;
	
	/**
	 * Returns a list of all filename filters expected by this PluginLoader
	 * 
	 * @return The filters
	 */
	public Pattern[] getPluginFileFilters();
	
	/**
	 * Enables the specified plugin
	 * <p>
	 * Attempting to enable a plugin that is already enabled will have no effect
	 * 
	 * @param plugin Plugin to enable
	 */
	public void enablePlugin( Plugin plugin );
	
	/**
	 * Disables the specified plugin
	 * <p>
	 * Attempting to disable a plugin that is not enabled will have no effect
	 * 
	 * @param plugin Plugin to disable
	 */
	public void disablePlugin( Plugin plugin );
}
