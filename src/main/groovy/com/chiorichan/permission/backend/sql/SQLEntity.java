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
import java.util.List;

import com.chiorichan.database.DatabaseEngine;
import com.chiorichan.permission.ChildPermission;
import com.chiorichan.permission.PermissibleEntity;
import com.chiorichan.permission.PermissibleGroup;
import com.chiorichan.permission.Permission;
import com.chiorichan.permission.PermissionDefault;
import com.chiorichan.permission.PermissionManager;
import com.chiorichan.permission.PermissionNamespace;
import com.chiorichan.permission.PermissionValue;
import com.chiorichan.util.ObjectFunc;
import com.google.common.base.Joiner;

public class SQLEntity extends PermissibleEntity
{
	public SQLEntity( String id )
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
			ResultSet rs = db.query( "SELECT * FROM `permissions_groups` WHERE `parent` = '" + getId() + "' AND `type` = '0';" );
			
			if ( rs.next() )
				do
				{
					PermissibleGroup grp = PermissionManager.INSTANCE.getGroup( rs.getString( "child" ) );
					groups.put( grp.getId(), grp );
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
		
		detachAllPermissions();
		try
		{
			ResultSet rs = db.query( "SELECT * FROM `permissions_entity` WHERE `owner` = '" + getId() + "' AND `type` = '0';" );
			
			if ( rs.next() )
				do
				{
					PermissionNamespace ns = new PermissionNamespace( rs.getString( "permission" ) );
					
					List<Permission> perms = PermissionManager.INSTANCE.getNodes( ns );
					
					if ( perms.isEmpty() && !ns.containsRegex() )
						perms.add( PermissionManager.INSTANCE.getNode( ns.fixInvalidChars().getNamespace() ) );
					
					for ( Permission perm : perms )
						if ( getChildPermission( perm ) == null )
						{
							PermissionValue childValue = ( rs.getString( "value" ) == null || rs.getString( "value" ).isEmpty() ) ? null : perm.getModel().createValue( rs.getString( "value" ) );
							attachPermission( new ChildPermission( perm, childValue, false, rs.getString( "ref" ).split( "|" ) ) );
						}
				}
				while ( rs.next() );
			
			/*
			 * Adds the EVERYBODY Permission Node to all entities.
			 */
			Permission perm = PermissionDefault.EVERYBODY.getNode();
			attachPermission( new ChildPermission( perm, null, false, "" ) );
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
			db.queryUpdate( String.format( "DELETE FROM `permissions_entity` WHERE `owner` = '%s' AND `type` = '0';", getId() ) );
		}
		catch ( SQLException e )
		{
			throw new RuntimeException( e );
		}
	}
	
	@Override
	public void save()
	{
		DatabaseEngine db = SQLBackend.getBackend().getSQL();
		try
		{
			remove();
			for ( ChildPermission cp : getChildPermissions() )
				db.queryUpdate( String.format( "INSERT INTO `permissions_entity` (`owner`,`type`,`ref`,`permission`,`value`) VALUES ('%s','0','%s','%s','%s');", getId(), Joiner.on( "|" ).join( cp.getReferences() ), cp.getPermission().getNamespace(), ObjectFunc.castToString( cp.getValue().getValue() ) ) );
		}
		catch ( SQLException e )
		{
			throw new RuntimeException( e );
		}
	}
}
