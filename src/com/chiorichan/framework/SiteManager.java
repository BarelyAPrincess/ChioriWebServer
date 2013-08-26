package com.chiorichan.framework;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.chiorichan.database.SqlConnector;

public class SiteManager
{
	SqlConnector sql;
	Map<String, Site> siteMap = new HashMap<String, Site>();
	
	public SiteManager ( SqlConnector sql0 )
	{
		sql = sql0;
	}
	
	public void loadSites ()
	{
		try
		{
			// Load sites from the database
			ResultSet rs = sql.query( "SELECT * FROM `sites`;" );
			
			if ( sql.getRowCount( rs ) > 0 )
			{
				do
				{
					siteMap.put( rs.getString( "siteID" ), new Site( rs ) );
				}
				while ( rs.next() );
			}
		}
		catch ( SQLException e )
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
}
