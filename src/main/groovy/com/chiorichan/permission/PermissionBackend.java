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

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class PermissionBackend
{
	/**
	 * Array of backend aliases
	 */
	protected static Map<String, Class<? extends PermissionBackend>> registedAliases = new HashMap<String, Class<? extends PermissionBackend>>();
	
	public static final int GROUP = 1;
	public static final int ENTITY = 0;
	
	protected static final String defaultBackend = "file";
	
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
	
	public abstract void setDefaultGroup( String child, String... site );
	
	/**
	 * Returns default group, a group that is assigned to a entity without a group set
	 * 
	 * @return Default group instance
	 */
	public abstract PermissibleGroup getDefaultGroup( String siteName );
	
	/**
	 * Return all registered groups
	 * 
	 * @return Array of PermissibleGroup
	 */
	public abstract PermissibleGroup[] getGroups();
	
	/**
	 * Return all registered and online entities
	 * 
	 * @return Array of PermissibleEntity
	 */
	public abstract PermissibleEntity[] getEntities();
	
	public abstract Set<String> getEntityNames( int type );
	
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
	 * @return PermissionBackend Class for alias provided
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings( "unchecked" )
	public static Class<? extends PermissionBackend> getBackendClass( String alias ) throws ClassNotFoundException
	{
		if ( !registedAliases.containsKey( alias ) )
		{
			return ( Class<? extends PermissionBackend> ) Class.forName( alias );
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
		
		PermissionManager.getLogger().info( alias + " backend registered!" );
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
	 * @param backendName
	 *            Class name or alias of backend
	 * @param fallBackBackend
	 *            name of backend that should be used if specified backend was not found or failed to initialize
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
			
			Constructor<? extends PermissionBackend> constructor = backendClass.getConstructor();
			return ( PermissionBackend ) constructor.newInstance();
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
	
	public abstract void reload() throws PermissionBackendException;
	
	public abstract void loadPermissionTree();
}
