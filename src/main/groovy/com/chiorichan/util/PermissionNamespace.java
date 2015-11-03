/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.util;

import com.chiorichan.permission.Permission;
import com.chiorichan.permission.PermissionManager;
import com.chiorichan.permission.PermissionType;

/**
 * Extends the base {@link Namespace} and adds permission specific methods
 */
public class PermissionNamespace extends Namespace
{
	public PermissionNamespace( String... namespace )
	{
		super( namespace );
	}
	
	public PermissionNamespace( String namespace )
	{
		super( namespace );
	}
	
	public Permission createPermission()
	{
		return PermissionManager.INSTANCE.createNode( getNamespace() );
	}
	
	public Permission createPermission( PermissionType type )
	{
		return PermissionManager.INSTANCE.createNode( getNamespace(), type );
	}
	
	public Permission getPermission()
	{
		return PermissionManager.INSTANCE.getNode( getNamespace() );
	}
	
	public boolean matches( Permission perm )
	{
		return matches( perm.getNamespace() );
	}
}
