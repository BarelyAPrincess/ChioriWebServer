package com.chiorichan.permission.backend;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.gradle.jarjar.com.google.common.collect.Lists;
import org.gradle.jarjar.com.google.common.collect.Sets;

import com.chiorichan.Loader;
import com.chiorichan.database.DatabaseEngine;
import com.chiorichan.permission.PermissibleEntity;
import com.chiorichan.permission.PermissibleGroup;
import com.chiorichan.permission.PermissionBackend;
import com.chiorichan.permission.PermissionBackendException;
import com.chiorichan.permission.PermissionManager;
import com.chiorichan.permission.backend.sql.SQLEntity;
import com.chiorichan.permission.backend.sql.SQLGroup;
import com.google.common.collect.Maps;

/**
 * @author Chiori Greene
 */
public class SQLBackend extends PermissionBackend
{
	public SQLBackend()
	{
		super();
	}
	
	@Override
	public void initialize() throws PermissionBackendException
	{
		if ( Loader.getDatabase() == null )
			throw new PermissionBackendException( "SQL connection is not configured, see config.yml" );
		
		PermissionManager.getLogger().info( "Successfully initalized SQL Backend!" );
	}
	
	@Override
	public PermissibleEntity getEntity( String name )
	{
		return new SQLEntity( name, this );
	}
	
	@Override
	public PermissibleGroup getGroup( String name )
	{
		return new SQLGroup( name, this );
	}
	
	public DatabaseEngine getSQL()
	{
		return Loader.getDatabase();
	}
	
	@Override
	public void setDefaultGroup( String child, String... site )
	{
		try
		{
			Map<String, String> defaults = Maps.newHashMap();
			Set<String> children = Sets.newHashSet();
			
			ResultSet result = getSQL().query( "SELECT * FROM `permissions_groups` WHERE `parent` = 'default' AND `type` = '1';" );
			
			if ( !result.next() )
				throw new RuntimeException( "There is no default group set. New entities will not have any groups." );
			
			do
			{
				String sites = result.getString( "sites" );
				if ( sites == null || sites.isEmpty() )
					defaults.put( "", result.getString( "child" ) );
				else
					for ( String siteA : sites.split( "|" ) )
						defaults.put( siteA.toLowerCase(), result.getString( "child" ) );
			}
			while( result.next() );
			
			// Update defaults
			for ( String s : site )
				defaults.put( s.toLowerCase(), child );
			
			// Gather child names
			for ( Entry<String, String> e : defaults.entrySet() )
				if ( !children.contains( e.getKey() ) )
					children.add( e.getKey() );
			
			// Delete old records
			getSQL().queryUpdate( "DELETE FROM `permissions_groups` WHERE `parent` = 'default' AND `type` = '1';" );
			
			// Save changes
			for ( String c : children )
			{
				String sites = "";
				for ( Entry<String, String> e : defaults.entrySet() )
					if ( e.getKey() == c )
						sites += "|" + e.getValue();
				
				if ( sites.length() > 0 )
					sites = sites.substring( 1 );
				
				getSQL().queryUpdate( "INSERT INTO `permissions_group` (`child`, `parent`, `type`, `sites`) VALUES ('" + c + "', 'default', '1', '" + sites + "');" );
			}
		}
		catch( SQLException e )
		{
			throw new RuntimeException( e );
		}
	}
	
	@Override
	public PermissibleGroup getDefaultGroup( String site )
	{
		try
		{
			Map<String, String> defaults = Maps.newHashMap();
			
			ResultSet result = getSQL().query( "SELECT * FROM `permissions_groups` WHERE `parent` = 'default' AND `type` = '1';" );
			
			if ( !result.next() )
				throw new RuntimeException( "There is no default group set. New entities will not have any groups." );
			
			do
			{
				String sites = result.getString( "sites" );
				if ( sites == null || sites.isEmpty() )
					defaults.put( "", result.getString( "child" ) );
				else
					for ( String siteA : sites.split( "|" ) )
						defaults.put( siteA.toLowerCase(), result.getString( "child" ) );
			}
			while( result.next() );
			
			if ( defaults.isEmpty() )
				throw new RuntimeException( "There is no default group set. New entities will not have any groups." );
			
			return getGroup( (site == null || site.isEmpty()) ? defaults.get( "" ) : defaults.get( site.toLowerCase() ) );
		}
		catch( SQLException e )
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
				return null;
			
			do
			{
				entities.add( result.getString( "owner" ) );
			}
			while( result.next() );
			
			return entities;
		}
		catch( SQLException e )
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
			entities.add( getGroup( entityName ) );
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
}
