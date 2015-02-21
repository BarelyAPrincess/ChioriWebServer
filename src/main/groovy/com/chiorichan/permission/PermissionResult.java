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

import com.chiorichan.permission.structure.Permission;
import com.chiorichan.permission.structure.PermissionValue;
import com.chiorichan.util.ObjectUtil;

/**
 * Is returned when Permissible#getPermission() is called
 * and symbolizes the unity of said permission and entity.
 */
public class PermissionResult
{
	public static final PermissionResult DUMMY = new PermissionResult();
	
	private PermissibleEntity entity = null;
	private Permission perm = null;
	private boolean inherited = false;

	public PermissionResult()
	{
		
	}
	
	public PermissionResult( PermissibleEntity entity, Permission perm )
	{
		this.entity = entity;
		this.perm = perm;
	}

	/**
	 * @return was this permission assigned to our entity?
	 */
	public boolean isAssigned()
	{
		return false;
	}
	
	/**
	 * @return was this permission assigned to our entity thru a group? Will return false if not assigned.
	 */
	public boolean isInherited()
	{
		if ( !isAssigned() )
			return false;
		
		return inherited;
	}
	
	/**
	 * @return was this entity assigned an custom value for this permission.
	 */
	public boolean isCustomValue()
	{
		return false;
	}
	
	/**
	 * A safe version of isTrue() in case you don't care to know if the permission is of type Boolean or not
	 * 
	 * @return is this permission true
	 */
	public boolean isTrue()
	{
		try
		{
			return isTrueWithException();
		}
		catch ( IllegalAccessException e )
		{
			return false;
		}
	}
	
	/**
	 * Used strictly for BOOLEAN permission nodes.
	 * 
	 * @return is this permission true
	 * @throws IllegalAccessException
	 *             Thrown if this permission node is not of type Boolean
	 */
	public boolean isTrueWithException() throws IllegalAccessException
	{
		if ( getValue().getType() != PermissionValue.PermissionType.BOOL )
			throw new IllegalAccessException( "This Permission Node is not of the type Boolean and can not be checked if true." );
		
		// TODO this!
		
		return false;
	}
	
	public Object getObject()
	{
		return getValue().getValue();
	}
	
	public String getString()
	{
		return ObjectUtil.castToString( getValue().getValue() );
	}
	
	public int getInt()
	{
		return ObjectUtil.castToInt( getValue().getValue() );
	}
	
	public PermissionValue<?> getDefaultValue()
	{
		return null;
	}
	
	public PermissionValue<?> getValue()
	{
		//perm.getValue()
		
		return null;
	}
	
	public PermissibleEntity getEntity()
	{
		return entity;
	}
	
	public Permission getPermission()
	{
		return perm;
	}
	
	/*
	 * Loader.getLogger().info( ConsoleColor.GREEN + "Checking `" + getId() + "` for permission `" + req + "` with result `" + perm.hasPermission( req ) + "`" );
	 * // Everyone
	 * if ( req.equals( "-1" ) || req.isEmpty() )
	 * return true;
	 * // OP Only
	 * if ( req.equals( "0" ) || req.equalsIgnoreCase( "op" ) || req.equalsIgnoreCase( "admin" ) || req.equalsIgnoreCase( "root" ) )
	 * return isOp();
	 * return perm.hasPermission( req );
	 */
}
