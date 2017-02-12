/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.zutils;

import com.chiorichan.logger.Log;
import com.chiorichan.tasks.Timings;
import com.google.common.collect.Maps;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.ocpsoft.prettytime.PrettyTime;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;

public class WebFunc
{
	@Deprecated
	public static Map<String, Object> cleanArray( Map<String, Object> data, Collection<String> allowedKeys )
	{
		return filter( data, allowedKeys );
	}

	public static String createGUID() throws UnsupportedEncodingException
	{
		return createGUID( Timings.epoch() + "-guid" );
	}

	public static String createGUID( String seed )
	{
		if ( seed == null )
			seed = "";

		byte[] bytes;
		try
		{
			bytes = seed.getBytes( "ISO-8859-1" );
		}
		catch ( UnsupportedEncodingException e )
		{
			bytes = new byte[0];
		}

		byte[] bytesScrambled = new byte[0];

		for ( byte b : bytes )
		{
			byte[] tbyte = new byte[2];
			new Random().nextBytes( bytes );

			tbyte[0] = ( byte ) ( b + tbyte[0] );
			tbyte[1] = ( byte ) ( b + tbyte[1] );

			bytesScrambled = ArrayUtils.addAll( bytesScrambled, tbyte );
		}

		return "{" + UUID.nameUUIDFromBytes( bytesScrambled ).toString() + "}";
	}

	public static String createTable( Collection<Object> tableData )
	{
		return createTable( tableData, null, null );
	}

	public static String createTable( Collection<Object> tableData, Collection<String> headerArray )
	{
		return createTable( tableData, headerArray, null, null );
	}

	public static String createTable( Collection<Object> tableData, Collection<String> headerArray, String tableId )
	{
		return createTable( tableData, headerArray, tableId, null );
	}

	public static String createTable( Collection<Object> tableData, Collection<String> headerArray, String tableId, String altTableClass )
	{
		Map<Object, Object> newData = Maps.newLinkedHashMap();

		Integer x = 0;
		for ( Object o : tableData )
		{
			newData.put( x.toString(), o );
			x++;
		}

		return createTable( newData, headerArray, tableId, altTableClass );
	}

	public static String createTable( Map<Object, Object> tableData )
	{
		return createTable( tableData, null, "" );
	}

	public static String createTable( Map<Object, Object> tableData, Collection<String> headerArray )
	{
		return createTable( tableData, headerArray, "" );
	}

	public static String createTable( Map<Object, Object> tableData, Collection<String> headerArray, String tableId )
	{
		return createTable( tableData, headerArray, tableId, null );
	}

	@SuppressWarnings( "unchecked" )
	public static String createTable( Map<Object, Object> tableData, Collection<String> headerArray, String tableId, String altTableClass )
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
				sb.append( "<th>" + col + "</th>\n" );
			sb.append( "</tr>\n" );
		}

		int colLength = headerArray != null ? headerArray.size() : tableData.size();
		for ( Object row : tableData.values() )
			if ( row instanceof Map )
				colLength = Math.max( ( ( Map<String, Object> ) row ).size(), colLength );

		for ( Object row : tableData.values() )
		{
			String clss = x % 2 == 0 ? "evenrowcolor" : "oddrowcolor";
			x++;

			if ( row instanceof Map || row instanceof Collection )
			{
				Map<Object, Object> map = Maps.newLinkedHashMap();

				if ( row instanceof Map )
					map = ( Map<Object, Object> ) row;
				else
				{
					int y = 0;
					for ( Object o : ( Collection<Object> ) row )
					{
						map.put( Integer.toString( y ), o );
						y++;
					}
				}

				sb.append( "<tr" );

				for ( Entry<Object, Object> e : map.entrySet() )
					try
					{
						if ( ZObjects.castToStringWithException( e.getKey() ).startsWith( ":" ) )
						{
							map.remove( e.getKey() );
							sb.append( " " + ZObjects.castToStringWithException( e.getKey() ).substring( 1 ) + "=\"" + ZObjects.castToStringWithException( e.getValue() ) + "\"" );
						}
					}
					catch ( ClassCastException ex )
					{
						ex.printStackTrace();
					}

				sb.append( " class=\"" + clss + "\">\n" );

				if ( map.size() == 1 )
					sb.append( "<td style=\"text-align: center; font-weight: bold;\" class=\"\" colspan=\"" + colLength + "\">" + map.get( 0 ) + "</td>\n" );
				else
				{
					int cc = 0;
					for ( Object col : map.values() )
						if ( col != null )
						{
							String subclass = col instanceof String && ( ( String ) col ).isEmpty() ? " emptyCol" : "";
							sb.append( "<td id=\"col_" + cc + "\" class=\"" + subclass + "\">" + col + "</td>\n" );
							cc++;
						}
				}
				sb.append( "</tr>\n" );
			}
			else
				sb.append( "<tr><td class=\"" + clss + "\" colspan=\"" + colLength + "\"><b><center>" + ZObjects.castToString( row ) + "</b></center></td></tr>\n" );
		}
		sb.append( "</table>\n" );

		return sb.toString();
	}

	public static String createUUID() throws UnsupportedEncodingException
	{
		return createUUID( Timings.epoch() + "-uuid" );
	}

	public static String createUUID( String seed ) throws UnsupportedEncodingException
	{
		return DigestUtils.md5Hex( createGUID( seed ) );
	}

	public static String escapeHTML( String l )
	{
		return StringEscapeUtils.escapeHtml4( l );
	}

	public static Map<String, Object> filter( Map<String, Object> data, Collection<String> allowedKeys )
	{
		return filter( data, allowedKeys, false );
	}

	/**
	 * Filters a map for the specified list of keys, removing keys that are not contained in the list.
	 * Groovy example: def filteredMap = getHttpUtils().filter( unfilteredMap, ["keyA", "keyB", "someKey"], false );
	 *
	 * @param data
	 *             The map that needs checking
	 * @param allowedKeys
	 *             A list of keys allowed
	 * @param caseSensitive
	 *             Will the key match be case sensitive or not
	 * @return The resulting map of filtered data
	 */
	public static Map<String, Object> filter( Map<String, Object> data, Collection<String> allowedKeys, boolean caseSensitive )
	{
		Map<String, Object> newArray = new LinkedHashMap<String, Object>();

		if ( !caseSensitive )
			allowedKeys = ZStrings.toLowerCaseList( allowedKeys );

		for ( Entry<String, Object> e : data.entrySet() )
			if ( !caseSensitive && allowedKeys.contains( e.getKey().toLowerCase() ) || allowedKeys.contains( e.getKey() ) )
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
			Log.get().warning( "NumberParseException was thrown: " + e.toString() );
			return phone;
		}
	}

	// This might change
	public static String formatTimeAgo( Date date )
	{
		PrettyTime p = new PrettyTime();
		return p.format( date );
	}

	public static Map<String, String> queryToMap( String query ) throws UnsupportedEncodingException
	{
		Map<String, String> result = new HashMap<String, String>();

		if ( query == null )
			return result;

		for ( String param : query.split( "&" ) )
		{
			String[] pair = param.split( "=" );
			try
			{
				if ( pair.length > 1 )
					result.put( URLDecoder.decode( ZStrings.trimEnd( pair[0], '%' ), "ISO-8859-1" ), URLDecoder.decode( ZStrings.trimEnd( pair[1], '%' ), "ISO-8859-1" ) );
				else if ( pair.length == 1 )
					result.put( URLDecoder.decode( ZStrings.trimEnd( pair[0], '%' ), "ISO-8859-1" ), "" );
			}
			catch ( IllegalArgumentException e )
			{
				Log.get().warning( "Malformed URL exception was thrown, key: `" + pair[0] + "`, val: '" + pair[1] + "'" );
			}
		}
		return result;
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
			allowedChars = ArrayUtils.addAll( allowedChars, "1", "2", "3", "4", "5", "6", "7", "8", "9", "0" );

		if ( letters )
			allowedChars = ArrayUtils.addAll( allowedChars, "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z" );

		String rtn = "";
		for ( int i = 0; i < length; i++ )
			rtn += allowedChars[new Random().nextInt( allowedChars.length )];

		return rtn;
	}

	public static String unescapeHTML( String l )
	{
		return StringEscapeUtils.unescapeHtml4( l );
	}
}
