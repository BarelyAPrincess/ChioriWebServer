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
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;

import com.chiorichan.ConsoleLogger;
import com.chiorichan.Loader;
import com.chiorichan.RunLevel;
import com.chiorichan.ServerManager;
import com.chiorichan.event.BuiltinEventCreator;
import com.chiorichan.event.EventBus;
import com.chiorichan.event.EventHandler;
import com.chiorichan.event.EventPriority;
import com.chiorichan.event.HandlerList;
import com.chiorichan.event.Listener;
import com.chiorichan.event.server.ServerRunLevelEvent;
import com.chiorichan.lang.InvalidDescriptionException;
import com.chiorichan.lang.InvalidPluginException;
import com.chiorichan.lang.PluginNotFoundException;
import com.chiorichan.lang.UnknownDependencyException;
import com.chiorichan.libraries.MavenReference;
import com.chiorichan.libraries.Libraries;
import com.chiorichan.plugin.loader.JavaPluginLoader;
import com.chiorichan.plugin.loader.Plugin;
import com.chiorichan.plugin.loader.PluginLoader;
import com.chiorichan.util.FileFunc;
import com.google.common.collect.Maps;

public class PluginManager extends BuiltinEventCreator implements Listener, ServerManager
{
	public static final PluginManager INSTANCE = new PluginManager();
	private static boolean isInitialized = false;
	
	private final Map<Pattern, PluginLoader> fileAssociations = new HashMap<Pattern, PluginLoader>();
	private final List<Plugin> plugins = new ArrayList<Plugin>();
	private final Map<String, Plugin> lookupNames = new HashMap<String, Plugin>();
	private static File updateDirectory = null;
	private Set<String> loadedPlugins = new HashSet<String>();
	
	public static void init()
	{
		if ( isInitialized )
			throw new IllegalStateException( "The Plugin Manager has already been initialized." );
		
		assert INSTANCE != null;
		
		INSTANCE.init0();
		
		isInitialized = true;
		
	}
	
	/**
	 * Registers with the event bus to we can load plugins in their correct order.
	 */
	private void init0()
	{
		EventBus.INSTANCE.registerEvents( this, this );
	}
	
	private PluginManager()
	{
		
	}
	
	public void loadPlugins()
	{
		registerInterface( JavaPluginLoader.class );
		// registerInterface( GroovyPluginLoader.class );
		
		File pluginFolder = ( File ) Loader.getOptions().valueOf( "plugins" );
		
		if ( pluginFolder.exists() )
		{
			Plugin[] plugins = loadPlugins( pluginFolder );
			for ( Plugin plugin : plugins )
			{
				try
				{
					String message = String.format( "Loading %s", plugin.getDescription().getFullName() );
					PluginManager.getLogger().info( message );
					plugin.onLoad();
				}
				catch ( Throwable ex )
				{
					getLogger().log( Level.SEVERE, ex.getMessage() + " initializing " + plugin.getDescription().getFullName() + " (Is it up to date?)", ex );
				}
			}
		}
		else
			pluginFolder.mkdir();
	}
	
	/**
	 * Registers the specified plugin loader
	 * 
	 * @param loader
	 *            Class name of the PluginLoader to register
	 * @throws IllegalArgumentException
	 *             Thrown when the given Class is not a valid PluginLoader
	 */
	public void registerInterface( Class<? extends PluginLoader> loader ) throws IllegalArgumentException
	{
		PluginLoader instance;
		
		if ( PluginLoader.class.isAssignableFrom( loader ) )
		{
			Constructor<? extends PluginLoader> constructor;
			
			try
			{
				constructor = loader.getConstructor();
				instance = constructor.newInstance();
			}
			catch ( NoSuchMethodException ex )
			{
				try
				{
					constructor = loader.getConstructor( Loader.class );
					instance = constructor.newInstance( Loader.getInstance() );
				}
				catch ( NoSuchMethodException ex1 )
				{
					String className = loader.getName();
					
					throw new IllegalArgumentException( String.format( "Class %s does not have a public %s(Server) constructor", className, className ), ex1 );
				}
				catch ( Exception ex1 )
				{
					throw new IllegalArgumentException( String.format( "Unexpected exception %s while attempting to construct a new instance of %s", ex.getClass().getName(), loader.getName() ), ex1 );
				}
			}
			catch ( Exception ex )
			{
				throw new IllegalArgumentException( String.format( "Unexpected exception %s while attempting to construct a new instance of %s", ex.getClass().getName(), loader.getName() ), ex );
			}
		}
		else
		{
			throw new IllegalArgumentException( String.format( "Class %s does not implement interface PluginLoader", loader.getName() ) );
		}
		
		Pattern[] patterns = instance.getPluginFileFilters();
		
		synchronized ( this )
		{
			for ( Pattern pattern : patterns )
			{
				fileAssociations.put( pattern, instance );
			}
		}
	}
	
	/**
	 * Loads the plugins contained within the specified directory
	 * 
	 * @param directory
	 *            Directory to check for plugins
	 * @return A list of all plugins loaded
	 */
	public Plugin[] loadPlugins( File directory )
	{
		Validate.notNull( directory, "Directory cannot be null" );
		Validate.isTrue( directory.isDirectory(), "Directory must be a directory" );
		
		List<Plugin> result = new ArrayList<Plugin>();
		Set<Pattern> filters = fileAssociations.keySet();
		
		if ( ! ( Loader.getInstance().getUpdateFolder().equals( "" ) ) )
		{
			updateDirectory = new File( directory, Loader.getInstance().getUpdateFolder() );
		}
		
		Map<String, File> plugins = new HashMap<String, File>();
		Map<String, Collection<MavenReference>> libraries = Maps.newHashMap();
		Map<String, Collection<String>> dependencies = Maps.newHashMap();
		Map<String, Collection<String>> softDependencies = Maps.newHashMap();
		
		// This is where it figures out all possible plugins
		for ( File file : directory.listFiles() )
		{
			PluginLoader loader = null;
			for ( Pattern filter : filters )
			{
				Matcher match = filter.matcher( file.getName() );
				if ( match.find() )
					loader = fileAssociations.get( filter );
			}
			
			if ( loader == null )
				continue;
			
			PluginDescriptionFile description = null;
			try
			{
				description = loader.getPluginDescription( file );
			}
			catch ( InvalidDescriptionException ex )
			{
				getLogger().log( Level.SEVERE, "Could not load '" + file.getPath() + "' in folder '" + directory.getPath() + "'", ex );
				continue;
			}
			
			plugins.put( description.getName(), file );
			
			Collection<String> softDependencySet = description.getSoftDepend();
			if ( softDependencySet != null )
				if ( softDependencies.containsKey( description.getName() ) )
					// Duplicates do not matter, they will be removed together if applicable
					softDependencies.get( description.getName() ).addAll( softDependencySet );
				else
					softDependencies.put( description.getName(), new LinkedList<String>( softDependencySet ) );
			
			Collection<MavenReference> librariesSet = description.getLibraries();
			if ( librariesSet != null )
				libraries.put( description.getName(), new LinkedList<MavenReference>( librariesSet ) );
			
			Collection<String> dependencySet = description.getDepend();
			if ( dependencySet != null )
				dependencies.put( description.getName(), new LinkedList<String>( dependencySet ) );
			
			Collection<String> loadBeforeSet = description.getLoadBefore();
			if ( loadBeforeSet != null )
				for ( String loadBeforeTarget : loadBeforeSet )
					if ( softDependencies.containsKey( loadBeforeTarget ) )
						softDependencies.get( loadBeforeTarget ).add( description.getName() );
					else
					{
						// softDependencies is never iterated, so 'ghost' plugins aren't an issue
						Collection<String> shortSoftDependency = new LinkedList<String>();
						shortSoftDependency.add( description.getName() );
						softDependencies.put( loadBeforeTarget, shortSoftDependency );
					}
		}
		
		while ( !plugins.isEmpty() )
		{
			boolean missingDependency = true;
			Iterator<String> pluginIterator = plugins.keySet().iterator();
			
			while ( pluginIterator.hasNext() )
			{
				String plugin = pluginIterator.next();
				
				if ( libraries.containsKey( plugin ) )
				{
					Iterator<MavenReference> librariesIterator = libraries.get( plugin ).iterator();
					
					while ( librariesIterator.hasNext() )
					{
						MavenReference library = librariesIterator.next();
						
						if ( Libraries.isLoaded( library ) )
						{
							librariesIterator.remove();
						}
						else
						{
							if ( !Libraries.loadLibrary( library ) )
							{
								missingDependency = false;
								File file = plugins.get( plugin );
								pluginIterator.remove();
								libraries.remove( plugin );
								softDependencies.remove( plugin );
								dependencies.remove( plugin );
								
								getLogger().severe( "Could not load '" + file.getPath() + "' in folder '" + directory.getPath() + "' due to issue with library '" + library + "'." );
								break;
							}
						}
					}
				}
				
				if ( dependencies.containsKey( plugin ) )
				{
					Iterator<String> dependencyIterator = dependencies.get( plugin ).iterator();
					
					while ( dependencyIterator.hasNext() )
					{
						String dependency = dependencyIterator.next();
						
						// Dependency loaded
						if ( loadedPlugins.contains( dependency ) )
						{
							dependencyIterator.remove();
							
							// We have a dependency not found
						}
						else if ( !plugins.containsKey( dependency ) )
						{
							missingDependency = false;
							File file = plugins.get( plugin );
							pluginIterator.remove();
							libraries.remove( plugin );
							softDependencies.remove( plugin );
							dependencies.remove( plugin );
							
							getLogger().severe( "Could not load '" + file.getPath() + "' in folder '" + directory.getPath() + "'", new UnknownDependencyException( dependency ) );
							break;
						}
					}
					
					if ( dependencies.containsKey( plugin ) && dependencies.get( plugin ).isEmpty() )
					{
						dependencies.remove( plugin );
					}
				}
				if ( softDependencies.containsKey( plugin ) )
				{
					Iterator<String> softDependencyIterator = softDependencies.get( plugin ).iterator();
					
					while ( softDependencyIterator.hasNext() )
					{
						String softDependency = softDependencyIterator.next();
						
						// Soft depend is no longer around
						if ( !plugins.containsKey( softDependency ) )
						{
							softDependencyIterator.remove();
						}
					}
					
					if ( softDependencies.get( plugin ).isEmpty() )
					{
						softDependencies.remove( plugin );
					}
				}
				if ( ! ( dependencies.containsKey( plugin ) || softDependencies.containsKey( plugin ) ) && plugins.containsKey( plugin ) )
				{
					// We're clear to load, no more soft or hard dependencies left
					File file = plugins.get( plugin );
					pluginIterator.remove();
					missingDependency = false;
					
					try
					{
						result.add( loadPlugin( file ) );
						loadedPlugins.add( plugin );
						continue;
					}
					catch ( InvalidPluginException ex )
					{
						getLogger().severe( "Could not load '" + file.getPath() + "' in folder '" + directory.getPath() + "'", ex );
					}
				}
			}
			
			if ( missingDependency )
			{
				// We now iterate over plugins until something loads
				// This loop will ignore soft dependencies
				pluginIterator = plugins.keySet().iterator();
				
				while ( pluginIterator.hasNext() )
				{
					String plugin = pluginIterator.next();
					
					if ( !dependencies.containsKey( plugin ) )
					{
						softDependencies.remove( plugin );
						missingDependency = false;
						File file = plugins.get( plugin );
						pluginIterator.remove();
						
						try
						{
							result.add( loadPlugin( file ) );
							loadedPlugins.add( plugin );
							break;
						}
						catch ( InvalidPluginException ex )
						{
							getLogger().severe( "Could not load '" + file.getPath() + "' in folder '" + directory.getPath() + "'", ex );
						}
					}
				}
				// We have no plugins left without a depend
				if ( missingDependency )
				{
					softDependencies.clear();
					dependencies.clear();
					Iterator<File> failedPluginIterator = plugins.values().iterator();
					
					while ( failedPluginIterator.hasNext() )
					{
						File file = failedPluginIterator.next();
						failedPluginIterator.remove();
						getLogger().severe( "Could not load '" + file.getPath() + "' in folder '" + directory.getPath() + "': circular dependency detected" );
					}
				}
			}
		}
		
		return result.toArray( new Plugin[result.size()] );
	}
	
	/**
	 * Loads the plugin in the specified file
	 * <p>
	 * File must be valid according to the current enabled Plugin interfaces
	 * 
	 * @param file
	 *            File containing the plugin to load
	 * @return The Plugin loaded, or null if it was invalid
	 * @throws InvalidPluginException
	 *             Thrown when the specified file is not a valid plugin
	 * @throws UnknownDependencyException
	 *             If a required dependency could not be found
	 */
	public synchronized Plugin loadPlugin( File file ) throws InvalidPluginException, UnknownDependencyException
	{
		Validate.notNull( file, "File cannot be null" );
		
		checkUpdate( file );
		
		Set<Pattern> filters = fileAssociations.keySet();
		Plugin result = null;
		
		for ( Pattern filter : filters )
		{
			String name = file.getName();
			Matcher match = filter.matcher( name );
			
			if ( match.find() )
			{
				PluginLoader loader = fileAssociations.get( filter );
				
				result = loader.loadPlugin( file );
			}
		}
		
		if ( result != null )
		{
			plugins.add( result );
			lookupNames.put( result.getDescription().getName(), result );
		}
		
		return result;
	}
	
	private void checkUpdate( File file )
	{
		if ( updateDirectory == null || !updateDirectory.isDirectory() )
		{
			return;
		}
		
		File updateFile = new File( updateDirectory, file.getName() );
		if ( updateFile.isFile() && FileFunc.copy( updateFile, file ) )
		{
			updateFile.delete();
		}
	}
	
	public void enablePlugin( final Plugin plugin )
	{
		if ( !plugin.isEnabled() )
		{
			try
			{
				plugin.getPluginLoader().enablePlugin( plugin );
			}
			catch ( Throwable ex )
			{
				getLogger().log( Level.SEVERE, "Error occurred (in the plugin loader) while enabling " + plugin.getDescription().getFullName() + " (Is it up to date?)", ex );
			}
			
			HandlerList.bakeAll();
		}
	}
	
	public void disablePlugins()
	{
		Plugin[] plugins = getPlugins();
		for ( int i = plugins.length - 1; i >= 0; i-- )
		{
			disablePlugin( plugins[i] );
		}
	}
	
	public void disablePlugin( final Plugin plugin )
	{
		if ( plugin.isEnabled() )
		{
			try
			{
				plugin.getPluginLoader().disablePlugin( plugin );
			}
			catch ( Throwable ex )
			{
				getLogger().log( Level.SEVERE, "Error occurred (in the plugin loader) while disabling " + plugin.getDescription().getFullName() + " (Is it up to date?)", ex );
			}
			
			try
			{
				// Loader.getScheduler().cancelTasks( plugin );
			}
			catch ( Throwable ex )
			{
				getLogger().log( Level.SEVERE, "Error occurred (in the plugin loader) while cancelling tasks for " + plugin.getDescription().getFullName() + " (Is it up to date?)", ex );
			}
			
			try
			{
				// Loader.getServicesManager().unregisterAll( plugin );
			}
			catch ( Throwable ex )
			{
				getLogger().log( Level.SEVERE, "Error occurred (in the plugin loader) while unregistering services for " + plugin.getDescription().getFullName() + " (Is it up to date?)", ex );
			}
			
			try
			{
				HandlerList.unregisterAll( plugin );
			}
			catch ( Throwable ex )
			{
				getLogger().log( Level.SEVERE, "Error occurred (in the plugin loader) while unregistering events for " + plugin.getDescription().getFullName() + " (Is it up to date?)", ex );
			}
			
			try
			{
				// Loader.getMessenger().unregisterIncomingPluginChannel( plugin );
				// Loader.getMessenger().unregisterOutgoingPluginChannel( plugin );
			}
			catch ( Throwable ex )
			{
				getLogger().log( Level.SEVERE, "Error occurred (in the plugin loader) while unregistering plugin channels for " + plugin.getDescription().getFullName() + " (Is it up to date?)", ex );
			}
		}
	}
	
	public void clearPlugins()
	{
		synchronized ( this )
		{
			disablePlugins();
			plugins.clear();
			lookupNames.clear();
			HandlerList.unregisterAll();
			fileAssociations.clear();
		}
	}
	
	public void shutdown()
	{
		clearPlugins();
	}
	
	public synchronized Plugin[] getPlugins()
	{
		return plugins.toArray( new Plugin[0] );
	}
	
	public Plugin getPluginByClassname( String className ) throws PluginNotFoundException
	{
		try
		{
			for ( Plugin plugin1 : getPlugins() )
			{
				if ( plugin1.getClass().getName().startsWith( className ) )
					return plugin1;
			}
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}
		
		throw new PluginNotFoundException( "We could not find a plugin with the classname '" + className + "', maybe it's not loaded." );
	}
	
	public Plugin getPluginByClassnameWithoutException( String className )
	{
		try
		{
			return getPluginByClassname( className );
		}
		catch ( PluginNotFoundException e )
		{
			getLogger().warning( e.getMessage() );
			return null;
		}
	}
	
	public Plugin getPluginByName( String pluginName ) throws PluginNotFoundException
	{
		try
		{
			for ( Plugin plugin1 : getPlugins() )
			{
				if ( plugin1.getClass().getCanonicalName().equals( pluginName ) || plugin1.getName().equalsIgnoreCase( pluginName ) )
					return plugin1;
			}
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}
		
		throw new PluginNotFoundException( "We could not find a plugin by the name '" + pluginName + "', maybe it's not loaded." );
	}
	
	public Plugin getPluginByNameWithoutException( String pluginName )
	{
		try
		{
			return getPluginByName( pluginName );
		}
		catch ( PluginNotFoundException e )
		{
			getLogger().warning( e.getMessage() );
			return null;
		}
	}
	
	private void loadPlugin( Plugin plugin )
	{
		try
		{
			enablePlugin( plugin );
		}
		catch ( Throwable ex )
		{
			getLogger().log( Level.SEVERE, ex.getMessage() + " loading " + plugin.getDescription().getFullName() + " (Is it up to date?)", ex );
		}
	}
	
	public static ConsoleLogger getLogger()
	{
		return Loader.getLogger( "PluginMgr" );
	}
	
	@Override
	public String getName()
	{
		return "Plugins Manager";
	}
	
	/**
	 * Loads plugins in order as PluginManager receives the notices from the EventBus
	 */
	@EventHandler( priority = EventPriority.NORMAL )
	public void onServerRunLevelEvent( ServerRunLevelEvent event )
	{
		RunLevel level = event.getRunLevel();
		
		Plugin[] plugins = getPlugins();
		
		for ( Plugin plugin : plugins )
		{
			if ( ( !plugin.isEnabled() ) && ( plugin.getDescription().getLoad() == level ) )
				loadPlugin( plugin );
		}
	}
}
