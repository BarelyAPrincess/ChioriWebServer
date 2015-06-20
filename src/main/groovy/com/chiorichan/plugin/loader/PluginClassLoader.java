/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.plugin.loader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.apache.commons.lang3.Validate;

import com.chiorichan.lang.PluginInvalidException;
import com.chiorichan.plugin.PluginDescriptionFile;

/**
 * A ClassLoader for plugins, to allow shared classes across multiple plugins
 *
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
public final class PluginClassLoader extends URLClassLoader
{
	private static final Map<Class<?>, PluginClassLoader> loaders = new WeakHashMap<Class<?>, PluginClassLoader>();
	
	private final JavaPluginLoader loader;
	private final Map<String, Class<?>> classes = new HashMap<String, Class<?>>();
	private final PluginDescriptionFile description;
	private final File dataFolder;
	private final File file;
	final Plugin plugin;
	private boolean initalized = false;
	
	PluginClassLoader( final JavaPluginLoader loader, final ClassLoader parent, final PluginDescriptionFile description, final File dataFolder, final File file ) throws PluginInvalidException, MalformedURLException
	{
		super( new URL[] {file.toURI().toURL()}, parent );
		
		Validate.notNull( loader, "Loader cannot be null" );
		
		this.loader = loader;
		this.description = description;
		this.dataFolder = dataFolder;
		this.file = file;
		
		try
		{
			Class<?> jarClass;
			try
			{
				jarClass = Class.forName( description.getMain(), true, this );
			}
			catch ( ClassNotFoundException ex )
			{
				throw new PluginInvalidException( "Cannot find main class `" + description.getMain() + "'", ex );
			}
			
			Class<? extends Plugin> pluginClass;
			try
			{
				pluginClass = jarClass.asSubclass( Plugin.class );
			}
			catch ( ClassCastException ex )
			{
				throw new PluginInvalidException( "main class `" + description.getMain() + "' does not extend Plugin", ex );
			}
			
			loaders.put( jarClass, this );
			
			plugin = pluginClass.newInstance();
		}
		catch ( IllegalAccessException ex )
		{
			throw new PluginInvalidException( "No public constructor", ex );
		}
		catch ( InstantiationException ex )
		{
			throw new PluginInvalidException( "Abnormal plugin type", ex );
		}
	}
	
	@Override
	protected Class<?> findClass( String name ) throws ClassNotFoundException
	{
		return findClass( name, true );
	}
	
	Class<?> findClass( String name, boolean checkGlobal ) throws ClassNotFoundException
	{
		if ( name.startsWith( "com.chiorichan." ) && !name.startsWith( "com.chiorichan.plugin." ) )
		{
			throw new ClassNotFoundException( name );
		}
		
		Class<?> result = classes.get( name );
		
		if ( result == null )
		{
			if ( checkGlobal )
			{
				result = loader.getClassByName( name );
			}
			
			if ( result == null )
			{
				result = super.findClass( name );
				
				if ( result != null )
				{
					loader.setClass( name, result );
				}
			}
			
			classes.put( name, result );
		}
		
		return result;
	}
	
	Set<String> getClasses()
	{
		return classes.keySet();
	}
	
	static synchronized void initalize( Plugin javaPlugin )
	{
		Validate.notNull( javaPlugin, "Initializing plugin cannot be null" );
		
		PluginClassLoader loader = loaders.get( javaPlugin.getClass() );
		
		if ( loader == null )
			throw new IllegalStateException( "Plugin was not properly initalized: '" + javaPlugin.getClass().getName() + "'." );
		
		if ( loader.initalized )
			throw new IllegalArgumentException( "Plugin already initialized: '" + javaPlugin.getClass().getName() + "'." );
		
		javaPlugin.init( loader.loader, loader.description, loader.dataFolder, loader.file, loader );
		loader.initalized = true;
	}
	
	public PluginLoader getPluginLoader()
	{
		return loader;
	}
	
	public Plugin getPlugin()
	{
		return plugin;
	}
}
