package ru.tehkode.permissions;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import ru.tehkode.permissions.events.PermissionEntityEvent;
import ru.tehkode.permissions.events.PermissionEvent;
import ru.tehkode.permissions.events.PermissionSystemEvent;
import ru.tehkode.permissions.exceptions.PermissionBackendException;

import com.chiorichan.Loader;
import com.chiorichan.account.bases.Account;
import com.chiorichan.configuration.Configuration;

public class PermissionManager
{
	
	public final static int TRANSIENT_PERMISSION = 0;
	protected static final Logger logger = Logger.getLogger( "" );
	protected Map<String, PermissionUser> users = new HashMap<String, PermissionUser>();
	protected Map<String, PermissionGroup> groups = new HashMap<String, PermissionGroup>();
	protected Map<String, PermissionGroup> defaultGroups = new HashMap<String, PermissionGroup>();
	protected PermissionBackend backend = null;
	protected Configuration config;
	protected Timer timer;
	protected boolean debugMode = false;
	protected boolean allowOps = false;
	protected boolean userAddGroupsLast = false;
	
	protected PermissionMatcher matcher = new RegExpMatcher();
	
	public PermissionManager(Configuration config) throws PermissionBackendException
	{
		this.config = config;
		this.initBackend();
		
		this.debugMode = config.getBoolean( "permissions.debug", debugMode );
		this.allowOps = config.getBoolean( "permissions.allowOps", allowOps );
		this.userAddGroupsLast = config.getBoolean( "permissions.user-add-groups-last", userAddGroupsLast );
	}
	
	/**
	 * Check if specified user has specified permission
	 * 
	 * @param user user object
	 * @param permission permission string to check against
	 * @return true on success false otherwise
	 */
	public boolean has( Account user, String permission )
	{
		return has( user.getName(), permission, user.getSite().getName() );
	}
	
	/**
	 * Check if user has specified permission in site
	 * 
	 * @param user user object
	 * @param permission permission as string to check against
	 * @param site site's name as string
	 * @return true on success false otherwise
	 */
	public boolean has( Account user, String permission, String site )
	{
		return this.has( user.getName(), permission, site );
	}
	
	/**
	 * Check if user with name has permission in site
	 * 
	 * @param userName user name
	 * @param permission permission as string to check against
	 * @param site site's name as string
	 * @return true on success false otherwise
	 */
	public boolean has( String userName, String permission, String site )
	{
		PermissionUser user = this.getUser( userName );
		
		if ( user == null )
		{
			return false;
		}
		
		return user.has( permission, site );
	}
	
	/**
	 * Return user's object
	 * 
	 * @param username get PermissionUser with given name
	 * @return PermissionUser instance
	 */
	public PermissionUser getUser( String username )
	{
		if ( username == null || username.isEmpty() )
		{
			throw new IllegalArgumentException( "Null or empty name passed! Name must not be empty" );
		}
		
		PermissionUser user = users.get( username.toLowerCase() );
		
		if ( user == null )
		{
			user = this.backend.getUser( username );
			if ( user != null )
			{
				user.initialize();
				this.users.put( username.toLowerCase(), user );
			}
			else
			{
				throw new IllegalStateException( "User " + username + " is null" );
			}
		}
		
		return user;
	}
	
	/**
	 * Return object of specified user
	 * 
	 * @param user user object
	 * @return PermissionUser instance
	 */
	public PermissionUser getUser( Account user )
	{
		return this.getUser( user.getName() );
	}
	
	/**
	 * Return all registered user objects
	 * 
	 * @return PermissionUser array
	 */
	public PermissionUser[] getUsers()
	{
		return backend.getUsers();
	}
	
	public Collection<String> getUserNames()
	{
		return backend.getRegisteredUserNames();
	}
	
	/**
	 * Return all users in group
	 * 
	 * @param groupName group's name
	 * @return PermissionUser array
	 */
	public PermissionUser[] getUsers( String groupName, String siteName )
	{
		return backend.getUsers( groupName, siteName );
	}
	
	public PermissionUser[] getUsers( String groupName )
	{
		return backend.getUsers( groupName );
	}
	
	/**
	 * Return all users in group and descendant groups
	 * 
	 * @param groupName group's name
	 * @param inheritance true return members of descendant groups of specified group
	 * @return PermissionUser array for groupnName
	 */
	public PermissionUser[] getUsers( String groupName, String siteName, boolean inheritance )
	{
		return backend.getUsers( groupName, siteName, inheritance );
	}
	
	public PermissionUser[] getUsers( String groupName, boolean inheritance )
	{
		return backend.getUsers( groupName, inheritance );
	}
	
	/**
	 * Reset in-memory object of specified user
	 * 
	 * @param userName user's name
	 */
	public void resetUser( String userName )
	{
		this.users.remove( userName.toLowerCase() );
	}
	
	/**
	 * Clear cache for specified user
	 * 
	 * @param userName
	 */
	public void clearUserCache( String userName )
	{
		PermissionUser user = this.getUser( userName );
		
		if ( user != null )
		{
			user.clearCache();
		}
	}
	
	/**
	 * Clear cache for specified user
	 * 
	 * @param user
	 */
	public void clearUserCache( Account user )
	{
		this.clearUserCache( user.getName() );
	}
	
	/**
	 * Return object for specified group
	 * 
	 * @param groupname group's name
	 * @return PermissionGroup object
	 */
	public PermissionGroup getGroup( String groupname )
	{
		if ( groupname == null || groupname.isEmpty() )
		{
			return null;
		}
		
		PermissionGroup group = groups.get( groupname.toLowerCase() );
		
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
	 * @return PermissionGroup array
	 */
	public PermissionGroup[] getGroups()
	{
		return backend.getGroups();
	}
	
	/**
	 * Return all child groups of specified group
	 * 
	 * @param groupName group's name
	 * @return PermissionGroup array
	 */
	public PermissionGroup[] getGroups( String groupName, String siteName )
	{
		return backend.getGroups( groupName, siteName );
	}
	
	public PermissionGroup[] getGroups( String groupName )
	{
		return backend.getGroups( groupName );
	}
	
	/**
	 * Return all descendants or child groups for groupName
	 * 
	 * @param groupName group's name
	 * @param inheritance true: only direct child groups would be returned
	 * @return PermissionGroup array for specified groupName
	 */
	public PermissionGroup[] getGroups( String groupName, String siteName, boolean inheritance )
	{
		return backend.getGroups( groupName, siteName, inheritance );
	}
	
	public PermissionGroup[] getGroups( String groupName, boolean inheritance )
	{
		return backend.getGroups( groupName, inheritance );
	}
	
	/**
	 * Return default group object
	 * 
	 * @return default group object. null if not specified
	 */
	public PermissionGroup getDefaultGroup( String siteName )
	{
		String siteIndex = siteName != null ? siteName : "";
		
		if ( !this.defaultGroups.containsKey( siteIndex ) )
		{
			this.defaultGroups.put( siteIndex, this.getDefaultGroup( siteName, this.getDefaultGroup( null, null ) ) );
		}
		
		return this.defaultGroups.get( siteIndex );
	}
	
	public PermissionGroup getDefaultGroup()
	{
		return this.getDefaultGroup( null );
	}
	
	private PermissionGroup getDefaultGroup( String siteName, PermissionGroup fallback )
	{
		PermissionGroup defaultGroup = this.backend.getDefaultGroup( siteName );
		
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
	 * @param group PermissionGroup group object
	 */
	public void setDefaultGroup( PermissionGroup group, String siteName )
	{
		if ( group == null || group.equals( this.defaultGroups ) )
		{
			return;
		}
		
		backend.setDefaultGroup( group, siteName );
		
		this.defaultGroups.clear();
		
		this.callEvent( PermissionSystemEvent.Action.DEFAULTGROUP_CHANGED );
		this.callEvent( new PermissionEntityEvent( group, PermissionEntityEvent.Action.DEFAULTGROUP_CHANGED ) );
	}
	
	public void setDefaultGroup( PermissionGroup group )
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
		this.callEvent( PermissionSystemEvent.Action.DEBUGMODE_TOGGLE );
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
	public Map<Integer, PermissionGroup> getRankLadder( String ladderName )
	{
		Map<Integer, PermissionGroup> ladder = new HashMap<Integer, PermissionGroup>();
		
		for ( PermissionGroup group : this.getGroups() )
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
		this.callEvent( PermissionSystemEvent.Action.WORLDINHERITANCE_CHANGED );
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
			this.backend = PermissionBackend.getBackend( backendName, this, config );
			this.backend.initialize();
		}
		
		this.callEvent( PermissionSystemEvent.Action.BACKEND_CHANGED );
	}
	
	/**
	 * Register new timer task
	 * 
	 * @param task TimerTask object
	 * @param delay delay in seconds
	 */
	protected void registerTask( TimerTask task, int delay )
	{
		if ( timer == null || delay == TRANSIENT_PERMISSION )
		{
			return;
		}
		
		timer.schedule( task, delay * 1000 );
	}
	
	/**
	 * Reset all in-memory groups and users, clean up runtime stuff, reloads backend
	 */
	public void reset() throws PermissionBackendException
	{
		this.clearCache();
		
		if ( this.backend != null )
		{
			this.backend.reload();
		}
		this.callEvent( PermissionSystemEvent.Action.RELOADED );
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
		timer.cancel();
	}
	
	public void initTimer()
	{
		if ( timer != null )
		{
			timer.cancel();
		}
		
		timer = new Timer( "PermissionsEx-Cleaner" );
	}
	
	protected void clearCache()
	{
		this.users.clear();
		this.groups.clear();
		this.defaultGroups.clear();
		
		// Close old timed Permission Timer
		this.initTimer();
	}
	
	private void initBackend() throws PermissionBackendException
	{
		String backendName = this.config.getString( "permissions.backend" );
		
		if ( backendName == null || backendName.isEmpty() )
		{
			backendName = PermissionBackend.defaultBackend; // Default backend
			this.config.set( "permissions.backend", backendName );
		}
		
		this.setBackend( backendName );
	}
	
	protected void callEvent( PermissionEvent event )
	{
		Loader.getEventBus().callEvent( event );
	}
	
	protected void callEvent( PermissionSystemEvent.Action action )
	{
		this.callEvent( new PermissionSystemEvent( action ) );
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
}
