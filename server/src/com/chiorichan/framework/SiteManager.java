package com.chiorichan.framework;

import java.io.File;
import java.io.FileFilter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.filefilter.WildcardFileFilter;

import com.chiorichan.Loader;
import com.chiorichan.database.SqlConnector;
import com.chiorichan.file.YamlConfiguration;

public class SiteManager
{
	SqlConnector sql;
	Map<String, Site> siteMap = new LinkedHashMap<String, Site>();
	
	public SiteManager(SqlConnector sql0)
	{
		sql = sql0;
	}
	
	public void loadSites()
	{
		// Load sites from YAML Filebase.
		File siteFileBase = new File( "sites" );
		
		if ( siteFileBase.isFile() )
			siteFileBase.delete();
		
		if ( !siteFileBase.exists() )
			siteFileBase.mkdirs();
		
		FileFilter fileFilter = new WildcardFileFilter( "*.yaml" );
		File[] files = siteFileBase.listFiles( fileFilter );
		
		if ( files != null && files.length > 0 )
			for ( File f : files )
			{
				try
				{
					Site site = new Site( f );
					
					if ( site != null )
					{
						siteMap.put( site.siteId, site );
						
						if ( site.siteId.equals( "framework" ) )
							site.setDatabase( Loader.getPersistenceManager().getSql() );
					}
				}
				catch ( SiteException e )
				{
					Loader.getLogger().severe( "Exception encountered while loading a site from YAML FileBase, Reason: " + e.getMessage() );
					if ( e.getCause() != null )
						e.getCause().printStackTrace();
				}
			}
		
		// Load sites from Framework Database.
		try
		{
			// Load sites from the database
			ResultSet rs = sql.query( "SELECT * FROM `sites`;" );
			
			if ( sql.getRowCount( rs ) > 0 )
			{
				do
				{
					try
					{
						Site site = new Site( rs );
						
						if ( site != null )
						{
							siteMap.put( site.siteId, site );
							
							if ( rs.getString( "siteID" ).equals( "framework" ) )
								site.setDatabase( Loader.getPersistenceManager().getSql() );
						}
					}
					catch ( SiteException e )
					{
						Loader.getLogger().severe( "Exception encountered while loading a site from Database, Reason: " + e.getMessage() );
						if ( e.getCause() != null )
							e.getCause().printStackTrace();
					}
					
				}
				while ( rs.next() );
			}
		}
		catch ( SQLException e )
		{
			Loader.getLogger().severe( "Exception encountered while loading a sites from Database", e );
		}
	}
	
	public Site getSiteById( String siteId )
	{
		return siteMap.get( siteId );
	}
	
	public Site getSiteByDomain( String domain )
	{
		for ( Site site : siteMap.values() )
		{
			if ( site != null )
			{
				if ( site.domain.equalsIgnoreCase( domain.trim() ) )
					return site;
			}
		}
		
		return null;
	}
	
	public List<Site> getSites()
	{
		return new ArrayList<Site>( siteMap.values() );
	}
	
	public Site getFrameworkSite()
	{
		return getSiteById( "framework" );
	}
}
