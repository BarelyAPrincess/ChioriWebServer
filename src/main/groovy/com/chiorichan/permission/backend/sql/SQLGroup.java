/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.permission.backend.sql;

import java.sql.SQLException;

import com.chiorichan.database.DatabaseEngine;
import com.chiorichan.permission.ChildPermission;
import com.chiorichan.permission.PermissibleGroup;
import com.chiorichan.util.ObjectFunc;
import com.google.common.base.Joiner;

public class SQLGroup extends PermissibleGroup
{
	public SQLGroup( String id )
	{
		super( id );
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
		DatabaseEngine db = SQLBackend.getBackend().getSQL();
		try
		{
			remove();
			for ( ChildPermission cp : getChildPermissions() )
				db.queryUpdate( String.format( "INSERT INTO `permissions_entity` (`owner`,`type`,`ref`,`permission`,`value`) VALUES ('%s','1','%s','%s','%s');", getId(), Joiner.on( "|" ).join( cp.getReferences() ), cp.getPermission().getNamespace(), ObjectFunc.castToString( cp.getValue().getValue() ) ) );
		}
		catch ( SQLException e )
		{
			throw new RuntimeException( e );
		}
	}
}
