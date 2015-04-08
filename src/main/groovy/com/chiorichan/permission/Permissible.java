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

import com.chiorichan.Loader;

public abstract class Permissible
{
	/**
	 * Used to reference the PermissibleEntity for the Permissible object.
	 * Referencing the entity here allows the Trash Collector to destroy this instance if the Permissible is no longer used.
	 */
	protected PermissibleEntity entity = null;
	
	public final boolean checkEntity()
	{
		if ( entity == null )
			entity = Loader.getPermissionManager().getEntity( this );
		
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
	 * Web users id will be in the form of `siteId`_`acctId`.
	 * 
	 * @return String
	 *         a unique identifier
	 */
	public abstract String getEntityId();
	
	/**
	 * @return boolean
	 *         is the connection remote
	 */
	public abstract boolean isRemote();
	
	/**
	 * If the entity is connected remotely then return the Remote Address.
	 * 
	 * @return String
	 *         an IPv4/IPv6 Address or null if no remote handlers
	 */
	public abstract String getIpAddr();
	
	public abstract boolean isValid();
}
