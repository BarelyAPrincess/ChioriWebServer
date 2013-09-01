package com.chiorichan.framework;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ArrayUtils;

import com.caucho.quercus.env.ArrayValueImpl;
import com.chiorichan.Loader;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

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
		for ( String s : allowedChars )
		{
			rtn += allowedChars[new Random().nextInt( rtn.length() - 1 )];
		}
		
		return rtn;
	}
	
	public Map<String, Object> cleanArray( ArrayValueImpl arr, List<String> allowedKeys )
	{
		Map<String, Object> newObj = arr.toJavaMap( fw.getEnv(), Map.class );
		
		for ( String k : newObj.keySet() )
		{
			if ( !allowedKeys.contains( k ) )
				newObj.remove( k );
		}
		
		return newObj;
	}
	
	public String formatPhone( String phone )
	{
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
		
		int colLength = 1;
		for ( Object row : tableData.values() )
		{
			Map<String, String> map;
			
			if ( row instanceof ArrayValueImpl )
			{
				map = ( (ArrayValueImpl) row ).toJavaMap( fw.getEnv(), HashMap.class );
				
				colLength = Math.max( map.size(), colLength );
			}
		}
		
		for ( Object row : tableData.values() )
		{
			Map<String, Object> map;
			
			if ( row instanceof ArrayValueImpl )
			{
				map = ( (ArrayValueImpl) row ).toJavaMap( fw.getEnv(), HashMap.class );
				
				String clss = ( x % 2 == 0 ) ? "evenrowcolor" : "oddrowcolor";
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
				x++;
			}
		}
		sb.append( "</table>\n" );
		
		if ( returnString )
			return sb.toString();
		
		try
		{
			fw.getServer().includeCode( sb.toString() );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
		return "";
	}
}
