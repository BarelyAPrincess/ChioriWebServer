package com.chiorichan.permission;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;

import com.chiorichan.ConsoleLogger;
import com.chiorichan.Loader;
import com.chiorichan.account.Account;
import com.chiorichan.file.YamlConfiguration;
import com.chiorichan.permission.event.PermissibleEntityEvent;
import com.chiorichan.permission.event.PermissibleEvent;
import com.chiorichan.permission.event.PermissibleSystemEvent;
import com.chiorichan.scheduler.TaskCreator;
import com.google.common.collect.Maps;

public class PermissionManager implements TaskCreator
{
	protected Map<String, PermissibleGroup> defaultGroups = new HashMap<String, PermissibleGroup>();
	protected Map<String, PermissibleEntity> users = new HashMap<String, PermissibleEntity>();
	protected Map<String, PermissibleGroup> groups = new HashMap<String, PermissibleGroup>();
	protected Map<String, PermissibleEntity> entities = Maps.newHashMap();
	protected PermissionMatcher matcher = new RegExpMatcher();
	protected PermissionBackend backend = null;
	protected boolean debugMode = false;
	protected YamlConfiguration config;
	
	public void init() throws PermissionBackendException
	{
		config = Loader.getConfig();
		
		initBackend();
		debugMode = config.getBoolean( "permissions.debug", debugMode );
	}
	
	private void initBackend() throws PermissionBackendException
	{
		String backendName = config.getString( "permissions.backend" );
		
		if ( backendName == null || backendName.isEmpty() )
		{
			backendName = PermissionBackend.defaultBackend; // Default backend
			this.config.set( "permissions.backend", backendName );
		}
		
		this.setBackend( backendName );
	}
	
	/**
	 * Check if specified entity has specified permission
	 * 
	 * @param entity entity object
	 * @param permission permission string to check against
	 * @return true on success false otherwise
	 */
	public boolean has( Permissible perm, String permission )
	{
		return has( perm.getId(), permission, perm.getType().toString() );
	}
	
	/**
	 * Check if entity has specified permission in site
	 * 
	 * @param entity entity object
	 * @param permission permission as string to check against
	 * @param site site's name as string
	 * @return true on success false otherwise
	 */
	public boolean has( Account entity, String permission, String site )
	{
		return this.has( entity.getName(), permission, site );
	}
	
	/**
	 * Check if entity with name has permission in site
	 * 
	 * @param entityName entity name
	 * @param permission permission as string to check against
	 * @param site site's name as string
	 * @return true on success false otherwise
	 */
	public boolean has( String entityName, String permission, String site )
	{
		PermissibleEntity entity = this.getEntity( entityName );
		
		if ( entity == null )
		{
			return false;
		}
		
		return entity.has( permission, site );
	}
	
	/**
	 * Return entity's object
	 * 
	 * @param entityname get PermissibleEntity with given name
	 * @return PermissibleEntity instance
	 */
	public PermissibleEntity getEntity( String entityname )
	{
		if ( entityname == null || entityname.isEmpty() )
		{
			throw new IllegalArgumentException( "Null or empty name passed! Name must not be empty" );
		}
		
		PermissibleEntity entity = entities.get( entityname.toLowerCase() );
		
		if ( entity == null )
		{
			entity = this.backend.getEntity( entityname );
			if ( entity != null )
			{
				entity.initialize();
				this.entities.put( entityname.toLowerCase(), entity );
			}
			else
			{
				throw new IllegalStateException( "Entity " + entityname + " is null" );
			}
		}
		
		return entity;
	}
	
	/**
	 * Return object of specified entity
	 * 
	 * @param entity entity object
	 * @return PermissibleEntity instance
	 */
	public PermissibleEntity getEntity( Account entity )
	{
		return this.getEntity( entity.getName() );
	}
	
	/**
	 * Return all registered entity objects
	 * 
	 * @return PermissibleEntity array
	 */
	public PermissibleEntity[] getEntities()
	{
		return backend.getEntities();
	}
	
	public Collection<String> getEntityNames()
	{
		return backend.getRegisteredEntityNames();
	}
	
	/**
	 * Return all entities in group
	 * 
	 * @param groupName group's name
	 * @return PermissibleEntity array
	 */
	public PermissibleEntity[] getEntities( String groupName, String siteName )
	{
		return backend.getEntities( groupName, siteName );
	}
	
	public PermissibleEntity[] getEntities( String groupName )
	{
		return backend.getEntities( groupName );
	}
	
	/**
	 * Return all entities in group and descendant groups
	 * 
	 * @param groupName group's name
	 * @param inheritance true return members of descendant groups of specified group
	 * @return PermissibleEntity array for groupnName
	 */
	public PermissibleEntity[] getEntities( String groupName, String siteName, boolean inheritance )
	{
		return backend.getEntities( groupName, siteName, inheritance );
	}
	
	public PermissibleEntity[] getEntities( String groupName, boolean inheritance )
	{
		return backend.getEntities( groupName, inheritance );
	}
	
	/**
	 * Reset in-memory object of specified entity
	 * 
	 * @param entityName entity's name
	 */
	public void resetEntity( String entityName )
	{
		this.entities.remove( entityName.toLowerCase() );
	}
	
	/**
	 * Clear cache for specified entity
	 * 
	 * @param entityName
	 */
	public void clearEntityCache( String entityName )
	{
		PermissibleEntity entity = this.getEntity( entityName );
		
		if ( entity != null )
		{
			entity.clearCache();
		}
	}
	
	/**
	 * Clear cache for specified entity
	 * 
	 * @param entity
	 */
	public void clearEntityCache( Account entity )
	{
		this.clearEntityCache( entity.getName() );
	}
	
	/**
	 * Return object for specified group
	 * 
	 * @param groupname group's name
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
				group.initialize();
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
	 * Return all child groups of specified group
	 * 
	 * @param groupName group's name
	 * @return PermissibleGroup array
	 */
	public PermissibleGroup[] getGroups( String groupName, String siteName )
	{
		return backend.getGroups( groupName, siteName );
	}
	
	public PermissibleGroup[] getGroups( String groupName )
	{
		return backend.getGroups( groupName );
	}
	
	/**
	 * Return all descendants or child groups for groupName
	 * 
	 * @param groupName group's name
	 * @param inheritance true: only direct child groups would be returned
	 * @return PermissibleGroup array for specified groupName
	 */
	public PermissibleGroup[] getGroups( String groupName, String siteName, boolean inheritance )
	{
		return backend.getGroups( groupName, siteName, inheritance );
	}
	
	public PermissibleGroup[] getGroups( String groupName, boolean inheritance )
	{
		return backend.getGroups( groupName, inheritance );
	}
	
	/**
	 * Return default group object
	 * 
	 * @return default group object. null if not specified
	 */
	public PermissibleGroup getDefaultGroup( String siteName )
	{
		String siteIndex = siteName != null ? siteName : "";
		
		if ( !this.defaultGroups.containsKey( siteIndex ) )
		{
			this.defaultGroups.put( siteIndex, this.getDefaultGroup( siteName, this.getDefaultGroup( null, null ) ) );
		}
		
		return this.defaultGroups.get( siteIndex );
	}
	
	public PermissibleGroup getDefaultGroup()
	{
		return this.getDefaultGroup( null );
	}
	
	private PermissibleGroup getDefaultGroup( String siteName, PermissibleGroup fallback )
	{
		PermissibleGroup defaultGroup = this.backend.getDefaultGroup( siteName );
		
		if ( defaultGroup == null && siteName == null )
		{
			throw new IllegalStateException( "No default group defined. Use \"pex set default group <group> [site]\" to define default group." );
		}
		
		if ( defaultGroup != null )
		{
			return defaultGroup;
		}
		
		if ( siteName != null )
		{
			// check site-inheritance
			for ( String parentSite : this.getSiteInheritance( siteName ) )
			{
				defaultGroup = this.getDefaultGroup( parentSite, null );
				if ( defaultGroup != null )
				{
					return defaultGroup;
				}
			}
		}
		
		return fallback;
	}
	
	/**
	 * Set default group to specified group
	 * 
	 * @param group PermissibleGroup group object
	 */
	public void setDefaultGroup( PermissibleGroup group, String siteName )
	{
		if ( group == null || group.equals( this.defaultGroups ) )
		{
			return;
		}
		
		backend.setDefaultGroup( group, siteName );
		
		this.defaultGroups.clear();
		
		this.callEvent( PermissibleSystemEvent.Action.DEFAULTGROUP_CHANGED );
		this.callEvent( new PermissibleEntityEvent( group, PermissibleEntityEvent.Action.DEFAULTGROUP_CHANGED ) );
	}
	
	public void setDefaultGroup( PermissibleGroup group )
	{
		this.setDefaultGroup( group, null );
	}
	
	/**
	 * Reset in-memory object for groupName
	 * 
	 * @param groupName group's name
	 */
	public void resetGroup( String groupName )
	{
		this.groups.remove( groupName );
	}
	
	/**
	 * Set debug mode
	 * 
	 * @param debug true enables debug mode, false disables
	 */
	public void setDebug( boolean debug )
	{
		this.debugMode = debug;
		this.callEvent( PermissibleSystemEvent.Action.DEBUGMODE_TOGGLE );
	}
	
	/**
	 * Return current state of debug mode
	 * 
	 * @return true debug is enabled, false if disabled
	 */
	public boolean isDebug()
	{
		return this.debugMode;
	}
	
	/**
	 * Return groups of specified rank ladder
	 * 
	 * @param ladderName
	 * @return Map of ladder, key - rank of group, value - group object. Empty map if ladder does not exist
	 */
	public Map<Integer, PermissibleGroup> getRankLadder( String ladderName )
	{
		Map<Integer, PermissibleGroup> ladder = new HashMap<Integer, PermissibleGroup>();
		
		for ( PermissibleGroup group : this.getGroups() )
		{
			if ( !group.isRanked() )
			{
				continue;
			}
			
			if ( group.getRankLadder().equalsIgnoreCase( ladderName ) )
			{
				ladder.put( group.getRank(), group );
			}
		}
		
		return ladder;
	}
	
	/**
	 * Return array of site names who has site inheritance
	 * 
	 * @param siteName Site name
	 * @return Array of parent site, if site does not exist return empty array
	 */
	public String[] getSiteInheritance( String siteName )
	{
		return backend.getSiteInheritance( siteName );
	}
	
	/**
	 * Set site inheritance parents for site
	 * 
	 * @param site site name which inheritance should be set
	 * @param parentSites array of parent site names
	 */
	public void setSiteInheritance( String site, String[] parentSites )
	{
		backend.setSiteInheritance( site, parentSites );
		this.callEvent( PermissibleSystemEvent.Action.WORLDINHERITANCE_CHANGED );
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
	 * Set backend to specified backend.
	 * This would also cause backend resetting.
	 * 
	 * @param backendName name of backend to set to
	 */
	public void setBackend( String backendName ) throws PermissionBackendException
	{
		synchronized ( this )
		{
			this.clearCache();
			this.backend = PermissionBackend.getBackend( backendName );
			this.backend.initialize();
		}
		
		this.callEvent( PermissibleSystemEvent.Action.BACKEND_CHANGED );
	}
	
	/**
	 * Register new timer task
	 * 
	 * @param task TimerTask object
	 * @param delay delay in seconds
	 */
	protected void registerTask( TimerTask task, int delay )
	{
		Loader.getScheduler().scheduleAsyncDelayedTask( this, task, delay * 50 );
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
		this.callEvent( PermissibleSystemEvent.Action.RELOADED );
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
	
	protected void callEvent( PermissibleEvent event )
	{
		Loader.getEventBus().callEvent( event );
	}
	
	protected void callEvent( PermissibleSystemEvent.Action action )
	{
		this.callEvent( new PermissibleSystemEvent( action ) );
	}
	
	public PermissionMatcher getPermissionMatcher()
	{
		return matcher;
	}
	
	public void setPermissionMatcher( PermissionMatcher matcher )
	{
		this.matcher = matcher;
	}
	
	public Collection<String> getGroupNames()
	{
		return backend.getRegisteredGroupNames();
	}
	
	public static ConsoleLogger getLogger()
	{
		return Loader.getLogger( "PermissionsManager" );
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
}
