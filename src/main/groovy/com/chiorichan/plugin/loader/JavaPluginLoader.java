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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;
import org.yaml.snakeyaml.error.YAMLException;

import com.chiorichan.Loader;
import com.chiorichan.Warning;
import com.chiorichan.Warning.WarningState;
import com.chiorichan.configuration.serialization.ConfigurationSerializable;
import com.chiorichan.configuration.serialization.ConfigurationSerialization;
import com.chiorichan.event.Event;
import com.chiorichan.event.EventBus;
import com.chiorichan.event.EventException;
import com.chiorichan.event.EventExecutor;
import com.chiorichan.event.EventHandler;
import com.chiorichan.event.Listener;
import com.chiorichan.event.RegisteredListener;
import com.chiorichan.event.TimedRegisteredListener;
import com.chiorichan.event.server.PluginDisableEvent;
import com.chiorichan.event.server.PluginEnableEvent;
import com.chiorichan.lang.AuthorNagException;
import com.chiorichan.lang.InvalidDescriptionException;
import com.chiorichan.lang.InvalidPluginException;
import com.chiorichan.lang.PluginUnconfiguredException;
import com.chiorichan.lang.UnknownDependencyException;
import com.chiorichan.plugin.PluginDescriptionFile;
import com.chiorichan.plugin.PluginManager;
import com.google.common.collect.ImmutableList;

/**
 * Represents a Java plugin loader, allowing plugins in the form of .jar
 */
public final class JavaPluginLoader implements PluginLoader
{
	private final Pattern[] fileFilters = new Pattern[] {Pattern.compile( "\\.jar$" )};
	private final Map<String, Class<?>> classes = new HashMap<String, Class<?>>();
	private final Map<String, PluginClassLoader> loaders = new LinkedHashMap<String, PluginClassLoader>();
	
	public Plugin loadPlugin( File file ) throws InvalidPluginException
	{
		Validate.notNull( file, "File cannot be null" );
		
		if ( !file.exists() )
		{
			throw new InvalidPluginException( new FileNotFoundException( file.getPath() + " does not exist" ) );
		}
		
		PluginDescriptionFile description;
		try
		{
			description = getPluginDescription( file );
		}
		catch ( InvalidDescriptionException ex )
		{
			throw new InvalidPluginException( ex );
		}
		
		File dataFolder = new File( file.getParentFile(), description.getName() );
		File oldDataFolder = getDataFolder( file );
		
		// Found old data folder
		if ( dataFolder.equals( oldDataFolder ) )
		{
			// They are equal -- nothing needs to be done!
		}
		else if ( dataFolder.isDirectory() && oldDataFolder.isDirectory() )
		{
			PluginManager.getLogger().log( Level.INFO, String.format( "While loading %s (%s) found old-data folder: %s next to the new one: %s", description.getName(), file, oldDataFolder, dataFolder ) );
		}
		else if ( oldDataFolder.isDirectory() && !dataFolder.exists() )
		{
			if ( !oldDataFolder.renameTo( dataFolder ) )
			{
				throw new InvalidPluginException( "Unable to rename old data folder: '" + oldDataFolder + "' to: '" + dataFolder + "'" );
			}
			PluginManager.getLogger().log( Level.INFO, String.format( "While loading %s (%s) renamed data folder: '%s' to '%s'", description.getName(), file, oldDataFolder, dataFolder ) );
		}
		
		if ( dataFolder.exists() && !dataFolder.isDirectory() )
		{
			throw new InvalidPluginException( String.format( "Projected datafolder: '%s' for %s (%s) exists and is not a directory", dataFolder, description.getName(), file ) );
		}
		
		List<String> depend = description.getDepend();
		if ( depend == null )
		{
			depend = ImmutableList.<String> of();
		}
		
		for ( String pluginName : depend )
		{
			if ( loaders == null )
			{
				throw new UnknownDependencyException( pluginName );
			}
			PluginClassLoader current = loaders.get( pluginName );
			
			if ( current == null )
			{
				throw new UnknownDependencyException( pluginName );
			}
		}
		
		PluginClassLoader loader;
		try
		{
			loader = new PluginClassLoader( this, getClass().getClassLoader(), description, dataFolder, file );
		}
		catch ( InvalidPluginException ex )
		{
			throw ex;
		}
		catch ( Throwable ex )
		{
			throw new InvalidPluginException( ex );
		}
		
		loaders.put( description.getName(), loader );
		
		return loader.plugin;
	}
	
	private File getDataFolder( File file )
	{
		File dataFolder = null;
		
		String filename = file.getName();
		int index = file.getName().lastIndexOf( "." );
		
		if ( index != -1 )
		{
			String name = filename.substring( 0, index );
			
			dataFolder = new File( file.getParentFile(), name );
		}
		else
		{
			// This is if there is no extension, which should not happen
			// Using _ to prevent name collision
			
			dataFolder = new File( file.getParentFile(), filename + "_" );
		}
		
		return dataFolder;
	}
	
	public PluginDescriptionFile getPluginDescription( File file ) throws InvalidDescriptionException
	{
		Validate.notNull( file, "File cannot be null" );
		
		JarFile jar = null;
		InputStream stream = null;
		
		try
		{
			jar = new JarFile( file );
			JarEntry entry = jar.getJarEntry( "plugin.yaml" );
			
			if ( entry == null )
				entry = jar.getJarEntry( "plugin.yml" );
			
			if ( entry == null )
			{
				throw new InvalidDescriptionException( new FileNotFoundException( "Jar does not contain plugin.yaml" ) );
			}
			
			stream = jar.getInputStream( entry );
			
			return new PluginDescriptionFile( stream );
			
		}
		catch ( IOException ex )
		{
			throw new InvalidDescriptionException( ex );
		}
		catch ( YAMLException ex )
		{
			throw new InvalidDescriptionException( ex );
		}
		finally
		{
			if ( jar != null )
			{
				try
				{
					jar.close();
				}
				catch ( IOException e )
				{
				}
			}
			if ( stream != null )
			{
				try
				{
					stream.close();
				}
				catch ( IOException e )
				{
				}
			}
		}
	}
	
	public Pattern[] getPluginFileFilters()
	{
		return fileFilters.clone();
	}
	
	Class<?> getClassByName( final String name )
	{
		Class<?> cachedClass = classes.get( name );
		
		if ( cachedClass != null )
		{
			return cachedClass;
		}
		else
		{
			for ( String current : loaders.keySet() )
			{
				PluginClassLoader loader = loaders.get( current );
				
				try
				{
					cachedClass = loader.findClass( name, false );
				}
				catch ( ClassNotFoundException cnfe )
				{
				}
				if ( cachedClass != null )
				{
					return cachedClass;
				}
			}
		}
		return null;
	}
	
	void setClass( final String name, final Class<?> clazz )
	{
		if ( !classes.containsKey( name ) )
		{
			classes.put( name, clazz );
			
			if ( ConfigurationSerializable.class.isAssignableFrom( clazz ) )
			{
				Class<? extends ConfigurationSerializable> serializable = clazz.asSubclass( ConfigurationSerializable.class );
				ConfigurationSerialization.registerClass( serializable );
			}
		}
	}
	
	private void removeClass( String name )
	{
		Class<?> clazz = classes.remove( name );
		
		try
		{
			if ( ( clazz != null ) && ( ConfigurationSerializable.class.isAssignableFrom( clazz ) ) )
			{
				Class<? extends ConfigurationSerializable> serializable = clazz.asSubclass( ConfigurationSerializable.class );
				ConfigurationSerialization.unregisterClass( serializable );
			}
		}
		catch ( NullPointerException ex )
		{
			// Boggle!
			// (Native methods throwing NPEs is not fun when you can't stop it before-hoof)
		}
	}
	
	public Map<Class<? extends Event>, Set<RegisteredListener>> createRegisteredListeners( Listener listener, final Plugin plugin )
	{
		Validate.notNull( plugin, "Plugin can not be null" );
		Validate.notNull( listener, "Listener can not be null" );
		
		boolean useTimings = EventBus.INSTANCE.useTimings();
		Map<Class<? extends Event>, Set<RegisteredListener>> ret = new HashMap<Class<? extends Event>, Set<RegisteredListener>>();
		Set<Method> methods;
		try
		{
			Method[] publicMethods = listener.getClass().getMethods();
			methods = new HashSet<Method>( publicMethods.length, Float.MAX_VALUE );
			for ( Method method : publicMethods )
			{
				methods.add( method );
			}
			for ( Method method : listener.getClass().getDeclaredMethods() )
			{
				methods.add( method );
			}
		}
		catch ( NoClassDefFoundError e )
		{
			PluginManager.getLogger().severe( "Plugin " + plugin.getDescription().getFullName() + " has failed to register events for " + listener.getClass() + " because " + e.getMessage() + " does not exist." );
			return ret;
		}
		
		for ( final Method method : methods )
		{
			final EventHandler eh = method.getAnnotation( EventHandler.class );
			if ( eh == null )
				continue;
			final Class<?> checkClass;
			if ( method.getParameterTypes().length != 1 || !Event.class.isAssignableFrom( checkClass = method.getParameterTypes()[0] ) )
			{
				PluginManager.getLogger().severe( plugin.getDescription().getFullName() + " attempted to register an invalid EventHandler method signature \"" + method.toGenericString() + "\" in " + listener.getClass() );
				continue;
			}
			final Class<? extends Event> eventClass = checkClass.asSubclass( Event.class );
			method.setAccessible( true );
			Set<RegisteredListener> eventSet = ret.get( eventClass );
			if ( eventSet == null )
			{
				eventSet = new HashSet<RegisteredListener>();
				ret.put( eventClass, eventSet );
			}
			
			for ( Class<?> clazz = eventClass; Event.class.isAssignableFrom( clazz ); clazz = clazz.getSuperclass() )
			{
				// This loop checks for extending deprecated events
				if ( clazz.getAnnotation( Deprecated.class ) != null )
				{
					Warning warning = clazz.getAnnotation( Warning.class );
					WarningState warningState = Loader.getInstance().getWarningState();
					if ( !warningState.printFor( warning ) )
					{
						break;
					}
					PluginManager.getLogger().log( Level.WARNING, String.format( "\"%s\" has registered a listener for %s on method \"%s\", but the event is Deprecated." + " \"%s\"; please notify the authors %s.", plugin.getDescription().getFullName(), clazz.getName(), method.toGenericString(), ( warning != null && warning.reason().length() != 0 ) ? warning.reason() : "Server performance will be affected", Arrays.toString( plugin.getDescription().getAuthors().toArray() ) ), warningState == WarningState.ON ? new AuthorNagException( null ) : null );
					break;
				}
			}
			
			EventExecutor executor = new EventExecutor()
			{
				public void execute( Listener listener, Event event ) throws EventException
				{
					try
					{
						if ( !eventClass.isAssignableFrom( event.getClass() ) )
						{
							return;
						}
						method.invoke( listener, event );
					}
					catch ( InvocationTargetException ex )
					{
						throw new EventException( ex.getCause() );
					}
					catch ( Throwable t )
					{
						throw new EventException( t );
					}
				}
			};
			if ( useTimings )
			{
				eventSet.add( new TimedRegisteredListener( listener, executor, eh.priority(), plugin, eh.ignoreCancelled() ) );
			}
			else
			{
				eventSet.add( new RegisteredListener( listener, executor, eh.priority(), plugin, eh.ignoreCancelled() ) );
			}
		}
		return ret;
	}
	
	public void enablePlugin( final Plugin plugin )
	{
		Validate.isTrue( plugin instanceof Plugin, "Plugin is not associated with this PluginLoader" );
		
		if ( !plugin.isEnabled() )
		{
			PluginManager.getLogger().info( "Enabling " + plugin.getDescription().getFullName() );
			
			Plugin jPlugin = ( Plugin ) plugin;
			
			String pluginName = jPlugin.getDescription().getName();
			
			if ( !loaders.containsKey( pluginName ) )
			{
				loaders.put( pluginName, ( PluginClassLoader ) jPlugin.getClassLoader() );
			}
			
			try
			{
				jPlugin.setEnabled( true );
			}
			catch ( PluginUnconfiguredException ex )
			{
				PluginManager.getLogger().severe( "The plugin " + plugin.getDescription().getFullName() + " has reported that it's unconfigured. The plugin wil be unavailable until this is resolved", ex );
			}
			catch ( Throwable ex )
			{
				PluginManager.getLogger().severe( "Error occurred while enabling " + plugin.getDescription().getFullName() + " (Is it up to date?)", ex );
			}
			
			// Perhaps abort here, rather than continue going, but as it stands,
			// an abort is not possible the way it's currently written
			EventBus.INSTANCE.callEvent( new PluginEnableEvent( plugin ) );
		}
	}
	
	@SuppressWarnings( "resource" )
	public void disablePlugin( Plugin plugin )
	{
		Validate.isTrue( plugin instanceof Plugin, "Plugin is not associated with this PluginLoader" );
		
		if ( plugin.isEnabled() )
		{
			String message = String.format( "Disabling %s", plugin.getDescription().getFullName() );
			PluginManager.getLogger().info( message );
			
			EventBus.INSTANCE.callEvent( new PluginDisableEvent( plugin ) );
			
			Plugin jPlugin = ( Plugin ) plugin;
			ClassLoader cloader = jPlugin.getClassLoader();
			
			try
			{
				jPlugin.setEnabled( false );
			}
			catch ( Throwable ex )
			{
				PluginManager.getLogger().log( Level.SEVERE, "Error occurred while disabling " + plugin.getDescription().getFullName() + " (Is it up to date?)", ex );
			}
			
			loaders.remove( jPlugin.getDescription().getName() );
			
			if ( cloader instanceof PluginClassLoader )
			{
				PluginClassLoader loader = ( PluginClassLoader ) cloader;
				Set<String> names = loader.getClasses();
				
				for ( String name : names )
				{
					removeClass( name );
				}
			}
		}
	}
}
