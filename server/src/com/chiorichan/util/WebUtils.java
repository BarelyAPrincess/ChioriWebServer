package com.chiorichan.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.io.IOUtils;

import com.chiorichan.Loader;

public class WebUtils
{
	/**
	 * Establishes an HttpURLConnection from a URL, with the correct configuration to receive content from the given URL.
	 * 
	 * @param url The URL to set up and receive content from
	 * @return A valid HttpURLConnection
	 * 
	 * @throws IOException The openConnection() method throws an IOException and the calling method is responsible for handling it.
	 */
	public static HttpURLConnection openHttpConnection( URL url ) throws IOException
	{
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setDoInput( true );
		conn.setDoOutput( false );
		System.setProperty( "http.agent", getUserAgent() );
		conn.setRequestProperty( "User-Agent", getUserAgent() );
		HttpURLConnection.setFollowRedirects( true );
		conn.setUseCaches( false );
		conn.setInstanceFollowRedirects( true );
		return conn;
	}
	
	/**
	 * Opens an HTTP connection to a web URL and tests that the response is a valid 200-level code
	 * and we can successfully open a stream to the content.
	 * 
	 * @param urlLoc The HTTP URL indicating the location of the content.
	 * @return True if the content can be accessed successfully, false otherwise.
	 */
	public static boolean pingHttpURL( String url )
	{
		InputStream stream = null;
		try
		{
			final HttpURLConnection conn = openHttpConnection( new URL( url ) );
			conn.setConnectTimeout( 10000 );
			
			int responseCode = conn.getResponseCode();
			int responseFamily = responseCode / 100;
			
			if ( responseFamily == 2 )
			{
				stream = conn.getInputStream();
				return true;
			}
			else
			{
				return false;
			}
		}
		catch ( IOException e )
		{
			return false;
		}
		finally
		{
			IOUtils.closeQuietly( stream );
		}
	}
	
	public static boolean sendTracking( String category, String action, String label )
	{
		String url = "http://www.google-analytics.com/collect";
		try
		{
			URL urlObj = new URL( url );
			HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();
			con.setRequestMethod( "POST" );
			
			String urlParameters = "v=1&tid=UA-32999410-1&cid=" + Loader.getConfig().getString( "server.installationUID", Loader.clientId ) + "&t=event&ec=" + category + "&ea=" + action + "&el=" + label;
			
			con.setDoOutput( true );
			DataOutputStream wr = new DataOutputStream( con.getOutputStream() );
			wr.writeBytes( urlParameters );
			wr.flush();
			wr.close();
			
			int responseCode = con.getResponseCode();
			Loader.getLogger().fine( "Analytics Response [" + category + "]: " + responseCode );
			
			BufferedReader in = new BufferedReader( new InputStreamReader( con.getInputStream() ) );
			String inputLine;
			StringBuffer response = new StringBuffer();
			
			while ( ( inputLine = in.readLine() ) != null )
			{
				response.append( inputLine );
			}
			in.close();
			
			return true;
		}
		catch ( IOException e )
		{
			return false;
		}
	}

	public static String getUserAgent()
	{
		return "ChioriWebServer/" + Loader.class.getPackage().getImplementationVersion() + "/" + System.getProperty( "java.version" );
	}
}
