/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * <p>
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 * <p>
 * All Rights Reserved.
 */
package com.chiorichan.updater;

import com.chiorichan.zutils.ZHttp;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DownloadUpdaterService
{
	private static final String API_PREFIX_ARTIFACT = "/job/ChioriWebServer/";
	// private static final String API_PREFIX_CHANNEL = "/api/1.0/downloads/channels/";
	// private static final DateDeserializer dateDeserializer = new DateDeserializer();
	private final String host;

	public DownloadUpdaterService( String host )
	{
		this.host = host;
	}

	public BuildArtifact getArtifact( String slug, String name )
	{
		try
		{
			return fetchArtifact( slug );
		}
		catch ( UnsupportedEncodingException ex )
		{
			Logger.getLogger( DownloadUpdaterService.class.getName() ).log( Level.WARNING, "Could not get " + name + ": " + ex.getClass().getSimpleName() );
		}
		catch ( UnknownHostException ex )
		{
			Logger.getLogger( DownloadUpdaterService.class.getName() ).log( Level.WARNING, "There was a problem resolving the host: " + host + ". Do you have a properly setup internet connection?" );
		}
		catch ( IOException ex )
		{
			Logger.getLogger( DownloadUpdaterService.class.getName() ).log( Level.WARNING, "Could not get " + name + ": " + ex.getClass().getSimpleName() );
		}

		return null;
	}

	public BuildArtifact fetchArtifact( String slug ) throws IOException
	{
		URL url = new URL( "http", host, API_PREFIX_ARTIFACT + slug + "/api/json" );
		return fetchArtifact( url );
	}

	public BuildArtifact fetchArtifact( URL url ) throws IOException
	{
		InputStreamReader reader = null;

		try
		{
			URLConnection connection = url.openConnection();
			connection.setRequestProperty( "User-Agent", ZHttp.getUserAgent() );
			reader = new InputStreamReader( connection.getInputStream() );
			// Gson gson = new GsonBuilder().registerTypeAdapter( Date.class, dateDeserializer ).setFieldNamingPolicy( FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES ).create();
			Gson gson = new Gson();
			return gson.fromJson( reader, BuildArtifact.class );
		}
		finally
		{
			if ( reader != null )
			{
				reader.close();
			}
		}
	}

	public ProjectArtifact getProjectArtifact() throws IOException
	{
		URL url = new URL( "http", host, API_PREFIX_ARTIFACT + "api/json" );
		InputStreamReader reader = null;

		try
		{
			URLConnection connection = url.openConnection();
			connection.setRequestProperty( "User-Agent", ZHttp.getUserAgent() );
			reader = new InputStreamReader( connection.getInputStream() );
			// Gson gson = new GsonBuilder().registerTypeAdapter( Date.class, dateDeserializer ).setFieldNamingPolicy( FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES ).create();
			Gson gson = new Gson();
			return gson.fromJson( reader, ProjectArtifact.class );
		}
		finally
		{
			if ( reader != null )
			{
				reader.close();
			}
		}
	}

	public Properties getBuildProperties( String slug, String name )
	{
		try
		{
			return fetchBuildProperties( slug );
		}
		catch ( UnsupportedEncodingException ex )
		{
			Logger.getLogger( DownloadUpdaterService.class.getName() ).log( Level.WARNING, "Could not get " + name + ": " + ex.getClass().getSimpleName() );
		}
		catch ( UnknownHostException ex )
		{
			Logger.getLogger( DownloadUpdaterService.class.getName() ).log( Level.WARNING, "There was a problem resolving the host: " + host + ". Do you have a properly setup internet connection?" );
		}
		catch ( IOException ex )
		{
			Logger.getLogger( DownloadUpdaterService.class.getName() ).log( Level.WARNING, "Could not get " + name + ": " + ex.getClass().getSimpleName() );
		}

		return null;
	}

	public Properties fetchBuildProperties( String slug ) throws IOException
	{
		URL url = new URL( "http", host, API_PREFIX_ARTIFACT + slug + "/artifact/build/dist/build.properties" );
		InputStreamReader reader = null;
		Properties prop = new Properties();

		try
		{
			URLConnection connection = url.openConnection();
			connection.setRequestProperty( "User-Agent", ZHttp.getUserAgent() );
			reader = new InputStreamReader( connection.getInputStream() );
			prop.load( reader );
		}
		finally
		{
			if ( reader != null )
			{
				reader.close();
			}
		}

		return prop;
	}
}
