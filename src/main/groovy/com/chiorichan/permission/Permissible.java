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

import com.chiorichan.permission.lang.PermissionDeniedException;
import com.chiorichan.permission.lang.PermissionDeniedException.PermissionDeniedReason;

public abstract class Permissible
{
	/**
	 * Used to reference the PermissibleEntity for the Permissible object.
	 */
	protected PermissibleEntity entity = null;
	
	public final boolean checkEntity()
	{
		if ( entity == null )
			entity = PermissionManager.INSTANCE.getEntity( this );
		
		return entity != null;
	}
	
	public final PermissibleEntity getPermissibleEntity()
	{
		checkEntity();
		return entity;
	}
	
	public final boolean isBanned()
	{
		if ( !checkEntity() )
			return false;
		
		return entity.isBanned();
	}
	
	public final boolean isWhitelisted()
	{
		if ( !checkEntity() )
			return false;
		
		return entity.isWhitelisted();
	}
	
	public final boolean isAdmin()
	{
		if ( !checkEntity() )
			return false;
		
		return entity.isAdmin();
	}
	
	/**
	 * Is this permissible on the OP list.
	 * 
	 * @return true if OP
	 */
	public final boolean isOp()
	{
		if ( !checkEntity() )
			return false;
		
		return entity.isOp();
	}
	
	public final PermissionResult checkPermission( String perm )
	{
		perm = Permission.parseNode( perm );
		return checkPermission( Permission.getNode( perm ) );
	}
	
	public final PermissionResult checkPermission( Permission perm )
	{
		if ( !checkEntity() )
			return new PermissionResult( null, perm, "" );
		
		return getPermissibleEntity().checkPermission( perm );
	}
	
	/**
	 * -1, everybody, everyone = Allow All!
	 * 0, op, root | sys.op = OP Only!
	 * admin | sys.admin = Admin Only!
	 */
	public final PermissionResult requirePermission( String req ) throws PermissionDeniedException
	{
		req = Permission.parseNode( req );
		return requirePermission( Permission.getNode( req ) );
	}
	
	public final PermissionResult requirePermission( Permission req ) throws PermissionDeniedException
	{
		PermissionResult perm = checkPermission( req );
		
		if ( perm.getPermission() != PermissionDefault.EVERYBODY.getNode() )
		{
			if ( perm.getEntity() == null )
			{
				throw new PermissionDeniedException( PermissionDeniedReason.LOGIN_PAGE );
			}
			
			if ( !perm.isTrue() )
			{
				if ( perm.getPermission() == PermissionDefault.OP.getNode() )
					throw new PermissionDeniedException( PermissionDeniedReason.OP_ONLY );
				throw new PermissionDeniedException( PermissionDeniedReason.DENIED.setPermission( req ) );
			}
		}
		
		return perm;
	}
	
	/**
	 * Get the unique identifier for this Permissible
	 * 
	 * @return String
	 *         a unique identifier
	 */
	public abstract String getEntityId();
}
