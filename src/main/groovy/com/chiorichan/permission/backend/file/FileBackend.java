/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.permission.backend.file;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.chiorichan.Loader;
import com.chiorichan.configuration.ConfigurationSection;
import com.chiorichan.configuration.file.FileConfiguration;
import com.chiorichan.configuration.file.YamlConfiguration;
import com.chiorichan.permission.ChildPermission;
import com.chiorichan.permission.PermissibleEntity;
import com.chiorichan.permission.PermissibleGroup;
import com.chiorichan.permission.Permission;
import com.chiorichan.permission.PermissionBackend;
import com.chiorichan.permission.PermissionManager;
import com.chiorichan.permission.PermissionModelValue;
import com.chiorichan.permission.PermissionNamespace;
import com.chiorichan.permission.PermissionType;
import com.chiorichan.permission.PermissionValue;
import com.chiorichan.permission.lang.PermissionBackendException;
import com.chiorichan.permission.lang.PermissionException;
import com.chiorichan.util.FileFunc;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;

/**
 * Provides the File Permission Backend
 */
public class FileBackend extends PermissionBackend
{
	private static FileBackend backend;
	public FileConfiguration permissions;
	public File permissionsFile;
	
	public FileBackend()
	{
		super();
		backend = this;
	}
	
	public static FileBackend getBackend()
	{
		return backend;
	}
	
	public void commit()
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
	public PermissibleGroup getDefaultGroup( String refs )
	{
		ConfigurationSection groups = permissions.getConfigurationSection( "groups" );
		
		if ( groups == null )
			throw new RuntimeException( "No groups defined. Check your permissions file." );
		
		String defaultGroupProperty = "default";
		if ( refs != null )
			defaultGroupProperty = FileFunc.buildPath( "refs", refs, defaultGroupProperty );
		
		for ( Map.Entry<String, Object> entry : groups.getValues( false ).entrySet() )
			if ( entry.getValue() instanceof ConfigurationSection )
			{
				ConfigurationSection groupSection = ( ConfigurationSection ) entry.getValue();
				
				if ( groupSection.getBoolean( defaultGroupProperty, false ) )
					return PermissionManager.INSTANCE.getGroup( entry.getKey() );
			}
		
		if ( refs == null )
			throw new RuntimeException( "Default user group is not defined. Please select one using the \"default: true\" property" );
		
		return null;
	}
	
	@Override
	public PermissibleEntity[] getEntities()
	{
		return new PermissibleEntity[0];
	}
	
	@Override
	public PermissibleEntity getEntity( String id )
	{
		return new FileEntity( id );
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
	public PermissibleGroup getGroup( String groupName )
	{
		return new FileGroup( groupName );
	}
	
	@Override
	public PermissibleGroup[] getGroups()
	{
		List<PermissibleGroup> groups = new LinkedList<PermissibleGroup>();
		ConfigurationSection groupsSection = permissions.getConfigurationSection( "groups" );
		
		if ( groupsSection == null )
			return new PermissibleGroup[0];
		
		for ( String groupName : groupsSection.getKeys( false ) )
			groups.add( PermissionManager.INSTANCE.getGroup( groupName ) );
		
		Collections.sort( groups );
		
		return groups.toArray( new PermissibleGroup[0] );
	}
	
	@Override
	public void initialize() throws PermissionBackendException
	{
		String permissionFilename = Loader.getConfig().getString( "permissions.file" );
		
		if ( permissionFilename == null )
		{
			permissionFilename = "permissions.yaml";
			Loader.getConfig().set( "permissions.file", "permissions.yaml" );
		}
		
		permissionsFile = new File( permissionFilename );
		
		FileConfiguration newPermissions = new YamlConfiguration();
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
	 * This method is called when the permissions config file does not exist
	 * and needs to be created, this also adds the defaults.
	 */
	private void initNewConfiguration() throws PermissionBackendException
	{
		if ( !permissionsFile.exists() )
			try
			{
				permissionsFile.createNewFile();
				
				setDefaultGroup( "default" );
				
				List<String> defaultPermissions = new LinkedList<String>();
				defaultPermissions.add( "com.chiorichan.*" );
				
				permissions.set( "groups/default/permissions", defaultPermissions );
				
				commit();
			}
			catch ( IOException e )
			{
				throw new PermissionBackendException( e );
			}
	}
	
	@Override
	public void loadEntities()
	{
		ConfigurationSection section = permissions.getConfigurationSection( "entities" );
		
		if ( section != null )
			for ( String s : section.getKeys( false ) )
			{
				PermissibleEntity entity = PermissionManager.INSTANCE.getEntity( s );
				ConfigurationSection result = section.getConfigurationSection( s );
				
				ConfigurationSection permissions = result.getConfigurationSection( "permissions", true );
				
				for ( String ss : permissions.getKeys( false ) )
				{
					ConfigurationSection permission = section.getConfigurationSection( ss );
					if ( permission != null && permission.getString( "permission" ) != null )
					{
						Permission perm = PermissionManager.INSTANCE.getNode( permission.getString( "permission" ).toLowerCase(), true );
						
						PermissionValue value = null;
						if ( permission.getString( "value" ) != null )
							value = perm.getModel().createValue( permission.getString( "value" ) );
						
						String refs = permission.isString( "refs" ) ? permission.getString( "refs" ) : "";
						entity.attachPermission( new ChildPermission( perm, value, false, refs.split( "|" ) ) );
					}
				}
			}
	}
	
	@Override
	public void loadGroups()
	{
		ConfigurationSection section = permissions.getConfigurationSection( "groups" );
		
		if ( section != null )
			for ( String s : section.getKeys( false ) )
			{
				PermissibleGroup group = PermissionManager.INSTANCE.getGroup( s );
				
				ConfigurationSection result = section.getConfigurationSection( s );
				ConfigurationSection permissions = result.getConfigurationSection( "permissions" );
				
				if ( permissions != null )
					for ( String ss : permissions.getKeys( false ) )
					{
						ConfigurationSection permission = section.getConfigurationSection( ss );
						
						Permission perm = PermissionManager.INSTANCE.getNode( permission.getString( "permission" ).toLowerCase(), true );
						
						PermissionValue value = null;
						if ( permission.getString( "value" ) != null )
							value = perm.getModel().createValue( permission.getString( "value" ) );
						
						String refs = permission.isString( "refs" ) ? permission.getString( "refs" ) : "";
						group.attachPermission( new ChildPermission( perm, value, true, refs.split( "|" ) ) );
					}
			}
	}
	
	@Override
	public void loadPermissions()
	{
		ConfigurationSection section = permissions.getConfigurationSection( "permissions" );
		
		if ( section != null )
			for ( String s : section.getKeys( false ) )
			{
				ConfigurationSection result = section.getConfigurationSection( s );
				
				try
				{
					PermissionNamespace ns = new PermissionNamespace( result.getString( "permission" ) );
					
					if ( !ns.containsOnlyValidChars() )
					{
						PermissionManager.getLogger().warning( String.format( "The permission '%s' contains invalid characters, namespaces can only contain the characters a-z, 0-9, and _, this will be fixed automatically.", ns ) );
						ns.fixInvalidChars();
						result.set( "permission", ns.getNamespace() );
					}
					
					Permission perm = new Permission( ns, PermissionType.valueOf( result.getString( "type" ) ) );
					
					PermissionModelValue model = perm.getModel();
					
					if ( result.get( "value" ) != null )
						model.setValue( result.get( "value" ) );
					
					if ( result.get( "default" ) != null )
						model.setValueDefault( result.get( "default" ) );
					
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
			}
	}
	
	@Override
	public void nodeCommit( Permission perm )
	{
		PermissionModelValue model = perm.getModel();
		
		ConfigurationSection permissionsSection = permissions.getConfigurationSection( "permissions", true );
		ConfigurationSection permission = permissionsSection.getConfigurationSection( perm.getNamespace(), true );
		
		permission.set( "type", perm.getType().name() );
		
		permission.set( "value", perm.getType() == PermissionType.DEFAULT ? null : model.getValue() );
		permission.set( "default", perm.getType() == PermissionType.DEFAULT ? null : model.getValueDefault() );
		
		permission.set( "max", perm.getType().hasMax() ? model.getMaxLen() : null );
		permission.set( "min", perm.getType().hasMin() ? 0 : null );
		permission.set( "enum", perm.getType() == PermissionType.ENUM ? model.getEnumsString() : null );
		permission.set( "description", model.hasDescription() ? model.getDescription() : null );
		
		commit();
	}
	
	@Override
	public void nodeDestroy( Permission perm )
	{
		ConfigurationSection permissionsSection = permissions.getConfigurationSection( "permissions", true );
		permissionsSection.set( perm.getNamespace(), null );
	}
	
	@Override
	public void nodeReload( Permission perm )
	{
		
	}
	
	@Override
	public void setDefaultGroup( String group, String... ref )
	{
		String refs = Joiner.on( "|" ).join( ref );
		
		ConfigurationSection groups = permissions.getConfigurationSection( "groups" );
		
		String defaultGroupProperty = "default";
		if ( refs != null )
			defaultGroupProperty = FileFunc.buildPath( "refs", refs, defaultGroupProperty );
		
		for ( Map.Entry<String, Object> entry : groups.getValues( false ).entrySet() )
			if ( entry.getValue() instanceof ConfigurationSection )
			{
				ConfigurationSection groupSection = ( ConfigurationSection ) entry.getValue();
				
				groupSection.set( defaultGroupProperty, false );
				
				if ( !groupSection.getName().equals( group ) )
					groupSection.set( defaultGroupProperty, null );
				else
					groupSection.set( defaultGroupProperty, true );
			}
		
		commit();
	}
}
