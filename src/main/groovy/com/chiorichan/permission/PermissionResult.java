/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.permission;

import com.chiorichan.account.AccountType;
import com.chiorichan.permission.lang.PermissionException;
import com.chiorichan.permission.lang.PermissionValueException;
import com.chiorichan.tasks.Timings;
import com.chiorichan.util.ObjectFunc;
import com.chiorichan.util.StringFunc;

/**
 * Is returned when {@link Permissible#getPermission()} is called
 * and symbolizes the unity of said permission and entity.
 */
public class PermissionResult
{
	public static final PermissionResult DUMMY = new PermissionResult( AccountType.ACCOUNT_NONE.getPermissibleEntity(), PermissionDefault.DEFAULT.getNode() );
	
	private ChildPermission childPerm = null;
	private final PermissibleBase entity;
	private final Permission perm;
	private final String ref;
	
	protected int timecode = Timings.epoch();
	
	PermissionResult( PermissibleBase entity, Permission perm )
	{
		this( entity, perm, "" );
	}
	
	PermissionResult( PermissibleBase entity, Permission perm, String ref )
	{
		ref = StringFunc.formatReference( ref );
		
		assert ( entity != null );
		assert ( perm != null );
		
		this.entity = entity;
		this.perm = perm;
		this.ref = ref;
		childPerm = entity.findChildPermission( perm, ref );
	}
	
	public PermissionResult assign()
	{
		if ( perm.getType() != PermissionType.DEFAULT )
			throw new PermissionException( String.format( "Can't assign the permission %s to entity %s, because the permission is of type %s, use assign(Object) with the appropriate value instead.", perm.getNamespace(), entity.getId(), perm.getType().name() ) );
		
		childPerm = new ChildPermission( perm, perm.getModel().createValue( true ), false );
		entity.childPermissions.add( childPerm );
		
		return this;
	}
	
	public PermissionResult assign( Object val )
	{
		if ( perm.getType() == PermissionType.DEFAULT )
			throw new PermissionException( String.format( "Can't assign the permission %s with value %s to entity %s, because the permission is of default type, which can't carry a value other than assigned or not.", perm.getNamespace(), val, entity.getId(), perm.getType().name() ) );
		
		if ( val == null )
			throw new PermissionValueException( "The assigned value must not be null." );
		
		childPerm = new ChildPermission( perm, perm.getModel().createValue( val ), false );
		entity.childPermissions.add( childPerm );
		
		return this;
	}
	
	/**
	 * See {@link Permission#commit()}<br>
	 * Caution: will commit changes made to other child values of the same permission node
	 * 
	 * @return The {@link PermissionResult} for chaining
	 */
	public PermissionResult commit()
	{
		perm.commit();
		return this;
	}
	
	public PermissibleBase getEntity()
	{
		return entity;
	}
	
	public int getInt()
	{
		return ObjectFunc.castToInt( getValue().getValue() );
	}
	
	public Permission getPermission()
	{
		return perm;
	}
	
	public String getReference()
	{
		return ref;
	}
	
	public String getString()
	{
		return ObjectFunc.castToString( getValue().getValue() );
	}
	
	public PermissionValue getValue()
	{
		if ( childPerm == null || childPerm.getValue() == null || !isAssigned() )
			return perm.getModel().getModelValue();
		
		return childPerm.getValue();
	}
	
	/**
	 * Returns a final object based on assignment of permission.
	 * 
	 * @return
	 *         Unassigned will return the default value.
	 */
	@SuppressWarnings( "unchecked" )
	public <T> T getValueObject()
	{
		Object obj;
		
		if ( isAssigned() )
		{
			if ( childPerm == null || childPerm.getValue() == null )
				obj = perm.getModel().getModelValue();
			else
				obj = childPerm.getObject();
		}
		else
			obj = perm.getModel().getValueDefault();
		
		try
		{
			return ( T ) obj;
		}
		catch ( ClassCastException e )
		{
			throw new PermissionValueException( String.format( "Can't cast %s to type", obj.getClass().getName() ), e );
		}
	}
	
	/**
	 * @return was this entity assigned an custom value for this permission.
	 */
	public boolean hasValue()
	{
		return perm.getType() != PermissionType.DEFAULT && childPerm != null && childPerm.getValue() != null;
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
		
		return childPerm.isInherited();
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
		if ( perm.getType() == PermissionType.DEFAULT )
			return isAssigned();
		
		if ( perm.getType() != PermissionType.BOOL )
			throw new PermissionValueException( String.format( "The permission %s is not of type boolean.", perm.getNamespace() ) );
		
		if ( !PermissionDefault.isDefault( perm ) && PermissionManager.allowOps && entity.isOp() )
			return true;
		
		return ( getValueObject() == null ) ? false : ObjectFunc.castToBool( getValueObject() );
	}
	
	@Override
	public String toString()
	{
		return String.format( "PermissionResult{name=%s,value=%s,isAssigned=%s}", perm.getNamespace(), getValueObject(), isAssigned() );
	}
}
