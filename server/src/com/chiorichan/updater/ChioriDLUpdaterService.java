package com.chiorichan.updater;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

public class ChioriDLUpdaterService
{
	private static final String API_PREFIX_ARTIFACT = "/api/1.0/downloads/projects/chioriwebserver/view/";
	private static final String API_PREFIX_CHANNEL = "/api/1.0/downloads/channels/";
	private static final DateDeserializer dateDeserializer = new DateDeserializer();
	private final String host;
	
	public ChioriDLUpdaterService(String host)
	{
		this.host = host;
	}
	
	public ArtifactDetails getArtifact( String slug, String name )
	{
		try
		{
			return fetchArtifact( slug );
		}
		catch ( UnsupportedEncodingException ex )
		{
			Logger.getLogger( ChioriDLUpdaterService.class.getName() ).log( Level.WARNING, "Could not get " + name + ": " + ex.getClass().getSimpleName() );
		}
		catch ( UnknownHostException ex )
		{
			Logger.getLogger( ChioriDLUpdaterService.class.getName() ).log( Level.WARNING, "There was a problem resolving the host: " + host + ". Do you have a properly setup internet connection?" );
		}
		catch ( IOException ex )
		{
			Logger.getLogger( ChioriDLUpdaterService.class.getName() ).log( Level.WARNING, "Could not get " + name + ": " + ex.getClass().getSimpleName() );
		}
		
		return null;
	}
	
	private String getUserAgent()
	{
		return "ChioriWebServer/" + ChioriDLUpdaterService.class.getPackage().getImplementationVersion() + "/" + System.getProperty( "java.version" );
	}
	
	public ArtifactDetails fetchArtifact( String slug ) throws IOException, UnknownHostException
	{
		URL url = new URL( "http", host, API_PREFIX_ARTIFACT + slug + "/" );
		InputStreamReader reader = null;
		
		try
		{
			URLConnection connection = url.openConnection();
			connection.setRequestProperty( "User-Agent", getUserAgent() );
			reader = new InputStreamReader( connection.getInputStream() );
			Gson gson = new GsonBuilder().registerTypeAdapter( Date.class, dateDeserializer ).setFieldNamingPolicy( FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES ).create();
			return gson.fromJson( reader, ArtifactDetails.class );
		}
		finally
		{
			if ( reader != null )
			{
				reader.close();
			}
		}
	}
	
	public ArtifactDetails.ChannelDetails getChannel( String slug, String name )
	{
		try
		{
			return fetchChannel( slug );
		}
		catch ( UnsupportedEncodingException ex )
		{
			Logger.getLogger( ChioriDLUpdaterService.class.getName() ).log( Level.WARNING, "Could not get " + name + ": " + ex.getClass().getSimpleName() );
		}
		catch ( IOException ex )
		{
			Logger.getLogger( ChioriDLUpdaterService.class.getName() ).log( Level.WARNING, "Could not get " + name + ": " + ex.getClass().getSimpleName() );
		}
		
		return null;
	}
	
	public ArtifactDetails.ChannelDetails fetchChannel( String slug ) throws IOException
	{
		URL url = new URL( "http", host, API_PREFIX_CHANNEL + slug + "/" );
		InputStreamReader reader = null;
		
		try
		{
			URLConnection connection = url.openConnection();
			connection.setRequestProperty( "User-Agent", getUserAgent() );
			reader = new InputStreamReader( connection.getInputStream() );
			Gson gson = new GsonBuilder().registerTypeAdapter( Date.class, dateDeserializer ).setFieldNamingPolicy( FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES ).create();
			ArtifactDetails.ChannelDetails fromJson = gson.fromJson( reader, ArtifactDetails.ChannelDetails.class );
			
			return fromJson;
		}
		finally
		{
			if ( reader != null )
			{
				reader.close();
			}
		}
	}
	
	@SuppressWarnings( "rawtypes" )
	static class DateDeserializer implements JsonDeserializer
	{
		private static final SimpleDateFormat format = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
		
		public Date deserialize( JsonElement je, Type type, JsonDeserializationContext jdc ) throws JsonParseException
		{
			try
			{
				return format.parse( je.getAsString() );
			}
			catch ( ParseException ex )
			{
				throw new JsonParseException( "Date is not formatted correctly", ex );
			}
		}
	}
}
