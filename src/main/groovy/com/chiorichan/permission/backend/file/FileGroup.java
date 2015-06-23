/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.permission.backend.file;

import com.chiorichan.configuration.ConfigurationSection;
import com.chiorichan.permission.ChildPermission;
import com.chiorichan.permission.PermissibleGroup;
import com.chiorichan.permission.Permission;
import com.chiorichan.permission.PermissionType;
import com.google.common.base.Joiner;

public class FileGroup extends PermissibleGroup
{
	public FileGroup( String name )
	{
		super( name );
	}
	
	@Override
	public void reloadGroups()
	{
		
	}
	
	@Override
	public void reloadPermissions()
	{
		
	}
	
	@Override
	public void remove()
	{
		FileBackend.getBackend().permissions.getConfigurationSection( "groups", true ).set( getId(), null );
	}
	
	@Override
	public void save()
	{
		ConfigurationSection entity = FileBackend.getBackend().permissions.getConfigurationSection( "groups", true ).getConfigurationSection( getId(), true );
		ConfigurationSection permissions = entity.getConfigurationSection( "permissions", true );
		
		for ( ChildPermission child : getChildPermissions() )
		{
			Permission perm = child.getPermission();
			ConfigurationSection sub = permissions.getConfigurationSection( perm.getLocalName(), true );
			sub.set( "permission", perm.getNamespace() );
			if ( perm.getType() != PermissionType.DEFAULT )
				sub.set( "value", child.getObject() );
			sub.set( "refs", Joiner.on( "|" ).join( child.getReferences() ) );
		}
	}
}
