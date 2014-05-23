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
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;

import com.chiorichan.Loader;
import com.chiorichan.command.Command;
import com.chiorichan.command.CommandMap;
import com.chiorichan.command.PluginCommandYamlParser;
import com.chiorichan.event.HandlerList;
import com.chiorichan.permissions.Permissible;
import com.chiorichan.permissions.Permission;
import com.chiorichan.permissions.PermissionDefault;
import com.chiorichan.util.FileUtil;
import com.google.common.collect.ImmutableSet;

/**
 * Handles all plugin management from the Server
 */
public final class PluginManager
{
	private final Loader server;
	private final Map<Pattern, PluginLoader> fileAssociations = new HashMap<Pattern, PluginLoader>();
	private final List<Plugin> plugins = new ArrayList<Plugin>();
	private final Map<String, Plugin> lookupNames = new HashMap<String, Plugin>();
	private static File updateDirectory = null;
	private final CommandMap commandMap;
	private final Map<String, Permission> permissions = new HashMap<String, Permission>();
	private final Map<Boolean, Set<Permission>> defaultPerms = new LinkedHashMap<Boolean, Set<Permission>>();
	private final Map<String, Map<Permissible, Boolean>> permSubs = new HashMap<String, Map<Permissible, Boolean>>();
	private final Map<Boolean, Map<Permissible, Boolean>> defSubs = new HashMap<Boolean, Map<Permissible, Boolean>>();
	private Set<String> loadedPlugins = new HashSet<String>();
	
	public PluginManager(Loader instance, CommandMap commandMap)
	{
		server = instance;
		this.commandMap = commandMap;
		
		defaultPerms.put( true, new HashSet<Permission>() );
		defaultPerms.put( false, new HashSet<Permission>() );
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
					instance = constructor.newInstance( server );
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
		
		if ( !( server.getUpdateFolder().equals( "" ) ) )
		{
			updateDirectory = new File( directory, server.getUpdateFolder() );
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
			List<Command> pluginCommands = PluginCommandYamlParser.parse( plugin );
			
			if ( !pluginCommands.isEmpty() )
			{
				commandMap.registerAll( plugin.getDescription().getName(), pluginCommands );
			}
			
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
				server.getServicesManager().unregisterAll( plugin );
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
				server.getMessenger().unregisterIncomingPluginChannel( plugin );
				server.getMessenger().unregisterOutgoingPluginChannel( plugin );
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
			permissions.clear();
			defaultPerms.get( true ).clear();
			defaultPerms.get( false ).clear();
		}
	}
	
	public Permission getPermission( String name )
	{
		return permissions.get( name.toLowerCase() );
	}
	
	public void addPermission( Permission perm )
	{
		String name = perm.getName().toLowerCase();
		
		if ( permissions.containsKey( name ) )
		{
			throw new IllegalArgumentException( "The permission " + name + " is already defined!" );
		}
		
		permissions.put( name, perm );
		calculatePermissionDefault( perm );
	}
	
	public Set<Permission> getDefaultPermissions( boolean op )
	{
		return ImmutableSet.copyOf( defaultPerms.get( op ) );
	}
	
	public void removePermission( Permission perm )
	{
		removePermission( perm.getName() );
	}
	
	public void removePermission( String name )
	{
		permissions.remove( name.toLowerCase() );
	}
	
	public void recalculatePermissionDefaults( Permission perm )
	{
		if ( permissions.containsValue( perm ) )
		{
			defaultPerms.get( true ).remove( perm );
			defaultPerms.get( false ).remove( perm );
			
			calculatePermissionDefault( perm );
		}
	}
	
	private void calculatePermissionDefault( Permission perm )
	{
		if ( ( perm.getDefault() == PermissionDefault.OP ) || ( perm.getDefault() == PermissionDefault.TRUE ) )
		{
			defaultPerms.get( true ).add( perm );
			dirtyPermissibles( true );
		}
		if ( ( perm.getDefault() == PermissionDefault.NOT_OP ) || ( perm.getDefault() == PermissionDefault.TRUE ) )
		{
			defaultPerms.get( false ).add( perm );
			dirtyPermissibles( false );
		}
	}
	
	private void dirtyPermissibles( boolean op )
	{
		Set<Permissible> permissibles = getDefaultPermSubscriptions( op );
		
		for ( Permissible p : permissibles )
		{
			p.recalculatePermissions();
		}
	}
	
	public void subscribeToPermission( String permission, Permissible permissible )
	{
		String name = permission.toLowerCase();
		Map<Permissible, Boolean> map = permSubs.get( name );
		
		if ( map == null )
		{
			map = new WeakHashMap<Permissible, Boolean>();
			permSubs.put( name, map );
		}
		
		map.put( permissible, true );
	}
	
	public void unsubscribeFromPermission( String permission, Permissible permissible )
	{
		String name = permission.toLowerCase();
		Map<Permissible, Boolean> map = permSubs.get( name );
		
		if ( map != null )
		{
			map.remove( permissible );
			
			if ( map.isEmpty() )
			{
				permSubs.remove( name );
			}
		}
	}
	
	public Set<Permissible> getPermissionSubscriptions( String permission )
	{
		String name = permission.toLowerCase();
		Map<Permissible, Boolean> map = permSubs.get( name );
		
		if ( map == null )
		{
			return ImmutableSet.of();
		}
		else
		{
			return ImmutableSet.copyOf( map.keySet() );
		}
	}
	
	public void subscribeToDefaultPerms( boolean op, Permissible permissible )
	{
		Map<Permissible, Boolean> map = defSubs.get( op );
		
		if ( map == null )
		{
			map = new WeakHashMap<Permissible, Boolean>();
			defSubs.put( op, map );
		}
		
		map.put( permissible, true );
	}
	
	public void unsubscribeFromDefaultPerms( boolean op, Permissible permissible )
	{
		Map<Permissible, Boolean> map = defSubs.get( op );
		
		if ( map != null )
		{
			map.remove( permissible );
			
			if ( map.isEmpty() )
			{
				defSubs.remove( op );
			}
		}
	}
	
	public Set<Permissible> getDefaultPermSubscriptions( boolean op )
	{
		Map<Permissible, Boolean> map = defSubs.get( op );
		
		if ( map == null )
		{
			return ImmutableSet.of();
		}
		else
		{
			return ImmutableSet.copyOf( map.keySet() );
		}
	}
	
	public Set<Permission> getPermissions()
	{
		return new HashSet<Permission>( permissions.values() );
	}
	
	public Plugin getPluginbyName( String pluginPath )
	{
		try
		{
			for ( Plugin plugin1 : Loader.getPluginManager().getPlugins() )
			{
				if ( plugin1.getClass().toString().substring( 6 ).equals( pluginPath ) || plugin1.getClass().getName().equals( pluginPath ) )
					return plugin1;
			}
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}
		
		return null;
	}
}
