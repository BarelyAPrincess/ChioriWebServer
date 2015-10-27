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
import java.util.Map;
import java.util.Map.Entry;

import com.chiorichan.LogColor;
import com.chiorichan.datastore.sql.bases.SQLDatastore;
import com.chiorichan.datastore.sql.query.SQLQuerySelect;
import com.chiorichan.permission.ChildPermission;
import com.chiorichan.permission.PermissibleEntity;
import com.chiorichan.permission.PermissibleGroup;
import com.chiorichan.permission.Permission;
import com.chiorichan.permission.PermissionManager;
import com.chiorichan.permission.PermissionValue;
import com.chiorichan.permission.References;
import com.chiorichan.util.PermissionNamespace;

public class SQLEntity extends PermissibleEntity
{
	public SQLEntity( String id )
	{
		super( id );
	}
	
	@Override
	public void reloadGroups()
	{
		SQLDatastore db = SQLBackend.getBackend().getSQL();
		
		clearGroups();
		try
		{
			ResultSet rs = db.table( "permissions_groups" ).select().where( "parent" ).matches( getId() ).and().where( "type" ).matches( "0" ).execute().result();
			
			if ( rs != null && rs.first() )
				do
				{
					PermissibleGroup grp = PermissionManager.INSTANCE.getGroup( rs.getString( "child" ) );
					addGroup( grp, References.format( rs.getString( "refs" ) ) );
				}
				while ( rs.next() );
			
			if ( rs != null )
				rs.close();
		}
		catch ( SQLException e )
		{
			throw new RuntimeException( e );
		}
	}
	
	@Override
	public void reloadPermissions()
	{
		SQLDatastore db = SQLBackend.getBackend().getSQL();
		
		clearPermissions();
		clearTimedPermissions();
		try
		{
			SQLQuerySelect select = db.table( "permissions_entity" ).select().where( "owner" ).matches( getId() ).and().where( "type" ).matches( "0" ).execute();
			
			if ( select.rowCount() > 0 )
				for ( Map<String, String> row : select.stringSet() )
				{
					PermissionNamespace ns = new PermissionNamespace( row.get( "permission" ) );
					
					if ( !ns.containsOnlyValidChars() )
					{
						PermissionManager.getLogger().warning( "We failed to add the permission %s to entity %s because it contained invalid characters, namespaces can only contain 0-9, a-z and _." );
						continue;
					}
					
					Collection<Permission> perms = ns.containsRegex() ? PermissionManager.INSTANCE.getNodes( ns ) : Arrays.asList( new Permission[] {ns.createPermission()} );
					
					for ( Permission perm : perms )
					{
						PermissionValue value = null;
						if ( row.get( "value" ) != null )
							value = perm.getModel().createValue( row.get( "value" ) );
						
						addPermission( new ChildPermission( this, perm, value, -1 ), References.format( row.get( "refs" ) ) );
					}
				}
			
			select.close();
		}
		catch ( SQLException e )
		{
			throw new RuntimeException( e );
		}
	}
	
	@Override
	public void remove()
	{
		SQLDatastore db = SQLBackend.getBackend().getSQL();
		try
		{
			// db.queryUpdate( String.format( "DELETE FROM `permissions_entity` WHERE `owner` = '%s' AND `type` = '0';", getId() ) );
			// db.queryUpdate( String.format( "DELETE FROM `permissions_groups` WHERE `parent` = '%s' AND `type` = '0';", getId() ) );
			
			db.table( "permissions_entity" ).delete().where( "owner" ).matches( getId() ).and().where( "type" ).matches( "0" ).execute();
			db.table( "permissions_groups" ).delete().where( "parent" ).matches( getId() ).and().where( "type" ).matches( "0" ).execute();
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
			PermissionManager.getLogger().info( LogColor.YELLOW + "Entity " + getId() + " being saved to backend" );
		
		try
		{
			SQLDatastore db = SQLBackend.getBackend().getSQL();
			remove();
			
			Collection<ChildPermission> children = getChildPermissions( null );
			for ( ChildPermission child : children )
			{
				Permission perm = child.getPermission();
				// db.queryUpdate( String.format( "INSERT INTO `permissions_entity` (`owner`,`type`,`refs`,`permission`,`value`) VALUES ('%s','0','%s','%s','%s');", getId(), child.getReferences().join(), perm.getNamespace(),
				// child.getObject() ) );
				db.table( "permissions_entity" ).insert().value( "owner", getId() ).value( "type", 0 ).value( "refs", child.getReferences().join() ).value( "permission", perm.getNamespace() ).value( "value", child.getObject() ).execute();
			}
			
			Collection<Entry<PermissibleGroup, References>> groups = getGroupEntrys( null );
			for ( Entry<PermissibleGroup, References> entry : groups )
				db.table( "permissions_groups" ).insert().value( "child", entry.getKey().getId() ).value( "parent", getId() ).value( "type", 0 ).value( "refs", entry.getValue().join() ).execute();
			// db.queryUpdate( String.format( "INSERT INTO `permissions_groups` (`child`, `parent`, `type`, `refs`) VALUES ('%s', '%s', '0', '%s');", entry.getKey().getId(), getId(), entry.getValue().join() ) );
		}
		catch ( SQLException e )
		{
			throw new RuntimeException( e );
		}
	}
}
