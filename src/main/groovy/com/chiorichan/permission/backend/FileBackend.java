/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.permission.backend;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.chiorichan.Loader;
import com.chiorichan.configuration.ConfigurationSection;
import com.chiorichan.configuration.file.FileConfiguration;
import com.chiorichan.configuration.file.YamlConfiguration;
import com.chiorichan.framework.Site;
import com.chiorichan.permission.PermissibleEntity;
import com.chiorichan.permission.PermissibleGroup;
import com.chiorichan.permission.PermissionBackend;
import com.chiorichan.permission.PermissionBackendException;
import com.chiorichan.permission.PermissionManager;
import com.chiorichan.permission.backend.file.FileEntity;
import com.chiorichan.permission.backend.file.FileGroup;
import com.chiorichan.permission.structure.ChildPermission;
import com.chiorichan.permission.structure.Permission;
import com.chiorichan.permission.structure.PermissionValue;
import com.chiorichan.permission.structure.PermissionValueBoolean;
import com.chiorichan.permission.structure.PermissionValueEnum;
import com.chiorichan.permission.structure.PermissionValueInt;
import com.chiorichan.permission.structure.PermissionValueVar;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;

public class FileBackend extends PermissionBackend
{
	public static final char PATH_SEPARATOR = '/';
	public FileConfiguration permissions;
	public File permissionsFile;
	
	public FileBackend()
	{
		super();
	}
	
	@Override
	public void initialize() throws PermissionBackendException
	{
		String permissionFilename = Loader.getConfig().getString( "permissions.file" );
		
		if ( permissionFilename == null )
		{
			permissionFilename = "permissions.yml";
			Loader.getConfig().set( "permissions.file", "permissions.yml" );
		}
		
		File baseDirectory = Loader.getRoot();
		
		permissionsFile = new File( baseDirectory, permissionFilename );
		
		reload();
	}
	
	@Override
	public PermissibleEntity getEntity( String id )
	{
		return new FileEntity( id, this );
	}
	
	@Override
	public PermissibleGroup getGroup( String groupName )
	{
		return new FileGroup( groupName, this );
	}
	
	@Override
	public PermissibleGroup getDefaultGroup( String siteName )
	{
		ConfigurationSection groups = permissions.getConfigurationSection( "groups" );
		
		if ( groups == null )
		{
			throw new RuntimeException( "No groups defined. Check your permissions file." );
		}
		
		String defaultGroupProperty = "default";
		if ( siteName != null )
		{
			defaultGroupProperty = buildPath( "sites", siteName, defaultGroupProperty );
		}
		
		for ( Map.Entry<String, Object> entry : groups.getValues( false ).entrySet() )
		{
			if ( entry.getValue() instanceof ConfigurationSection )
			{
				ConfigurationSection groupSection = ( ConfigurationSection ) entry.getValue();
				
				if ( groupSection.getBoolean( defaultGroupProperty, false ) )
				{
					return Loader.getPermissionManager().getGroup( entry.getKey() );
				}
			}
		}
		
		if ( siteName == null )
		{
			throw new RuntimeException( "Default user group is not defined. Please select one using the \"default: true\" property" );
		}
		
		return null;
	}
	
	@Override
	public void setDefaultGroup( String group, String... site )
	{
		String siteName = Joiner.on( "|" ).join( site );
		
		ConfigurationSection groups = permissions.getConfigurationSection( "groups" );
		
		String defaultGroupProperty = "default";
		if ( siteName != null )
		{
			defaultGroupProperty = buildPath( "sites", siteName, defaultGroupProperty );
		}
		
		for ( Map.Entry<String, Object> entry : groups.getValues( false ).entrySet() )
		{
			if ( entry.getValue() instanceof ConfigurationSection )
			{
				ConfigurationSection groupSection = ( ConfigurationSection ) entry.getValue();
				
				groupSection.set( defaultGroupProperty, false );
				
				if ( !groupSection.getName().equals( group ) )
				{
					groupSection.set( defaultGroupProperty, null );
				}
				else
				{
					groupSection.set( defaultGroupProperty, true );
				}
			}
		}
		
		save();
	}
	
	@Override
	public PermissibleGroup[] getGroups()
	{
		List<PermissibleGroup> groups = new LinkedList<PermissibleGroup>();
		ConfigurationSection groupsSection = permissions.getConfigurationSection( "groups" );
		
		if ( groupsSection == null )
		{
			return new PermissibleGroup[0];
		}
		
		for ( String groupName : groupsSection.getKeys( false ) )
		{
			groups.add( Loader.getPermissionManager().getGroup( groupName ) );
		}
		
		Collections.sort( groups );
		
		return groups.toArray( new PermissibleGroup[0] );
	}
	
	public static String buildPath( String... path )
	{
		StringBuilder builder = new StringBuilder();
		
		boolean first = true;
		char separator = PATH_SEPARATOR; // permissions.options().pathSeparator();
		
		for ( String node : path )
		{
			if ( node.isEmpty() )
			{
				continue;
			}
			
			if ( !first )
			{
				builder.append( separator );
			}
			
			builder.append( node );
			
			first = false;
		}
		
		return builder.toString();
	}
	
	@Override
	public void reload() throws PermissionBackendException
	{
		FileConfiguration newPermissions = new YamlConfiguration();
		newPermissions.options().pathSeparator( PATH_SEPARATOR );
		try
		{
			
			newPermissions.load( permissionsFile );
			
			PermissionManager.getLogger().info( "Permissions file successfully loaded" );
			
			permissions = newPermissions;
		}
		catch ( FileNotFoundException e )
		{
			if ( permissions == null )
			{
				// First load, load even if the file doesn't exist
				permissions = newPermissions;
				initNewConfiguration();
			}
		}
		catch ( Throwable e )
		{
			throw new PermissionBackendException( "Error loading permissions file!", e );
		}
	}
	
	/**
	 * This method is called when the file the permissions config is supposed to save to
	 * does not exist yet,This adds default permissions & stuff
	 */
	private void initNewConfiguration() throws PermissionBackendException
	{
		if ( !permissionsFile.exists() )
		{
			try
			{
				permissionsFile.createNewFile();
				
				permissions.set( "groups/default/default", true );
				
				List<String> defaultPermissions = new LinkedList<String>();
				defaultPermissions.add( "com.chiorichan.*" );
				
				permissions.set( "groups/default/permissions", defaultPermissions );
				
				save();
			}
			catch ( IOException e )
			{
				throw new PermissionBackendException( e );
			}
		}
	}
	
	public void save()
	{
		try
		{
			permissions.save( permissionsFile );
		}
		catch ( IOException e )
		{
			Logger.getLogger( "" ).severe( "[PermissionsEx] Error during saving permissions file: " + e.getMessage() );
		}
	}
	
	@Override
	public PermissibleEntity[] getEntities()
	{
		return new PermissibleEntity[0];
	}
	
	@Override
	public Set<String> getEntityNames( int type )
	{
		ConfigurationSection section = permissions.getConfigurationSection( ( type == 1 ) ? "groups" : "entities" );
		
		if ( section == null )
			return Sets.newHashSet();
		
		return section.getKeys( false );
	}
	
	@Override
	public void loadPermissionTree()
	{
		ConfigurationSection section = permissions.getConfigurationSection( "permissions" );
		
		if ( section != null )
			for ( String s : section.getKeys( false ) )
			{
				String permName = s.toLowerCase();
				ConfigurationSection result = section.getConfigurationSection( s );
				Permission perm = Permission.crawlPermissionStack( permName, true );
				
				if ( result.getString( "type" ) != null )
					switch ( result.getString( "type" ) )
					{
						case "BOOL":
							perm.setValue( new PermissionValueBoolean( permName, result.getBoolean( "value" ) ) );
							break;
						case "ENUM":
							perm.setValue( new PermissionValueEnum( permName, result.getString( "value", "" ), result.getInt( "maxlen", -1 ), Splitter.on( "|" ).splitToList( result.getString( "enum", "" ) ) ) );
							break;
						case "VAR":
							perm.setValue( new PermissionValueVar( permName, result.getString( "value", "" ), result.getInt( "maxlen", -1 ) ) );
							break;
						case "INT":
							perm.setValue( new PermissionValueInt( permName, result.getInt( "value" ) ) );
							break;
					}
				
				if ( result.getString( "description" ) != null )
					perm.setDescription( result.getString( "description" ) );
			}
		
		section = permissions.getConfigurationSection( "entities" );
		
		if ( section != null )
			for ( String s : section.getKeys( false ) )
			{
				PermissibleEntity entity = Loader.getPermissionManager().getEntity( s );
				
				ConfigurationSection result = section.getConfigurationSection( s );
				ConfigurationSection permissions = result.getConfigurationSection( "permissions" );
				
				for ( String ss : permissions.getKeys( false ) )
				{
					ConfigurationSection permission = section.getConfigurationSection( ss );
					if ( permission != null && permission.getString( "permission" ) != null )
					{
						Permission perm = Permission.crawlPermissionStack( permission.getString( "permission" ).toLowerCase(), true );
						
						List<Site> sites = Loader.getSiteManager().parseSites( ( permission.getString( "sites" ) == null ) ? "" : permission.getString( "sites" ) );
						
						PermissionValue<?> value = null;
						if ( permission.getString( "value" ) != null )
							value = perm.getValue().createChild( permission.getString( "value" ) );
						
						entity.attachPermission( new ChildPermission( perm, sites, value ) );
					}
				}
			}
		
		section = permissions.getConfigurationSection( "groups" );
		
		if ( section != null )
			for ( String s : section.getKeys( false ) )
			{
				ConfigurationSection result = section.getConfigurationSection( s );
				
				PermissibleEntity group = Loader.getPermissionManager().getGroup( s );
				
				ConfigurationSection permissions = result.getConfigurationSection( "permissions" );
				
				if ( permissions != null )
					for ( String ss : permissions.getKeys( false ) )
					{
						ConfigurationSection permission = section.getConfigurationSection( ss );
						
						Permission perm = Permission.crawlPermissionStack( permission.getString( "permission" ).toLowerCase(), true );
						
						List<Site> sites = Loader.getSiteManager().parseSites( ( permission.getString( "sites" ) == null ) ? "" : permission.getString( "sites" ) );
						
						PermissionValue<?> value = null;
						if ( permission.getString( "value" ) != null )
							value = perm.getValue().createChild( permission.getString( "value" ) );
						
						group.attachPermission( new ChildPermission( perm, sites, value ) );
					}
			}
	}
}
