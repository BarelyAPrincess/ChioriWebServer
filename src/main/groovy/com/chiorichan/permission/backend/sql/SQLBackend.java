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
import java.util.Collections;
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
import com.chiorichan.permission.PermissionBackendException;
import com.chiorichan.permission.PermissionException;
import com.chiorichan.permission.PermissionManager;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class SQLBackend extends PermissionBackend
{
	public SQLBackend()
	{
		super();
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
	
	public DatabaseEngine getSQL()
	{
		return Loader.getDatabase();
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
	public Set<String> getEntityNames( int type )
	{
		try
		{
			Set<String> entities = Sets.newHashSet();
			
			ResultSet result = getSQL().query( "SELECT * FROM `permissions_entity` WHERE `type` = " + type + ";" );
			
			if ( !result.next() )
				return Sets.newHashSet();
			
			do
			{
				entities.add( result.getString( "owner" ) );
			}
			while ( result.next() );
			
			return entities;
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
		{
			entities.add( getEntity( entityName ) );
		}
		
		return entities.toArray( new PermissibleEntity[0] );
	}
	
	@Override
	public PermissibleGroup[] getGroups()
	{
		Set<String> groupNames = getEntityNames( GROUP );
		List<PermissibleGroup> groups = Lists.newArrayList();
		
		for ( String groupName : groupNames )
		{
			groups.add( getGroup( groupName ) );
		}
		
		Collections.sort( groups );
		
		return groups.toArray( new PermissibleGroup[0] );
	}
	
	@Override
	public void reload()
	{
		
	}
	
	@Override
	public void loadPermissionTree()
	{
		try
		{
			ResultSet result = getSQL().query( "SELECT * FROM `permissions`" );
			
			if ( result.next() )
				do
				{
					try
					{
						SQLPermission.initNode( result );
					}
					catch ( PermissionException e )
					{
						PermissionManager.getLogger().warning( e.getMessage() );
					}
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
	public Permission createNode( String namespace ) throws PermissionException
	{
		return SQLPermission.initNode( namespace, null );
	}
	
	@Override
	public Permission createNode( String namespace, Permission parent ) throws PermissionException
	{
		return SQLPermission.initNode( namespace, parent );
	}
	
	@Override
	public PermissibleEntity getEntity( String id )
	{
		return new SQLEntity( id, this );
	}
	
	@Override
	public PermissibleGroup getGroup( String id )
	{
		return new SQLGroup( id, this );
	}
}
