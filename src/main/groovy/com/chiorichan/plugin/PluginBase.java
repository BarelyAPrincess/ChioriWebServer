/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.plugin;

import java.io.File;
import java.io.InputStream;

import com.chiorichan.configuration.file.FileConfiguration;
import com.chiorichan.event.EventCreator;
import com.chiorichan.libraries.LibrarySource;
import com.chiorichan.plugin.loader.Plugin;
import com.chiorichan.plugin.loader.PluginLoader;
import com.chiorichan.tasks.TaskCreator;

public abstract class PluginBase implements EventCreator, TaskCreator, LibrarySource
{
	/**
	 * Returns the folder that the plugin data's files are located in. The folder may not yet exist.
	 * 
	 * @return The folder
	 */
	public abstract File getDataFolder();
	
	/**
	 * Returns the plugin.yaml file containing the details for this plugin
	 * 
	 * @return Contents of the plugin.yaml file
	 */
	public abstract PluginDescriptionFile getDescription();
	
	/**
	 * Gets a {@link FileConfiguration} for this plugin, read through "config.yml"
	 * <p>
	 * If there is a default config.yml embedded in this plugin, it will be provided as a default for this Configuration.
	 * 
	 * @return Plugin configuration
	 */
	public abstract FileConfiguration getConfig();
	
	/**
	 * Gets an embedded resource in this plugin
	 * 
	 * @param filename
	 *            Filename of the resource
	 * @return File if found, otherwise null
	 */
	public abstract InputStream getResource( String filename );
	
	/**
	 * Saves the {@link FileConfiguration} retrievable by {@link #getConfig()}.
	 */
	public abstract void saveConfig();
	
	/**
	 * Saves the raw contents of the default config.yml file to the location retrievable by {@link #getConfig()}. If
	 * there is no default config.yml embedded in the plugin, an empty config.yml file is saved. This should fail
	 * silently if the config.yml already exists.
	 */
	public abstract void saveDefaultConfig();
	
	/**
	 * Saves the raw contents of any resource embedded with a plugin's .jar file assuming it can be found using {@link #getResource(String)}. The resource is saved into the plugin's data folder using the same hierarchy as the
	 * .jar file (subdirectories are preserved).
	 * 
	 * @param resourcePath
	 *            the embedded resource path to look for within the plugin's .jar file. (No preceding slash).
	 * @param replace
	 *            if true, the embedded resource will overwrite the contents of an existing file.
	 * @throws IllegalArgumentException
	 *             if the resource path is null, empty, or points to a nonexistent resource.
	 */
	public abstract void saveResource( String resourcePath, boolean replace );
	
	/**
	 * Discards any data in {@link #getConfig()} and reloads from disk.
	 */
	public abstract void reloadConfig();
	
	/**
	 * Gets the associated PluginLoader responsible for this plugin
	 * 
	 * @return PluginLoader that controls this plugin
	 */
	public abstract PluginLoader getPluginLoader();
	
	/**
	 * Returns a value indicating whether or not this plugin is currently enabled
	 * 
	 * @return true if this plugin is enabled, otherwise false
	 */
	public abstract boolean isEnabled();
	
	/**
	 * Called when this plugin is disabled
	 */
	public abstract void onDisable();
	
	/**
	 * Called after a plugin is loaded but before it has been enabled. When mulitple plugins are loaded, the onLoad() for
	 * all plugins is called before any onEnable() is called.
	 */
	public abstract void onLoad();
	
	/**
	 * Called when this plugin is enabled
	 */
	public abstract void onEnable();
	
	/**
	 * Simple boolean if we can still nag to the logs about things
	 * 
	 * @return boolean whether we can nag
	 */
	public abstract boolean isNaggable();
	
	/**
	 * Set naggable state
	 * 
	 * @param canNag
	 *            is this plugin still naggable?
	 */
	public abstract void setNaggable( boolean canNag );
	
	/**
	 * Returns the name of the plugin.
	 * <p>
	 * This should return the bare name of the plugin and should be used for comparison.
	 * 
	 * @return name of the plugin
	 */
	public final String getName()
	{
		return getDescription().getName();
	}
	
	@Override
	public final int hashCode()
	{
		try
		{
			return getName().hashCode();
		}
		catch ( NullPointerException e )
		{
			return super.hashCode();
		}
	}
	
	@Override
	public final boolean equals( Object obj )
	{
		if ( this == obj )
		{
			return true;
		}
		if ( obj == null )
		{
			return false;
		}
		if ( ! ( obj instanceof Plugin ) )
		{
			return false;
		}
		try
		{
			return getName().equals( ( ( Plugin ) obj ).getName() );
		}
		catch ( NullPointerException e )
		{
			return false;
		}
	}
}
