package ru.tehkode.permissions.backends.memory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;

import ru.tehkode.permissions.PermissionEntity;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.backends.MemoryBackend;

public class MemoryEntity extends PermissionEntity
{
	
	protected MemoryBackend backend;
	
	protected HashMap<String, String> sitePrefix;
	protected HashMap<String, String> siteSuffix;
	protected HashMap<String, String[]> sitesPermissions;
	protected Map<String, Map<String, String>> sitesOptions;
	protected String[] commonPermissions;
	protected Map<String, Set<String>> parents;
	
	public MemoryEntity(String name, PermissionManager manager, MemoryBackend backend)
	{
		super( name, manager );
		
		this.backend = backend;
		
		sitePrefix = new HashMap<String, String>();
		siteSuffix = new HashMap<String, String>();
		sitesPermissions = new HashMap<String, String[]>();
		sitesOptions = new HashMap<String, Map<String, String>>();
		parents = new HashMap<String, Set<String>>();
		commonPermissions = new String[0];
	}
	
	@Override
	public String getPrefix( String siteName )
	{
		return sitePrefix.containsKey( siteName ) ? sitePrefix.get( siteName ) : "";
	}
	
	@Override
	public void setPrefix( String prefix, String siteName )
	{
		sitePrefix.put( siteName, prefix );
		this.save();
	}
	
	@Override
	public String getSuffix( String siteName )
	{
		return siteSuffix.containsKey( siteName ) ? siteSuffix.get( siteName ) : "";
	}
	
	@Override
	public void setSuffix( String suffix, String siteName )
	{
		siteSuffix.put( siteName, suffix );
		this.save();
	}
	
	@Override
	public String[] getPermissions( String site )
	{
		String[] perms = sitesPermissions.containsKey( site ) ? sitesPermissions.get( site ) : new String[0];
		if ( commonPermissions != null )
		{
			perms = (String[]) ArrayUtils.addAll( perms, commonPermissions );
		}
		return perms;
	}
	
	@Override
	public Map<String, String[]> getAllPermissions()
	{
		return sitesPermissions;
	}
	
	@Override
	public void setPermissions( String[] permissions, String site )
	{
		if ( site == "" )
		{
			commonPermissions = permissions;
		}
		else
		{
			sitesPermissions.put( site, permissions );
		}
		
		this.save();
	}
	
	@Override
	public String getOption( String option, String site, String defaultValue )
	{
		if ( sitesOptions.containsKey( site ) )
		{
			Map<String, String> siteOption = sitesOptions.get( site );
			if ( siteOption.containsKey( option ) )
			{
				return siteOption.get( option );
			}
		}
		return defaultValue;
	}
	
	@Override
	public void setOption( String option, String value, String site )
	{
		Map<String, String> newOption = new HashMap<String, String>();
		newOption.put( option, value );
		sitesOptions.put( site, newOption );
		
		this.save();
	}
	
	@Override
	public Map<String, String> getOptions( String site )
	{
		return sitesOptions.containsKey( site ) ? sitesOptions.get( site ) : new HashMap<String, String>();
	}
	
	@Override
	public Map<String, Map<String, String>> getAllOptions()
	{
		return sitesOptions;
	}
	
	@Override
	public void save()
	{
		
	}
	
	@Override
	public void remove()
	{
		// Do Nothing
	}
	
	@Override
	public String[] getSites()
	{
		Set<String> sites = new HashSet<String>();
		
		sites.addAll( sitesOptions.keySet() );
		sites.addAll( sitesPermissions.keySet() );
		
		return sites.toArray( new String[0] );
	}
	
	public void setParents( String[] parentGroups, String siteName )
	{
		parents.put( siteName, new HashSet<String>( Arrays.asList( parentGroups ) ) );
	}
	
	public String[] getParentNames( String siteName )
	{
		if ( this.parents == null )
		{}
		
		if ( this.parents.containsKey( siteName ) )
		{
			return this.parents.get( siteName ).toArray( new String[0] );
		}
		
		return new String[0];
	}
	
}
