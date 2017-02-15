/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * <p>
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 * <p>
 * All Rights Reserved.
 */
package com.chiorichan.http;

import com.chiorichan.AppConfig;
import com.chiorichan.datastore.sql.SQLTable;
import com.chiorichan.datastore.sql.bases.SQLDatastore;
import com.chiorichan.datastore.sql.query.SQLQuerySelect;
import com.chiorichan.logger.Log;
import com.chiorichan.site.Site;
import com.chiorichan.zutils.ZIO;
import com.chiorichan.zutils.ZObjects;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Keeps track of routes
 */
public class Routes
{
	public enum RouteType
	{
		NOTSET(), SQL(), FILE();

		@Override
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

	/*
	 * Prevents file and sql lag from reloading the routes for every dozen requests made within a small span of time.
	 */
	private long lastRequest = 0;
	private Set<Route> routes = new HashSet<>();

	private Site site;

	public Routes( Site site )
	{
		this.site = site;
	}

	public Route searchRoutes( String uri, String host ) throws IOException
	{
		synchronized ( this )
		{
			File routesFile = new File( site.directory(), "routes" );

			if ( routes.size() < 1 || System.currentTimeMillis() - lastRequest > 2500 )
			{
				routes.clear();

				try
				{
					if ( routesFile.exists() )
						for ( String l : ZIO.readFileToLines( routesFile ) )
							try
							{
								if ( !l.startsWith( "#" ) )
									routes.add( new Route( l, site ) );
							}
							catch ( IOException e1 )
							{
								// Ignore
							}
				}
				catch ( IOException e )
				{
					e.printStackTrace();
				}

				try
				{
					SQLDatastore sql = AppConfig.get().getDatabase();

					if ( sql != null && sql.initialized() )
					{
						if ( !sql.table( "pages" ).exists() )
						{
							Log.get().info( "We detected the non-existence of table 'pages' in the server database, we will attempt to create it now." );

							SQLTable table = sql.table( "pages" );
							table.addColumnVar( "site", 255 );
							table.addColumnVar( "domain", 255 );
							table.addColumnVar( "page", 255 );
							table.addColumnVar( "title", 255 );
							table.addColumnVar( "reqlevel", 255, "-1" );
							table.addColumnVar( "theme", 255 );
							table.addColumnVar( "view", 255 );
							table.addColumnText( "html" );
							table.addColumnVar( "file", 255 );
						}

						// ResultSet rs = sql.query( "SELECT * FROM `pages` WHERE (subdomain = '" + subdomain + "' OR subdomain = '') AND domain = '" + domain + "' UNION SELECT * FROM `pages` WHERE (subdomain = '" + subdomain +
						// "' OR subdomain = '') AND domain = '';" );

						SQLQuerySelect result = sql.table( "pages" ).select().where( "domain" ).matches( host ).or().where( "domain" ).matches( "" ).execute();

						for ( Map<String, Object> row : result.set() )
							routes.add( new Route( ZObjects.castMap( row, String.class, String.class ), site ) );

						// ResultSet rs = sql.query( "SELECT * FROM `pages` WHERE domain = '" + domain + "' OR domain = '';" );
						// if ( sql.getRowCount( rs ) > 0 )
						// do
						// routes.add( new Route( rs, site ) );
						// while ( rs.next() );
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
				Map<String, Route> matches = new TreeMap<>();
				int keyInter = 0;

				for ( Route route : routes )
				{
					String weight = route.match( uri, host );
					if ( weight != null )
					{
						matches.put( weight + keyInter, route );
						keyInter++;
					}
				}

				if ( matches.size() > 0 )
					return ( Route ) matches.values().toArray()[0];
				else
					Log.get().fine( String.format( "Failed to find a page redirect... {host=%s,uri=%s}", host, uri ) );
			}
			else
				Log.get().fine( String.format( "Failed to find a page redirect... {host=%s,uri=%s}", host, uri ) );

			return null;
		}
	}
}
