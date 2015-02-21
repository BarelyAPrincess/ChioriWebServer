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

public abstract class PermissibleEntityProxy extends PermissibleEntity
{
	public PermissibleEntityProxy( String id, PermissionBackend permBackend )
	{
		super( id, permBackend );
	}
	
	public void attachPermission( ChildPermission perm )
	{
		childPermissions.put( perm.getPermission().getNamespace(), perm );
	}
	
	public void detachPermission( ChildPermission perm )
	{
		detachPermission( perm.getPermission().getNamespace() );
	}
	
	public void detachPermission( String perm )
	{
		childPermissions.remove( perm );
	}
	
	public void detachAllPermissions()
	{
		childPermissions.clear();
	}
	
	public ChildPermission[] getChildPermissions()
	{
		return childPermissions.values().toArray( new ChildPermission[0] );
	}
	
	public void clearGroups()
	{
		groups.clear();
	}
}
