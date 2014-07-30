package com.chiorichan.framework;

import java.io.File;
import java.io.FileFilter;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.filefilter.WildcardFileFilter;

import com.chiorichan.Loader;
import com.chiorichan.StartupException;
import com.chiorichan.database.SqlConnector;
import com.chiorichan.util.FileUtil;

public class SiteManager
{
	Map<String, Site> siteMap;
	
	public SiteManager()
	{
		siteMap = new LinkedHashMap<String, Site>();
	}
	
	public void init() throws StartupException
	{
		if ( siteMap.size() > 0 )
			throw new StartupException( "Site manager already has sites loaded. Please unload the existing sites first." );
		
		SqlConnector sql = Loader.getPersistenceManager().getDatabase();
		
		// Load sites from YAML Filebase.
		File siteFileBase = new File( "sites" );
		
		FileUtil.directoryHealthCheck( siteFileBase );
		
		// We make sure the default framework YAML FileBase exists and if not we copy it from the Jar.
		if ( !new File( siteFileBase, "000-default.yaml" ).exists() )
		{
			try
			{
				FileUtil.copy( new File( getClass().getClassLoader().getResource( "com/chiorichan/default-site.yaml" ).toURI() ), new File( siteFileBase, "000-default.yaml" ) );
			}
			catch ( URISyntaxException e1 )
			{}
		}
		
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
					}
				}
				catch ( SiteException e )
				{
					Loader.getLogger().warning( "Exception encountered while loading a site from YAML FileBase, Reason: " + e.getMessage() );
					if ( e.getCause() != null )
						e.getCause().printStackTrace();
				}
			}
		
		// Load sites from Framework Database.
		if ( sql != null && sql.isConnected() )
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
							}
						}
						catch ( SiteException e )
						{
							Loader.getLogger().severe( "Exception encountered while loading a site from SQL DataBase, Reason: " + e.getMessage() );
							if ( e.getCause() != null )
								e.getCause().printStackTrace();
						}
						
					}
					while ( rs.next() );
				}
			}
			catch ( SQLException e )
			{
				Loader.getLogger().warning( "Exception encountered while loading a sites from Database", e );
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

	public void reload()
	{
		// RELOAD ALL
	}
}
