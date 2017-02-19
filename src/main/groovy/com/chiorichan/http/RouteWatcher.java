package com.chiorichan.http;

import com.chiorichan.configuration.ConfigurationSection;
import com.chiorichan.configuration.types.yaml.YamlConfiguration;
import com.chiorichan.io.FileWatcher;
import com.chiorichan.logger.Log;
import com.chiorichan.net.NetworkManager;
import com.chiorichan.zutils.ZIO;
import com.chiorichan.zutils.ZObjects;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class RouteWatcher extends FileWatcher
{
	private Routes parent;

	public RouteWatcher( Routes parent, File fileToWatch )
	{
		super( fileToWatch );

		this.parent = parent;
	}

	@Override
	public void readChanges()
	{
		if ( !fileToWatch.exists() )
			return;

		Set<Route> routes = parent.routes;

		if ( fileToWatch.getName().endsWith( ".json" ) )
		{
			routes.clear();

			int line = 0;
			AtomicInteger inx = new AtomicInteger();

			Log.get().fine( "Loading Routes from JSON file '" + ZIO.relPath( fileToWatch ) + "'" );

			try
			{
				for ( String l : ZIO.readFileToLines( fileToWatch ) )
				{
					try
					{
						line++;
						if ( !l.startsWith( "#" ) && !ZObjects.isEmpty( l.trim() ) )
						{
							Map<String, String> values = new HashMap<>();
							Map<String, String> rewrites = new HashMap<>();

							JSONObject obj = new JSONObject( l );

							String id = obj.optString( "id" );
							if ( ZObjects.isEmpty( id ) )
							{
								do
								{
									id = "route_rule_" + String.format( "%04d", inx.getAndIncrement() );
								}
								while ( parent.hasRoute( id ) );
							}
							else if ( parent.hasRoute( id ) )
							{
								NetworkManager.getLogger().severe( String.format( "Found duplicate route id '%s' in route file '%s', route will be ignored.", id, ZIO.relPath( fileToWatch ) ) );
								continue;
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

							routes.add( new Route( id, parent.site, values, rewrites ) );
						}
					}
					catch ( JSONException e )
					{
						Log.get().severe( "Failed to parse '" + ZIO.relPath( fileToWatch ) + "' file, line " + line + ".", e );
					}
				}
			}
			catch ( IOException e )
			{
				Log.get().severe( "Failed to load '" + ZIO.relPath( fileToWatch ) + "' file.", e );
			}

			Log.get().fine( "Finished Loading Routes from JSON file '" + ZIO.relPath( fileToWatch ) + "'" );
		}
		else if ( fileToWatch.getName().endsWith( ".yaml" ) )
		{
			Log.get().fine( "Loading Routes from YAML file '" + ZIO.relPath( fileToWatch ) + "'" );

			YamlConfiguration yaml = YamlConfiguration.loadConfiguration( fileToWatch );

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

					if ( parent.hasRoute( id ) )
					{
						NetworkManager.getLogger().severe( String.format( "Found duplicate route id '%s' in route file '%s', route will be ignored.", id, ZIO.relPath( fileToWatch ) ) );
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

					routes.add( new Route( id, parent.site, values, rewrites ) );
				}

			Log.get().fine( "Finished Loading Routes from YAML file '" + ZIO.relPath( fileToWatch ) + "'" );
		}
	}
}
