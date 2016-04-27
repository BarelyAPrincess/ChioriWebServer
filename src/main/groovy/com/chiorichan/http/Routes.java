/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2016 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.http;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import com.chiorichan.AppController;
import com.chiorichan.datastore.sql.SQLTable;
import com.chiorichan.datastore.sql.bases.SQLDatastore;
import com.chiorichan.datastore.sql.query.SQLQuerySelect;
import com.chiorichan.logger.Log;
import com.chiorichan.site.Site;
import com.chiorichan.util.MapCaster;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

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

	/**
	 * Prevents file and sql lag from reloading the routes for every dozen requests made within a small span of time.
	 */
	private long lastRequest = 0;
	private Set<Route> routes = Sets.newHashSet();

	private Site site;

	public Routes( Site site )
	{
		this.site = site;
	}

	public Route searchRoutes( String uri, String domain, String subdomain ) throws IOException
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
					{
						String contents = FileUtils.readFileToString( routesFile );
						for ( String l : contents.split( "\n" ) )
							try
							{
								if ( !l.startsWith( "#" ) )
									routes.add( new Route( l, site ) );
							}
							catch ( IOException e1 )
							{

							}
					}
				}
				catch ( IOException e )
				{
					e.printStackTrace();
				}

				try
				{
					SQLDatastore sql = AppController.config().getDatabase();

					if ( sql != null && sql.initalized() )
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

						SQLQuerySelect result = sql.table( "pages" ).select().where( "domain" ).matches( domain ).or().where( "domain" ).matches( "" ).execute();

						for ( Map<String, Object> row : result.set() )
							routes.add( new Route( new MapCaster<String, String>( String.class, String.class ).castTypes( row ), site ) );

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
					return ( Route ) matches.values().toArray()[0];
				else
					Log.get().fine( "Failed to find a page redirect for... '" + subdomain + "." + domain + "' '" + uri + "'" );
			}
			else
				Log.get().fine( "Failed to find a page redirect for... '" + subdomain + "." + domain + "' '" + uri + "'" );

			return null;
		}
	}
}
