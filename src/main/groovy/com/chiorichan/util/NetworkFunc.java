/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import com.chiorichan.Loader;

/**
 * Provides Network Utilities
 * 
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
public class NetworkFunc
{
	public static final String REGEX_IPV4 = "^([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])\\.([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])\\.([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])\\.([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])$";
	public static final String REGEX_IPV6 = "^((?:[0-9A-Fa-f]{1,4}))((?::[0-9A-Fa-f]{1,4}))*::((?:[0-9A-Fa-f]{1,4}))((?::[0-9A-Fa-f]{1,4}))*|((?:[0-9A-Fa-f]{1,4}))((?::[0-9A-Fa-f]{1,4})){7}$";
	
	/**
	 * Establishes an HttpURLConnection from a URL, with the correct configuration to receive content from the given URL.
	 * 
	 * @param url
	 *            The URL to set up and receive content from
	 * @return A valid HttpURLConnection
	 * 
	 * @throws IOException
	 *             The openConnection() method throws an IOException and the calling method is responsible for handling it.
	 */
	public static HttpURLConnection openHttpConnection( URL url ) throws IOException
	{
		HttpURLConnection conn = ( HttpURLConnection ) url.openConnection();
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
	 * @param url
	 *            The HTTP URL indicating the location of the content.
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
				IOUtils.closeQuietly( stream );
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
	
	/**
	 * TODO The server was lagging with this! WHY???
	 * Maybe we should change our metrics system
	 */
	public static boolean sendTracking( String category, String action, String label )
	{
		try
		{
			String url = "http://www.google-analytics.com/collect";
			
			URL urlObj = new URL( url );
			HttpURLConnection con = ( HttpURLConnection ) urlObj.openConnection();
			con.setRequestMethod( "POST" );
			
			String urlParameters = "v=1&tid=UA-60405654-1&cid=" + Loader.getClientId() + "&t=event&ec=" + category + "&ea=" + action + "&el=" + label;
			
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
	
	public static Date getNTPDate()
	{
		String[] hosts = new String[] {"ntp02.oal.ul.pt", "ntp04.oal.ul.pt", "ntp.xs4all.nl"};
		
		NTPUDPClient client = new NTPUDPClient();
		// We want to timeout if a response takes longer than 5 seconds
		client.setDefaultTimeout( 5000 );
		
		for ( String host : hosts )
		{
			
			try
			{
				InetAddress hostAddr = InetAddress.getByName( host );
				// System.out.println( "> " + hostAddr.getHostName() + "/" + hostAddr.getHostAddress() );
				TimeInfo info = client.getTime( hostAddr );
				Date date = new Date( info.getReturnTime() );
				return date;
				
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}
		}
		
		client.close();
		
		return null;
		
	}
	
	public static byte[] readUrl( String url )
	{
		try
		{
			return readUrlWithException( url );
		}
		catch ( IOException e )
		{
			return null;
		}
	}
	
	public static byte[] readUrl( String url, String user, String pass )
	{
		try
		{
			return readUrlWithException( url, user, pass );
		}
		catch ( IOException e )
		{
			return null;
		}
	}
	
	public static byte[] readUrlWithException( String url ) throws IOException
	{
		return readUrlWithException( url, null, null );
	}
	
	public static byte[] readUrlWithException( String surl, String user, String pass ) throws IOException
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		URL url = new URL( surl );
		URLConnection uc = url.openConnection();
		
		if ( user != null || pass != null )
		{
			String userpass = user + ":" + pass;
			String basicAuth = "Basic " + new String( Base64.encodeBase64( userpass.getBytes() ) );
			uc.setRequestProperty( "Authorization", basicAuth );
		}
		
		InputStream is = uc.getInputStream();
		
		byte[] byteChunk = new byte[4096];
		int n;
		
		while ( ( n = is.read( byteChunk ) ) > 0 )
		{
			out.write( byteChunk, 0, n );
		}
		
		is.close();
		
		return out.toByteArray();
	}
	
	public static String getUserAgent()
	{
		return Versioning.getProductSimple() + "/" + Versioning.getVersion() + "/" + Versioning.getJavaVersion();
	}
	
	public static boolean isValidIPv4( String ip )
	{
		if ( ip == null )
			return false;
		
		return ip.matches( REGEX_IPV4 );
	}
	
	public static boolean isValidIPv6( String ip )
	{
		if ( ip == null )
			return false;
		
		return ip.matches( REGEX_IPV6 );
	}
}
