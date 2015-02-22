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

import com.chiorichan.permission.structure.ChildPermission;
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
	private ChildPermission childPerm = null;
	
	public PermissionResult()
	{
		
	}
	
	public PermissionResult( PermissibleEntity entity, Permission perm )
	{
		this.entity = entity;
		this.perm = perm;
		
		childPerm = recursiveEntityScan( entity, perm );
	}
	
	private ChildPermission recursiveEntityScan( PermissibleEntity pe, Permission perm )
	{
		if ( pe.childPermissions.containsKey( perm.getNamespace() ) )
			return pe.childPermissions.get( perm.getNamespace() );
		
		for ( PermissibleGroup group : pe.groups.values() )
		{
			ChildPermission childPerm = recursiveEntityScan( group, perm );
			if ( childPerm != null )
				return childPerm;
		}
		
		return null;
	}
	
	/**
	 * @return was this permission assigned to our entity?
	 */
	public boolean isAssigned()
	{
		return childPerm != null;
	}
	
	/**
	 * @return was this permission assigned to our entity thru a group? Will return false if not assigned.
	 */
	public boolean isInherited()
	{
		if ( !isAssigned() )
			return false;
		
		return childPerm.isInherited;
	}
	
	/**
	 * @return was this entity assigned an custom value for this permission.
	 */
	public boolean hasCustomValue()
	{
		return ( childPerm != null && childPerm.getValue() != null );
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
			throw new IllegalAccessException( "This Permission Node is not type Boolean and can not be checked if true." );
		
		if ( !perm.getNamespace().equals( Permission.OP ) && PermissionManager.allowOps && entity.isOp() )
			return true;
		
		return ( getObject() == null ) ? false : ( Boolean ) getObject();
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
		return perm.getValue();
	}
	
	public PermissionValue<?> getValue()
	{
		if ( childPerm == null || childPerm.getValue() == null )
			return perm.getValue();
		
		return childPerm.getValue();
	}
	
	/**
	 * Returns a final object based on assignment state.
	 * 
	 * @return
	 *         Unassigned will return the default value.
	 */
	public Object getObject()
	{
		if ( isAssigned() )
		{
			if ( childPerm == null || childPerm.getValue() == null )
				return perm.getObject();
			else
				return childPerm.getObject();
		}
		else
			return perm.getValue().getDefault();
	}
	
	public PermissibleEntity getEntity()
	{
		return entity;
	}
	
	public Permission getPermission()
	{
		return perm;
	}
	
	@Override
	public String toString()
	{
		return "PermissionResult{value=" + getObject() + ",isAssigned=" + isAssigned() + ",permission=" + perm + "}";
	}
}
