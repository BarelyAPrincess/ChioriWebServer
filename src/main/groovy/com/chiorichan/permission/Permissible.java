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
			PermissionManager.INSTANCE.getEntity( this );
		
		entity.setVirtual( isVirtual() );
		
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
		perm = PermissionManager.parseNode( perm );
		return checkPermission( PermissionManager.INSTANCE.getNode( perm ) );
	}
	
	public final PermissionResult checkPermission( String perm, String ref )
	{
		perm = PermissionManager.parseNode( perm );
		return checkPermission( PermissionManager.INSTANCE.getNode( perm ), ref );
	}
	
	public final PermissionResult checkPermission( Permission perm, String ref )
	{
		PermissibleEntity entity = checkEntity() ? getPermissibleEntity() : AccountType.ACCOUNT_NONE.getPermissibleEntity();
		return entity.checkPermission( perm, ref );
	}
	
	public final PermissionResult checkPermission( Permission perm )
	{
		return checkPermission( perm, "" );
	}
	
	/**
	 * -1, everybody, everyone = Allow All!
	 * 0, op, root | sys.op = OP Only!
	 * admin | sys.admin = Admin Only!
	 */
	public final PermissionResult requirePermission( String req, String... refs ) throws PermissionDeniedException
	{
		req = PermissionManager.parseNode( req );
		return requirePermission( PermissionManager.INSTANCE.getNode( req, true ), refs );
	}
	
	
	public final PermissionResult requirePermission( Permission req, String... refs ) throws PermissionDeniedException
	{
		PermissionResult result = checkPermission( req );
		
		if ( result.getPermission() != PermissionDefault.EVERYBODY.getNode() )
		{
			if ( result.getEntity() == null )
				throw new PermissionDeniedException( PermissionDeniedReason.LOGIN_PAGE.setPermission( req ) );
			
			if ( !result.isTrue() )
			{
				if ( result.getPermission() == PermissionDefault.OP.getNode() )
					throw new PermissionDeniedException( PermissionDeniedReason.OP_ONLY );
				
				for ( String ref : refs )
				{
					result.recalculatePermissions( ref );
					if ( result.isTrue() )
						return result;
				}
				
				throw new PermissionDeniedException( PermissionDeniedReason.DENIED.setPermission( req ) );
			}
		}
		
		return result;
	}
	
	/**
	 * Get the unique identifier for this Permissible
	 * 
	 * @return String
	 *         a unique identifier
	 */
	public abstract String getEntityId();
	
	/**
	 * Indicates if the permissible entity should be allowed to save, i.e., keep it's groups and permissions persistent between restarts
	 * 
	 * @return True if so
	 */
	public abstract boolean isVirtual();
}
