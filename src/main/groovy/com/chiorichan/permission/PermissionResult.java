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

import java.util.List;

import com.chiorichan.util.Common;
import com.chiorichan.util.ObjectUtil;
import com.google.common.collect.Lists;

/**
 * Is returned when Permissible#getPermission() is called
 * and symbolizes the unity of said permission and entity.
 */
public class PermissionResult
{
	public static final PermissionResult DUMMY = new PermissionResult();
	
	private PermissibleEntity entity = null;
	private Permission perm = null;
	private String ref = "";
	private ChildPermission<?> childPerm = null;
	protected int timecode = Common.getEpoch();
	
	public PermissionResult()
	{
		
	}
	
	public PermissionResult( PermissibleEntity entity, Permission perm )
	{
		this( entity, perm, "" );
	}
	
	public PermissionResult( PermissibleEntity entity, Permission perm, String ref )
	{
		if ( ref == null )
			ref = "";
		
		this.entity = entity;
		this.perm = perm;
		this.ref = ref;
		
		if ( entity != null )
			childPerm = recursiveEntityScan( entity );
	}
	
	/**
	 * Used as a constant tracker for already checked groups, prevents infinite looping.
	 * e.g., User -> Group1 -> Group2 -> Group3 -> Group1
	 */
	private List<PermissibleGroup> groupStackTrace = null;
	
	private ChildPermission<?> recursiveEntityScan( PermissibleEntity pe )
	{
		ChildPermission<?> result = pe.getChildPermission( perm.getNamespace(), ref );
		
		if ( result != null )
			return result;
		
		boolean isFirst = false;
		
		if ( groupStackTrace == null )
		{
			groupStackTrace = Lists.newArrayList();
			isFirst = true;
		}
		
		for ( PermissibleGroup group : pe.groups.values() )
		{
			if ( !groupStackTrace.contains( group ) )
			{
				groupStackTrace.add( group );
				ChildPermission<?> childPerm = recursiveEntityScan( group );
				if ( childPerm != null )
				{
					result = childPerm;
					break;
				}
			}
		}
		
		if ( isFirst )
			groupStackTrace = null;
		
		return result;
	}
	
	public String getReference()
	{
		return ref;
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
		
		if ( !PermissionDefault.isDefault( perm ) && PermissionManager.allowOps && entity.isOp() )
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
				return perm.getValue().getValue();
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
