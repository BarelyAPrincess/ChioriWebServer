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
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.chiorichan.Loader;
import com.chiorichan.datastore.sql.SQLExecute;
import com.chiorichan.datastore.sql.bases.SQLDatastore;
import com.chiorichan.datastore.sql.query.SQLQuerySelect;
import com.chiorichan.permission.PermissibleEntity;
import com.chiorichan.permission.PermissibleGroup;
import com.chiorichan.permission.Permission;
import com.chiorichan.permission.PermissionBackend;
import com.chiorichan.permission.PermissionDefault;
import com.chiorichan.permission.PermissionManager;
import com.chiorichan.permission.PermissionModelValue;
import com.chiorichan.permission.PermissionType;
import com.chiorichan.permission.References;
import com.chiorichan.permission.lang.PermissionBackendException;
import com.chiorichan.permission.lang.PermissionException;
import com.chiorichan.permission.lang.PermissionValueException;
import com.chiorichan.util.Namespace;
import com.chiorichan.util.ObjectFunc;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Provides the SQL Permission Backend
 */
public class SQLBackend extends PermissionBackend
{
	private static SQLBackend backend;
	
	public SQLBackend()
	{
		super();
		backend = this;
	}
	
	public static SQLBackend getBackend()
	{
		return backend;
	}
	
	@Override
	public void commit()
	{
		// Nothing to do here!
	}
	
	@Override
	public PermissibleGroup getDefaultGroup( References refs )
	{
		try
		{
			Map<References, String> defaults = Maps.newHashMap();
			
			SQLExecute<SQLQuerySelect> result = getSQL().table( "permissions_groups" ).select().where( "parent" ).matches( "default" ).and().where( "type" ).matches( 1 ).execute().result();
			// ResultSet result = getSQL().query( "SELECT * FROM `permissions_groups` WHERE `parent` = 'default' AND `type` = '1';" );
			
			if ( result.rowCount() < 1 )
				throw new RuntimeException( "There is no default group set. New entities will not have any groups." );
			
			do
			{
				References ref = References.format( result.getString( "ref" ) );
				if ( ref.isEmpty() )
					defaults.put( References.format( "" ), result.getString( "child" ) );
				else
					defaults.put( ref, result.getString( "child" ) );
			}
			while ( result.next() );
			
			if ( defaults.isEmpty() )
				throw new RuntimeException( "There is no default group set. New entities will not have any groups." );
			
			return getGroup( ( refs == null || refs.isEmpty() ) ? defaults.get( "" ) : defaults.get( refs ) );
		}
		catch ( SQLException e )
		{
			throw new RuntimeException( e );
		}
	}
	
	@Override
	public PermissibleEntity getEntity( String id )
	{
		return new SQLEntity( id );
	}
	
	@Override
	public Collection<String> getEntityNames()
	{
		return getEntityNames( 0 );
	}
	
	@Override
	public Collection<String> getEntityNames( int type )
	{
		try
		{
			Set<String> entities = Sets.newHashSet();
			
			SQLExecute<SQLQuerySelect> select = getSQL().table( "permissions_entity" ).select().where( "type" ).matches( type ).execute().result();
			// ResultSet result = getSQL().query( "SELECT * FROM `permissions_entity` WHERE `type` = " + type + ";" );
			
			for ( Map<String, String> row : select.stringSet() )
				entities.add( row.get( "owner" ) );
			
			// while ( result.next() )
			// entities.add( result.getString( "owner" ) );
			
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
		return new SQLGroup( id );
	}
	
	@Override
	public Collection<String> getGroupNames()
	{
		return getEntityNames( 1 );
	}
	
	public SQLDatastore getSQL()
	{
		return Loader.getDatabase();
	}
	
	@Override
	public void initialize() throws PermissionBackendException
	{
		SQLDatastore db = getSQL();
		
		if ( db == null )
			throw new PermissionBackendException( "SQL connection is not configured, see config.yml" );
		
		Set<String> missingTables = Sets.newHashSet();
		
		try
		{
			if ( !db.table( "permissions" ).exists() )
				missingTables.add( "permissions" );
			
			if ( !db.table( "permissions_entity" ).exists() )
				missingTables.add( "permissions_entity" );
			
			if ( !db.table( "permissions_groups" ).exists() )
				missingTables.add( "permissions_groups" );
		}
		catch ( SQLException e )
		{
			// Ignore
		}
		
		if ( !missingTables.isEmpty() )
			throw new PermissionBackendException( "SQL connection is configured but your missing tables: " + Joiner.on( "," ).join( missingTables ) + ", check the SQL Backend getting started guide for help." );
		
		// TODO Create these tables.
		
		PermissionManager.getLogger().info( "Successfully initalized SQL Backend!" );
	}
	
	@Override
	public void loadEntities()
	{
		// TODO
	}
	
	@Override
	public void loadGroups()
	{
		// TODO
	}
	
	@Override
	public void loadPermissions()
	{
		try
		{
			ResultSet result = getSQL().table( "permissions" ).select().execute().result();
			// ResultSet result = getSQL().query( "SELECT * FROM `permissions`" );
			
			if ( result.next() )
				do
					try
					{
						Namespace ns = new Namespace( result.getString( "permission" ) );
						
						if ( !ns.containsOnlyValidChars() )
						{
							PermissionManager.getLogger().warning( String.format( "The permission '%s' contains invalid characters, namespaces can only contain the characters a-z, 0-9, and _, this will be fixed automatically.", ns ) );
							ns.fixInvalidChars();
							this.updateDBValue( ns, "permission", ns.getNamespace() );
						}
						
						Permission perm = new Permission( ns, PermissionType.valueOf( result.getString( "type" ) ) );
						
						PermissionModelValue model = perm.getModel();
						
						if ( result.getObject( "value" ) != null )
							model.setValue( result.getObject( "value" ) );
						
						if ( result.getObject( "default" ) != null )
							model.setValueDefault( result.getObject( "default" ) );
						
						if ( perm.getType().hasMax() )
							model.setMaxLen( Math.min( result.getInt( "max" ), perm.getType().maxValue() ) );
						
						if ( perm.getType() == PermissionType.ENUM )
							model.setEnums( new HashSet<String>( Splitter.on( "|" ).splitToList( result.getString( "enum" ) ) ) );
						
						model.setDescription( result.getString( "description" ) );
					}
					catch ( PermissionException e )
					{
						PermissionManager.getLogger().warning( e.getMessage() );
					}
				while ( result.next() );
			
			result.close();
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
			SQLDatastore db = getSQL();
			
			PermissionModelValue model = perm.getModel();
			
			SQLQuerySelect select = db.table( "permissions" ).select().where( "permission" ).matches( perm.getNamespace() ).execute();
			// ResultSet rs = db.query( "SELECT * FROM `permissions` WHERE `permission` = '" + perm.getNamespace() + "';" );
			
			if ( select.rowCount() < 1 )
			{
				if ( !PermissionDefault.isDefault( perm ) && ( perm.getType() != PermissionType.DEFAULT || !perm.hasChildren() || perm.getModel().hasDescription() ) )
					db.table( "permissions" ).insert().values( new String[] {"permission", "value", "default", "type", "enum", "maxlen", "description"}, new Object[] {perm.getNamespace(), model.getValue(), model.getValueDefault(), perm.getType().name(), model.getEnumsString(), model.getMaxLen(), model.getDescription()} ).execute();
				// db.queryUpdate( "INSERT INTO `permissions` (`permission`, `value`, `default`, `type`, `enum`, `maxlen`, `description`) VALUES (?, ?, ?, ?, ?, ?, ?);", );
			}
			else
			{
				if ( select.rowCount() > 1 )
					PermissionManager.getLogger().warning( String.format( "We found more then one permission node with the namespace '%s', please fix this, or you might experience unexpected behavior. %s", perm.getNamespace(), Loader.getRandomGag() ) );
				
				if ( perm.getType() == PermissionType.DEFAULT && db.table( "permissions" ).delete().where( "permission" ).matches( perm.getNamespace() ).limit( 1 ).execute().rowCount() < 0 )
					// !db.delete( "permissions", String.format( "`permission` = '%s'", perm.getNamespace() ), 1 ) )
					PermissionManager.getLogger().warning( "The SQLBackend failed to remove the permission node '" + perm.getNamespace() + "' from the database. " + Loader.getRandomGag() );
				else
				{
					Namespace ns = perm.getPermissionNamespace();
					
					updateDBValue( ns, "type", perm.getType().name() );
					
					if ( perm.getType() != PermissionType.DEFAULT )
					{
						updateDBValue( ns, "value", model.getValue() );
						updateDBValue( ns, "default", model.getValueDefault() );
					}
					
					if ( perm.getType().hasMax() )
						updateDBValue( ns, "maxlen", model.getMaxLen() );
					
					if ( perm.getType().hasMin() )
						updateDBValue( ns, "min", 0 );
					
					if ( perm.getType() == PermissionType.ENUM )
						updateDBValue( ns, "enum", model.getEnumsString() );
					
					if ( model.hasDescription() )
						updateDBValue( ns, "description", model.getDescription() );
				}
			}
		}
		catch ( SQLException e )
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public void nodeDestroy( Permission perm )
	{
		SQLDatastore db = getSQL();
		// db.delete( "permissions", String.format( "`permission` = '%s'", perm.getNamespace() ) );
		try
		{
			db.table( "permissions" ).delete().where( "permission" ).matches( perm.getNamespace() ).limit( 1 ).execute();
		}
		catch ( SQLException e )
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public void nodeReload( Permission perm )
	{
		SQLDatastore db = getSQL();
		
		try
		{
			SQLQuerySelect select = db.table( "permissions" ).select().where( "permission" ).matches( perm.getNamespace() ).execute();
			// ResultSet rs = db.query( "SELECT * FROM `permissions` WHERE `permission` = '" + perm.getNamespace() + "';" );
			
			if ( select.rowCount() > 0 )
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
	public void reloadBackend() throws PermissionBackendException
	{
		// TODO Auto-generated method stub
	}
	
	@Override
	public void setDefaultGroup( String child, References ref )
	{
		try
		{
			Map<String, String> defaults = Maps.newHashMap();
			Set<String> children = Sets.newHashSet();
			
			ResultSet result = getSQL().table( "permissions_groups" ).select().where( "parent" ).matches( "default" ).and().where( "type" ).matches( "1" ).execute().result();
			// ResultSet result = getSQL().query( "SELECT * FROM `permissions_groups` WHERE `parent` = 'default' AND `type` = '1';" );
			
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
			// getSQL().queryUpdate( "DELETE FROM `permissions_groups` WHERE `parent` = 'default' AND `type` = '1';" );
			getSQL().table( "permissions_groups" ).delete().where( "parent" ).matches( "default" ).and().where( "type" ).matches( "1" ).execute();
			
			// Save changes
			for ( String c : children )
			{
				String refs = "";
				for ( Entry<String, String> e : defaults.entrySet() )
					if ( e.getKey() == c )
						refs += "|" + e.getValue();
				
				if ( refs.length() > 0 )
					refs = refs.substring( 1 );
				
				getSQL().table( "permissions_group" ).insert().values( new String[] {"child", "parent", "type", "ref"}, new String[] {c, "default", "1", refs} ).execute();
				// getSQL().queryUpdate( "INSERT INTO `permissions_group` (`child`, `parent`, `type`, `ref`) VALUES ('" + c + "', 'default', '1', '" + refs + "');" );
			}
		}
		catch ( SQLException e )
		{
			throw new RuntimeException( e );
		}
	}
	
	private int updateDBValue( Namespace ns, String key, Object val ) throws SQLException, PermissionValueException
	{
		try
		{
			return updateDBValue( ns, key, ObjectFunc.castToStringWithException( val ) );
		}
		catch ( ClassCastException e )
		{
			throw new PermissionValueException( "We could not cast the Object %s for key %s.", val.getClass().getName(), key );
		}
	}
	
	private int updateDBValue( Namespace ns, String key, String val ) throws SQLException
	{
		SQLDatastore db = getSQL();
		
		if ( key == null )
			return 0;
		
		if ( val == null )
			val = "";
		
		return db.table( "permissions" ).update().value( key, val ).where( "permission" ).matches( ns.getNamespace() ).execute().rowCount();
		// return db.queryUpdate( "UPDATE `permissions` SET `" + key + "` = ? WHERE `permission` = ?;", val, ns.getNamespace() );
	}
}
