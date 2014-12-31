/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.plugin;

import java.io.File;
import java.io.InputStream;

import com.chiorichan.configuration.file.FileConfiguration;

/**
 * Represents a Plugin
 * <p>
 * The use of {@link PluginBase} is recommended for actual Implementation
 */
public interface Plugin
{
	/**
	 * Returns the folder that the plugin data's files are located in. The folder may not yet exist.
	 * 
	 * @return The folder
	 */
	public File getDataFolder();
	
	/**
	 * Returns the plugin.yaml file containing the details for this plugin
	 * 
	 * @return Contents of the plugin.yaml file
	 */
	public PluginDescriptionFile getDescription();
	
	/**
	 * Gets a {@link FileConfiguration} for this plugin, read through "config.yml"
	 * <p>
	 * If there is a default config.yml embedded in this plugin, it will be provided as a default for this Configuration.
	 * 
	 * @return Plugin configuration
	 */
	public FileConfiguration getConfig();
	
	/**
	 * Gets an embedded resource in this plugin
	 * 
	 * @param filename
	 *           Filename of the resource
	 * @return File if found, otherwise null
	 */
	public InputStream getResource( String filename );
	
	/**
	 * Saves the {@link FileConfiguration} retrievable by {@link #getConfig()}.
	 */
	public void saveConfig();
	
	/**
	 * Saves the raw contents of the default config.yml file to the location retrievable by {@link #getConfig()}. If
	 * there is no default config.yml embedded in the plugin, an empty config.yml file is saved. This should fail
	 * silently if the config.yml already exists.
	 */
	public void saveDefaultConfig();
	
	/**
	 * Saves the raw contents of any resource embedded with a plugin's .jar file assuming it can be found using
	 * {@link #getResource(String)}. The resource is saved into the plugin's data folder using the same hierarchy as the
	 * .jar file (subdirectories are preserved).
	 * 
	 * @param resourcePath
	 *           the embedded resource path to look for within the plugin's .jar file. (No preceding slash).
	 * @param replace
	 *           if true, the embedded resource will overwrite the contents of an existing file.
	 * @throws IllegalArgumentException
	 *            if the resource path is null, empty, or points to a nonexistent resource.
	 */
	public void saveResource( String resourcePath, boolean replace );
	
	/**
	 * Discards any data in {@link #getConfig()} and reloads from disk.
	 */
	public void reloadConfig();
	
	/**
	 * Gets the associated PluginLoader responsible for this plugin
	 * 
	 * @return PluginLoader that controls this plugin
	 */
	public PluginLoader getPluginLoader();
	
	/**
	 * Returns a value indicating whether or not this plugin is currently enabled
	 * 
	 * @return true if this plugin is enabled, otherwise false
	 */
	public boolean isEnabled();
	
	/**
	 * Called when this plugin is disabled
	 */
	public void onDisable();
	
	/**
	 * Called after a plugin is loaded but before it has been enabled. When mulitple plugins are loaded, the onLoad() for
	 * all plugins is called before any onEnable() is called.
	 */
	public void onLoad();
	
	/**
	 * Called when this plugin is enabled
	 */
	public void onEnable();
	
	/**
	 * Simple boolean if we can still nag to the logs about things
	 * 
	 * @return boolean whether we can nag
	 */
	public boolean isNaggable();
	
	/**
	 * Set naggable state
	 * 
	 * @param canNag
	 *           is this plugin still naggable?
	 */
	public void setNaggable( boolean canNag );
	
	/**
	 * Returns the name of the plugin.
	 * <p>
	 * This should return the bare name of the plugin and should be used for comparison.
	 * 
	 * @return name of the plugin
	 */
	public String getName();
}
