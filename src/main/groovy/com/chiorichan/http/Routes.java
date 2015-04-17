/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
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
import com.chiorichan.site.Site;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Keeps track of routes
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
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
	
	public Routes( Site site )
	{
		this.site = site;
	}
	
	public Route searchRoutes( String uri, String domain, String subdomain ) throws IOException
	{
		synchronized ( this )
		{
			File routesFile = new File( Loader.getWebRoot() + Loader.PATH_SEPERATOR + site.getRoot() + Loader.PATH_SEPERATOR + "routes" );
			
			if ( routes.size() < 1 || System.currentTimeMillis() - lastRequest > 2500 )
			{
				routes.clear();
				
				try
				{
					if ( routesFile.exists() )
					{
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
						if ( !sql.tableExist( "pages" ) )
						{
							DatabaseEngine.getLogger().info( "We detected the non-existence of table 'pages' in the server database, we will attempt to create it now." );
							
							String table = "CREATE TABLE `pages` (";
							table += " `site` varchar(255) NOT NULL,";
							table += " `domain` varchar(255) NOT NULL,";
							table += " `page` varchar(255) NOT NULL,";
							table += " `title` varchar(255) NOT NULL,";
							table += " `reqlevel` varchar(255) NOT NULL DEFAULT '-1',";
							table += " `theme` varchar(255) NOT NULL,";
							table += " `view` varchar(255) NOT NULL,";
							table += " `html` text NOT NULL,";
							table += " `file` varchar(255) NOT NULL";
							table += ");";
							
							sql.queryUpdate( table );
						}
						
						// ResultSet rs = sql.query( "SELECT * FROM `pages` WHERE (subdomain = '" + subdomain + "' OR subdomain = '') AND domain = '" + domain + "' UNION SELECT * FROM `pages` WHERE (subdomain = '" + subdomain +
						// "' OR subdomain = '') AND domain = '';" );
						ResultSet rs = sql.query( "SELECT * FROM `pages` WHERE domain = '" + domain + "' OR domain = '';" );
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
					return ( Route ) matches.values().toArray()[0];
				}
				else
					Loader.getLogger().finer( "Failed to find a page redirect for Rewrite... '" + subdomain + "." + domain + "' '" + uri + "'" );
			}
			else
				Loader.getLogger().finer( "Failed to find a page redirect for Rewrite... '" + subdomain + "." + domain + "' '" + uri + "'" );
			
			return null;
		}
	}
}
