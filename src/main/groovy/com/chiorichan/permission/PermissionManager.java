/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.permission;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;

import com.chiorichan.ConsoleColor;
import com.chiorichan.ConsoleLogger;
import com.chiorichan.Loader;
import com.chiorichan.ServerManager;
import com.chiorichan.account.AccountInstance;
import com.chiorichan.account.event.AccountPreLoginEvent;
import com.chiorichan.account.lang.AccountResult;
import com.chiorichan.configuration.file.YamlConfiguration;
import com.chiorichan.event.EventBus;
import com.chiorichan.event.EventCreator;
import com.chiorichan.event.EventHandler;
import com.chiorichan.event.EventPriority;
import com.chiorichan.event.Listener;
import com.chiorichan.permission.backend.file.FileBackend;
import com.chiorichan.permission.backend.memory.MemoryBackend;
import com.chiorichan.permission.backend.sql.SQLBackend;
import com.chiorichan.permission.event.PermissibleEntityEvent;
import com.chiorichan.permission.event.PermissibleEvent;
import com.chiorichan.permission.event.PermissibleSystemEvent;
import com.chiorichan.permission.lang.PermissionBackendException;
import com.chiorichan.plugin.PluginDescriptionFile;
import com.chiorichan.tasks.TaskCreator;
import com.chiorichan.tasks.TaskManager;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class PermissionManager implements ServerManager, TaskCreator, EventCreator, Listener
{
	/**
	 * Holds the OFFICAL instance of Permission Manager.
	 */
	public static final PermissionManager INSTANCE = new PermissionManager();
	
	/**
	 * Has this manager already been initialized?
	 */
	private static boolean isInitialized = false;
	
	Map<String, PermissibleGroup> defaultGroups = new HashMap<String, PermissibleGroup>();
	Map<String, PermissibleGroup> groups = new HashMap<String, PermissibleGroup>();
	Map<String, PermissibleEntity> entities = Maps.newHashMap();
	Set<Permission> roots = Sets.newConcurrentHashSet();
	PermissionBackend backend = null;
	YamlConfiguration config;
	boolean hasWhitelist = false;
	
	static boolean debugMode = false;
	static boolean allowOps = true;
	
	private PermissionManager()
	{
		
	}
	
	public static void init() throws PermissionBackendException
	{
		if ( isInitialized )
			throw new IllegalStateException( "The Permission Manager has already been initialized." );
		
		assert INSTANCE != null;
		
		INSTANCE.init0();
		
		isInitialized = true;
	}
	
	public void init0() throws PermissionBackendException
	{
		config = Loader.getConfig();
		debugMode = config.getBoolean( "permissions.debug", debugMode );
		allowOps = config.getBoolean( "permissions.allowOps", allowOps );
		
		hasWhitelist = config.getBoolean( "settings.whitelist" );
		
		initBackend();
	}
	
	private void initBackend() throws PermissionBackendException
	{
		PermissionBackend.registerBackendAlias( "sql", SQLBackend.class );
		PermissionBackend.registerBackendAlias( "file", FileBackend.class );
		PermissionBackend.registerBackendAlias( "memory", MemoryBackend.class );
		
		String backendName = config.getString( "permissions.backend" );
		
		if ( backendName == null || backendName.isEmpty() )
		{
			backendName = PermissionBackend.defaultBackend; // Default backend
			config.set( "permissions.backend", backendName );
		}
		
		setBackend( backendName );
	}
	
	/**
	 * Set backend to specified backend.
	 * This would also cause backend resetting.
	 * 
	 * @param backendName
	 *            name of backend to set to
	 */
	public void setBackend( String backendName ) throws PermissionBackendException
	{
		synchronized ( this )
		{
			clearCache();
			backend = PermissionBackend.getBackend( backendName );
			backend.initialize();
			
			loadData();
		}
		
		callEvent( PermissibleSystemEvent.Action.BACKEND_CHANGED );
	}
	
	public void setWhitelist( boolean value )
	{
		hasWhitelist = value;
		Loader.getConfig().set( "settings.whitelist", value );
	}
	
	public boolean hasWhitelist()
	{
		return hasWhitelist;
	}
	
	public void reload()
	{
		hasWhitelist = Loader.getConfig().getBoolean( "settings.whitelist" );
	}
	
	
	/**
	 * Return entity's object
	 * 
	 * @param entityname
	 *            get PermissibleEntity with given name
	 * @return PermissibleEntity instance
	 */
	public PermissibleEntity getEntity( Permissible permissible )
	{
		if ( permissible == null )
			throw new IllegalArgumentException( "Null entity passed! Name must not be empty" );
		
		if ( permissible.getEntityId() == null )
			return null;
		
		if ( permissible.entity == null )
		{
			if ( entities.containsKey( permissible.getEntityId() ) )
				permissible.entity = entities.get( permissible.getEntityId() );
			else
			{
				PermissibleEntity entity = backend.getEntity( permissible.getEntityId() );
				entities.put( permissible.getEntityId(), entity );
				permissible.entity = entity;
			}
		}
		
		return permissible.entity;
	}
	
	public PermissibleEntity getEntity( String permissible )
	{
		if ( permissible == null )
			throw new IllegalArgumentException( "Null entity passed! Name must not be empty" );
		
		if ( entities.containsKey( permissible ) )
			return entities.get( permissible );
		else
		{
			PermissibleEntity entity = backend.getEntity( permissible );
			entities.put( permissible, entity );
			return entity;
		}
	}
	
	/**
	 * Return all registered entity objects
	 * 
	 * @return PermissibleEntity array
	 */
	public PermissibleEntity[] getEntities()
	{
		return entities.values().toArray( new PermissibleEntity[0] );
	}
	
	/**
	 * Reset in-memory object of specified entity
	 * 
	 * @param entityName
	 *            entity's name
	 */
	public void resetEntity( Permissible entity )
	{
		entities.remove( entity.getEntityId() );
	}
	
	/**
	 * Forcefully saves groups and entities to the backend data source.
	 */
	public void saveData()
	{
		
	}
	
	/**
	 * Loads all groups and entities from the backend data source.
	 */
	public void loadData()
	{
		if ( isDebug() )
			getLogger().warning( ConsoleColor.YELLOW + "Permission debug is enabled!" );
		
		entities.clear();
		groups.clear();
		
		if ( isDebug() )
			getLogger().info( ConsoleColor.YELLOW + "Loading permissions from backend!" );
		
		/*
		 * This method loads all permissions and groups from the backend data store.
		 */
		backend.loadPermissionTree();
		
		/*
		 * Calling all the default permissions from here initializes them.
		 * They are created them if they were not loaded by the backend already.
		 * 
		 * Sidenote: Might be able to skip this but that might cause problems
		 * if the first call is not to getPermissionNode() which is the method
		 * that creates it if non-existent.
		 */
		PermissionDefault.DEFAULT.getNode();
		PermissionDefault.EVERYBODY.getNode();
		PermissionDefault.ADMIN.getNode();
		PermissionDefault.OP.getNode();
		PermissionDefault.BANNED.getNode();
		PermissionDefault.WHITELISTED.getNode();
		
		if ( isDebug() )
		{
			getLogger().info( ConsoleColor.YELLOW + "Dumping Loaded Permissions:" );
			for ( Permission root : Permission.getRootNodes( false ) )
				root.debugPermissionStack( 0 );
		}
		
		if ( config.getBoolean( "permissions.preloadGroups", true ) )
		{
			if ( isDebug() )
				getLogger().info( ConsoleColor.YELLOW + "Preloading groups from backend!" );
			for ( PermissibleGroup group : backend.getGroups() )
				groups.put( group.getId(), group );
		}
		
		if ( config.getBoolean( "permissions.preloadEntities", true ) )
		{
			if ( isDebug() )
				getLogger().info( ConsoleColor.YELLOW + "Preloading entities from backend!" );
			for ( PermissibleEntity entity : backend.getEntities() )
				entities.put( entity.getId(), entity );
		}
	}
	
	/**
	 * Return object for specified group
	 * 
	 * @param groupname
	 *            group's name
	 * @return PermissibleGroup object
	 */
	public PermissibleGroup getGroup( String groupname )
	{
		if ( groupname == null || groupname.isEmpty() )
		{
			return null;
		}
		
		PermissibleGroup group = groups.get( groupname.toLowerCase() );
		
		if ( group == null )
		{
			group = this.backend.getGroup( groupname );
			if ( group != null )
			{
				this.groups.put( groupname.toLowerCase(), group );
			}
			else
			{
				throw new IllegalStateException( "Group " + groupname + " is null" );
			}
		}
		
		return group;
	}
	
	/**
	 * Return all groups
	 * 
	 * @return PermissibleGroup array
	 */
	public PermissibleGroup[] getGroups()
	{
		return backend.getGroups();
	}
	
	/**
	 * Finds entities assigned provided permission.
	 * 
	 * @param perm
	 *            The permission to check for.
	 * @return a list of permissibles that have that permission assigned to them.
	 * @see PermissionManager#getEntitiesWithPermission(Permission)
	 */
	public List<PermissibleEntity> getEntitiesWithPermission( String perm )
	{
		return getEntitiesWithPermission( Permission.getNode( perm ) );
	}
	
	/**
	 * Finds entities assigned provided permission.
	 * WARNING: Will not return a complete list if permissions.preloadEntities config is false.
	 * 
	 * @param perm
	 *            The permission to check for.
	 * @return a list of permissibles that have that permission assigned to them.
	 */
	public List<PermissibleEntity> getEntitiesWithPermission( Permission perm )
	{
		List<PermissibleEntity> result = Lists.newArrayList();
		
		for ( PermissibleEntity entity : entities.values() )
		{
			if ( entity.checkPermission( perm ).isAssigned() )
				result.add( entity );
		}
		
		return result;
	}
	
	/**
	 * Return default group object
	 * 
	 * @return default group object. null if not specified
	 */
	public PermissibleGroup getDefaultGroup( String refName )
	{
		String refIndex = refName != null ? refName : "";
		
		if ( !this.defaultGroups.containsKey( refIndex ) )
		{
			this.defaultGroups.put( refIndex, this.getDefaultGroup( refName, this.getDefaultGroup( null, null ) ) );
		}
		
		return this.defaultGroups.get( refIndex );
	}
	
	public PermissibleGroup getDefaultGroup()
	{
		return this.getDefaultGroup( null );
	}
	
	private PermissibleGroup getDefaultGroup( String refName, PermissibleGroup fallback )
	{
		PermissibleGroup defaultGroup = this.backend.getDefaultGroup( refName );
		
		if ( defaultGroup == null && refName == null )
		{
			getLogger().warning( "No default group defined. Use \"perm set default group <group> [ref]\" to define default group." );
			return fallback;
		}
		
		if ( defaultGroup != null )
		{
			return defaultGroup;
		}
		
		return fallback;
	}
	
	/**
	 * Set default group to specified group
	 * 
	 * @param group
	 *            PermissibleGroup group object
	 */
	public void setDefaultGroup( PermissibleGroup group, String refName )
	{
		if ( group == null || group.equals( this.defaultGroups ) )
		{
			return;
		}
		
		backend.setDefaultGroup( group.getId(), refName );
		
		this.defaultGroups.clear();
		
		callEvent( PermissibleSystemEvent.Action.DEFAULTGROUP_CHANGED );
		callEvent( new PermissibleEntityEvent( group, PermissibleEntityEvent.Action.DEFAULTGROUP_CHANGED ) );
	}
	
	public void setDefaultGroup( PermissibleGroup group )
	{
		this.setDefaultGroup( group, null );
	}
	
	/**
	 * Reset in-memory object for groupName
	 * 
	 * @param groupName
	 *            group's name
	 */
	public void resetGroup( String groupName )
	{
		this.groups.remove( groupName );
	}
	
	/**
	 * Set debug mode
	 * 
	 * @param debug
	 *            true enables debug mode, false disables
	 */
	public static void setDebug( boolean debug )
	{
		debugMode = debug;
		callEvent( PermissibleSystemEvent.Action.DEBUGMODE_TOGGLE );
	}
	
	/**
	 * Return current state of debug mode
	 * 
	 * @return true debug is enabled, false if disabled
	 */
	public boolean isDebug()
	{
		return debugMode;
	}
	
	/**
	 * Return current backend
	 * 
	 * @return current backend object
	 */
	public PermissionBackend getBackend()
	{
		return this.backend;
	}
	
	/**
	 * Register new timer task
	 * 
	 * @param task
	 *            TimerTask object
	 * @param delay
	 *            delay in seconds
	 */
	protected void registerTask( TimerTask task, int delay )
	{
		TaskManager.INSTANCE.scheduleAsyncDelayedTask( this, task, delay * 50 );
	}
	
	/**
	 * Reset all in-memory groups and entities, clean up runtime stuff, reloads backend
	 */
	public void reset() throws PermissionBackendException
	{
		this.clearCache();
		
		if ( this.backend != null )
		{
			this.backend.reload();
		}
		
		callEvent( PermissibleSystemEvent.Action.RELOADED );
	}
	
	public void end()
	{
		try
		{
			reset();
		}
		catch ( PermissionBackendException ignore )
		{
			// Ignore because we're shutting down so who cares
		}
	}
	
	protected void clearCache()
	{
		this.entities.clear();
		this.groups.clear();
		this.defaultGroups.clear();
	}
	
	protected static void callEvent( PermissibleEvent event )
	{
		EventBus.INSTANCE.callEvent( event );
	}
	
	protected static void callEvent( PermissibleSystemEvent.Action action )
	{
		callEvent( new PermissibleSystemEvent( action ) );
	}
	
	/**
	 * Check if specified entity has specified permission
	 * 
	 * @param entity
	 *            entity object
	 * @param perm
	 *            permission string to check against
	 * @return true on success false otherwise
	 */
	public PermissionResult checkPermission( Permissible entity, String perm )
	{
		return checkPermission( entity.getEntityId(), perm, "" );
	}
	
	/**
	 * Check if entity has specified permission in ref
	 * 
	 * @param entity
	 *            entity object
	 * @param perm
	 *            permission as string to check against
	 * @param ref
	 *            ref used for this perm
	 * @return true on success false otherwise
	 */
	public PermissionResult checkPermission( AccountInstance entity, String perm, String ref )
	{
		return this.checkPermission( entity.getAcctId(), perm, ref );
	}
	
	/**
	 * Check if entity with name has permission in ref
	 * 
	 * @param entityId
	 *            entity name
	 * @param permission
	 *            permission as string to check against
	 * @param ref
	 *            ref's name as string
	 * @return true on success false otherwise
	 */
	public PermissionResult checkPermission( String entityId, String permission, String ref )
	{
		PermissibleEntity entity = getEntity( entityId );
		
		if ( entity == null )
			throw new RuntimeException( "Entity returned null! This is a bug and needs to be reported to the developers." );
		
		return entity.checkPermission( permission, ref );
	}
	
	public static ConsoleLogger getLogger()
	{
		return Loader.getLogger( "PermMgr" );
	}
	
	@Override
	public boolean isEnabled()
	{
		return true;
	}
	
	@Override
	public String getName()
	{
		return "PermissionsManager";
	}
	
	@Override
	public PluginDescriptionFile getDescription()
	{
		return null;
	}
	
	// TODO Make more checks
	@EventHandler( priority = EventPriority.HIGHEST )
	public void onAccountLoginEvent( AccountPreLoginEvent event )
	{
		PermissibleEntity entity = getEntity( event.getAccount().getAcctId() );
		
		if ( hasWhitelist() && entity.isWhitelisted() )
		{
			event.fail( AccountResult.ACCOUNT_NOT_WHITELISTED );
			return;
		}
		
		if ( entity.isBanned() )
		{
			event.fail( AccountResult.ACCOUNT_BANNED );
			return;
		}
	}
}
