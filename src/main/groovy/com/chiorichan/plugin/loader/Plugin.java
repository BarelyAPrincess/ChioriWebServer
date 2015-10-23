/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.plugin.loader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.lang3.Validate;

import com.chiorichan.ServerLogger;
import com.chiorichan.Loader;
import com.chiorichan.configuration.file.FileConfiguration;
import com.chiorichan.configuration.file.YamlConfiguration;
import com.chiorichan.plugin.PluginBase;
import com.chiorichan.plugin.PluginInformation;
import com.chiorichan.plugin.lang.PluginException;

public abstract class Plugin extends PluginBase
{
	private ClassLoader classLoader = null;
	private File configFile = null;
	private File dataFolder = null;
	private PluginInformation description = null;
	private File file = null;
	private boolean isEnabled = false;
	private PluginLoader loader = null;
	private boolean naggable = true;
	private FileConfiguration newConfig = null;
	
	public Plugin()
	{
		PluginClassLoader.initalize( this );
	}
	
	/*
	 * protected Plugin( final PluginLoader loader, final PluginDescriptionFile description, final File dataFolder, final File file )
	 * {
	 * final ClassLoader classLoader = this.getClass().getClassLoader();
	 * if ( classLoader instanceof PluginClassLoader )
	 * {
	 * throw new IllegalStateException( "Cannot use initialization constructor at runtime" );
	 * }
	 * init( loader, description, dataFolder, file, classLoader );
	 * }
	 */
	
	/**
	 * This method provides fast access to the plugin that has {@link #getProvidingPlugin(Class) provided} the given plugin class, which is
	 * usually the plugin that implemented it.
	 * <p>
	 * An exception to this would be if plugin's jar that contained the class does not extend the class, where the intended plugin would have resided in a different jar / classloader.
	 * 
	 * @param clazz
	 *            the class desired
	 * @return the plugin that provides and implements said class
	 * @throws IllegalArgumentException
	 *             if clazz is null
	 * @throws IllegalArgumentException
	 *             if clazz does not extend {@link Plugin}
	 * @throws IllegalStateException
	 *             if clazz was not provided by a plugin,
	 *             for example, if called with <code>Plugin.getPlugin(Plugin.class)</code>
	 * @throws IllegalStateException
	 *             if called from the static initializer for
	 *             given Plugin
	 * @throws ClassCastException
	 *             if plugin that provided the class does not
	 *             extend the class
	 */
	public static <T extends Plugin> T getPlugin( Class<T> clazz )
	{
		Validate.notNull( clazz, "Null class cannot have a plugin" );
		if ( !Plugin.class.isAssignableFrom( clazz ) )
			throw new IllegalArgumentException( clazz + " does not extend " + Plugin.class );
		final ClassLoader cl = clazz.getClassLoader();
		if ( ! ( cl instanceof PluginClassLoader ) )
			throw new IllegalArgumentException( clazz + " is not initialized by " + PluginClassLoader.class );
		Plugin plugin = ( ( PluginClassLoader ) cl ).plugin;
		if ( plugin == null )
			throw new IllegalStateException( "Cannot get plugin for " + clazz + " from a static initializer" );
		return clazz.cast( plugin );
	}
	
	/**
	 * This method provides fast access to the plugin that has provided the
	 * given class.
	 * 
	 * @throws IllegalArgumentException
	 *             if the class is not provided by a
	 *             Plugin
	 * @throws IllegalArgumentException
	 *             if class is null
	 * @throws IllegalStateException
	 *             if called from the static initializer for
	 *             given Plugin
	 */
	public static Plugin getProvidingPlugin( Class<?> clazz )
	{
		Validate.notNull( clazz, "Null class cannot have a plugin" );
		final ClassLoader cl = clazz.getClassLoader();
		if ( ! ( cl instanceof PluginClassLoader ) )
			throw new IllegalArgumentException( clazz + " is not provided by " + PluginClassLoader.class );
		Plugin plugin = ( ( PluginClassLoader ) cl ).plugin;
		if ( plugin == null )
			throw new IllegalStateException( "Cannot get plugin for " + clazz + " from a static initializer" );
		return plugin;
	}
	
	/**
	 * Returns the ClassLoader which holds this plugin
	 * 
	 * @return ClassLoader holding this plugin
	 */
	protected final ClassLoader getClassLoader()
	{
		return classLoader;
	}
	
	@Override
	public final FileConfiguration getConfig()
	{
		if ( newConfig == null )
			reloadConfig();
		return newConfig;
	}
	
	/**
	 * Returns the folder that the plugin data's files are located in. The
	 * folder may not yet exist.
	 * 
	 * @return The folder.
	 */
	@Override
	public final File getDataFolder()
	{
		return dataFolder;
	}
	
	/**
	 * Returns the plugin.yaml file containing the details for this plugin
	 * 
	 * @return Contents of the plugin.yaml file
	 */
	@Override
	public final PluginInformation getDescription()
	{
		return description;
	}
	
	/**
	 * Returns the file which contains this plugin
	 * 
	 * @return File containing this plugin
	 */
	protected final File getFile()
	{
		return file;
	}
	
	public final ServerLogger getLogger()
	{
		return Loader.getLogger( getDescription().getName() );
	}
	
	/**
	 * Gets the associated PluginLoader responsible for this plugin
	 * 
	 * @return PluginLoader that controls this plugin
	 */
	@Override
	public final PluginLoader getPluginLoader()
	{
		return loader;
	}
	
	@Override
	public final InputStream getResource( String filename )
	{
		if ( filename == null )
			throw new IllegalArgumentException( "Filename cannot be null" );
		
		try
		{
			URL url = getClassLoader().getResource( filename );
			
			if ( url == null )
				return null;
			
			URLConnection connection = url.openConnection();
			connection.setUseCaches( false );
			return connection.getInputStream();
		}
		catch ( IOException ex )
		{
			return null;
		}
	}
	
	final void init( PluginLoader loader, PluginInformation description, File dataFolder, File file, ClassLoader classLoader )
	{
		this.loader = loader;
		this.file = file;
		this.description = description;
		this.dataFolder = dataFolder;
		this.classLoader = classLoader;
		
		File yamlFile = new File( dataFolder, "config.yaml" );
		File ymlFile = new File( dataFolder, "config.yml" );
		
		if ( ymlFile.exists() )
			ymlFile.renameTo( yamlFile );
		
		configFile = yamlFile;
	}
	
	/**
	 * Returns a value indicating whether or not this plugin is currently
	 * enabled
	 * 
	 * @return true if this plugin is enabled, otherwise false
	 */
	@Override
	public final boolean isEnabled()
	{
		return isEnabled;
	}
	
	@Override
	public final boolean isNaggable()
	{
		return naggable;
	}
	
	@Override
	public void reloadConfig()
	{
		newConfig = YamlConfiguration.loadConfiguration( configFile );
		
		InputStream defConfigStream = getResource( "config.yaml" );
		
		if ( defConfigStream == null )
			defConfigStream = getResource( "config.yml" );
		
		if ( defConfigStream != null )
		{
			YamlConfiguration defConfig = YamlConfiguration.loadConfiguration( defConfigStream );
			
			newConfig.setDefaults( defConfig );
		}
	}
	
	@Override
	public void saveConfig()
	{
		try
		{
			getConfig().save( configFile );
		}
		catch ( IOException ex )
		{
			getLogger().severe( "Could not save config to " + configFile, ex );
		}
	}
	
	@Override
	public void saveDefaultConfig()
	{
		if ( !configFile.exists() )
			try
			{
				saveResource( "config.yaml", false );
			}
			catch ( IllegalArgumentException e )
			{
				saveResource( "config.yml", false );
			}
	}
	
	@Override
	public void saveResource( String resourcePath, boolean replace )
	{
		if ( resourcePath == null || resourcePath.equals( "" ) )
			throw new IllegalArgumentException( "ResourcePath cannot be null or empty" );
		
		resourcePath = resourcePath.replace( '\\', '/' );
		InputStream in = getResource( resourcePath );
		if ( in == null )
			throw new IllegalArgumentException( "The embedded resource '" + resourcePath + "' cannot be found in " + file );
		
		File outFile = new File( dataFolder, resourcePath );
		int lastIndex = resourcePath.lastIndexOf( '/' );
		File outDir = new File( dataFolder, resourcePath.substring( 0, lastIndex >= 0 ? lastIndex : 0 ) );
		
		if ( !outDir.exists() )
			outDir.mkdirs();
		
		try
		{
			if ( !outFile.exists() || replace )
			{
				OutputStream out = new FileOutputStream( outFile );
				byte[] buf = new byte[1024];
				int len;
				while ( ( len = in.read( buf ) ) > 0 )
					out.write( buf, 0, len );
				out.close();
				in.close();
			}
			else
				getLogger().warning( "Could not save " + outFile.getName() + " to " + outFile + " because " + outFile.getName() + " already exists." );
		}
		catch ( IOException ex )
		{
			getLogger().severe( "Could not save " + outFile.getName() + " to " + outFile, ex );
		}
	}
	
	/**
	 * Sets the enabled state of this plugin
	 * 
	 * @param enabled
	 *            true if enabled, otherwise false
	 */
	protected final void setEnabled( final boolean enabled ) throws PluginException
	{
		if ( isEnabled != enabled )
		{
			isEnabled = enabled;
			
			if ( isEnabled )
				onEnable();
			else
				onDisable();
		}
	}
	
	@Override
	public final void setNaggable( boolean canNag )
	{
		naggable = canNag;
	}
	
	@Override
	public String toString()
	{
		return description.getFullName();
	}
}
