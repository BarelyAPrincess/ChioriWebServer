/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.permission.backend.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map.Entry;

import com.chiorichan.ConsoleColor;
import com.chiorichan.database.DatabaseEngine;
import com.chiorichan.permission.ChildPermission;
import com.chiorichan.permission.PermissibleGroup;
import com.chiorichan.permission.Permission;
import com.chiorichan.permission.PermissionManager;
import com.chiorichan.permission.PermissionValue;
import com.chiorichan.permission.References;
import com.chiorichan.util.Namespace;

public class SQLGroup extends PermissibleGroup
{
	public SQLGroup( String id )
	{
		super( id );
	}
	
	@Override
	public void reloadGroups()
	{
		DatabaseEngine db = SQLBackend.getBackend().getSQL();
		
		clearGroups();
		try
		{
			ResultSet rs = db.query( "SELECT * FROM `permissions_groups` WHERE `parent` = '" + getId() + "' AND `type` = '1';" );
			
			if ( rs.next() )
				do
				{
					PermissibleGroup grp = PermissionManager.INSTANCE.getGroup( rs.getString( "child" ) );
					addGroup( grp, References.format( rs.getString( "refs" ) ) );
				}
				while ( rs.next() );
		}
		catch ( SQLException e )
		{
			throw new RuntimeException( e );
		}
	}
	
	@Override
	public void reloadPermissions()
	{
		DatabaseEngine db = SQLBackend.getBackend().getSQL();
		
		clearPermissions();
		clearTimedPermissions();
		try
		{
			ResultSet rs = db.query( "SELECT * FROM `permissions_entity` WHERE `owner` = '" + getId() + "' AND `type` = '1';" );
			
			if ( rs.next() )
				do
				{
					Namespace ns = new Namespace( rs.getString( "permission" ) );
					
					if ( !ns.containsOnlyValidChars() )
					{
						PermissionManager.getLogger().warning( "We failed to add the permission %s to entity %s because it contained invalid characters, namespaces can only contain 0-9, a-z and _." );
						continue;
					}
					
					Collection<Permission> perms = ns.containsRegex() ? PermissionManager.INSTANCE.getNodes( ns ) : Arrays.asList( new Permission[] {PermissionManager.INSTANCE.getNode( ns, true )} );
					
					for ( Permission perm : perms )
					{
						PermissionValue value = null;
						if ( rs.getString( "value" ) != null )
							value = perm.getModel().createValue( rs.getString( "value" ) );
						
						addPermission( new ChildPermission( this, perm, value, getWeight() ), References.format( rs.getString( "refs" ) ) );
					}
				}
				while ( rs.next() );
		}
		catch ( SQLException e )
		{
			throw new RuntimeException( e );
		}
	}
	
	@Override
	public void remove()
	{
		DatabaseEngine db = SQLBackend.getBackend().getSQL();
		try
		{
			db.queryUpdate( String.format( "DELETE FROM `permissions_entity` WHERE `owner` = '%s' AND `type` = '1';", getId() ) );
		}
		catch ( SQLException e )
		{
			throw new RuntimeException( e );
		}
	}
	
	@Override
	public void save()
	{
		if ( isVirtual() )
			return;
		
		if ( isDebug() )
			PermissionManager.getLogger().info( ConsoleColor.YELLOW + "Group " + getId() + " being saved to backend" );
		
		try
		{
			DatabaseEngine db = SQLBackend.getBackend().getSQL();
			remove();
			
			Collection<ChildPermission> children = getChildPermissions( null );
			for ( ChildPermission child : children )
			{
				Permission perm = child.getPermission();
				db.queryUpdate( String.format( "INSERT INTO `permissions_entity` (`owner`,`type`,`refs`,`permission`,`value`) VALUES ('%s','1','%s','%s','%s');", getId(), child.getReferences().join(), perm.getNamespace(), child.getObject() ) );
			}
			
			Collection<Entry<PermissibleGroup, References>> groups = getGroupEntrys( null );
			for ( Entry<PermissibleGroup, References> entry : groups )
				db.queryUpdate( String.format( "INSERT INTO `permissions_groups` (`child`, `parent`, `type`, `refs`) VALUES ('', '', '1', '');", entry.getKey().getId(), getId(), entry.getValue().join() ) );
		}
		catch ( SQLException e )
		{
			throw new RuntimeException( e );
		}
	}
}
