package ru.tehkode.permissions.backends.file;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ru.tehkode.permissions.PermissionEntity;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.backends.FileBackend;

import com.chiorichan.configuration.ConfigurationSection;

public class FileEntity extends PermissionEntity
{
	
	protected ConfigurationSection node;
	protected FileBackend backend;
	protected String nodePath;
	
	public FileEntity(String entityName, PermissionManager manager, FileBackend backend, String baseNode)
	{
		super( entityName, manager );
		
		this.backend = backend;
		this.node = this.getNode( baseNode, this.getName() );
	}
	
	protected final ConfigurationSection getNode( String baseNode, String entityName )
	{
		this.nodePath = FileBackend.buildPath( baseNode, entityName );
		
		ConfigurationSection entityNode = backend.permissions.getConfigurationSection( this.nodePath );
		
		if ( entityNode != null )
		{
			this.virtual = false;
			return entityNode;
		}
		
		ConfigurationSection users = backend.permissions.getConfigurationSection( baseNode );
		
		if ( users != null )
		{
			for ( Map.Entry<String, Object> entry : users.getValues( false ).entrySet() )
			{
				if ( entry.getKey().equalsIgnoreCase( entityName ) && entry.getValue() instanceof ConfigurationSection )
				{
					this.setName( entry.getKey() );
					this.nodePath = FileBackend.buildPath( baseNode, this.getName() );
					this.virtual = false;
					
					return (ConfigurationSection) entry.getValue();
				}
			}
		}
		
		this.virtual = true;
		
		// Silly workaround for empty nodes
		ConfigurationSection section = backend.permissions.createSection( nodePath );
		backend.permissions.set( nodePath, null );
		
		return section;
	}
	
	public ConfigurationSection getConfigNode()
	{
		return this.node;
	}
	
	@Override
	public String[] getPermissions( String site )
	{
		List<String> permissions = this.node.getStringList( formatPath( site, "permissions" ) );
		
		if ( permissions == null )
		{
			return new String[0];
		}
		
		return permissions.toArray( new String[permissions.size()] );
	}
	
	@Override
	public void setPermissions( String[] permissions, String site )
	{
		this.node.set( formatPath( site, "permissions" ), permissions.length > 0 ? Arrays.asList( permissions ) : null );
		
		this.save();
	}
	
	@Override
	public String[] getSites()
	{
		ConfigurationSection sitesSection = this.node.getConfigurationSection( "sites" );
		
		if ( sitesSection == null )
		{
			return new String[0];
		}
		
		return sitesSection.getKeys( false ).toArray( new String[0] );
	}
	
	@Override
	public Map<String, String> getOptions( String site )
	{
		
		ConfigurationSection optionsSection = this.node.getConfigurationSection( formatPath( site, "options" ) );
		
		if ( optionsSection != null )
		{
			return this.collectOptions( optionsSection );
		}
		
		return new HashMap<String, String>( 0 );
	}
	
	@Override
	public String getOption( String option, String site, String defaultValue )
	{
		return this.node.getString( formatPath( site, "options", option ), defaultValue );
	}
	
	@Override
	public void setOption( String option, String value, String site )
	{
		this.node.set( formatPath( site, "options", option ), value );
		
		this.save();
	}
	
	@Override
	public String getPrefix( String siteName )
	{
		return this.node.getString( formatPath( siteName, "prefix" ) );
	}
	
	@Override
	public String getSuffix( String siteName )
	{
		return this.node.getString( formatPath( siteName, "suffix" ) );
	}
	
	@Override
	public void setPrefix( String prefix, String siteName )
	{
		this.node.set( formatPath( siteName, "prefix" ), prefix );
		
		this.save();
	}
	
	@Override
	public void setSuffix( String suffix, String siteName )
	{
		this.node.set( formatPath( siteName, "suffix" ), suffix );
		
		this.save();
	}
	
	@Override
	public Map<String, String[]> getAllPermissions()
	{
		Map<String, String[]> allPermissions = new HashMap<String, String[]>();
		
		// Common permissions
		List<String> commonPermissions = this.node.getStringList( "permissions" );
		if ( commonPermissions != null )
		{
			allPermissions.put( null, commonPermissions.toArray( new String[0] ) );
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
					allPermissions.put( site, sitePermissions.toArray( new String[0] ) );
				}
			}
		}
		
		return allPermissions;
	}
	
	@Override
	public Map<String, Map<String, String>> getAllOptions()
	{
		Map<String, Map<String, String>> allOptions = new HashMap<String, Map<String, String>>();
		
		allOptions.put( null, this.getOptions( null ) );
		
		for ( String siteName : this.getSites() )
		{
			allOptions.put( siteName, this.getOptions( siteName ) );
		}
		
		return allOptions;
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
	
	@Override
	public void save()
	{
		this.backend.permissions.set( nodePath, node );
		
		this.backend.save();
		this.virtual = false;
	}
	
	@Override
	public void remove()
	{
		this.backend.permissions.set( nodePath, null );
		
		this.backend.save();
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
