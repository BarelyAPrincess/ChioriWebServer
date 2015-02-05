/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
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

import com.chiorichan.ConsoleLogger;
import com.chiorichan.Loader;
import com.chiorichan.file.FileConfiguration;
import com.chiorichan.file.YamlConfiguration;
import com.chiorichan.plugin.PluginBase;
import com.chiorichan.plugin.PluginDescriptionFile;

public abstract class Plugin extends PluginBase
{
	private boolean isEnabled = false;
	private PluginLoader loader = null;
	private File file = null;
	private PluginDescriptionFile description = null;
	private File dataFolder = null;
	private ClassLoader classLoader = null;
	private boolean naggable = true;
	private FileConfiguration newConfig = null;
	private File configFile = null;
	
	public Plugin()
	{
		final ClassLoader classLoader = this.getClass().getClassLoader();
		if ( !(classLoader instanceof PluginClassLoader) )
		{
			throw new IllegalStateException( "Plugin requires " + PluginClassLoader.class.getName() );
		}
		((PluginClassLoader) classLoader).initialize( this );
	}
	
	protected Plugin( final PluginLoader loader, final PluginDescriptionFile description, final File dataFolder, final File file )
	{
		final ClassLoader classLoader = this.getClass().getClassLoader();
		if ( classLoader instanceof PluginClassLoader )
		{
			throw new IllegalStateException( "Cannot use initialization constructor at runtime" );
		}
		init( loader, description, dataFolder, file, classLoader );
	}
	
	/**
	 * Returns the folder that the plugin data's files are located in. The
	 * folder may not yet exist.
	 * 
	 * @return The folder.
	 */
	public final File getDataFolder()
	{
		return dataFolder;
	}
	
	/**
	 * Gets the associated PluginLoader responsible for this plugin
	 * 
	 * @return PluginLoader that controls this plugin
	 */
	public final PluginLoader getPluginLoader()
	{
		return loader;
	}
	
	/**
	 * Returns a value indicating whether or not this plugin is currently
	 * enabled
	 * 
	 * @return true if this plugin is enabled, otherwise false
	 */
	public final boolean isEnabled()
	{
		return isEnabled;
	}
	
	/**
	 * Returns the file which contains this plugin
	 * 
	 * @return File containing this plugin
	 */
	protected File getFile()
	{
		return file;
	}
	
	/**
	 * Returns the plugin.yaml file containing the details for this plugin
	 * 
	 * @return Contents of the plugin.yaml file
	 */
	public final PluginDescriptionFile getDescription()
	{
		return description;
	}
	
	public FileConfiguration getConfig()
	{
		if ( newConfig == null )
		{
			reloadConfig();
		}
		return newConfig;
	}
	
	public void reloadConfig()
	{
		newConfig = YamlConfiguration.loadConfiguration( configFile );
		
		InputStream defConfigStream = getResource( "config.yml" );
		if ( defConfigStream != null )
		{
			YamlConfiguration defConfig = YamlConfiguration.loadConfiguration( defConfigStream );
			
			newConfig.setDefaults( defConfig );
		}
	}
	
	public void saveConfig()
	{
		try
		{
			getConfig().save( configFile );
		}
		catch( IOException ex )
		{
			getLogger().severe( "Could not save config to " + configFile, ex );
		}
	}
	
	public void saveDefaultConfig()
	{
		if ( !configFile.exists() )
		{
			saveResource( "config.yml", false );
		}
	}
	
	public void saveResource( String resourcePath, boolean replace )
	{
		if ( resourcePath == null || resourcePath.equals( "" ) )
		{
			throw new IllegalArgumentException( "ResourcePath cannot be null or empty" );
		}
		
		resourcePath = resourcePath.replace( '\\', '/' );
		InputStream in = getResource( resourcePath );
		if ( in == null )
		{
			throw new IllegalArgumentException( "The embedded resource '" + resourcePath + "' cannot be found in " + file );
		}
		
		File outFile = new File( dataFolder, resourcePath );
		int lastIndex = resourcePath.lastIndexOf( '/' );
		File outDir = new File( dataFolder, resourcePath.substring( 0, lastIndex >= 0 ? lastIndex : 0 ) );
		
		if ( !outDir.exists() )
		{
			outDir.mkdirs();
		}
		
		try
		{
			if ( !outFile.exists() || replace )
			{
				OutputStream out = new FileOutputStream( outFile );
				byte[] buf = new byte[1024];
				int len;
				while( (len = in.read( buf )) > 0 )
				{
					out.write( buf, 0, len );
				}
				out.close();
				in.close();
			}
			else
			{
				getLogger().warning( "Could not save " + outFile.getName() + " to " + outFile + " because " + outFile.getName() + " already exists." );
			}
		}
		catch( IOException ex )
		{
			getLogger().severe( "Could not save " + outFile.getName() + " to " + outFile, ex );
		}
	}
	
	public InputStream getResource( String filename )
	{
		if ( filename == null )
		{
			throw new IllegalArgumentException( "Filename cannot be null" );
		}
		
		try
		{
			URL url = getClassLoader().getResource( filename );
			
			if ( url == null )
			{
				return null;
			}
			
			URLConnection connection = url.openConnection();
			connection.setUseCaches( false );
			return connection.getInputStream();
		}
		catch( IOException ex )
		{
			return null;
		}
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
	
	/**
	 * Sets the enabled state of this plugin
	 * 
	 * @param enabled
	 *             true if enabled, otherwise false
	 */
	protected final void setEnabled( final boolean enabled )
	{
		if ( isEnabled != enabled )
		{
			isEnabled = enabled;
			
			if ( isEnabled )
			{
				onEnable();
			}
			else
			{
				onDisable();
			}
		}
	}
	
	final void init( PluginLoader loader, PluginDescriptionFile description, File dataFolder, File file, ClassLoader classLoader )
	{
		this.loader = loader;
		this.file = file;
		this.description = description;
		this.dataFolder = dataFolder;
		this.classLoader = classLoader;
		this.configFile = new File( dataFolder, "config.yml" );
	}
	
	public void onLoad()
	{
	}
	
	public void onDisable()
	{
	}
	
	public void onEnable()
	{
	}
	
	public final boolean isNaggable()
	{
		return naggable;
	}
	
	public final void setNaggable( boolean canNag )
	{
		this.naggable = canNag;
	}
	
	@Override
	public String toString()
	{
		return description.getFullName();
	}
	
	/**
	 * This method provides fast access to the plugin that has {@link #getProvidingPlugin(Class) provided} the given plugin class, which is
	 * usually the plugin that implemented it.
	 * <p>
	 * An exception to this would be if plugin's jar that contained the class does not extend the class, where the intended plugin would have resided in a different jar / classloader.
	 * 
	 * @param clazz
	 *             the class desired
	 * @return the plugin that provides and implements said class
	 * @throws IllegalArgumentException
	 *              if clazz is null
	 * @throws IllegalArgumentException
	 *              if clazz does not extend {@link Plugin}
	 * @throws IllegalStateException
	 *              if clazz was not provided by a plugin,
	 *              for example, if called with <code>Plugin.getPlugin(Plugin.class)</code>
	 * @throws IllegalStateException
	 *              if called from the static initializer for
	 *              given Plugin
	 * @throws ClassCastException
	 *              if plugin that provided the class does not
	 *              extend the class
	 */
	public static <T extends Plugin> T getPlugin( Class<T> clazz )
	{
		Validate.notNull( clazz, "Null class cannot have a plugin" );
		if ( !Plugin.class.isAssignableFrom( clazz ) )
		{
			throw new IllegalArgumentException( clazz + " does not extend " + Plugin.class );
		}
		final ClassLoader cl = clazz.getClassLoader();
		if ( !(cl instanceof PluginClassLoader) )
		{
			throw new IllegalArgumentException( clazz + " is not initialized by " + PluginClassLoader.class );
		}
		Plugin plugin = ((PluginClassLoader) cl).plugin;
		if ( plugin == null )
		{
			throw new IllegalStateException( "Cannot get plugin for " + clazz + " from a static initializer" );
		}
		return clazz.cast( plugin );
	}
	
	/**
	 * This method provides fast access to the plugin that has provided the
	 * given class.
	 * 
	 * @throws IllegalArgumentException
	 *              if the class is not provided by a
	 *              Plugin
	 * @throws IllegalArgumentException
	 *              if class is null
	 * @throws IllegalStateException
	 *              if called from the static initializer for
	 *              given Plugin
	 */
	public static Plugin getProvidingPlugin( Class<?> clazz )
	{
		Validate.notNull( clazz, "Null class cannot have a plugin" );
		final ClassLoader cl = clazz.getClassLoader();
		if ( !(cl instanceof PluginClassLoader) )
		{
			throw new IllegalArgumentException( clazz + " is not provided by " + PluginClassLoader.class );
		}
		Plugin plugin = ((PluginClassLoader) cl).plugin;
		if ( plugin == null )
		{
			throw new IllegalStateException( "Cannot get plugin for " + clazz + " from a static initializer" );
		}
		return plugin;
	}
	
	public final ConsoleLogger getLogger()
	{
		return Loader.getLogger( getDescription().getName() );
	}
}
