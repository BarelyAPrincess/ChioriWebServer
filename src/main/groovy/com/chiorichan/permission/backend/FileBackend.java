package com.chiorichan.permission.backend;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
	public String[] getSiteInheritance( String site )
	{
		if ( site != null && !site.isEmpty() )
		{
			List<String> parentSites = permissions.getStringList( buildPath( "sites", site, "/inheritance" ) );
			if ( parentSites != null )
			{
				return parentSites.toArray( new String[parentSites.size()] );
			}
		}
		
		return new String[0];
	}
	
	@Override
	public void setSiteInheritance( String site, String[] parentSites )
	{
		if ( site == null || site.isEmpty() )
		{
			return;
		}
		
		permissions.set( buildPath( "sites", site, "inheritance" ), Arrays.asList( parentSites ) );
		save();
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
	public void setDefaultGroup( PermissibleGroup group, String siteName )
	{
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
				
				if ( !groupSection.getName().equals( group.getName() ) )
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
	
	@Override
	public PermissibleEntity[] getRegisteredEntities()
	{
		List<PermissibleEntity> users = new LinkedList<PermissibleEntity>();
		ConfigurationSection usersSection = permissions.getConfigurationSection( "users" );
		
		if ( usersSection != null )
		{
			for ( String userName : usersSection.getKeys( false ) )
			{
				users.add( Loader.getPermissionsManager().getEntity( userName ) );
			}
		}
		
		return users.toArray( new PermissibleEntity[users.size()] );
	}
	
	@Override
	public Collection<String> getRegisteredEntityNames()
	{
		ConfigurationSection users = permissions.getConfigurationSection( "users" );
		return users != null ? users.getKeys( false ) : Collections.<String> emptySet();
	}
	
	@Override
	public Collection<String> getRegisteredGroupNames()
	{
		ConfigurationSection groups = permissions.getConfigurationSection( "groups" );
		return groups != null ? groups.getKeys( false ) : Collections.<String> emptySet();
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
				
				// Load default permissions
				permissions.set( "groups/default/default", true );
				
				List<String> defaultPermissions = new LinkedList<String>();
				// Specify here default permissions
				defaultPermissions.add( "modifysite.*" );
				
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
	
	private void dumpGroupInfo( PermissibleGroup group, String siteName, ConfigurationSection groupSection )
	{
		String sitePath = siteName == null ? "" : buildPath( "sites", siteName );
		// site-specific prefix
		String prefix = group.getOwnPrefix( siteName );
		if ( prefix != null && !prefix.isEmpty() )
		{
			groupSection.set( buildPath( sitePath, "prefix" ), prefix );
		}
		
		String suffix = group.getOwnSuffix( siteName );
		if ( suffix != null && !suffix.isEmpty() )
		{
			groupSection.set( buildPath( sitePath, "suffix" ), suffix );
		}
		
		if ( group.isDefault( siteName ) )
		{
			groupSection.set( buildPath( sitePath, "default" ), true );
		}
	}
	
	private void dumpEntityInfo( PermissibleEntity user, String siteName, ConfigurationSection userSection )
	{
		final String[] groups = user.getGroupsNames( siteName );
		final String prefix = user.getOwnPrefix( siteName ), suffix = user.getOwnSuffix( siteName );
		final String pathPrefix = siteName == null ? "" : buildPath( "sites", siteName );
		// Inheritance
		if ( groups.length > 0 )
		{
			userSection.set( buildPath( pathPrefix, "group" ), Arrays.asList( groups ) );
		}
		
		// Prefix
		if ( prefix != null && !prefix.isEmpty() )
		{
			userSection.set( buildPath( pathPrefix, "prefix" ), user.getOwnPrefix( siteName ) );
		}
		
		// Suffix
		if ( suffix != null && !suffix.isEmpty() )
		{
			userSection.set( buildPath( pathPrefix, "suffix" ), suffix );
		}
	}
	
	@Override
	public void dumpData( OutputStreamWriter writer ) throws IOException
	{
		YamlConfiguration config = new YamlConfiguration();
		config.options().pathSeparator( PATH_SEPARATOR ).indent( 4 );
		
		// Groups
		for ( PermissibleGroup group : Loader.getPermissionsManager().getGroups() )
		{
			ConfigurationSection groupSection = config.createSection( buildPath( "groups", group.getName() ) );
			
			// Inheritance
			if ( group.getParentGroupsNames().length > 0 )
			{
				groupSection.set( "inheritance", Arrays.asList( group.getParentGroupsNames() ) );
			}
			
			dumpEntityData( group, groupSection );
			
			// site-specific inheritance
			for ( Map.Entry<String, PermissibleGroup[]> entry : group.getAllParentGroups().entrySet() )
			{
				if ( entry.getKey() == null )
					continue;
				
				List<String> groups = new ArrayList<String>();
				for ( PermissibleGroup parentGroup : entry.getValue() )
				{
					if ( parentGroup == null )
					{
						continue;
					}
					
					groups.add( parentGroup.getName() );
				}
				
				if ( groups.isEmpty() )
					continue;
				
				groupSection.set( buildPath( "sites", entry.getKey(), "inheritance" ), groups );
			}
			
			// site specific stuff
			for ( String siteName : group.getSites() )
			{
				if ( siteName == null )
					continue;
				dumpGroupInfo( group, siteName, groupSection );
			}
			dumpGroupInfo( group, null, groupSection );
		}
		
		// Site inheritance
		for ( Site site : Loader.getSiteManager().getSites() )
		{
			String[] parentSites = manager.getSiteInheritance( site.getName() );
			if ( parentSites.length == 0 )
			{
				continue;
			}
			
			config.set( buildPath( "sites", site.getName(), "inheritance" ), Arrays.asList( parentSites ) );
		}
		
		// Entitys setup
		for ( PermissibleEntity user : manager.getEntitys() )
		{
			ConfigurationSection userSection = config.createSection( buildPath( "users", user.getName() ) );
			dumpEntityInfo( user, null, userSection );
			dumpEntityData( user, userSection );
			for ( String site : user.getSites() )
			{
				if ( site == null )
					continue;
				dumpEntityInfo( user, null, userSection );
			}
		}
		
		// Write data
		writer.write( config.saveToString() );
		writer.flush();
	}
	
	// Some of the methods are common in PermissionEntity. Sadly not very many of them.
	private void dumpEntityData( PermissibleEntity entity, ConfigurationSection section )
	{
		
		// Permissions
		for ( Map.Entry<String, String[]> entry : entity.getAllPermissions().entrySet() )
		{
			if ( entry.getValue().length == 0 )
				continue;
			
			String nodePath = "permissions";
			if ( entry.getKey() != null && !entry.getKey().isEmpty() )
			{
				nodePath = buildPath( "sites", entry.getKey(), nodePath );
			}
			
			section.set( nodePath, Arrays.asList( entry.getValue() ) );
		}
		
		// Options
		for ( Map.Entry<String, Map<String, String>> entry : entity.getAllOptions().entrySet() )
		{
			if ( entry.getValue().isEmpty() )
				continue;
			
			String nodePath = "options";
			if ( entry.getKey() != null && !entry.getKey().isEmpty() )
			{
				nodePath = buildPath( "sites", entry.getKey(), nodePath );
			}
			
			section.set( nodePath, entry.getValue() );
		}
	}
}
