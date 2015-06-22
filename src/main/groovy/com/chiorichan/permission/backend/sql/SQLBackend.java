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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.chiorichan.Loader;
import com.chiorichan.database.DatabaseEngine;
import com.chiorichan.permission.PermissibleEntity;
import com.chiorichan.permission.PermissibleGroup;
import com.chiorichan.permission.Permission;
import com.chiorichan.permission.PermissionBackend;
import com.chiorichan.permission.PermissionManager;
import com.chiorichan.permission.PermissionModelValue;
import com.chiorichan.permission.PermissionNamespace;
import com.chiorichan.permission.PermissionType;
import com.chiorichan.permission.lang.PermissionBackendException;
import com.chiorichan.permission.lang.PermissionException;
import com.chiorichan.permission.lang.PermissionValueException;
import com.chiorichan.util.ObjectFunc;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Provides the SQL Permission Backend
 */
public class SQLBackend extends PermissionBackend
{
	public SQLBackend()
	{
		super();
	}
	
	@Override
	public PermissibleGroup getDefaultGroup( String ref )
	{
		try
		{
			Map<String, String> defaults = Maps.newHashMap();
			
			ResultSet result = getSQL().query( "SELECT * FROM `permissions_groups` WHERE `parent` = 'default' AND `type` = '1';" );
			
			if ( !result.next() )
				throw new RuntimeException( "There is no default group set. New entities will not have any groups." );
			
			do
			{
				String refs = result.getString( "ref" );
				if ( refs == null || refs.isEmpty() )
					defaults.put( "", result.getString( "child" ) );
				else
					for ( String r : refs.split( "|" ) )
						defaults.put( r.toLowerCase(), result.getString( "child" ) );
			}
			while ( result.next() );
			
			if ( defaults.isEmpty() )
				throw new RuntimeException( "There is no default group set. New entities will not have any groups." );
			
			return getGroup( ( ref == null || ref.isEmpty() ) ? defaults.get( "" ) : defaults.get( ref.toLowerCase() ) );
		}
		catch ( SQLException e )
		{
			throw new RuntimeException( e );
		}
	}
	
	@Override
	public PermissibleEntity[] getEntities()
	{
		Set<String> entityNames = getEntityNames( ENTITY );
		List<PermissibleEntity> entities = Lists.newArrayList();
		
		for ( String entityName : entityNames )
			entities.add( getEntity( entityName ) );
		
		return entities.toArray( new PermissibleEntity[0] );
	}
	
	@Override
	public PermissibleEntity getEntity( String id )
	{
		return new SQLEntity( id, this );
	}
	
	@Override
	public Set<String> getEntityNames( int type )
	{
		try
		{
			Set<String> entities = Sets.newHashSet();
			
			ResultSet result = getSQL().query( "SELECT * FROM `permissions_entity` WHERE `type` = " + type + ";" );
			
			if ( !result.next() )
				return Sets.newHashSet();
			
			do
				entities.add( result.getString( "owner" ) );
			while ( result.next() );
			
			return entities;
		}
		catch ( SQLException e )
		{
			throw new RuntimeException( e );
		}
	}
	
	@Override
	public PermissibleGroup getGroup( String id )
	{
		return new SQLGroup( id, this );
	}
	
	@Override
	public PermissibleGroup[] getGroups()
	{
		Set<String> groupNames = getEntityNames( GROUP );
		List<PermissibleGroup> groups = Lists.newArrayList();
		
		for ( String groupName : groupNames )
			groups.add( getGroup( groupName ) );
		
		Collections.sort( groups );
		
		return groups.toArray( new PermissibleGroup[0] );
	}
	
	public DatabaseEngine getSQL()
	{
		return Loader.getDatabase();
	}
	
	@Override
	public void initialize() throws PermissionBackendException
	{
		DatabaseEngine db = Loader.getDatabase();
		
		if ( db == null )
			throw new PermissionBackendException( "SQL connection is not configured, see config.yml" );
		
		Set<String> missingTables = Sets.newHashSet();
		
		if ( !db.tableExist( "permissions" ) )
			missingTables.add( "permissions" );
		
		if ( !db.tableExist( "permissions_entity" ) )
			missingTables.add( "permissions_entity" );
		
		if ( !db.tableExist( "permissions_groups" ) )
			missingTables.add( "permissions_groups" );
		
		if ( !missingTables.isEmpty() )
			throw new PermissionBackendException( "SQL connection is configured but your missing tables: " + Joiner.on( "," ).join( missingTables ) + ", check the SQL Backend getting started guide for help." );
		
		// TODO Create these tables.
		
		PermissionManager.getLogger().info( "Successfully initalized SQL Backend!" );
	}
	
	@Override
	public void loadPermissionTree()
	{
		try
		{
			ResultSet result = getSQL().query( "SELECT * FROM `permissions`" );
			
			if ( result.next() )
				do
					try
					{
						nodeCreate( result );
					}
					catch ( PermissionException e )
					{
						PermissionManager.getLogger().warning( e.getMessage() );
					}
				while ( result.next() );
		}
		catch ( SQLException e )
		{
			/*
			 * TODO Do something if columns don't exist.
			 * Caused by: java.sql.SQLException: Column 'permission' not found.
			 */
			throw new RuntimeException( e );
		}
	}
	
	@Override
	public void nodeCommit( Permission perm )
	{
		try
		{
			DatabaseEngine db = getSQL();
			
			if ( !PermissionManager.isInitialized() )
			{
				Loader.getLogger().warning( "There was an attempt to commit() changes made to a permission node before we finished loading." );
				return;
			}
			
			PermissionModelValue model = perm.getModel();
			
			ResultSet rs = db.query( "SELECT * FROM `permissions` WHERE `permission` = '" + perm.getNamespace() + "';" );
			
			if ( db.getRowCount( rs ) < 1 )
			{
				if ( perm.getType() != PermissionType.DEFAULT )
					db.queryUpdate( "INSERT INTO `permissions` (`permission`, `value`, `default`, `type`, `enum`, `maxlen`, `description`) VALUES (?, ?, ?, ?, ?, ?, ?);", perm.getNamespace(), model.getValue(), model.getValueDefault(), perm.getType().name(), model.getEnumsString(), model.getMaxLen(), model.getDescription() );
			}
			else
			{
				if ( db.getRowCount( rs ) > 1 )
					PermissionManager.getLogger().warning( String.format( "We found more then one permission node with the namespace '%s', please fix this, or you might experience unexpected behavior. %s", perm.getNamespace(), Loader.getRandomGag() ) );
				
				if ( perm.getType() == PermissionType.DEFAULT && !db.delete( "permissions", String.format( "`permission` = '%s'", perm.getNamespace() ), 1 ) )
					PermissionManager.getLogger().warning( "The SQLBackend failed to remove the permission node '" + perm.getNamespace() + "' from the database. " + Loader.getRandomGag() );
				else
				{
					updateDBValue( perm, "type", perm.getType().name() );
					
					if ( perm.getType() != PermissionType.DEFAULT )
					{
						updateDBValue( perm, "value", model.getValue() );
						updateDBValue( perm, "default", model.getValueDefault() );
					}
					
					if ( perm.getType().hasMax() )
						updateDBValue( perm, "max", model.getMaxLen() );
					
					if ( perm.getType().hasMin() )
						updateDBValue( perm, "min", 0 );
					
					if ( perm.getType() == PermissionType.ENUM )
						updateDBValue( perm, "enum", model.getEnumsString() );
					
					if ( model.hasDescription() )
						updateDBValue( perm, "description", model.getDescription() );
				}
			}
		}
		catch ( SQLException e )
		{
			e.printStackTrace();
		}
	}
	
	private Permission nodeCreate( ResultSet result ) throws SQLException, PermissionException
	{
		PermissionNamespace ns = new PermissionNamespace( result.getString( "permission" ) );
		
		// TODO Remove invalid characters
		if ( !ns.containsOnlyValidChars() )
			throw new PermissionException( "The permission '" + ns.getNamespace() + "' contains invalid characters. Permission namespaces can only contain the characters a-z, 0-9, and _." );
		
		Permission parent = ( ns.getNodeCount() <= 1 ) ? null : PermissionManager.INSTANCE.getNode( ns.getParent(), true );
		Permission perm = new Permission( ns.getLocalName(), PermissionType.valueOf( result.getString( "type" ) ), parent );
		
		PermissionModelValue model = perm.getModel();
		
		if ( result.getObject( "value" ) != null )
			model.setValue( result.getObject( "value" ), false );
		
		if ( result.getObject( "default" ) != null )
			model.setValueDefault( result.getObject( "default" ), false );
		
		if ( perm.getType().hasMax() )
			model.setMaxLen( Math.min( result.getInt( "max" ), perm.getType().maxValue() ) );
		
		if ( perm.getType() == PermissionType.ENUM )
			model.setEnums( new HashSet<String>( Splitter.on( "|" ).splitToList( result.getString( "enum" ) ) ) );
		
		model.setDescription( result.getString( "description" ), false );
		
		return perm;
	}
	
	@Override
	public void nodeDestroy( Permission perm )
	{
		
	}
	
	@Override
	public void nodeReload( Permission perm )
	{
		DatabaseEngine db = getSQL();
		
		try
		{
			ResultSet rs = db.query( "SELECT * FROM `permissions` WHERE `permission` = '" + perm.getNamespace() + "';" );
			
			if ( db.getRowCount( rs ) > 0 )
			{
				// TODO RELOAD!
			}
		}
		catch ( SQLException e )
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public void reload() throws PermissionBackendException
	{
		
	}
	
	@Override
	public void setDefaultGroup( String child, String... ref )
	{
		try
		{
			Map<String, String> defaults = Maps.newHashMap();
			Set<String> children = Sets.newHashSet();
			
			ResultSet result = getSQL().query( "SELECT * FROM `permissions_groups` WHERE `parent` = 'default' AND `type` = '1';" );
			
			// throw new RuntimeException( "There is no default group set. New entities will not have any groups." );
			if ( result.next() )
				do
				{
					String refs = result.getString( "ref" );
					if ( refs == null || refs.isEmpty() )
						defaults.put( "", result.getString( "child" ) );
					else
						for ( String r : refs.split( "|" ) )
							defaults.put( r.toLowerCase(), result.getString( "child" ) );
				}
				while ( result.next() );
			
			// Update defaults
			for ( String s : ref )
				defaults.put( s.toLowerCase(), child );
			
			// Remove duplicate children
			for ( Entry<String, String> e : defaults.entrySet() )
				if ( !children.contains( e.getKey() ) )
					children.add( e.getKey() );
			
			// Delete old records
			getSQL().queryUpdate( "DELETE FROM `permissions_groups` WHERE `parent` = 'default' AND `type` = '1';" );
			
			// Save changes
			for ( String c : children )
			{
				String refs = "";
				for ( Entry<String, String> e : defaults.entrySet() )
					if ( e.getKey() == c )
						refs += "|" + e.getValue();
				
				if ( refs.length() > 0 )
					refs = refs.substring( 1 );
				
				getSQL().queryUpdate( "INSERT INTO `permissions_group` (`child`, `parent`, `type`, `ref`) VALUES ('" + c + "', 'default', '1', '" + refs + "');" );
			}
		}
		catch ( SQLException e )
		{
			throw new RuntimeException( e );
		}
	}
	
	private int updateDBValue( Permission perm, String key, Object val ) throws SQLException, PermissionValueException
	{
		try
		{
			return updateDBValue( perm, key, ObjectFunc.castToStringWithException( val ) );
		}
		catch ( ClassCastException e )
		{
			throw new PermissionValueException( "We could not cast the Object %s for key %s.", val.getClass().getName(), key );
		}
	}
	
	private int updateDBValue( Permission perm, String key, String val ) throws SQLException
	{
		DatabaseEngine db = getSQL();
		
		if ( key == null )
			return 0;
		
		if ( val == null )
			val = "";
		
		return db.queryUpdate( "UPDATE `permissions` SET `" + key + "` = ? WHERE `permission` = ?;", val, perm.getNamespace() );
	}
}
