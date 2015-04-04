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

import org.apache.commons.lang3.Validate;

import com.chiorichan.lang.InvalidPluginException;
import com.chiorichan.plugin.PluginDescriptionFile;

/**
 * A ClassLoader for plugins, to allow shared classes across multiple plugins
 */
final class PluginClassLoader extends URLClassLoader
{
	private final JavaPluginLoader loader;
	private final Map<String, Class<?>> classes = new HashMap<String, Class<?>>();
	private final PluginDescriptionFile description;
	private final File dataFolder;
	private final File file;
	final Plugin plugin;
	private Plugin pluginInit;
	private IllegalStateException pluginState;
	
	PluginClassLoader( final JavaPluginLoader loader, final ClassLoader parent, final PluginDescriptionFile description, final File dataFolder, final File file ) throws InvalidPluginException, MalformedURLException
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
				throw new InvalidPluginException( "Cannot find mane class `" + description.getMain() + "'", ex );
			}
			
			Class<? extends Plugin> pluginClass;
			try
			{
				pluginClass = jarClass.asSubclass( Plugin.class );
			}
			catch ( ClassCastException ex )
			{
				throw new InvalidPluginException( "main class `" + description.getMain() + "' does not extend Plugin", ex );
			}
			
			plugin = pluginClass.newInstance();
		}
		catch ( IllegalAccessException ex )
		{
			throw new InvalidPluginException( "No public constructor", ex );
		}
		catch ( InstantiationException ex )
		{
			throw new InvalidPluginException( "Abnormal plugin type", ex );
		}
	}
	
	@Override
	protected Class<?> findClass( String name ) throws ClassNotFoundException
	{
		return findClass( name, true );
	}
	
	Class<?> findClass( String name, boolean checkGlobal ) throws ClassNotFoundException
	{
		if ( name.startsWith( "com.chiorichan." ) && !name.startsWith( "com.chiorichan.plugin.builtin." ) )
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
	
	synchronized void initialize( Plugin javaPlugin )
	{
		Validate.notNull( javaPlugin, "Initializing plugin cannot be null" );
		Validate.isTrue( javaPlugin.getClass().getClassLoader() == this, "Cannot initialize plugin outside of this class loader" );
		if ( this.plugin != null || this.pluginInit != null )
		{
			throw new IllegalArgumentException( "Plugin already initialized!", pluginState );
		}
		
		pluginState = new IllegalStateException( "Initial initialization" );
		this.pluginInit = javaPlugin;
		
		javaPlugin.init( loader, description, dataFolder, file, this );
	}
}
