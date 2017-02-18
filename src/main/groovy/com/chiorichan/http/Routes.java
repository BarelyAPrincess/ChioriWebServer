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
import com.chiorichan.net.NetworkManager;
import com.chiorichan.site.Site;
import com.chiorichan.zutils.ZIO;
import com.chiorichan.zutils.ZObjects;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

			return routes.stream().filter( r -> id.equalsIgnoreCase( r.getId() ) || id.matches( r.getId() ) ).findFirst().orElse( null );
		}
	}

	public boolean hasRoute( String id )
	{
		return routes.stream().filter( r -> id.equalsIgnoreCase( r.getId() ) || id.matches( r.getId() ) ).findAny().isPresent();
	}

	public RouteResult searchRoutes( String uri, String host ) throws IOException
	{
		synchronized ( this )
		{
			checkRoutes();

			Map<String, RouteResult> matches = new TreeMap<>();
			int keyInter = 0;

			for ( Route route : routes )
			{
				RouteResult result = route.match( uri, host );
				if ( result != null )
				{
					matches.put( result.getWeight() + keyInter, result );
					keyInter++;
				}
			}

			if ( matches.size() > 0 )
				return ( RouteResult ) matches.values().toArray()[0];
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
			int inc = 0;

			try
			{
				if ( routesJson.exists() )
				{
					for ( String l : ZIO.readFileToLines( routesJson ) )
					{
						line++;
						if ( !l.startsWith( "#" ) && !ZObjects.isEmpty( l ) )
						{
							Map<String, String> values = new HashMap<>();
							Map<String, String> rewrites = new HashMap<>();

							JSONObject obj = new JSONObject( l );

							String id = obj.optString( "id" );
							if ( ZObjects.isEmpty( id ) )
							{
								do
								{
									id = "route_rule_" + String.format( "%04d", inc );
									inc++;
								}
								while ( !hasRoute( id ) );
							}
							else
							{
								if ( hasRoute( id ) )
								{
									NetworkManager.getLogger().severe( String.format( "Found duplicate route id '%s' in route file '%s', route will be ignored.", id, ZIO.relPath( routesJson ) ) );
									continue;
								}
							}

							for ( String sectionKey : obj.keySet() )
							{
								Object sectionObject = obj.get( sectionKey );

								if ( sectionObject instanceof JSONObject && "vargs".equals( sectionKey ) )
								{
									for ( String argsKey : ( ( JSONObject ) sectionObject ).keySet() )
									{
										Object argsObject = ( ( JSONObject ) sectionObject ).get( argsKey );
										if ( !( argsObject instanceof JSONObject ) && !( argsObject instanceof JSONArray ) )
											try
											{
												rewrites.put( argsKey, ZObjects.castToStringWithException( argsObject ) );
											}
											catch ( Exception e )
											{
												// Ignore
											}
									}
								}
								else if ( !( sectionObject instanceof JSONObject ) && !( sectionObject instanceof JSONArray ) )
								{
									try
									{
										values.put( sectionKey, ZObjects.castToStringWithException( sectionObject ) );
									}
									catch ( Exception e )
									{
										// Ignore
									}
								}
							}

							routes.add( new Route( id, site, values, rewrites ) );
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
						String id = key;
						ConfigurationSection section = yaml.getConfigurationSection( key );
						if ( section.contains( "id" ) )
						{
							id = section.getString( "id" );
							section.set( "id", null );
						}

						if ( hasRoute( id ) )
						{
							NetworkManager.getLogger().severe( String.format( "Found duplicate route id '%s' in route file '%s', route will be ignored.", id, ZIO.relPath( routesJson ) ) );
							continue;
						}

						Map<String, String> values = new HashMap<>();
						Map<String, String> rewrites = new HashMap<>();

						for ( String sectionKey : section.getKeys() )
						{
							if ( section.isConfigurationSection( sectionKey ) && "vargs".equals( sectionKey ) )
							{
								ConfigurationSection args = section.getConfigurationSection( sectionKey );
								for ( String argsKey : args.getKeys() )
									if ( !args.isConfigurationSection( argsKey ) )
										try
										{
											rewrites.put( argsKey, ZObjects.castToStringWithException( args.get( argsKey ) ) );
										}
										catch ( Exception e )
										{
											// Ignore
										}
							}
							else if ( !section.isConfigurationSection( sectionKey ) )
							{
								try
								{
									values.put( sectionKey, ZObjects.castToStringWithException( section.get( sectionKey ) ) );
								}
								catch ( Exception e )
								{
									// Ignore
								}
							}
						}

						routes.add( new Route( id, site, values, rewrites ) );
					}
			}
		}
		lastRequest = System.currentTimeMillis();
	}
}
