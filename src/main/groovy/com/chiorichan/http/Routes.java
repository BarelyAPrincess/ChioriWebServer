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

import com.chiorichan.configuration.ConfigurationSection;
import com.chiorichan.configuration.types.yaml.YamlConfiguration;
import com.chiorichan.logger.Log;
import com.chiorichan.site.Site;
import com.chiorichan.zutils.ZIO;
import com.chiorichan.zutils.ZObjects;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Keeps track of routes
 */
public class Routes
{
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

	public Route routeUrl( String id )
	{
		ZObjects.notEmpty( id );

		synchronized ( this )
		{
			checkRoutes();

			return routes.stream().filter( r -> r.hasParam( "id" ) && ( r.getParam( "id" ).equalsIgnoreCase( id ) || r.getParam( "id" ).matches( id ) ) ).findFirst().orElse( null );
		}
	}

	public Route searchRoutes( String uri, String host ) throws IOException
	{
		synchronized ( this )
		{
			checkRoutes();

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
				Log.get().fine( String.format( "Failed to find route for... {host=%s,uri=%s}", host, uri ) );

			return null;
		}
	}

	private void checkRoutes()
	{
		if ( routes.size() < 1 || System.currentTimeMillis() - lastRequest > 2500 )
		{
			File routesJson = new File( site.directory(), "routes.json" );
			File routesYaml = new File( site.directory(), "routes.yaml" );

			routes.clear();

			int line = 0;

			try
			{
				if ( routesJson.exists() )
				{
					for ( String l : ZIO.readFileToLines( routesJson ) )
					{
						line++;
						if ( !l.startsWith( "#" ) && !ZObjects.isEmpty( l ) )
						{
							Map<String, String> values = new HashMap<String, String>()
							{
								{
									JSONObject obj = new JSONObject( l );
									for ( String key : obj.keySet() )
										try
										{
											put( key, ZObjects.castToStringWithException( obj.get( key ) ) );
										}
										catch ( Exception e )
										{
											// Ignore
										}
								}
							};

							routes.add( new Route( values, site ) );
						}
					}
				}
			}
			catch ( IOException e )
			{
				Log.get().severe( "Failed to load 'routes.json' file.", e );
			}
			catch ( JSONException e )
			{
				Log.get().severe( "Failed to parse 'routes.json' file, line " + line + ".", e );
			}

			if ( routesYaml.exists() )
			{
				YamlConfiguration yaml = YamlConfiguration.loadConfiguration( routesYaml );

				for ( String key : yaml.getKeys() )
					if ( yaml.isConfigurationSection( key ) )
					{
						Map<String, String> values = new HashMap<String, String>()
						{{
							put( "id", key );
							ConfigurationSection section = yaml.getConfigurationSection( key );
							for ( String subkey : section.getKeys() )
							{
								if ( !section.isConfigurationSection( subkey ) )
									try
									{
										put( subkey, ZObjects.castToStringWithException( section.get( subkey ) ) );
									}
									catch ( Exception e )
									{
										// Ignore
									}
							}
						}};
						routes.add( new Route( values, site ) );
					}
			}
		}
		lastRequest = System.currentTimeMillis();
	}
}
