package ru.tehkode.permissions.backends.file;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionsGroupData;
import ru.tehkode.permissions.PermissionsUserData;
import ru.tehkode.permissions.backends.FileBackend;

import com.chiorichan.configuration.ConfigurationSection;

public class FileData implements PermissionsUserData, PermissionsGroupData
{
	
	protected transient FileConfig config;
	
	protected String nodePath;
	
	protected ConfigurationSection node;
	
	protected boolean virtual = true;
	
	public FileData(String basePath, String name, FileConfig config)
	{
		this.config = config;
		
		this.node = findNode( name, basePath );
	}
	
	private ConfigurationSection findNode( String entityName, String basePath )
	{
		this.nodePath = FileBackend.buildPath( basePath, entityName );
		
		ConfigurationSection entityNode = this.config.getConfigurationSection( this.nodePath );
		
		if ( entityNode != null )
		{
			this.virtual = false;
			return entityNode;
		}
		
		ConfigurationSection users = this.config.getConfigurationSection( basePath );
		
		if ( users != null )
		{
			for ( Map.Entry<String, Object> entry : users.getValues( false ).entrySet() )
			{
				if ( entry.getKey().equalsIgnoreCase( entityName ) && entry.getValue() instanceof ConfigurationSection )
				{
					this.nodePath = FileBackend.buildPath( basePath, entityName );
					return (ConfigurationSection) entry.getValue();
				}
			}
		}
		
		// Silly workaround for empty nodes
		ConfigurationSection section = this.config.createSection( nodePath );
		this.config.set( nodePath, null );
		
		return section;
		
	}
	
	/**
	 * Permissions
	 */
	@Override
	public List<String> getPermissions( String siteName )
	{
		List<String> result = this.node.getStringList( formatPath( siteName, "permissions" ) );
		
		return result == null ? new LinkedList<String>() : result;
	}
	
	@Override
	public void setPermissions( List<String> permissions, String siteName )
	{
		this.node.set( formatPath( siteName, "permissions" ), permissions.isEmpty() ? null : permissions );
	}
	
	@Override
	public Map<String, List<String>> getPermissionsMap()
	{
		Map<String, List<String>> allPermissions = new HashMap<String, List<String>>();
		
		// Common permissions
		List<String> commonPermissions = this.node.getStringList( "permissions" );
		if ( commonPermissions != null )
		{
			allPermissions.put( null, commonPermissions );
		}
		
		// Site-specific permissions
		ConfigurationSection sitesSection = this.node.getConfigurationSection( "sites" );
		if ( sitesSection != null )
		{
			for ( String site : sitesSection.getKeys( false ) )
			{
				List<String> sitePermissions = this.node.getStringList( FileBackend.buildPath( "sites", site, "permissions" ) );
				if ( commonPermissions != null )
				{
					allPermissions.put( site, sitePermissions );
				}
			}
		}
		
		return allPermissions;
	}
	
	@Override
	public Set<String> getSites()
	{
		ConfigurationSection sitesSection = this.node.getConfigurationSection( "sites" );
		
		if ( sitesSection == null )
		{
			return new HashSet<String>();
		}
		
		return sitesSection.getKeys( false );
	}
	
	@Override
	public String getPrefix( String siteName )
	{
		return this.node.getString( formatPath( siteName, "prefix" ) );
	}
	
	@Override
	public void setPrefix( String prefix, String siteName )
	{
		this.node.set( formatPath( siteName, "prefix" ), prefix );
	}
	
	@Override
	public String getSuffix( String siteName )
	{
		return this.node.getString( formatPath( siteName, "suffix" ) );
	}
	
	@Override
	public void setSuffix( String suffix, String siteName )
	{
		this.node.set( formatPath( siteName, "suffix" ), suffix );
	}
	
	@Override
	public String getOption( String option, String siteName )
	{
		return this.node.getString( formatPath( siteName, "options", option ) );
	}
	
	@Override
	public void setOption( String option, String siteName, String value )
	{
		this.node.set( formatPath( siteName, "options", option ), value );
	}
	
	@Override
	public Map<String, String> getOptions( String siteName )
	{
		ConfigurationSection optionsSection = this.node.getConfigurationSection( formatPath( siteName, "options" ) );
		
		if ( optionsSection == null )
		{
			return new HashMap<String, String>( 0 );
		}
		
		return collectOptions( optionsSection );
	}
	
	@Override
	public Map<String, Map<String, String>> getOptionsMap()
	{
		Map<String, Map<String, String>> allOptions = new HashMap<String, Map<String, String>>();
		
		allOptions.put( null, this.getOptions( null ) );
		
		for ( String siteName : this.getSites() )
		{
			allOptions.put( siteName, this.getOptions( siteName ) );
		}
		
		return allOptions;
	}
	
	@Override
	public boolean isVirtual()
	{
		return this.config.isConfigurationSection( this.nodePath );
	}
	
	@Override
	public void save()
	{
		this.config.save();
	}
	
	@Override
	public void remove()
	{
		this.config.set( nodePath, null );
		this.save();
	}
	
	@Override
	public List<String> getGroups( String siteName )
	{
		Object groups = this.node.get( FileEntity.formatPath( siteName, "group" ) );
		
		if ( groups instanceof String )
		{ // old style
			String[] groupsArray;
			String groupsString = ( (String) groups );
			if ( groupsString.contains( "," ) )
			{
				groupsArray = ( (String) groups ).split( "," );
			}
			else
			{
				groupsArray = new String[] { groupsString };
			}
			
			return Arrays.asList( groupsArray );
		}
		else if ( groups instanceof List )
		{
			return (List<String>) groups;
		}
		else
		{
			return new ArrayList<String>( 0 );
		}
	}
	
	@Override
	public void setGroups( List<PermissionGroup> groups, String siteName )
	{
		this.node.set( FileEntity.formatPath( siteName, "group" ), groups );
	}
	
	@Override
	public List<String> getParents( String siteName )
	{
		List<String> parents = this.node.getStringList( FileEntity.formatPath( siteName, "inheritance" ) );
		
		if ( parents.isEmpty() )
		{
			return new ArrayList<String>( 0 );
		}
		
		return parents;
	}
	
	@Override
	public void setParents( String siteName, List<String> parents )
	{
		this.node.set( FileEntity.formatPath( siteName, "inheritance" ), parents );
	}
	
	private Map<String, String> collectOptions( ConfigurationSection section )
	{
		Map<String, String> options = new LinkedHashMap<String, String>();
		
		for ( String key : section.getKeys( true ) )
		{
			if ( section.isConfigurationSection( key ) )
			{
				continue;
			}
			
			options.put( key.replace( section.getRoot().options().pathSeparator(), '.' ), section.getString( key ) );
		}
		
		return options;
	}
	
	protected static String formatPath( String siteName, String node, String value )
	{
		String path = FileBackend.buildPath( node, value );
		
		if ( siteName != null && !siteName.isEmpty() )
		{
			path = FileBackend.buildPath( "sites", siteName, path );
		}
		
		return path;
	}
	
	protected static String formatPath( String siteName, String node )
	{
		String path = node;
		
		if ( siteName != null && !siteName.isEmpty() )
		{
			path = FileBackend.buildPath( "sites", siteName, path );
		}
		
		return path;
	}
}
