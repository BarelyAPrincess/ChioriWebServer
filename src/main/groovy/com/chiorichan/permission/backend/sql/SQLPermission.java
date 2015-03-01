/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.permission.backend.sql;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.chiorichan.Loader;
import com.chiorichan.database.DatabaseEngine;
import com.chiorichan.permission.Permission;
import com.chiorichan.permission.PermissionBackend;
import com.chiorichan.permission.PermissionException;
import com.chiorichan.permission.PermissionManager;
import com.chiorichan.permission.PermissionNamespace;
import com.chiorichan.permission.PermissionValue;
import com.chiorichan.permission.PermissionValueBoolean;
import com.chiorichan.permission.PermissionValueEnum;
import com.chiorichan.permission.PermissionValueInt;
import com.chiorichan.permission.PermissionValueVar;
import com.chiorichan.permission.PermissionValue.PermissionType;
import com.chiorichan.util.ObjectUtil;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

public class SQLPermission extends Permission
{
	private static SQLBackend backend;
	
	/**
	 * This method is used internally, please use {@link Permission#getNode} instead.
	 * 
	 * @param result
	 *            The SQL ResultSet from the SQL Database.
	 * @return The freshly created SQLPermission instance.
	 * @throws PermissionException
	 *             Thrown for database and permission problems, duh.
	 */
	protected static SQLPermission initNode( ResultSet result ) throws PermissionException
	{
		try
		{
			PermissionNamespace ns = new PermissionNamespace( result.getString( "permission" ) );
			
			// TODO Remove invalid characters
			if ( !ns.containsOnlyValidChars() )
				throw new PermissionException( "The permission '" + ns.getNamespace() + "' contains invalid characters. Permission namespaces can only contain the characters a-z, 0-9, and _." );
			
			Permission parent = ( ns.getNodeCount() <= 1 ) ? null : Permission.getNode( ns.getParent(), true );
			SQLPermission perm = new SQLPermission( ns.getLocalName(), parent );
			
			switch ( result.getString( "type" ) )
			{
				case "BOOL":
					perm.setValue( new PermissionValueBoolean( ns.getLocalName(), result.getBoolean( "value" ), result.getBoolean( "default" ) ), false );
					break;
				case "ENUM":
					perm.setValue( new PermissionValueEnum( ns.getLocalName(), result.getString( "value" ), result.getString( "default" ), result.getInt( "maxlen" ), Splitter.on( "|" ).splitToList( result.getString( "enum" ) ) ), false );
					break;
				case "VAR":
					perm.setValue( new PermissionValueVar( ns.getLocalName(), result.getString( "value" ), result.getString( "default" ), result.getInt( "maxlen" ) ), false );
					break;
				case "INT":
					perm.setValue( new PermissionValueInt( ns.getLocalName(), result.getInt( "value" ), result.getInt( "value" ) ), false );
					break;
			}
			
			perm.setDescription( result.getString( "description" ), false );
			
			return perm;
		}
		catch ( SQLException e )
		{
			throw new PermissionException( e );
		}
	}
	
	/**
	 * This method is used internally, please use {@link Permission#getNode} instead.
	 * 
	 * @param namespace
	 * @param parent
	 * @return
	 * @throws PermissionException
	 */
	protected static SQLPermission initNode( String namespace, Permission parent ) throws PermissionException
	{
		PermissionNamespace ns = new PermissionNamespace( namespace );
		
		// TODO Remove invalid characters
		if ( !ns.containsOnlyValidChars() )
			throw new PermissionException( "The permission '" + ns.getNamespace() + "' contains invalid characters. Permission namespaces can only contain the characters a-z, 0-9, and _." );
		
		return new SQLPermission( ns.getLocalName(), parent );
	}
	
	private SQLPermission( String localName, Permission parent )
	{
		super( localName, parent );
	}
	
	private SQLPermission( String localName )
	{
		super( localName );
	}
	
	@Override
	public void saveNode()
	{
		if ( !changesMade )
			return;
		
		DatabaseEngine db = getBackend().getSQL();
		
		try
		{
			ResultSet rs = db.query( "SELECT * FROM `permissions` WHERE `permission` = '" + getNamespace() + "';" );
			
			if ( db.getRowCount( rs ) < 1 )
			{
				if ( hasValue() || hasDescription() )
				{
					String enumArray = "";
					int maxLen = 0;
					
					PermissionValue<?> value = getValue();
					
					if ( value.getType() == PermissionType.ENUM )
					{
						enumArray = Joiner.on( "|" ).join( ( ( PermissionValueEnum ) value ).getEnumList() );
						maxLen = ( ( PermissionValueEnum ) value ).getMaxLen();
					}
					
					if ( value.getType() == PermissionType.VAR )
					{
						maxLen = ( ( PermissionValueVar ) value ).getMaxLen();
					}
					
					db.queryUpdate( "INSERT INTO `permissions` (`permission`, `default`, `value`, `type`, `enum`, `maxlen`, `description`) VALUES (?, ?, ?, ?, ?, ?, ?);", getNamespace(), ObjectUtil.castToString( value.getDefault() ), ObjectUtil.castToString( value.getValue() ), value.getType().toString(), enumArray, maxLen, getDescription() );
				}
			}
			else
			{
				if ( db.getRowCount( rs ) > 1 )
					PermissionManager.getLogger().warning( "We found more then one permission node with the namespace '" + getNamespace() + "', please fix this. " + Loader.getRandomGag() );
				
				if ( hasValue() || hasDescription() )
				{
					PermissionValue<?> value = getValue();
					
					if ( hasValue() )
					{
						updateDBValue( "default", ObjectUtil.castToString( value.getDefault() ) );
						updateDBValue( "value", ObjectUtil.castToString( value.getValue() ) );
						updateDBValue( "type", value.getType().toString() );
						
						if ( value.getType() == PermissionType.ENUM )
						{
							updateDBValue( "enum", Joiner.on( "|" ).join( ( ( PermissionValueEnum ) value ).getEnumList() ) );
							updateDBValue( "maxlen", "" + ( ( PermissionValueEnum ) value ).getMaxLen() );
						}
						
						if ( value.getType() == PermissionType.VAR )
							updateDBValue( "maxlen", "" + ( ( PermissionValueVar ) value ).getMaxLen() );
					}
					
					if ( hasDescription() )
						updateDBValue( "description", getDescription() );
				}
				else
				{
					if ( !db.delete( "permissions", "`permission` = '" + getNamespace() + "'", 1 ) )
						PermissionManager.getLogger().warning( "The SQLBackend failed to remove the permission node '" + getNamespace() + "' from the database. " + Loader.getRandomGag() );
				}
			}
		}
		catch ( SQLException e )
		{
			e.printStackTrace();
		}
		
		changesMade = false;
	}
	
	private int updateDBValue( String key, String val ) throws SQLException
	{
		DatabaseEngine db = getBackend().getSQL();
		
		if ( key == null )
			return 0;
		
		if ( val == null )
			val = "";
		
		return db.queryUpdate( "UPDATE `permissions` SET `" + key + "` = ? WHERE `permission` = ?;", val, getNamespace() );
	}
	
	@Override
	public void reloadNode()
	{
		DatabaseEngine db = getBackend().getSQL();
		
		try
		{
			ResultSet rs = db.query( "SELECT * FROM `permissions` WHERE `permission` = '" + getNamespace() + "';" );
			
			if ( db.getRowCount( rs ) > 0 )
			{
				// TODO RELOAD!
			}
		}
		catch ( SQLException e )
		{
			e.printStackTrace();
		}
		
		changesMade = false;
	}
	
	@Override
	public void destroyNode()
	{
		// TODO Find reason why if would be unsafe to destroy this node! Children nodes maybe?
	}
	
	public SQLBackend getBackend()
	{
		if ( backend == null )
		{
			if ( Loader.getPermissionManager().getBackend() instanceof SQLBackend )
				backend = ( SQLBackend ) Loader.getPermissionManager().getBackend();
			else
				backend = ( SQLBackend ) PermissionBackend.getBackend( "sql" );
		}
		
		if ( backend == null )
			throw new RuntimeException( "The permissions system seems to be misbehaving, we tried to get the SQLBackend and failed! " + Loader.getRandomGag() );
		
		return backend;
	}
}
