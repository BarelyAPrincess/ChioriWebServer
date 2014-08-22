package com.chiorichan.framework;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;

import net.glxn.qrgen.QRCode;
import net.glxn.qrgen.image.ImageType;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import com.chiorichan.Loader;
import com.chiorichan.exceptions.ShellExecuteException;
import com.chiorichan.factory.CodeEvalFactory;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.sun.jersey.core.util.Base64;

public class WebUtils
{
	public static String QRPNG( String code ) throws IOException
	{
		return QRPNG( code, 200 );
	}
	
	public static String QRPNG( String code, int size ) throws IOException
	{
		if ( size < 1 )
			return "";
		
		Validate.notNull( code );
		Validate.notEmpty( code );
		
		return new String( QRCode.from( code ).withSize( size, size ).setMargin( 1 ).to( ImageType.PNG ).stream().toByteArray(), "ISO-8859-1" );
	}
	
	public static String randomNum()
	{
		return randomNum( 8, true, false, new String[0] );
	}
	
	public static String randomNum( int length )
	{
		return randomNum( length, true, false, new String[0] );
	}
	
	public static String randomNum( int length, boolean numbers )
	{
		return randomNum( length, numbers, false, new String[0] );
	}
	
	public static String randomNum( int length, boolean numbers, boolean letters )
	{
		return randomNum( length, numbers, letters, new String[0] );
	}
	
	public static String randomNum( int length, boolean numbers, boolean letters, String[] allowedChars )
	{
		if ( allowedChars == null )
			allowedChars = new String[0];
		
		if ( numbers )
			allowedChars = ArrayUtils.addAll( allowedChars, new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "0" } );
		
		if ( letters )
			allowedChars = ArrayUtils.addAll( allowedChars, new String[] { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z" } );
		
		String rtn = "";
		for ( int i = 0; i < length; i++ )
		{
			rtn += allowedChars[new Random().nextInt( allowedChars.length )];
		}
		
		return rtn;
	}
	
	public static Map<String, Object> cleanArray( Map<String, Object> oldObj, List<String> allowedKeys )
	{
		Map<String, Object> newArray = new LinkedHashMap<String, Object>();
		
		for ( Entry<String, Object> e : oldObj.entrySet() )
			if ( allowedKeys.contains( e.getKey() ) )
				newArray.put( e.getKey(), e.getValue() );
		
		return newArray;
	}
	
	public static String formatPhone( String phone )
	{
		if ( phone == null || phone.isEmpty() )
			return "";
		
		phone = phone.replaceAll( "[ -()\\.]", "" );
		
		PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
		try
		{
			PhoneNumber num = phoneUtil.parse( phone, "US" );
			return phoneUtil.format( num, PhoneNumberFormat.NATIONAL );
		}
		catch ( NumberParseException e )
		{
			Loader.getLogger().warning( "NumberParseException was thrown: " + e.toString() );
			return phone;
		}
	}
	
	public static String createUUID() throws UnsupportedEncodingException
	{
		return createUUID( "" );
	}
	
	public static String createUUID( String seed ) throws UnsupportedEncodingException
	{
		return DigestUtils.md5Hex( createGUID( seed ) );
	}
	
	public static String createGUID() throws UnsupportedEncodingException
	{
		return createGUID( "" );
	}
	
	public static String createGUID( String seed ) throws UnsupportedEncodingException
	{
		if ( seed == null )
			seed = "";
		
		byte[] bytes = seed.getBytes( "ISO-8859-1" );
		byte[] bytesScrambled = new byte[0];
		
		for ( byte b : bytes )
		{
			byte[] tbyte = new byte[2];
			new Random().nextBytes( bytes );
			
			tbyte[0] = (byte) ( b + tbyte[0] );
			tbyte[1] = (byte) ( b + tbyte[1] );
			
			bytesScrambled = ArrayUtils.addAll( bytesScrambled, tbyte );
		}
		
		return "{" + UUID.nameUUIDFromBytes( bytesScrambled ).toString() + "}";
	}
	
	public static String createTable( List<Object> tableData )
	{
		return createTable( tableData, null, null );
	}
	
	public static String createTable( List<Object> tableData, List<String> headerArray )
	{
		return createTable( tableData, headerArray, null, null );
	}
	
	public static String createTable( List<Object> tableData, List<String> headerArray, String tableId )
	{
		return createTable( tableData, headerArray, tableId, null );
	}
	
	public static String createTable( List<Object> tableData, List<String> headerArray, String tableId, String altTableClass )
	{
		Map<String, Object> newData = new LinkedHashMap<String, Object>();
		
		Integer x = 0;
		for ( Object o : tableData )
		{
			newData.put( x.toString(), o );
			x++;
		}
		
		return createTable( newData, headerArray, tableId, altTableClass );
	}
	
	public static String createTable( Map<String, Object> tableData )
	{
		return createTable( tableData, null, "" );
	}
	
	public static String createTable( Map<String, Object> tableData, List<String> headerArray )
	{
		return createTable( tableData, headerArray, "" );
	}
	
	public static String createTable( Map<String, Object> tableData, List<String> headerArray, String tableId )
	{
		return createTable( tableData, headerArray, tableId, null );
	}
	
	@SuppressWarnings( "unchecked" )
	public static String createTable( Map<String, Object> tableData, List<String> headerArray, String tableId, String altTableClass )
	{
		if ( tableId == null )
			tableId = "";
		
		if ( tableData == null )
			return "";
		
		if ( altTableClass == null || altTableClass.isEmpty() )
			altTableClass = "altrowstable";
		
		StringBuilder sb = new StringBuilder();
		int x = 0;
		sb.append( "<table id=\"" + tableId + "\" class=\"" + altTableClass + "\">\n" );
		
		if ( headerArray != null )
		{
			sb.append( "<tr>\n" );
			for ( String col : headerArray )
			{
				sb.append( "<th>" + col + "</th>\n" );
			}
			sb.append( "</tr>\n" );
		}
		
		int colLength = headerArray.size();
		for ( Object row : tableData.values() )
		{
			if ( row instanceof Map )
			{
				colLength = Math.max( ( (Map<String, Object>) row ).size(), colLength );
			}
		}
		
		for ( Object row : tableData.values() )
		{
			String clss = ( x % 2 == 0 ) ? "evenrowcolor" : "oddrowcolor";
			x++;
			
			if ( row instanceof Map || row instanceof List )
			{
				Map<String, Object> map = new LinkedHashMap<String, Object>();
				
				if ( row instanceof Map )
					map = (Map<String, Object>) row;
				else
				{
					int y = 0;
					for ( Object o : (List<Object>) row )
					{
						map.put( y + "", o );
						y++;
					}
				}
				
				sb.append( "<tr id=\"" + map.get( "rowId" ) + "\" rel=\"" + map.get( "metaData" ) + "\" class=\"" + clss + "\">\n" );
				
				map.remove( "rowId" );
				map.remove( "metaData" );
				
				if ( map.size() == 1 )
				{
					sb.append( "<td style=\"text-align: center; font-weight: bold;\" class=\"\" colspan=\"" + colLength + "\">" + map.get( 0 ) + "</td>\n" );
				}
				else
				{
					int cc = 0;
					for ( Object col : map.values() )
					{
						if ( col != null )
						{
							String subclass = ( col instanceof String && ( (String) col ).isEmpty() ) ? " emptyCol" : "";
							sb.append( "<td id=\"col_" + cc + "\" class=\"" + subclass + "\">" + col + "</td>\n" );
							cc++;
						}
					}
				}
				sb.append( "</tr>\n" );
			}
			else if ( row instanceof String )
			{
				sb.append( "<tr><td class=\"" + clss + "\" colspan=\"" + colLength + "\"><b><center>" + ( (String) row ) + "</b></center></td></tr>\n" );
			}
			else
			{
				sb.append( "<tr><td class=\"" + clss + "\" colspan=\"" + colLength + "\"><b><center>" + row.toString() + "</b></center></td></tr>\n" );
			}
		}
		sb.append( "</table>\n" );
		
		return sb.toString();
	}
	
	public static String escapeHTML( String l )
	{
		return StringUtils.replaceEach( l, new String[] { "&", "\"", "<", ">" }, new String[] { "&amp;", "&quot;", "&lt;", "&gt;" } );
	}
	
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
	
	public static Date getNTPDate()
	{
		String[] hosts = new String[] { "ntp02.oal.ul.pt", "ntp04.oal.ul.pt", "ntp.xs4all.nl" };
		
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
	
	public static byte[] readUrl( String url ) throws IOException
	{
		return readUrl( url, null, null );
	}
	
	public static byte[] readUrl( String surl, String user, String pass ) throws IOException
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		URL url = new URL( surl );
		URLConnection uc = url.openConnection();
		if ( user != null || pass != null )
		{
			String userpass = user + ":" + pass;
			String basicAuth = "Basic " + new String( Base64.encode( userpass.getBytes() ) );
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
	
	public static String evalFile( CodeEvalFactory factory, Site site, String file ) throws IOException, ShellExecuteException
	{
		if ( file == null || file.isEmpty() )
			return "";
		
		File packFile = new File( file );
		
		if ( site == null )
			site = Loader.getSiteManager().getFrameworkSite();
		
		if ( packFile == null || !packFile.exists() )
			return "";
		
		return factory.eval( packFile, site );
	}
	
	public static String evalPackage( CodeEvalFactory factory, Site site, String pack ) throws IOException, ShellExecuteException
	{
		File packFile = null;
		
		if ( site != null )
			packFile = site.getResource( pack );
		else
			site = Loader.getSiteManager().getFrameworkSite();
		
		if ( packFile == null || !packFile.exists() )
			packFile = Loader.getSiteManager().getFrameworkSite().getResource( pack );
		
		if ( packFile == null || !packFile.exists() )
			return "";
		
		return factory.eval( packFile, site );
	}
}
