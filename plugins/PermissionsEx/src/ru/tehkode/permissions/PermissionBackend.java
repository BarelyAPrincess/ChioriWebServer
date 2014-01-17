package ru.tehkode.permissions;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import ru.tehkode.permissions.bukkit.PermissionsEx;
import ru.tehkode.permissions.exceptions.PermissionBackendException;

import com.chiorichan.Loader;
import com.chiorichan.configuration.Configuration;
import com.chiorichan.framework.Site;
import com.chiorichan.user.User;

/**
 * @author t3hk0d3
 */
public abstract class PermissionBackend
{
	
	protected final static String defaultBackend = "file";
	protected PermissionManager manager;
	protected Configuration config;
	protected boolean createUserRecords = false;
	
	protected PermissionBackend(PermissionManager manager, Configuration config)
	{
		this.manager = manager;
		this.config = config;
		
		this.createUserRecords = config.getBoolean( "permissions.createUserRecords", this.createUserRecords );
	}
	
	/**
	 * Backend initialization should be done here
	 */
	public abstract void initialize() throws PermissionBackendException;
	
	/**
	 * Returns new PermissionUser object for specified user name
	 * 
	 * @param name User name
	 * @return PermissionUser for specified user, or null on error.
	 */
	public abstract PermissionUser getUser( String name );
	
	/**
	 * Returns new PermissionGroup object for specified group name
	 * 
	 * @param name Group name
	 * @return PermissionGroup object, or null on error
	 */
	public abstract PermissionGroup getGroup( String name );
	
	/*
	 * Creates new group with specified name, or returns PermissionGroup object,
	 * if there is such group already exists.
	 * @param name Group name
	 * @returns PermissionGroup instance for specified group
	 */
	public PermissionGroup createGroup( String name )
	{
		return this.manager.getGroup( name );
	}
	
	/**
	 * Removes the specified group
	 * 
	 * @param groupName Name of the group which should be removed
	 * @return true if group was removed, false if group has child groups
	 */
	public boolean removeGroup( String groupName )
	{
		if ( this.getGroups( groupName ).length > 0 )
		{
			return false;
		}
		
		for ( PermissionUser user : this.getUsers( groupName ) )
		{
			user.removeGroup( groupName );
		}
		
		this.manager.getGroup( groupName ).remove();
		
		return true;
	}
	
	/**
	 * Returns default group, a group that is assigned to a user without a group set
	 * 
	 * @return Default group instance
	 */
	public abstract PermissionGroup getDefaultGroup( String siteName );
	
	/**
	 * Set group as default group
	 * 
	 * @param group
	 */
	public abstract void setDefaultGroup( PermissionGroup group, String siteName );
	
	/**
	 * Returns an array of site names of specified site name
	 * 
	 * @param site site name
	 * @return Array of parent sites. If there is no parent site return empty array
	 */
	public abstract String[] getSiteInheritance( String site );
	
	/**
	 * Set site inheritance parents for specified site
	 * 
	 * @param site site name which inheritance should be set
	 * @param parentSites array of parent site names
	 */
	public abstract void setSiteInheritance( String site, String[] parentSites );
	
	/**
	 * Return all registered groups
	 * 
	 * @return
	 */
	public abstract PermissionGroup[] getGroups();
	
	/**
	 * Return child groups of specified group
	 * 
	 * @param groupName
	 * @return empty array if group has no children, empty or not exist
	 */
	public PermissionGroup[] getGroups( String groupName )
	{
		return this.getGroups( groupName, null );
	}
	
	public PermissionGroup[] getGroups( String groupName, String siteName )
	{
		return this.getGroups( groupName, siteName, false );
	}
	
	/**
	 * Return child groups of specified group.
	 * 
	 * @param groupName
	 * @param inheritance - If true a full list of descendants will be returned
	 * @return empty array if group has no children, empty or not exist
	 */
	public PermissionGroup[] getGroups( String groupName, boolean inheritance )
	{
		Set<PermissionGroup> groups = new HashSet<PermissionGroup>();
		
		for ( Site site : Loader.getInstance().getSites() )
		{
			groups.addAll( Arrays.asList( getGroups( groupName, site.getName(), inheritance ) ) );
		}
		
		// Common space users
		groups.addAll( Arrays.asList( getGroups( groupName, null, inheritance ) ) );
		
		return groups.toArray( new PermissionGroup[0] );
	}
	
	public PermissionGroup[] getGroups( String groupName, String siteName, boolean inheritance )
	{
		List<PermissionGroup> groups = new LinkedList<PermissionGroup>();
		
		for ( PermissionGroup group : this.getGroups() )
		{
			if ( !groups.contains( group ) && group.isChildOf( groupName, siteName, inheritance ) )
			{
				groups.add( group );
			}
		}
		
		return groups.toArray( new PermissionGroup[0] );
	}
	
	/**
	 * Return all registered and online users
	 * 
	 * @return
	 */
	public PermissionUser[] getUsers()
	{
		Set<PermissionUser> users = new HashSet<PermissionUser>();
		
		for ( User user : Loader.getInstance().getOnlineUsers() )
		{
			users.add( this.manager.getUser( user ) );
		}
		
		users.addAll( Arrays.asList( this.getRegisteredUsers() ) );
		
		return users.toArray( new PermissionUser[0] );
	}
	
	/**
	 * Return all registered users
	 * 
	 * @return
	 */
	public abstract PermissionUser[] getRegisteredUsers();
	
	public Collection<String> getRegisteredUserNames()
	{
		PermissionUser[] users = getRegisteredUsers();
		List<String> ret = new ArrayList<String>( users.length );
		for ( PermissionUser user : users )
		{
			ret.add( user.getName() );
		}
		return Collections.unmodifiableCollection( ret );
	}
	
	public Collection<String> getRegisteredGroupNames()
	{
		PermissionGroup[] groups = getGroups();
		List<String> ret = new ArrayList<String>( groups.length );
		for ( PermissionGroup group : groups )
		{
			ret.add( group.getName() );
		}
		return Collections.unmodifiableCollection( ret );
	}
	
	/**
	 * Return users of specified group.
	 * 
	 * @param groupName
	 * @return null if there is no such group
	 */
	public PermissionUser[] getUsers( String groupName )
	{
		return getUsers( groupName, false );
	}
	
	public PermissionUser[] getUsers( String groupName, String siteName )
	{
		return getUsers( groupName, siteName, false );
	}
	
	/**
	 * Return users of specified group (and child groups)
	 * 
	 * @param groupName
	 * @param inheritance - If true return users list of descendant groups too
	 * @return
	 */
	public PermissionUser[] getUsers( String groupName, boolean inheritance )
	{
		Set<PermissionUser> users = new HashSet<PermissionUser>();
		
		for ( PermissionUser user : this.getUsers() )
		{
			if ( user.inGroup( groupName, inheritance ) )
			{
				users.add( user );
			}
		}
		
		return users.toArray( new PermissionUser[0] );
	}
	
	public PermissionUser[] getUsers( String groupName, String siteName, boolean inheritance )
	{
		Set<PermissionUser> users = new HashSet<PermissionUser>();
		
		for ( PermissionUser user : this.getUsers() )
		{
			if ( user.inGroup( groupName, siteName, inheritance ) )
			{
				users.add( user );
			}
		}
		
		return users.toArray( new PermissionUser[0] );
	}
	
	/**
	 * Reload backend (reread permissions file, reconnect to database, etc)
	 */
	public abstract void reload() throws PermissionBackendException;
	
	/**
	 * Dump data to native backend format
	 * 
	 * @param writer Writer where dumped data should be written to
	 * @throws IOException
	 */
	public abstract void dumpData( OutputStreamWriter writer ) throws IOException;
	
	/**
	 * Array of backend aliases
	 */
	protected static Map<String, Class<? extends PermissionBackend>> registedAliases = new HashMap<String, Class<? extends PermissionBackend>>();
	
	/**
	 * Return class name for alias
	 * 
	 * @param alias
	 * @return Class name if found or alias if there is no such class name present
	 */
	public static String getBackendClassName( String alias )
	{
		
		if ( registedAliases.containsKey( alias ) )
		{
			return registedAliases.get( alias ).getName();
		}
		
		return alias;
	}
	
	/**
	 * Returns Class object for specified alias, if there is no alias registered
	 * then try to find it using Class.forName(alias)
	 * 
	 * @param alias
	 * @return
	 * @throws ClassNotFoundException
	 */
	public static Class<? extends PermissionBackend> getBackendClass( String alias ) throws ClassNotFoundException
	{
		if ( !registedAliases.containsKey( alias ) )
		{
			return (Class<? extends PermissionBackend>) Class.forName( alias );
		}
		
		return registedAliases.get( alias );
	}
	
	/**
	 * Register new alias for specified backend class
	 * 
	 * @param alias
	 * @param backendClass
	 */
	public static void registerBackendAlias( String alias, Class<? extends PermissionBackend> backendClass )
	{
		if ( !PermissionBackend.class.isAssignableFrom( backendClass ) )
		{
			throw new RuntimeException( "Provided class should be subclass of PermissionBackend" );
		}
		
		registedAliases.put( alias, backendClass );
		
		Logger.getLogger( "" ).info( "[PermissionsEx] " + alias + " backend registered!" );
	}
	
	/**
	 * Return alias for specified backend class
	 * If there is no such class registered the fullname of this class would
	 * be returned using backendClass.getName();
	 * 
	 * @param backendClass
	 * @return alias or class fullname when not found using backendClass.getName()
	 */
	public static String getBackendAlias( Class<? extends PermissionBackend> backendClass )
	{
		if ( registedAliases.containsValue( backendClass ) )
		{
			for ( String alias : registedAliases.keySet() )
			{ // Is there better way to find key by value?
				if ( registedAliases.get( alias ).equals( backendClass ) )
				{
					return alias;
				}
			}
		}
		
		return backendClass.getName();
	}
	
	/**
	 * Returns new backend class instance for specified backendName
	 * 
	 * @param backendName Class name or alias of backend
	 * @param config Configuration object to access backend settings
	 * @return new instance of PermissionBackend object
	 */
	public static PermissionBackend getBackend( String backendName, Configuration config )
	{
		return getBackend( backendName, PermissionsEx.getPermissionManager(), config, defaultBackend );
	}
	
	/**
	 * Returns new Backend class instance for specified backendName
	 * 
	 * @param backendName Class name or alias of backend
	 * @param manager PermissionManager object
	 * @param config Configuration object to access backend settings
	 * @return new instance of PermissionBackend object
	 */
	public static PermissionBackend getBackend( String backendName, PermissionManager manager, Configuration config )
	{
		return getBackend( backendName, manager, config, defaultBackend );
	}
	
	/**
	 * Returns new Backend class instance for specified backendName
	 * 
	 * @param backendName Class name or alias of backend
	 * @param manager PermissionManager object
	 * @param config Configuration object to access backend settings
	 * @param fallBackBackend name of backend that should be used if specified backend was not found or failed to initialize
	 * @return new instance of PermissionBackend object
	 */
	public static PermissionBackend getBackend( String backendName, PermissionManager manager, Configuration config, String fallBackBackend )
	{
		if ( backendName == null || backendName.isEmpty() )
		{
			backendName = defaultBackend;
		}
		
		String className = getBackendClassName( backendName );
		
		try
		{
			Class<? extends PermissionBackend> backendClass = getBackendClass( backendName );
			
			Logger.getLogger( "" ).info( "[PermissionsEx] Initializing " + backendName + " backend" );
			
			Constructor<? extends PermissionBackend> constructor = backendClass.getConstructor( PermissionManager.class, Configuration.class );
			return (PermissionBackend) constructor.newInstance( manager, config );
		}
		catch ( ClassNotFoundException e )
		{
			
			Logger.getLogger( "" ).warning( "[PermissionsEx] Specified backend \"" + backendName + "\" are not found." );
			
			if ( fallBackBackend == null )
			{
				throw new RuntimeException( e );
			}
			
			if ( !className.equals( getBackendClassName( fallBackBackend ) ) )
			{
				return getBackend( fallBackBackend, manager, config, null );
			}
			else
			{
				throw new RuntimeException( e );
			}
		}
		catch ( Exception e )
		{
			throw new RuntimeException( e );
		}
	}
	
	public boolean isCreateUserRecords()
	{
		return this.createUserRecords;
	}
}
