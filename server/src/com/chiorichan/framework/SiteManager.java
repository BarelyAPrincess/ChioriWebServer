package com.chiorichan.framework;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.chiorichan.Loader;
import com.chiorichan.database.SqlConnector;

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
		try
		{
			// Load sites from the database
			ResultSet rs = sql.query( "SELECT * FROM `sites`;" );
			
			if ( sql.getRowCount( rs ) > 0 )
			{
				do
				{
					if ( rs.getString( "siteID" ).equals( "framework" ) )
						siteMap.put( "framework", new FrameworkSite( rs ).setDatabase( Loader.getPersistenceManager().getSql() ) );
					else
						siteMap.put( rs.getString( "siteID" ), new Site( rs ) );
				}
				while ( rs.next() );
			}
		}
		catch ( SQLException | SiteException e )
		{
			// TODO: Better this error catch. Also make some way for a new table to be created if not exist.
			e.printStackTrace();
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
}
