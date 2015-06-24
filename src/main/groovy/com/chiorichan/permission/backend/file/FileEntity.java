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
		
		ConfigurationSection section = FileBackend.getBackend().permissions.getConfigurationSection( "entities." + getId() );
		groups.clear();
		
		for ( String group : section.getStringList( "groups", new ArrayList<String>() ) )
			addGroup( PermissionManager.INSTANCE.getGroup( group ) );
	}
	
	@Override
	public void reloadPermissions()
	{
		if ( isDebug() )
			PermissionManager.getLogger().info( ConsoleColor.YELLOW + "Permissions being loaded for entity " + getId() );
		
		ConfigurationSection permissions = FileBackend.getBackend().permissions.getConfigurationSection( "entities." + getId() + ".permissions" );
		childPermissions.clear();
		
		if ( permissions != null )
			for ( String ss : permissions.getKeys( false ) )
			{
				ConfigurationSection permission = permissions.getConfigurationSection( ss );
				Permission perm = PermissionManager.INSTANCE.getNode( ss.replaceAll( "/", "." ).toLowerCase(), true );
				
				PermissionValue value = null;
				if ( permission.getString( "value" ) != null )
					value = perm.getModel().createValue( permission.getString( "value" ) );
				
				attachPermission( new ChildPermission( perm, value, -1, permission.getStringList( "refs", new ArrayList<String>() ).toArray( new String[0] ) ) );
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
		if ( isVirtual() )
			return;
		
		if ( isDebug() )
			PermissionManager.getLogger().info( ConsoleColor.YELLOW + "Entity " + getId() + " being saved to backend" );
		
		ConfigurationSection root = FileBackend.getBackend().permissions.getConfigurationSection( "entities." + getId(), true );
		
		if ( getChildPermissions().size() > 0 )
		{
			ConfigurationSection permissions = root.getConfigurationSection( "entities." + getId(), true );
			
			for ( ChildPermission child : getChildPermissions() )
			{
				Permission perm = child.getPermission();
				ConfigurationSection sub = permissions.getConfigurationSection( perm.getNamespace().replaceAll( "\\.", "/" ), true );
				if ( perm.getType() != PermissionType.DEFAULT )
					sub.set( "value", child.getObject() );
				
				sub.set( "refs", child.getReferences().isEmpty() ? null : child.getReferences() );
			}
		}
		
		if ( groups.size() > 0 )
			root.set( "groups", groups.values() );
	}
}
