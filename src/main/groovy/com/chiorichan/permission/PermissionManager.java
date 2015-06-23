/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.permission;

import java.util.Arrays;
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
import com.chiorichan.event.BuiltinEventCreator;
import com.chiorichan.event.EventBus;
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
import com.chiorichan.tasks.TaskCreator;
import com.chiorichan.tasks.TaskManager;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class PermissionManager extends BuiltinEventCreator implements ServerManager, TaskCreator, Listener
{
	static boolean allowOps = true;
	
	static boolean debugMode = false;
	
	/**
	 * Holds the OFFICAL instance of Permission Manager.
	 */
	public static final PermissionManager INSTANCE = new PermissionManager();
	
	/**
	 * Has this manager already been initialized?
	 */
	private static boolean isInitialized = false;
	private PermissionBackend backend = null;
	private YamlConfiguration config;
	private Map<String, PermissibleGroup> defaultGroups = new HashMap<String, PermissibleGroup>();
	private Map<String, PermissibleEntity> entities = Maps.newHashMap();
	private Map<String, PermissibleGroup> groups = new HashMap<String, PermissibleGroup>();
	private boolean hasWhitelist = false;
	
	private final Set<Permission> permissions = Sets.newConcurrentHashSet();
	
	private PermissionManager()
	{
		
	}
	
	protected static void callEvent( PermissibleEvent event )
	{
		EventBus.INSTANCE.callEvent( event );
	}
	
	protected static void callEvent( PermissibleSystemEvent.Action action )
	{
		callEvent( new PermissibleSystemEvent( action ) );
	}
	
	public static ConsoleLogger getLogger()
	{
		return Loader.getLogger( "PermMgr" );
	}
	
	public static void init() throws PermissionBackendException
	{
		if ( isInitialized )
			throw new IllegalStateException( "The Permission Manager has already been initialized." );
		
		assert INSTANCE != null;
		
		INSTANCE.init0();
		
		isInitialized = true;
	}
	
	public static boolean isInitialized()
	{
		return isInitialized;
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
	
	public void addPermission( Permission permission )
	{
		permissions.add( permission );
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
	
	protected void clearCache()
	{
		entities.clear();
		groups.clear();
		defaultGroups.clear();
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
	
	/**
	 * Return current backend
	 * 
	 * @return current backend object
	 */
	public PermissionBackend getBackend()
	{
		return backend;
	}
	
	public PermissibleGroup getDefaultGroup()
	{
		return this.getDefaultGroup( null );
	}
	
	/**
	 * Return default group object
	 * 
	 * @return default group object. null if not specified
	 */
	public PermissibleGroup getDefaultGroup( String refName )
	{
		String refIndex = refName != null ? refName : "";
		
		if ( !defaultGroups.containsKey( refIndex ) )
			defaultGroups.put( refIndex, this.getDefaultGroup( refName, this.getDefaultGroup( null, null ) ) );
		
		return defaultGroups.get( refIndex );
	}
	
	private PermissibleGroup getDefaultGroup( String refName, PermissibleGroup fallback )
	{
		PermissibleGroup defaultGroup = backend.getDefaultGroup( refName );
		
		if ( defaultGroup == null && refName == null )
		{
			getLogger().warning( "No default group defined. Use \"perm set default group <group> [ref]\" to define default group." );
			return fallback;
		}
		
		if ( defaultGroup != null )
			return defaultGroup;
		
		return fallback;
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
			if ( entity.checkPermission( perm ).isAssigned() )
				result.add( entity );
		
		return result;
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
		return getEntitiesWithPermission( getNode( perm ) );
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
			if ( entities.containsKey( permissible.getEntityId() ) )
				permissible.entity = entities.get( permissible.getEntityId() );
			else
			{
				PermissibleEntity entity = backend.getEntity( permissible.getEntityId() );
				entities.put( permissible.getEntityId(), entity );
				permissible.entity = entity;
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
	 * Return object for specified group
	 * 
	 * @param groupname
	 *            group's name
	 * @return PermissibleGroup object
	 */
	public PermissibleGroup getGroup( String groupname )
	{
		if ( groupname == null || groupname.isEmpty() )
			return null;
		
		PermissibleGroup group = groups.get( groupname.toLowerCase() );
		
		if ( group == null )
		{
			group = backend.getGroup( groupname );
			if ( group != null )
				groups.put( groupname.toLowerCase(), group );
			else
				throw new IllegalStateException( "Group " + groupname + " is null" );
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
	
	@Override
	public String getName()
	{
		return "PermissionsManager";
	}
	
	/**
	 * Attempts to find a Permission Node.
	 * Will not create the node if non-existent.
	 * 
	 * @param namespace
	 *            The namespace to find, e.g., com.chiorichan.user
	 * @return The found permission, null if non-existent
	 */
	public Permission getNode( String namespace )
	{
		String[] nodes = namespace.split( "\\." );
		
		if ( nodes.length < 1 )
			return null;
		
		Permission curr = getRootNode( nodes[0] );
		
		if ( curr == null )
			return null;
		
		if ( nodes.length == 1 )
			return curr;
		
		for ( String node : Arrays.copyOfRange( nodes, 1, nodes.length ) )
		{
			Permission child = curr.getChild( node.toLowerCase() );
			if ( child == null )
				return null;
			else
				curr = child;
		}
		
		return curr;
	}
	
	public Permission getNode( String namespace, boolean createNode )
	{
		if ( createNode )
			return getNode( namespace, PermissionType.DEFAULT );
		else
			return getNode( namespace );
	}
	
	/**
	 * Finds a registered permission node in the stack by crawling.
	 * 
	 * @param namespace
	 *            The full name space we need to crawl for.
	 * @param createChildren
	 *            Indicates if we should create the child node if non-existent.
	 * @return The child node based on the namespace. Will return NULL if non-existent and createChildren is false.
	 */
	public Permission getNode( String namespace, PermissionType type )
	{
		String[] nodes = namespace.split( "\\." );
		
		if ( nodes.length < 1 )
			return null;
		
		Permission curr = getRootNode( nodes[0] );
		
		if ( curr == null )
			curr = new Permission( nodes[0] );
		curr.setType( type );
		permissions.add( curr );
		
		if ( nodes.length == 1 )
			return curr;
		
		for ( String node : Arrays.copyOfRange( nodes, 1, nodes.length ) )
		{
			Permission child = curr.getChild( node.toLowerCase() );
			if ( child == null )
			{
				child = new Permission( node, curr );
				child.setType( type );
				curr.addChild( child );
				curr = child;
			}
			else
				curr = child;
		}
		
		return curr;
	}
	
	protected Permission getNodeByLocalName( String name )
	{
		for ( Permission perm : permissions )
			if ( perm.getLocalName().equalsIgnoreCase( name ) )
				return perm;
		return null;
	}
	
	/**
	 * Finds registered permission nodes.
	 * 
	 * @param namespace
	 *            The full name space we need to crawl for.
	 * @return A list of permissions that matched the namespace. Will return more then one if namespace contained asterisk.
	 */
	public List<Permission> getNodes( PermissionNamespace ns )
	{
		if ( ns == null )
			return Lists.newArrayList();
		
		if ( ns.getNodeCount() < 1 )
			return Lists.newArrayList();
		
		List<Permission> matches = Lists.newArrayList();
		
		for ( Permission p : permissions )
			if ( ns.matches( p ) )
				matches.add( p );
		
		return matches;
	}
	
	public List<Permission> getNodes( String ns )
	{
		return getNodes( new PermissionNamespace( ns ) );
	}
	
	protected Permission getRootNode( String name )
	{
		for ( Permission perm : permissions )
			if ( perm.parent == null && perm.getLocalName().equalsIgnoreCase( name ) )
				return perm;
		return null;
	}
	
	public List<Permission> getRootNodes()
	{
		return getRootNodes( true );
	}
	
	public List<Permission> getRootNodes( boolean ignoreSysNode )
	{
		List<Permission> rootNodes = Lists.newArrayList();
		for ( Permission p : permissions )
			if ( p.parent == null && !p.getNamespace().startsWith( "sys" ) && ignoreSysNode )
				rootNodes.add( p );
		return rootNodes;
	}
	
	public boolean hasWhitelist()
	{
		return hasWhitelist;
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
	 * Return current state of debug mode
	 * 
	 * @return true debug is enabled, false if disabled
	 */
	public boolean isDebug()
	{
		return debugMode;
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
		
		backend.loadPermissions();
		backend.loadEntities();
		backend.loadGroups();
		
		PermissionDefault.initNodes();
		
		if ( isDebug() )
		{
			getLogger().info( ConsoleColor.YELLOW + "Dumping Loaded Permissions:" );
			for ( Permission root : getRootNodes( false ) )
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
	
	/**
	 * Attempts to parse if a permission string is actually a reference to the EVERYBODY (-1, everybody, everyone), OP (0, op, root) or ADMIN (admin) permission nodes;
	 * 
	 * @param perm
	 *            The permission string to parse
	 * @return A string for the permission node, will return the original string if no match was found.
	 */
	public String parseNode( String perm )
	{
		// Everyone
		if ( perm == null || perm.isEmpty() || perm.equals( "-1" ) || perm.equals( "everybody" ) || perm.equals( "everyone" ) )
			perm = PermissionDefault.EVERYBODY.getNameSpace();
		
		// OP Only
		if ( perm.equals( "0" ) || perm.equalsIgnoreCase( "op" ) || perm.equalsIgnoreCase( "root" ) )
			perm = PermissionDefault.OP.getNameSpace();
		
		if ( perm.equalsIgnoreCase( "admin" ) )
			perm = PermissionDefault.ADMIN.getNameSpace();
		return perm;
	}
	
	/**
	 * Attempts to move a permission from one namespace to another.
	 * e.g., com.chiorichan.oldspace1.same.oldname -> com.chiorichan.newspace2.same.newname.
	 * 
	 * @param newNamespace
	 *            The new namespace you wish to use.
	 * @param appendLocalName
	 *            Pass true if you wish the method to append the LocalName to the new namespace.
	 *            If the localname of the new namespace is different then this permission will be renamed.
	 * @return true is move/rename was successful.
	 */
	public boolean refactorNamespace( String newNamespace, boolean appendLocalName )
	{
		// PermissionNamespace ns = getNamespaceObj();
		// TODO THIS!
		return false;
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
	
	public void reload() throws PermissionBackendException
	{
		reset();
		
		backend.loadEntities();
		backend.loadGroups();
		
		hasWhitelist = Loader.getConfig().getBoolean( "settings.whitelist" );
	}
	
	/**
	 * Reset all in-memory groups and entities, clean up runtime stuff, reloads backend
	 */
	public void reset() throws PermissionBackendException
	{
		clearCache();
		
		entities.clear();
		groups.clear();
		
		callEvent( PermissibleSystemEvent.Action.RELOADED );
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
	 * Reset in-memory object for groupName
	 * 
	 * @param groupName
	 *            group's name
	 */
	public void resetGroup( String groupName )
	{
		groups.remove( groupName );
	}
	
	/**
	 * Forcefully saves groups and entities to the backend data source.
	 */
	public void saveData()
	{
		for ( Permission p : permissions )
			p.commit();
		
		for ( PermissibleEntity entity : entities.values() )
			entity.save();
		
		for ( PermissibleGroup entity : groups.values() )
			entity.save();
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
	
	public void setDefaultGroup( PermissibleGroup group )
	{
		this.setDefaultGroup( group, null );
	}
	
	/**
	 * Set default group to specified group
	 * 
	 * @param group
	 *            PermissibleGroup group object
	 */
	public void setDefaultGroup( PermissibleGroup group, String refName )
	{
		if ( group == null || group.equals( defaultGroups ) )
			return;
		
		backend.setDefaultGroup( group.getId(), refName );
		
		defaultGroups.clear();
		
		callEvent( PermissibleSystemEvent.Action.DEFAULTGROUP_CHANGED );
		callEvent( new PermissibleEntityEvent( group, PermissibleEntityEvent.Action.DEFAULTGROUP_CHANGED ) );
	}
	
	public void setWhitelist( boolean value )
	{
		hasWhitelist = value;
		Loader.getConfig().set( "settings.whitelist", value );
	}
}
