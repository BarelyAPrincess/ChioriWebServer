/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.permission.backend.file;

import java.util.ArrayList;

import com.chiorichan.ConsoleColor;
import com.chiorichan.configuration.ConfigurationSection;
import com.chiorichan.permission.ChildPermission;
import com.chiorichan.permission.PermissibleEntity;
import com.chiorichan.permission.Permission;
import com.chiorichan.permission.PermissionManager;
import com.chiorichan.permission.PermissionType;
import com.chiorichan.permission.PermissionValue;
import com.google.common.base.Joiner;

public class FileEntity extends PermissibleEntity
{
	public FileEntity( String userName )
	{
		super( userName );
	}
	
	@Override
	public void reloadGroups()
	{
		if ( isDebug() )
			PermissionManager.getLogger().info( ConsoleColor.YELLOW + "Groups being loaded for entity " + getId() );
		
		ConfigurationSection result = FileBackend.getBackend().permissions.getConfigurationSection( "entities/" + getId(), true );
		groups.clear();
		
		for ( String group : result.getStringList( "groups", new ArrayList<String>() ) )
			addGroup( PermissionManager.INSTANCE.getGroup( group ) );
	}
	
	@Override
	public void reloadPermissions()
	{
		if ( isDebug() )
			PermissionManager.getLogger().info( ConsoleColor.YELLOW + "Permissions being loaded for entity " + getId() );
		
		ConfigurationSection result = FileBackend.getBackend().permissions.getConfigurationSection( "entities." + getId(), true );
		ConfigurationSection permissions = result.getConfigurationSection( "permissions" );
		childPermissions.clear();
		
		if ( permissions != null )
			for ( String ss : permissions.getKeys( false ) )
			{
				ConfigurationSection permission = permissions.getConfigurationSection( ss );
				Permission perm = PermissionManager.INSTANCE.getNode( ss.replaceAll( "/", "." ).toLowerCase(), true );
				
				PermissionValue value = null;
				if ( permission.getString( "value" ) != null )
					value = perm.getModel().createValue( permission.getString( "value" ) );
				
				String refs = permission.isString( "refs" ) ? permission.getString( "refs" ) : "";
				attachPermission( new ChildPermission( perm, value, -1, refs.split( "|" ) ) );
			}
	}
	
	@Override
	public void remove()
	{
		FileBackend.getBackend().permissions.getConfigurationSection( "entities", true ).set( getId(), null );
	}
	
	@Override
	public void save()
	{
		ConfigurationSection entity = FileBackend.getBackend().permissions.getConfigurationSection( "entities." + getId(), true );
		ConfigurationSection permissions = entity.getConfigurationSection( "permissions", true );
		
		for ( ChildPermission child : getChildPermissions() )
		{
			Permission perm = child.getPermission();
			ConfigurationSection sub = permissions.getConfigurationSection( perm.getNamespace().replaceAll( "\\.", "/" ), true );
			sub.set( "permission", perm.getNamespace() );
			if ( perm.getType() != PermissionType.DEFAULT )
				sub.set( "value", child.getObject() );
			sub.set( "refs", Joiner.on( "|" ).join( child.getReferences() ) );
		}
	}
}
