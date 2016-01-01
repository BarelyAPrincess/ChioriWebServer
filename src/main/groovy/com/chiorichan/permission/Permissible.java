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
		if ( AccountType.isNoneAccount( entity ) )
			PermissionManager.INSTANCE.getEntity( this );

		if ( entity == null )
			entity = AccountType.ACCOUNT_NONE.getEntity();

		return !AccountType.isNoneAccount( entity );
	}

	public final PermissibleEntity getPermissibleEntity()
	{
		checkEntity();
		return entity;
	}

	public final void destroyEntity()
	{
		entity = AccountType.ACCOUNT_NONE.getEntity();
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

	public final PermissionResult checkPermission( String perm, References refs )
	{
		perm = PermissionManager.parseNode( perm );
		return checkPermission( PermissionManager.INSTANCE.getNode( perm ), refs );
	}

	public final PermissionResult checkPermission( Permission perm, References refs )
	{
		PermissibleEntity entity = getPermissibleEntity();
		return entity.checkPermission( perm, refs );
	}

	public final PermissionResult checkPermission( String perm, String... refs )
	{
		return checkPermission( perm, References.format( refs ) );
	}

	public final PermissionResult checkPermission( Permission perm, String... refs )
	{
		return checkPermission( perm, References.format( refs ) );
	}

	public final PermissionResult checkPermission( Permission perm )
	{
		return checkPermission( perm, References.format( "" ) );
	}

	/**
	 * -1, everybody, everyone = Allow All!
	 * 0, op, root | sys.op = OP Only!
	 * admin | sys.admin = Admin Only!
	 */
	public final PermissionResult requirePermission( String req, References refs ) throws PermissionDeniedException
	{
		req = PermissionManager.parseNode( req );
		return requirePermission( PermissionManager.INSTANCE.createNode( req ), refs );
	}

	public final PermissionResult requirePermission( String req, String... refs ) throws PermissionDeniedException
	{
		req = PermissionManager.parseNode( req );
		return requirePermission( PermissionManager.INSTANCE.createNode( req ), References.format( refs ) );
	}

	public final PermissionResult requirePermission( Permission req, String... refs ) throws PermissionDeniedException
	{
		return requirePermission( req, References.format( refs ) );
	}

	public final PermissionResult requirePermission( Permission req, References refs ) throws PermissionDeniedException
	{
		PermissionResult result = checkPermission( req );

		if ( result.getPermission() != PermissionDefault.EVERYBODY.getNode() )
		{
			if ( result.getEntity() == null || AccountType.isNoneAccount( result.getEntity() ) )
				throw new PermissionDeniedException( PermissionDeniedReason.LOGIN_PAGE.setPermission( req ) );

			if ( !result.isTrue() )
			{
				if ( result.getPermission() == PermissionDefault.OP.getNode() )
					throw new PermissionDeniedException( PermissionDeniedReason.OP_ONLY );

				result.recalculatePermissions( refs );
				if ( result.isTrue() )
					return result;

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
	public abstract String getId();
}
