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
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;

import com.chiorichan.Loader;
import com.chiorichan.account.bases.Sentient;
import com.chiorichan.bus.events.HandlerList;
import com.chiorichan.configuration.ConfigurationSection;
import com.chiorichan.permissions.Permissible;
import com.chiorichan.permissions.Permission;
import com.chiorichan.permissions.helpers.DefaultPermissions;
import com.chiorichan.plugin.anon.AnonymousLoader;
import com.chiorichan.plugin.groovy.GroovyPluginLoader;
import com.chiorichan.plugin.java.JavaPluginLoader;
import com.chiorichan.util.FileUtil;
import com.google.common.collect.ImmutableList;

/**
 * Handles all plugin management from the Server
 */
public final class PluginManager
{
	private final Map<Pattern, PluginLoader> fileAssociations = new HashMap<Pattern, PluginLoader>();
	private final List<Plugin> plugins = new ArrayList<Plugin>();
	private final Map<String, Plugin> lookupNames = new HashMap<String, Plugin>();
	private static File updateDirectory = null;
	private Set<String> loadedPlugins = new HashSet<String>();
	private PluginLoadOrder currentState = PluginLoadOrder.INITIALIZATION;
	private AnonymousLoader anonLoader;
	
	private static PluginManager instance;
	
	public PluginManager()
	{
		instance = this;
	}
	
	public void init()
	{
		
	}
	
	public static PluginManager getInstance()
	{
		return instance;
	}
	
	public void shutdown()
	{
		clearPlugins();
	}
	
	public void enablePlugins( PluginLoadOrder type )
	{
		currentState = type;
		
		Plugin[] plugins = getPlugins();
		
		for ( Plugin plugin : plugins )
		{
			if ( ( !plugin.isEnabled() ) && ( plugin.getDescription().getLoad() == type ) )
				loadPlugin( plugin );
		}
		
		if ( type == PluginLoadOrder.POSTSERVER )
		{
			Loader.getPermissionsManager().loadCustomPermissions();
			DefaultPermissions.registerCorePermissions();
		}
	}
	
	public void loadPlugins()
	{
		registerInterface( JavaPluginLoader.class );
		registerInterface( GroovyPluginLoader.class );
		
		anonLoader = new AnonymousLoader();
		
		File pluginFolder = (File) Loader.getOptions().valueOf( "plugins" );
		
		if ( pluginFolder.exists() )
		{
			Plugin[] plugins = loadPlugins( pluginFolder );
			for ( Plugin plugin : plugins )
			{
				try
				{
					String message = String.format( "Loading %s", plugin.getDescription().getFullName() );
					Loader.getLogger().info( message );
					plugin.onLoad();
				}
				catch ( Throwable ex )
				{
					Loader.getLogger().log( Level.SEVERE, ex.getMessage() + " initializing " + plugin.getDescription().getFullName() + " (Is it up to date?)", ex );
				}
			}
		}
		else
			pluginFolder.mkdir();
	}
	
	public PluginLoadOrder getCurrentLoadState()
	{
		return currentState;
	}
	
	/**
	 * Registers the specified plugin loader
	 * 
	 * @param loader
	 *             Class name of the PluginLoader to register
	 * @throws IllegalArgumentException
	 *              Thrown when the given Class is not a valid PluginLoader
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
	 *             Directory to check for plugins
	 * @return A list of all plugins loaded
	 */
	public Plugin[] loadPlugins( File directory )
	{
		Validate.notNull( directory, "Directory cannot be null" );
		Validate.isTrue( directory.isDirectory(), "Directory must be a directory" );
		
		List<Plugin> result = new ArrayList<Plugin>();
		Set<Pattern> filters = fileAssociations.keySet();
		
		if ( !( Loader.getInstance().getUpdateFolder().equals( "" ) ) )
		{
			updateDirectory = new File( directory, Loader.getInstance().getUpdateFolder() );
		}
		
		Map<String, File> plugins = new HashMap<String, File>();
		Map<String, Collection<String>> dependencies = new HashMap<String, Collection<String>>();
		Map<String, Collection<String>> softDependencies = new HashMap<String, Collection<String>>();
		
		// This is where it figures out all possible plugins
		for ( File file : directory.listFiles() )
		{
			PluginLoader loader = null;
			for ( Pattern filter : filters )
			{
				Matcher match = filter.matcher( file.getName() );
				if ( match.find() )
				{
					loader = fileAssociations.get( filter );
				}
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
				Loader.getLogger().log( Level.SEVERE, "Could not load '" + file.getPath() + "' in folder '" + directory.getPath() + "'", ex );
				continue;
			}
			
			plugins.put( description.getName(), file );
			
			// TODO: Make it so packets can be registered with the TCP Network from the plugin.yml file
			
			Collection<String> softDependencySet = description.getSoftDepend();
			if ( softDependencySet != null )
			{
				if ( softDependencies.containsKey( description.getName() ) )
				{
					// Duplicates do not matter, they will be removed together if applicable
					softDependencies.get( description.getName() ).addAll( softDependencySet );
				}
				else
				{
					softDependencies.put( description.getName(), new LinkedList<String>( softDependencySet ) );
				}
			}
			
			Collection<String> dependencySet = description.getDepend();
			if ( dependencySet != null )
			{
				dependencies.put( description.getName(), new LinkedList<String>( dependencySet ) );
			}
			
			Collection<String> loadBeforeSet = description.getLoadBefore();
			if ( loadBeforeSet != null )
			{
				for ( String loadBeforeTarget : loadBeforeSet )
				{
					if ( softDependencies.containsKey( loadBeforeTarget ) )
					{
						softDependencies.get( loadBeforeTarget ).add( description.getName() );
					}
					else
					{
						// softDependencies is never iterated, so 'ghost' plugins aren't an issue
						Collection<String> shortSoftDependency = new LinkedList<String>();
						shortSoftDependency.add( description.getName() );
						softDependencies.put( loadBeforeTarget, shortSoftDependency );
					}
				}
			}
		}
		
		while ( !plugins.isEmpty() )
		{
			boolean missingDependency = true;
			Iterator<String> pluginIterator = plugins.keySet().iterator();
			
			while ( pluginIterator.hasNext() )
			{
				String plugin = pluginIterator.next();
				
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
							softDependencies.remove( plugin );
							dependencies.remove( plugin );
							
							Loader.getLogger().log( Level.SEVERE, "Could not load '" + file.getPath() + "' in folder '" + directory.getPath() + "'", new UnknownDependencyException( dependency ) );
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
				if ( !( dependencies.containsKey( plugin ) || softDependencies.containsKey( plugin ) ) && plugins.containsKey( plugin ) )
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
						Loader.getLogger().log( Level.SEVERE, "Could not load '" + file.getPath() + "' in folder '" + directory.getPath() + "'", ex );
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
							Loader.getLogger().log( Level.SEVERE, "Could not load '" + file.getPath() + "' in folder '" + directory.getPath() + "'", ex );
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
						Loader.getLogger().log( Level.SEVERE, "Could not load '" + file.getPath() + "' in folder '" + directory.getPath() + "': circular dependency detected" );
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
	 *             File containing the plugin to load
	 * @return The Plugin loaded, or null if it was invalid
	 * @throws InvalidPluginException
	 *              Thrown when the specified file is not a valid plugin
	 * @throws UnknownDependencyException
	 *              If a required dependency could not be found
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
		if ( updateFile.isFile() && FileUtil.copy( updateFile, file ) )
		{
			updateFile.delete();
		}
	}
	
	/**
	 * Checks if the given plugin is loaded and returns it when applicable
	 * <p>
	 * Please note that the name of the plugin is case-sensitive
	 * 
	 * @param name
	 *             Name of the plugin to check
	 * @return Plugin if it exists, otherwise null
	 */
	public synchronized Plugin getPlugin( String name )
	{
		return lookupNames.get( name );
	}
	
	public synchronized Plugin[] getPlugins()
	{
		return plugins.toArray( new Plugin[0] );
	}
	
	/**
	 * Checks if the given plugin is enabled or not
	 * <p>
	 * Please note that the name of the plugin is case-sensitive.
	 * 
	 * @param name
	 *             Name of the plugin to check
	 * @return true if the plugin is enabled, otherwise false
	 */
	public boolean isPluginEnabled( String name )
	{
		Plugin plugin = getPlugin( name );
		
		return isPluginEnabled( plugin );
	}
	
	/**
	 * Checks if the given plugin is enabled or not
	 * 
	 * @param plugin
	 *             Plugin to check
	 * @return true if the plugin is enabled, otherwise false
	 */
	public boolean isPluginEnabled( Plugin plugin )
	{
		if ( ( plugin != null ) && ( plugins.contains( plugin ) ) )
		{
			return plugin.isEnabled();
		}
		else
		{
			return false;
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
				Loader.getLogger().log( Level.SEVERE, "Error occurred (in the plugin loader) while enabling " + plugin.getDescription().getFullName() + " (Is it up to date?)", ex );
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
				Loader.getLogger().log( Level.SEVERE, "Error occurred (in the plugin loader) while disabling " + plugin.getDescription().getFullName() + " (Is it up to date?)", ex );
			}
			
			try
			{
				Loader.getScheduler().cancelTasks( plugin );
			}
			catch ( Throwable ex )
			{
				Loader.getLogger().log( Level.SEVERE, "Error occurred (in the plugin loader) while cancelling tasks for " + plugin.getDescription().getFullName() + " (Is it up to date?)", ex );
			}
			
			try
			{
				Loader.getServicesManager().unregisterAll( plugin );
			}
			catch ( Throwable ex )
			{
				Loader.getLogger().log( Level.SEVERE, "Error occurred (in the plugin loader) while unregistering services for " + plugin.getDescription().getFullName() + " (Is it up to date?)", ex );
			}
			
			try
			{
				HandlerList.unregisterAll( plugin );
			}
			catch ( Throwable ex )
			{
				Loader.getLogger().log( Level.SEVERE, "Error occurred (in the plugin loader) while unregistering events for " + plugin.getDescription().getFullName() + " (Is it up to date?)", ex );
			}
			
			try
			{
				Loader.getMessenger().unregisterIncomingPluginChannel( plugin );
				Loader.getMessenger().unregisterOutgoingPluginChannel( plugin );
			}
			catch ( Throwable ex )
			{
				Loader.getLogger().log( Level.SEVERE, "Error occurred (in the plugin loader) while unregistering plugin channels for " + plugin.getDescription().getFullName() + " (Is it up to date?)", ex );
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
	
	public Plugin getPluginbyName( String pluginPath )
	{
		try
		{
			for ( Plugin plugin1 : getPlugins() )
			{
				if ( plugin1.getClass().getCanonicalName().equals( pluginPath ) || plugin1.getName().equalsIgnoreCase( pluginPath ) )
					return plugin1;
			}
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}
		
		return null;
	}
	
	public int broadcastMessage( String message )
	{
		return broadcast( message, Loader.BROADCAST_CHANNEL_USERS );
	}
	
	public int broadcast( String message, String permission )
	{
		int count = 0;
		Set<Permissible> permissibles = Loader.getPermissionsManager().getPermissionSubscriptions( permission );
		
		for ( Permissible permissible : permissibles )
		{
			if ( permissible instanceof Sentient && permissible.hasPermission( permission ) )
			{
				Sentient user = (Sentient) permissible;
				user.sendMessage( message );
				count++;
			}
		}
		
		return count;
	}
	
	public Map<String, String[]> getCommandAliases()
	{
		ConfigurationSection section = Loader.getConfig().getConfigurationSection( "aliases" );
		Map<String, String[]> result = new LinkedHashMap<String, String[]>();
		
		if ( section != null )
			for ( String key : section.getKeys( false ) )
			{
				List<String> commands;
				
				if ( section.isList( key ) )
					commands = section.getStringList( key );
				else
					commands = ImmutableList.of( section.getString( key ) );
				
				result.put( key, commands.toArray( new String[commands.size()] ) );
			}
		
		return result;
	}
	
	private void loadPlugin( Plugin plugin )
	{
		try
		{
			enablePlugin( plugin );
			
			List<Permission> perms = plugin.getDescription().getPermissions();
			
			for ( Permission perm : perms )
			{
				try
				{
					Loader.getPermissionsManager().addPermission( perm );
				}
				catch ( IllegalArgumentException ex )
				{
					Loader.getLogger().log( Level.WARNING, "Plugin " + plugin.getDescription().getFullName() + " tried to register permission '" + perm.getName() + "' but it's already registered", ex );
				}
			}
		}
		catch ( Throwable ex )
		{
			Loader.getLogger().log( Level.SEVERE, ex.getMessage() + " loading " + plugin.getDescription().getFullName() + " (Is it up to date?)", ex );
		}
	}
	
	public AnonymousLoader getAnonLoader()
	{
		return anonLoader;
	}
}
