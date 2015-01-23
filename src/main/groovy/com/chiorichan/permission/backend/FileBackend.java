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

import org.gradle.jarjar.com.google.common.base.Joiner;
import org.gradle.jarjar.com.google.common.collect.Sets;

import com.chiorichan.Loader;
import com.chiorichan.configuration.ConfigurationSection;
import com.chiorichan.configuration.file.FileConfiguration;
import com.chiorichan.configuration.file.YamlConfiguration;
import com.chiorichan.permission.PermissibleEntity;
import com.chiorichan.permission.PermissibleGroup;
import com.chiorichan.permission.PermissionBackend;
import com.chiorichan.permission.PermissionBackendException;
import com.chiorichan.permission.backend.file.FileEntity;
import com.chiorichan.permission.backend.file.FileGroup;

public class FileBackend extends PermissionBackend
{
	public final static char PATH_SEPARATOR = '/';
	public FileConfiguration permissions;
	public File permissionsFile;
	
	public FileBackend()
	{
		super();
	}
	
	@Override
	public void initialize() throws PermissionBackendException
	{
		String permissionFilename = Loader.getConfig().getString( "permissions.backends.file.file" );
		
		if ( permissionFilename == null )
		{
			permissionFilename = "permissions.yml";
			Loader.getConfig().set( "permissions.backends.file.file", "permissions.yml" );
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
				ConfigurationSection groupSection = (ConfigurationSection) entry.getValue();
				
				if ( groupSection.getBoolean( defaultGroupProperty, false ) )
				{
					return Loader.getPermissionsManager().getGroup( entry.getKey() );
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
				ConfigurationSection groupSection = (ConfigurationSection) entry.getValue();
				
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
			groups.add( Loader.getPermissionsManager().getGroup( groupName ) );
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
			
			Logger.getLogger( "PermissionsEx" ).info( "Permissions file successfully reloaded" );
			
			permissions = newPermissions;
		}
		catch( FileNotFoundException e )
		{
			if ( permissions == null )
			{
				// First load, load even if the file doesn't exist
				permissions = newPermissions;
				initNewConfiguration();
			}
		}
		catch( Throwable e )
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
				
				// Load default permissions
				permissions.set( "groups/default/default", true );
				
				List<String> defaultPermissions = new LinkedList<String>();
				// Specify here default permissions
				defaultPermissions.add( "modifysite.*" );
				
				permissions.set( "groups/default/permissions", defaultPermissions );
				
				save();
			}
			catch( IOException e )
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
		catch( IOException e )
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
		return Sets.newHashSet();
	}
	
	@Override
	public void loadPermissionTree()
	{
		
	}
}
