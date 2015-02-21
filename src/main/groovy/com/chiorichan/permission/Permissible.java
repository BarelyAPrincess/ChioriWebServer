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
import com.chiorichan.permission.structure.Permission;

public abstract class Permissible
{
	/**
	 * Used to reference the PermissibleEntity for the Permissible object.
	 * Referencing the entity here allows the Trash Collector to destroy this instance if the Permissible is no longer used.
	 */
	protected PermissibleEntity entity = null;
	
	public final PermissibleEntity getPermissibleEntity()
	{
		if ( entity == null )
			entity = Loader.getPermissionManager().getEntity( this );
		
		return entity;
	}
	
	/**
	 * Is this permissible on the OP list.
	 * 
	 * @return true if OP
	 */
	public final boolean isOp()
	{
		return getPermissibleEntity().isOp( this );
	}
	
	public final PermissionResult checkPermission( String perm )
	{
		return getPermissibleEntity().checkPermission( perm );
	}
	
	public final PermissionResult checkPermission( Permission perm )
	{
		return getPermissibleEntity().checkPermission( perm );
	}
	
	/**
	 * Web users id will be in the form of `siteId`_`acctId`.
	 * 
	 * @return String
	 *         a unique identifier
	 */
	public abstract String getId();
	
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
}
