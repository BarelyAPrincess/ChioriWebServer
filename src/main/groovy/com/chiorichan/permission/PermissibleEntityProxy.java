/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.permission;


public abstract class PermissibleEntityProxy extends PermissibleEntity
{
	public PermissibleEntityProxy( String id )
	{
		super( id );
	}
	
	public final void attachPermission( ChildPermission perm )
	{
		childPermissions.add( perm );
	}
	
	public final void clearGroups()
	{
		groups.clear();
	}
	
	public final void detachAllPermissions()
	{
		childPermissions.clear();
	}
	
	public final void detachPermission( ChildPermission perm )
	{
		detachPermission( perm.getPermission().getNamespace() );
	}
	
	public final void detachPermission( String perm )
	{
		childPermissions.remove( perm );
	}
	
	public final ChildPermission[] getChildPermissions()
	{
		return childPermissions.toArray( new ChildPermission[0] );
	}
}
