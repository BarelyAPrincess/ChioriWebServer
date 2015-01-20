package com.chiorichan.permission;

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

import com.chiorichan.Loader;
import com.chiorichan.configuration.Configuration;
import com.chiorichan.framework.Site;
import com.sun.jna.platform.win32.Advapi32Util.Account;

public abstract class PermissionBackend
{
	protected final static String defaultBackend = "file";
	
	/**
	 * Backend initialization should be done here
	 */
	public abstract void initialize() throws PermissionBackendException;
	
	/**
	 * Returns new PermissibleEntity object for specified id
	 * 
	 * @param id
	 * @return PermissibleEntity for specified id, or null on error.
	 */
	public abstract PermissibleEntity getEntity( String id );
	
	/**
	 * Returns new PermissibleGroup object for specified id
	 * 
	 * @param id
	 * @return PermissibleGroup object, or null on error
	 */
	public abstract PermissibleGroup getGroup( String id );
	
	/*
	 * Creates new group with specified id, or returns PermissibleGroup object,
	 * if there is such group already exists.
	 * @param id
	 * @returns PermissibleGroup instance for specified group
	 */
	public PermissibleGroup createGroup( String id )
	{
		return Loader.getPermissionsManager().getGroup( id );
	}
	
	/**
	 * Removes the specified group
	 * 
	 * @param groupName Name of the group which should be removed
	 * @return true if group was removed, false if group has child groups
	 */
	public boolean removeGroup( String groupName )
	{
		if ( getGroups( groupName ).length > 0 )
		{
			return false;
		}
		
		for ( PermissibleEntity entity : getEntities( groupName ) )
		{
			entity.removeGroup( groupName );
		}
		
		Loader.getPermissionsManager().getGroup( groupName ).remove();
		
		return true;
	}
	
	/**
	 * Returns default group, a group that is assigned to a entity without a group set
	 * 
	 * @return Default group instance
	 */
	public abstract PermissibleGroup getDefaultGroup( String siteName );
	
	/**
	 * Set group as default group
	 * 
	 * @param group
	 */
	public abstract void setDefaultGroup( PermissibleGroup group, String siteName );
	
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
	public abstract PermissibleGroup[] getGroups();
	
	/**
	 * Return child groups of specified group
	 * 
	 * @param groupName
	 * @return empty array if group has no children, empty or not exist
	 */
	public PermissibleGroup[] getGroups( String groupName )
	{
		return getGroups( groupName, null );
	}
	
	public PermissibleGroup[] getGroups( String groupName, String siteName )
	{
		return getGroups( groupName, siteName, false );
	}
	
	/**
	 * Return child groups of specified group.
	 * 
	 * @param groupName
	 * @param inheritance - If true a full list of descendants will be returned
	 * @return empty array if group has no children, empty or not exist
	 */
	public PermissibleGroup[] getGroups( String groupName, boolean inheritance )
	{
		Set<PermissibleGroup> groups = new HashSet<PermissibleGroup>();
		
		for ( Site site : Loader.getSiteManager().getSites() )
		{
			groups.addAll( Arrays.asList( getGroups( groupName, site.getName(), inheritance ) ) );
		}
		
		// Common space entities
		groups.addAll( Arrays.asList( getGroups( groupName, null, inheritance ) ) );
		
		return groups.toArray( new PermissibleGroup[0] );
	}
	
	public PermissibleGroup[] getGroups( String groupName, String siteName, boolean inheritance )
	{
		List<PermissibleGroup> groups = new LinkedList<PermissibleGroup>();
		
		for ( PermissibleGroup group : getGroups() )
		{
			if ( !groups.contains( group ) && group.isChildOf( groupName, siteName, inheritance ) )
			{
				groups.add( group );
			}
		}
		
		return groups.toArray( new PermissibleGroup[0] );
	}
	
	/**
	 * Return all registered and online entities
	 * 
	 * @return
	 */
	public PermissibleEntity[] getEntities()
	{
		Set<PermissibleEntity> entities = new HashSet<PermissibleEntity>();
		
		for ( PermissibleEntity entity : Loader.getPermissionsManager().getEntities() )
		{
			entities.add( entity );
		}
		
		entities.addAll( Arrays.asList( getRegisteredEntities() ) );
		
		return entities.toArray( new PermissibleEntity[0] );
	}
	
	/**
	 * Return all registered entities
	 * 
	 * @return
	 */
	public abstract PermissibleEntity[] getRegisteredEntities();
	
	public Collection<String> getRegisteredEntityNames()
	{
		PermissibleEntity[] entities = getRegisteredEntities();
		List<String> ret = new ArrayList<String>( entities.length );
		for ( PermissibleEntity entity : entities )
		{
			ret.add( entity.getName() );
		}
		return Collections.unmodifiableCollection( ret );
	}
	
	public Collection<String> getRegisteredGroupNames()
	{
		PermissibleGroup[] groups = getGroups();
		List<String> ret = new ArrayList<String>( groups.length );
		for ( PermissibleGroup group : groups )
		{
			ret.add( group.getName() );
		}
		return Collections.unmodifiableCollection( ret );
	}
	
	/**
	 * Return entities of specified group.
	 * 
	 * @param groupName
	 * @return null if there is no such group
	 */
	public PermissibleEntity[] getEntities( String groupName )
	{
		return getEntities( groupName, false );
	}
	
	public PermissibleEntity[] getEntities( String groupName, String siteName )
	{
		return getEntities( groupName, siteName, false );
	}
	
	/**
	 * Return entities of specified group (and child groups)
	 * 
	 * @param groupName
	 * @param inheritance - If true return entities list of descendant groups too
	 * @return
	 */
	public PermissibleEntity[] getEntities( String groupName, boolean inheritance )
	{
		Set<PermissibleEntity> entities = new HashSet<PermissibleEntity>();
		
		for ( PermissibleEntity entity : getEntities() )
		{
			if ( entity.inGroup( groupName, inheritance ) )
			{
				entities.add( entity );
			}
		}
		
		return entities.toArray( new PermissibleEntity[0] );
	}
	
	public PermissibleEntity[] getEntities( String groupName, String siteName, boolean inheritance )
	{
		Set<PermissibleEntity> entities = new HashSet<PermissibleEntity>();
		
		for ( PermissibleEntity entity : getEntities() )
		{
			if ( entity.inGroup( groupName, siteName, inheritance ) )
			{
				entities.add( entity );
			}
		}
		
		return entities.toArray( new PermissibleEntity[0] );
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
	
	public static PermissionBackend getBackend( String backendName )
	{
		return getBackend( backendName, null );
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
	public static PermissionBackend getBackend( String backendName, String fallBackBackend )
	{
		if ( backendName == null || backendName.isEmpty() )
		{
			backendName = defaultBackend;
		}
		
		String className = getBackendClassName( backendName );
		
		try
		{
			Class<? extends PermissionBackend> backendClass = getBackendClass( backendName );
			
			PermissionManager.getLogger().info( "Initializing " + backendName + " backend" );
			
			Constructor<? extends PermissionBackend> constructor = backendClass.getConstructor( PermissionManager.class, Configuration.class );
			return (PermissionBackend) constructor.newInstance();
		}
		catch ( ClassNotFoundException e )
		{
			
			PermissionManager.getLogger().warning( "Specified backend \"" + backendName + "\" are not found." );
			
			if ( fallBackBackend == null )
			{
				throw new RuntimeException( e );
			}
			
			if ( !className.equals( getBackendClassName( fallBackBackend ) ) )
			{
				return getBackend( fallBackBackend, null );
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
}
