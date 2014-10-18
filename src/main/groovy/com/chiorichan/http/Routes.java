package com.chiorichan.http;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import com.chiorichan.Loader;
import com.chiorichan.database.DatabaseEngine;
import com.chiorichan.framework.Site;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class Routes
{
	/**
	 * Prevents file and sql lag from reloading the routes for every dozen requests made within a small span of time.
	 */
	private long lastRequest = 0;
	private Set<Route> routes = Sets.newHashSet();
	private Site site;
	
	public enum RouteType
	{
		NOTSET(), SQL(), FILE();
		
		public String toString()
		{
			switch ( this )
			{
				case FILE:
					return "File";
				case SQL:
					return "Sql";
				default:
					return "Not Set";
			}
		}
	}
	
	public Routes(Site _site)
	{
		site = _site;
	}
	
	public Route searchRoutes( String uri, String domain, String subdomain ) throws IOException
	{
		File routesFile = new File( Loader.getWebRoot() + Loader.PATH_SEPERATOR + site.getRoot() + Loader.PATH_SEPERATOR + "routes" );
		
		if ( routes.size() < 1 || System.currentTimeMillis() - lastRequest > 1000 )
		{
			try
			{
				if ( routesFile.exists() )
				{
					routes.clear();
					String contents = FileUtils.readFileToString( routesFile );
					for ( String l : contents.split( "\n" ) )
					{
						try
						{
							routes.add( new Route( l, site ) );
						}
						catch ( IOException e1 )
						{	
							
						}
					}
				}
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}
			
			try
			{
				DatabaseEngine sql = Loader.getDatabase();
				
				if ( sql != null )
				{
					ResultSet rs = sql.query( "SELECT * FROM `pages` WHERE (site = '" + subdomain + "' OR site = '') AND domain = '" + domain + "' UNION SELECT * FROM `pages` WHERE (site = '" + subdomain + "' OR site = '') AND domain = '';" );
					if ( sql.getRowCount( rs ) > 0 )
					{
						do
						{
							routes.add( new Route( rs, site ) );
						}
						while ( rs.next() );
					}
				}
			}
			catch ( SQLException e )
			{
				throw new IOException( e );
			}
		}
		lastRequest = System.currentTimeMillis();
		
		if ( routes.size() > 0 )
		{
			Map<String, Route> matches = Maps.newTreeMap();
			int keyInter = 0;
			
			for ( Route route : routes )
			{
				String weight = route.match( domain, subdomain, uri );
				if ( weight != null )
				{
					matches.put( weight + keyInter, route );
					keyInter++;
				}
			}
			
			if ( matches.size() > 0 )
			{
				return (Route) matches.values().toArray()[0];
			}
			else
				Loader.getLogger().fine( "Failed to find a page redirect for Rewrite... '" + subdomain + "." + domain + "' '" + uri + "'" );
		}
		else
			Loader.getLogger().fine( "Failed to find a page redirect for Rewrite... '" + subdomain + "." + domain + "' '" + uri + "'" );
		
		return null;
	}
}
