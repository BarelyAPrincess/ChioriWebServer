/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.permission;

import java.io.OutputStreamWriter;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.chiorichan.permission.lang.PermissionBackendException;

/**
 * Provides the basis of Permission Backend classes
 */
public abstract class PermissionBackend
{
	protected static final String defaultBackend = "file";
	
	public static final int ENTITY = 0;
	public static final int GROUP = 1;
	
	/**
	 * Array of backend aliases
	 */
	protected static Map<String, Class<? extends PermissionBackend>> registedAliases = new HashMap<String, Class<? extends PermissionBackend>>();
	
	// TODO Make it so node can be changed from one backend to another with ease and without restarting.
	
	private static PermissionBackend getBackend( Class<? extends PermissionBackend> backendClass )
	{
		try
		{
			PermissionManager.getLogger().info( "Initializing " + backendClass.getName() + " backend" );
			Constructor<? extends PermissionBackend> constructor = backendClass.getConstructor();
			return constructor.newInstance();
		}
		catch ( Exception e )
		{
			throw new RuntimeException( e );
		}
	}
	
	/**
	 * Return a specific Backend instance, be that the current backend or not.
	 * 
	 * @param backendName
	 *            Class name or alias of backend
	 * @return instance of PermissionBackend object
	 */
	public static PermissionBackend getBackend( String backendName )
	{
		try
		{
			return getBackendWithException( backendName );
		}
		catch ( ClassNotFoundException e )
		{
			return null;
		}
	}
	
	public static PermissionBackend getBackend( String backendName, String fallBackBackend )
	{
		try
		{
			return getBackendWithException( backendName, fallBackBackend );
		}
		catch ( ClassNotFoundException e )
		{
			return null;
		}
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
			for ( String alias : registedAliases.keySet() )
				if ( registedAliases.get( alias ).equals( backendClass ) )
					return alias;
		
		return backendClass.getName();
	}
	
	/**
	 * Returns Class object for specified alias, if there is no alias registered
	 * then try to find it using Class.forName(alias)
	 * 
	 * @param alias
	 * @return PermissionBackend Class for alias provided
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings( "unchecked" )
	public static Class<? extends PermissionBackend> getBackendClass( String alias ) throws ClassNotFoundException
	{
		if ( !registedAliases.containsKey( alias ) )
			return ( Class<? extends PermissionBackend> ) Class.forName( alias );
		
		return registedAliases.get( alias );
	}
	
	/**
	 * Return class name for alias
	 * 
	 * @param alias
	 * @return Class name if found or alias if there is no such class name present
	 */
	public static String getBackendClassName( String alias )
	{
		
		if ( registedAliases.containsKey( alias ) )
			return registedAliases.get( alias ).getName();
		
		return alias;
	}
	
	public static PermissionBackend getBackendWithException( String backendName ) throws ClassNotFoundException
	{
		Class<? extends PermissionBackend> backendClass = getBackendClass( backendName );
		if ( PermissionManager.INSTANCE.getBackend() != null && PermissionManager.INSTANCE.getBackend().getClass() == backendClass )
			return PermissionManager.INSTANCE.getBackend();
		else
			return getBackend( backendClass );
	}
	
	/**
	 * Returns a new Backend class instance for specified backendName
	 * 
	 * @param backendName
	 *            Class name or alias of backend
	 * @param fallBackBackend
	 *            name of backend that should be used if specified backend was not found or failed to initialize
	 * @return new instance of PermissionBackend object
	 */
	public static PermissionBackend getBackendWithException( String backendName, String fallBackBackend ) throws ClassNotFoundException
	{
		if ( backendName == null || backendName.isEmpty() )
			backendName = defaultBackend;
		
		try
		{
			return getBackendWithException( backendName );
		}
		catch ( ClassNotFoundException e )
		{
			PermissionManager.getLogger().warning( "Specified backend \"" + backendName + "\" was not found." );
			
			if ( fallBackBackend == null )
				throw e;
			
			if ( !getBackendClassName( backendName ).equals( getBackendClassName( fallBackBackend ) ) )
				return getBackend( fallBackBackend );
			else
				throw e;
		}
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
			throw new RuntimeException( "Provided class should be subclass of PermissionBackend" );
		
		registedAliases.put( alias, backendClass );
		
		PermissionManager.getLogger().fine( alias + " backend registered!" );
	}
	
	public abstract void commit();
	
	public void dumpData( OutputStreamWriter outputStreamWriter )
	{
		// TODO Auto-generated method stub
	}
	
	/**
	 * Returns default group, a group that is assigned to a entity without a group set
	 * 
	 * @return Default group instance
	 */
	public abstract PermissibleGroup getDefaultGroup( References refs );
	
	/**
	 * Returns new PermissibleEntity object for specified id
	 * 
	 * @param id
	 * @return PermissibleEntity for specified id, or null on error.
	 */
	public abstract PermissibleEntity getEntity( String id );
	
	/**
	 * This method loads all entity names from the backend.
	 */
	public abstract Collection<String> getEntityNames();
	
	public abstract Collection<String> getEntityNames( int type );
	
	/**
	 * Returns new PermissibleGroup object for specified id
	 * 
	 * @param id
	 * @return PermissibleGroup object, or null on error
	 */
	public abstract PermissibleGroup getGroup( String id );
	
	/**
	 * This method loads all group names from the backend.
	 */
	public abstract Collection<String> getGroupNames();
	
	/**
	 * Backend initialization should be done here
	 */
	public abstract void initialize() throws PermissionBackendException;
	
	/**
	 * This method loads all entities from the backend.
	 * 
	 * @throws PermissionBackendException
	 */
	public abstract void loadEntities() throws PermissionBackendException;
	
	/**
	 * This method loads all groups from the backend.
	 * 
	 * @throws PermissionBackendException
	 */
	public abstract void loadGroups() throws PermissionBackendException;
	
	/**
	 * This method loads all permissions from the backend.
	 * 
	 * @throws PermissionBackendException
	 */
	public abstract void loadPermissions() throws PermissionBackendException;
	
	/**
	 * Commits any changes made to the permission node to the backend for saving
	 */
	public abstract void nodeCommit( Permission perm );
	
	/**
	 * Destroys the permission node and it's children, removing it from both the backend and memory.<br>
	 * <br>
	 * Warning: could be considered unsafe to destroy a permission node without first removing all child values
	 */
	public abstract void nodeDestroy( Permission perm );
	
	/**
	 * Disregards any changes made to the permission node and reloads from the backend
	 */
	public abstract void nodeReload( Permission perm );
	
	public abstract void reloadBackend() throws PermissionBackendException;
	
	/**
	 * Sets the default group
	 */
	public abstract void setDefaultGroup( String child, References refs );
}
