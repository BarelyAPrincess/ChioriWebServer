package com.chiorichan.framework;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import javax.ws.rs.core.MediaType;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ArrayUtils;

import com.chiorichan.Loader;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.multipart.FormDataMultiPart;

public class FrameworkFunctions
{
	protected Framework fw;
	
	public FrameworkFunctions(Framework fw0)
	{
		fw = fw0;
	}
	
	public String randomNum()
	{
		return randomNum( 8, true, false, new String[0] );
	}
	
	public String randomNum( int length )
	{
		return randomNum( length, true, false, new String[0] );
	}
	
	public String randomNum( int length, boolean numbers )
	{
		return randomNum( length, numbers, false, new String[0] );
	}
	
	public String randomNum( int length, boolean numbers, boolean letters )
	{
		return randomNum( length, numbers, letters, new String[0] );
	}
	
	public String randomNum( int length, boolean numbers, boolean letters, String[] allowedChars )
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
	
	public Map<String, Object> cleanArray( LinkedHashMap<String, Object> oldObj, List<String> allowedKeys )
	{
		Map<String, Object> newArray = new LinkedHashMap<String, Object>();
		
		for ( Entry<String, Object> e : oldObj.entrySet() )
			if ( allowedKeys.contains( e.getKey() ) )
				newArray.put( e.getKey(), e.getValue() );
		
		return newArray;
	}
	
	public String formatPhone( String phone )
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
	
	public String createUUID() throws UnsupportedEncodingException
	{
		return createUUID( "" );
	}
	
	public String createUUID( String seed ) throws UnsupportedEncodingException
	{
		return DigestUtils.md5Hex( createGUID( seed ) );
	}
	
	public String createGUID() throws UnsupportedEncodingException
	{
		return createGUID( "" );
	}
	
	public String createGUID( String seed ) throws UnsupportedEncodingException
	{
		if ( seed == null )
			seed = "";
		
		byte[] bytes = ArrayUtils.addAll( seed.getBytes( "ISO-8859-1" ), fw.getRequestId().getBytes( "ISO-8859-1" ) );
		byte[] bytesScrambled = new byte[0];
		
		for ( byte b : bytes )
		{
			byte[] tbyte = new byte[2];
			new Random().nextBytes( bytes );
			
			tbyte[0] = (byte) ( b + tbyte[0] );
			tbyte[1] = (byte) ( b + tbyte[1] );
			
			bytesScrambled = ArrayUtils.addAll( bytesScrambled, tbyte );
		}
		
		String hash = new String( bytesScrambled );
		String guid = hash.substring( 0, 8 );
		guid += "-" + hash.substring( 8, 4 );
		guid += "-" + hash.substring( 12, 4 );
		guid += "-" + hash.substring( 16, 4 );
		guid += "-" + hash.substring( 20, 12 );
		
		return "{" + guid + "}";
	}
	
	public String createTable( Map<String, Object> tableData )
	{
		return createTable( tableData, null, "", false );
	}
	
	public String createTable( Map<String, Object> tableData, List<String> headerArray )
	{
		return createTable( tableData, headerArray, "", false );
	}
	
	public String createTable( Map<String, Object> tableData, List<String> headerArray, String tableId )
	{
		return createTable( tableData, headerArray, tableId, false );
	}
	
	public String createTable( Map<String, Object> tableData, List<String> headerArray, String tableId, boolean returnString )
	{
		if ( tableId == null )
			tableId = "";
		
		if ( tableData == null )
			return "";
		
		StringBuilder sb = new StringBuilder();
		int x = 0;
		sb.append( "<table id=\"" + tableId + "\" class=\"altrowstable\">\n" );
		
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
			Map<String, String> map;
			
			if ( row instanceof Map )
			{
				colLength = Math.max( ( (Map) row ).size(), colLength );
			}
		}
		
		for ( Object row : tableData.values() )
		{
			Map<String, Object> map;
			
			String clss = ( x % 2 == 0 ) ? "evenrowcolor" : "oddrowcolor";
			x++;
			
			if ( row instanceof Map )
			{
				map = (Map<String, Object>) row;
				
				sb.append( "<tr id=\"" + map.get( "rowId" ) + "\" rel=\"" + map.get( "metaData" ) + "\" class=\"" + clss + "\">\n" );
				
				map.remove( "rowId" );
				map.remove( "metaData" );
				
				if ( map.size() == 1 )
				{
					sb.append( "<td style=\"text-align: center; font-weight: bold;\" class=\"" + clss + "\" colspan=\"" + colLength + "\">" + map + "</td>\n" );
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
		
		if ( returnString )
			return sb.toString();
		
		try
		{
			fw.getServer().includeCode( sb.toString() );
		}
		catch ( IOException | CodeParsingException e )
		{
			e.printStackTrace();
		}
		return "";
	}
	
	public static ClientResponse CreateMailingList( String apiKey )
	{
		Client client = Client.create();
		client.addFilter( new HTTPBasicAuthFilter( "api", apiKey ) );
		WebResource webResource = client.resource( "https://api.mailgun.net/v2/lists" );
		MultivaluedMapImpl formData = new MultivaluedMapImpl();
		formData.add( "address", "dev@samples.mailgun.org" );
		formData.add( "description", "Mailgun developers list" );
		return webResource.type( MediaType.APPLICATION_FORM_URLENCODED ).post( ClientResponse.class, formData );
		
	}
	
	public static ClientResponse AddListMember( String apiKey )
	{
		Client client = Client.create();
		client.addFilter( new HTTPBasicAuthFilter( "api", apiKey ) );
		WebResource webResource = client.resource( "https://api.mailgun.net/v2/lists/" + "dev@samples.mailgun.org/members" );
		MultivaluedMapImpl formData = new MultivaluedMapImpl();
		formData.add( "address", "bar@example.com" );
		formData.add( "subscribed", true );
		formData.add( "name", "Bob Bar" );
		formData.add( "description", "Developer" );
		formData.add( "vars", "{\"age\": 26}" );
		return webResource.type( MediaType.APPLICATION_FORM_URLENCODED ).post( ClientResponse.class, formData );
	}
	
	public static ClientResponse fireMailgun( String apiKey, String from, String to, String subject, String html, String url )
	{
		Client client = Client.create();
		client.addFilter( new HTTPBasicAuthFilter( "api", apiKey ) );
		WebResource webResource = client.resource( url );
		FormDataMultiPart form = new FormDataMultiPart();
		form.field( "from", from );
		form.field( "to", to );
		form.field( "subject", subject );
		form.field( "html", html );
		return webResource.type( MediaType.MULTIPART_FORM_DATA_TYPE ).post( ClientResponse.class, form );
	}
}
