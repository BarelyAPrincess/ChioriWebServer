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
import com.chiorichan.lang.DeprecatedDetail;
import com.chiorichan.lang.ErrorReporting;
import com.chiorichan.lang.PluginDescriptionInvalidException;
import com.chiorichan.lang.PluginException;
import com.chiorichan.lang.PluginInvalidException;
import com.chiorichan.lang.PluginUnconfiguredException;
import com.chiorichan.lang.UnknownDependencyException;
import com.chiorichan.plugin.PluginDescriptionFile;
import com.chiorichan.plugin.PluginManager;
import com.chiorichan.util.FileFunc;
import com.google.common.collect.ImmutableList;

/**
 * Represents a Java plugin loader, allowing plugins in the form of .jar
 */
public final class JavaPluginLoader implements PluginLoader
{
	private final Map<String, Class<?>> classes = new HashMap<String, Class<?>>();
	private final Pattern[] fileFilters = new Pattern[] {Pattern.compile( "\\.jar$" )};
	private final Map<String, PluginClassLoader> loaders = new LinkedHashMap<String, PluginClassLoader>();
	
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
				methods.add( method );
			for ( Method method : listener.getClass().getDeclaredMethods() )
				methods.add( method );
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
			
			if ( ErrorReporting.E_DEPRECATED.isEnabledLevel() )
				for ( Class<?> clazz = eventClass; Event.class.isAssignableFrom( clazz ); clazz = clazz.getSuperclass() )
				{
					if ( clazz.isAnnotationPresent( DeprecatedDetail.class ) )
					{
						DeprecatedDetail deprecated = clazz.getAnnotation( DeprecatedDetail.class );
						PluginManager.getLogger().warning( String.format( "The plugin '%s' has registered a listener for %s on method '%s', but the event is Deprecated because '%s'; please notify the authors %s.", plugin.getDescription().getFullName(), clazz.getName(), method.toGenericString(), deprecated.reason(), Arrays.toString( plugin.getDescription().getAuthors().toArray() ) ) );
						break;
					}
					
					if ( clazz.isAnnotationPresent( Deprecated.class ) )
					{
						PluginManager.getLogger().warning( String.format( "The plugin '%s' has registered a listener for %s on method '%s', but the event is Deprecated! Please notify the authors %s.", plugin.getDescription().getFullName(), clazz.getName(), method.toGenericString(), Arrays.toString( plugin.getDescription().getAuthors().toArray() ) ) );
						break;
					}
				}
			
			EventExecutor executor = new EventExecutor()
			{
				@Override
				public void execute( Listener listener, Event event ) throws EventException
				{
					try
					{
						if ( !eventClass.isAssignableFrom( event.getClass() ) )
							return;
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
				eventSet.add( new TimedRegisteredListener( listener, executor, eh.priority(), plugin, eh.ignoreCancelled() ) );
			else
				eventSet.add( new RegisteredListener( listener, executor, eh.priority(), plugin, eh.ignoreCancelled() ) );
		}
		return ret;
	}
	
	@Override
	@SuppressWarnings( "resource" )
	public void disablePlugin( Plugin plugin )
	{
		Validate.isTrue( plugin instanceof Plugin, "Plugin is not associated with this PluginLoader" );
		
		if ( plugin.isEnabled() )
		{
			String message = String.format( "Disabling %s", plugin.getDescription().getFullName() );
			PluginManager.getLogger().info( message );
			
			EventBus.INSTANCE.callEvent( new PluginDisableEvent( plugin ) );
			
			Plugin jPlugin = plugin;
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
					removeClass( name );
			}
		}
	}
	
	@Override
	public void enablePlugin( final Plugin plugin )
	{
		Validate.isTrue( plugin instanceof Plugin, "Plugin is not associated with this PluginLoader" );
		
		if ( !plugin.isEnabled() )
		{
			PluginManager.getLogger().info( "Enabling " + plugin.getDescription().getFullName() );
			
			Plugin jPlugin = plugin;
			
			String pluginName = jPlugin.getDescription().getName();
			
			if ( !loaders.containsKey( pluginName ) )
				loaders.put( pluginName, ( PluginClassLoader ) jPlugin.getClassLoader() );
			
			try
			{
				jPlugin.setEnabled( true );
			}
			catch ( PluginUnconfiguredException ex )
			{
				// Manually thrown by plugins to convey when they are unconfigured
				PluginManager.getLogger().severe( String.format( "The plugin %s has reported that it's unconfigured, the plugin has been disabled until this is resolved.", plugin.getDescription().getFullName() ), ex );
			}
			catch ( PluginException ex )
			{
				// Manually thrown by plugins to convey an issue
				PluginManager.getLogger().severe( String.format( "The plugin %s has thrown the internal PluginException, the plugin has been disabled until this is resolved.", plugin.getDescription().getFullName() ), ex );
			}
			catch ( Throwable ex )
			{
				// Thrown for unexpected internal plugin problems
				PluginManager.getLogger().severe( String.format( "Error occurred while enabling %s (Is it up to date?)", plugin.getDescription().getFullName() ), ex );
			}
			
			// Perhaps abort here, rather than continue going, but as it stands,
			// an abort is not possible the way it's currently written
			EventBus.INSTANCE.callEvent( new PluginEnableEvent( plugin ) );
		}
	}
	
	Class<?> getClassByName( final String name )
	{
		Class<?> cachedClass = classes.get( name );
		
		if ( cachedClass != null )
			return cachedClass;
		else
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
					return cachedClass;
			}
		return null;
	}
	
	@SuppressWarnings( "unused" )
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
			dataFolder = new File( file.getParentFile(), filename + "_" );
		
		return dataFolder;
	}
	
	@Override
	public PluginDescriptionFile getPluginDescription( File file ) throws PluginDescriptionInvalidException
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
				throw new PluginDescriptionInvalidException( new FileNotFoundException( "Jar does not contain plugin.yaml" ) );
			
			stream = jar.getInputStream( entry );
			
			return new PluginDescriptionFile( stream );
			
		}
		catch ( IOException ex )
		{
			throw new PluginDescriptionInvalidException( ex );
		}
		catch ( YAMLException ex )
		{
			throw new PluginDescriptionInvalidException( ex );
		}
		finally
		{
			if ( jar != null )
				try
				{
					jar.close();
				}
				catch ( IOException e )
				{
				}
			if ( stream != null )
				try
				{
					stream.close();
				}
				catch ( IOException e )
				{
				}
		}
	}
	
	@Override
	public Pattern[] getPluginFileFilters()
	{
		return fileFilters.clone();
	}
	
	@Override
	public Plugin loadPlugin( File file ) throws PluginInvalidException
	{
		Validate.notNull( file, "File cannot be null" );
		
		if ( !file.exists() )
			throw new PluginInvalidException( new FileNotFoundException( file.getPath() + " does not exist" ) );
		
		PluginDescriptionFile description;
		try
		{
			description = getPluginDescription( file );
		}
		catch ( PluginDescriptionInvalidException ex )
		{
			throw new PluginInvalidException( ex );
		}
		
		File dataFolder = new File( file.getParentFile(), description.getName().replaceAll( "\\W", "" ) );
		// File dataFolderOption2 = getDataFolder( file );
		
		List<String> depend = description.getDepend();
		if ( depend == null )
			depend = ImmutableList.<String> of();
		
		for ( String pluginName : depend )
		{
			if ( loaders == null )
				throw new UnknownDependencyException( pluginName );
			PluginClassLoader current = loaders.get( pluginName );
			
			if ( current == null )
				throw new UnknownDependencyException( pluginName );
		}
		
		PluginClassLoader loader;
		try
		{
			loader = new PluginClassLoader( this, getClass().getClassLoader(), description, dataFolder, file );
		}
		catch ( PluginInvalidException ex )
		{
			throw ex;
		}
		catch ( Throwable ex )
		{
			throw new PluginInvalidException( ex );
		}
		
		loaders.put( description.getName(), loader );
		
		if ( description.hasNatives() )
			try
			{
				FileFunc.extractNatives( file, dataFolder );
			}
			catch ( IOException e )
			{
				PluginManager.getLogger().severe( "We had a problem trying to extract native libraries from plugin file '" + file + "':", e );
			}
		
		return loader.plugin;
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
}
