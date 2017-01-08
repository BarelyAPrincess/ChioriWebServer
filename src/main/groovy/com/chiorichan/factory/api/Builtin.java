/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * <p>
 * Copyright 2016 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.factory.api;

import com.chiorichan.util.*;
import groovy.lang.Script;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

import org.ocpsoft.prettytime.PrettyTime;

import com.chiorichan.AppConfig;
import com.chiorichan.database.DatabaseEngineLegacy;
import com.chiorichan.lang.DiedException;
import com.chiorichan.logger.Log;
import com.chiorichan.tasks.Timings;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

public abstract class Builtin extends Script
{
	public static String apache_get_version()
	{
		return "THIS IS NOT APACHE YOU DUMMY!!!";
	}

	/**
	 * Provides an easy array collections method. Not sure if this is counterintuitive since technically Groovy provides an easier array of [], so what, i.e., def val = array( "obj1", "ovj2", "obj3" ); Comparable to PHP's array function,
	 * http://www.w3schools.com/php/func_array.asp
	 *
	 * @param vals Elements of the array separated as an argument
	 * @return The array of elements
	 */
	@SuppressWarnings( "unchecked" )
	public static <T> T[] array( T... vals )
	{
		return vals;
	}

	public static boolean asBool( Object obj )
	{
		return ObjectFunc.castToBool( obj );
	}

	public static double asDouble( Object obj )
	{
		return ObjectFunc.castToDouble( obj );
	}

	public static int asInt( Object obj )
	{
		return ObjectFunc.castToInt( obj );
	}

	public static long asLong( Object obj )
	{
		return ObjectFunc.castToLong( obj );
	}

	public static String asString( Object obj )
	{
		return ObjectFunc.castToString( obj );
	}

	public static byte[] base64Decode( String str )
	{
		return SecureFunc.base64Decode( str );
	}

	public static String base64DecodeString( String str )
	{
		return SecureFunc.base64DecodeString( str );
	}

	public static String base64Encode( byte[] bytes )
	{
		return SecureFunc.base64Encode( bytes );
	}

	public static String base64Encode( String str )
	{
		return SecureFunc.base64Encode( str );
	}

	public static int count( Collection<Object> list )
	{
		return list == null ? 0 : list.size();
	}

	public static int count( Map<Object, Object> maps )
	{
		return maps == null ? 0 : maps.size();
	}

	public static int count( Object[] var )
	{
		return var == null ? 0 : var.length;
	}

	public static int count( String var )
	{
		return var == null ? 0 : var.length();
	}

	public static String createTable( Collection<?> tableData )
	{
		return createTable( tableData, null, null, null );
	}

	public static String createTable( Collection<?> tableData, String tableId )
	{
		return createTable( tableData, null, tableId, null );
	}

	public static String createTable( Collection<?> tableData, String tableId, String altTableClass )
	{
		return createTable( tableData, null, tableId, altTableClass );
	}

	public static String createTable( Collection<?> tableData, Collection<String> tableHeader )
	{
		return createTable( tableData, tableHeader, null, null );
	}

	public static String createTable( Collection<?> tableData, Collection<String> tableHeader, String tableId )
	{
		return createTable( tableData, tableHeader, tableId, null );
	}

	public static String createTable( Collection<?> tableData, Collection<String> tableHeader, String tableId, String altTableClass )
	{
		Map<String, Object> newData = new TreeMap<>();

		Integer x = 0;
		for ( Object o : tableData )
		{
			newData.put( x.toString(), o );
			x++;
		}

		return createTable( newData, tableHeader, tableId, altTableClass );
	}

	public static String createTable( Map<?, ?> tableData )
	{
		return createTable( tableData, null, null, null );
	}

	public static String createTable( Map<?, ?> tableData, String tableId )
	{
		return createTable( tableData, null, tableId, null );
	}

	public static String createTable( Map<?, ?> tableData, String tableId, String altTableClass )
	{
		return createTable( tableData, null, tableId, altTableClass );
	}

	public static String createTable( Map<?, ?> tableData, Collection<String> tableHeader )
	{
		return createTable( tableData, tableHeader, "" );
	}

	public static String createTable( Map<?, ?> tableData, Collection<String> tableHeader, String tableId )
	{
		return createTable( tableData, tableHeader, tableId, null );
	}

	@SuppressWarnings( "unchecked" )
	public static String createTable( Map<?, ?> tableData, Collection<String> tableHeader, String tableId, String altTableClass )
	{
		if ( tableData == null )
			return "";

		if ( altTableClass == null || altTableClass.length() == 0 )
			altTableClass = "altrowstable";

		StringBuilder sb = new StringBuilder();
		int x = 0;
		sb.append( "<table " + ( tableId == null ? "" : " id=\"" + tableId + "\"" ) + " class=\"" + altTableClass + "\">\n" );

		if ( tableHeader != null )
		{
			sb.append( "<thead>\n" );
			sb.append( "<tr>\n" );
			for ( String col : tableHeader )
				sb.append( "<th>" + col + "</th>\n" );
			sb.append( "</tr>\n" );
			sb.append( "</thead>\n" );
		}

		sb.append( "<tbody>\n" );

		int colLength = tableHeader != null ? tableHeader.size() : tableData.size();
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
						if ( ObjectFunc.castToStringWithException( e.getKey() ).startsWith( ":" ) )
						{
							map.remove( e.getKey() );
							sb.append( " " + ObjectFunc.castToStringWithException( e.getKey() ).substring( 1 ) + "=\"" + ObjectFunc.castToStringWithException( e.getValue() ) + "\"" );
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
							String subclass = col instanceof String && ( ( String ) col ).length() == 0 ? "tblEmptyCol" : "";
							sb.append( "<td id=\"col_" + cc + "\" class=\"" + subclass + "\">" + col + "</td>\n" );
							cc++;
						}
				}
				sb.append( "</tr>\n" );
			}
			else if ( row instanceof String )
				sb.append( "<tr class=\"" + clss + "\"><td id=\"tblStringRow\" colspan=\"" + colLength + "\"><b><center>" + ( String ) row + "</center></b></td></tr>\n" );
			else
				sb.append( "<tr class=\"" + clss + "\"><td id=\"tblStringRow\" colspan=\"" + colLength + "\"><b><center>" + row.toString() + "</center></b></td></tr>\n" );
		}

		sb.append( "</tbody>\n" );
		sb.append( "</table>\n" );

		return sb.toString();
	}

	public static String date()
	{
		return date( "" );
	}

	public static String date( Date date )
	{
		return date( "", date, null );
	}

	public static String date( String format )
	{
		return date( format, "" );
	}

	public static String date( String format, Date date )
	{
		return date( format, date, null );
	}

	public static String date( String format, Date date, String def )
	{
		return date( format, ObjectFunc.castToString( date.getTime() / 1000 ), def );
	}

	public static String date( String format, Object data )
	{
		return date( format, data, null );
	}

	public static String date( String format, Object data, String def )
	{
		return date( format, ObjectFunc.castToString( data ), def );
	}

	public static String date( String format, String data )
	{
		return date( format, data, null );
	}

	public static String date( String format, String data, String def )
	{
		Date date = new Date();

		if ( data != null && !data.isEmpty() )
		{
			data = data.trim();

			if ( data.length() > 10 )
				data = data.substring( 0, 10 );

			try
			{
				date = new Date( Long.parseLong( data ) * 1000 );
			}
			catch ( NumberFormatException e )
			{
				if ( def != null )
					return def;
			}
		}

		if ( format == null || format.isEmpty() )
			format = "MMM dx YYYY";

		if ( format.contains( "U" ) )
			format = format.replaceAll( "U", date.getTime() / 1000 + "" );

		if ( format.contains( "x" ) )
		{
			Calendar var1 = Calendar.getInstance();
			var1.setTime( date );
			int day = var1.get( Calendar.DAY_OF_MONTH );
			String suffix = "";

			if ( day >= 11 && day <= 13 )
				suffix = "'th'";
			else
				switch ( day % 10 )
				{
					case 1:
						suffix = "'st'";
						break;
					case 2:
						suffix = "'nd'";
						break;
					case 3:
						suffix = "'rd'";
						break;
					default:
						suffix = "'th'";
						break;
				}

			format = format.replaceAll( "x", suffix );
		}

		return new SimpleDateFormat( format ).format( date );
	}

	/**
	 * Default format is M/d/yyyy
	 *
	 * @param date you wish to convert
	 * @return Long containing the epoch of provided date
	 */
	public static Long dateToEpoch( String date )
	{
		return dateToEpoch( date, null );
	}

	public static Long dateToEpoch( String date, String format )
	{
		try
		{
			if ( format == null )
				format = "M/d/yyyy";

			SimpleDateFormat sdf = new SimpleDateFormat( format );

			return sdf.parse( date ).getTime() / 1000;
		}
		catch ( ParseException e )
		{
			return 0L;
		}
	}

	public static void die() throws DiedException
	{
		die( null );
	}

	/**
	 * Forcibly kills script exception by throwing a DiedException
	 *
	 * @param msg The message to return
	 * @throws DiedException
	 */
	public static void die( String msg ) throws DiedException
	{
		throw new DiedException( msg );
	}

	public static File dirname( File path )
	{
		return dirname( path, 1 );
	}

	public static File dirname( File path, int levels )
	{
		if ( levels < 1 )
			levels = 1;
		for ( int x = 0; x < levels; x++ )
			path = path.getParentFile();
		return path;
	}

	public static File dirname( String path )
	{
		return dirname( path, 1 );
	}

	public static File dirname( String path, int levels )
	{
		return dirname( FileFunc.isAbsolute( path ) ? new File( path ) : new File( AppConfig.get().getDirectory().getAbsolutePath(), path ), levels );
	}

	public static boolean empty( Collection<Object> list )
	{
		return list == null || list.size() < 1;
	}

	@SuppressWarnings( "rawtypes" )
	public static boolean empty( Iterator list )
	{
		return list == null || list.hasNext();
	}

	public static boolean empty( Map<Object, Object> maps )
	{
		return maps == null || maps.size() < 1;
	}

	public static boolean empty( Object o )
	{
		if ( o == null )
			return true;
		return false;
	}

	public static boolean empty( String var )
	{
		if ( var == null )
			return true;

		if ( var.isEmpty() )
			return true;

		return false;
	}

	public static int epoch()
	{
		return Timings.epoch();
	}

	public static Collection<String> explode( String limiter, String data )
	{
		if ( data == null || data.isEmpty() )
			return Lists.newArrayList();

		return new ArrayList<String>( Splitter.on( limiter ).splitToList( data ) );
	}

	public static Map<String, String> explode( String limiter, String separator, String data )
	{
		if ( data == null || data.isEmpty() )
			return Maps.newHashMap();

		return new HashMap<String, String>( Splitter.on( limiter ).withKeyValueSeparator( separator ).split( data ) );
	}

	public static boolean file_exists( File file )
	{
		return file.exists();
	}

	public static boolean file_exists( String file )
	{
		return ( FileFunc.isAbsolute( file ) ? new File( file ) : new File( AppConfig.get().getDirectory().getAbsolutePath(), file ) ).exists();
	}

	public static Map<String, Object> filter( Map<String, Object> data, Collection<String> allowedKeys )
	{
		return filter( data, allowedKeys, false );
	}

	/**
	 * Filters a map for the specified list of keys, removing keys that are not contained in the list. Groovy example: def filteredMap = getHttpUtils().filter( unfilteredMap, ["keyA", "keyB", "someKey"], false );
	 *
	 * @param data          The map that needs checking
	 * @param allowedKeys   A list of keys allowed
	 * @param caseSensitive Will the key match be case sensitive or not
	 * @return The resulting map of filtered data
	 */
	public static Map<String, Object> filter( Map<String, Object> data, Collection<String> allowedKeys, boolean caseSensitive )
	{
		Map<String, Object> newArray = new LinkedHashMap<String, Object>();

		if ( !caseSensitive )
			allowedKeys = StringFunc.toLowerCaseList( allowedKeys );

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

	/**
	 * Returns the current server copyright string
	 *
	 * @return Copyright string
	 */
	public static String getCopyright()
	{
		return Versioning.getCopyright();
	}

	/**
	 * Returns the current server product string
	 */
	public static String getProduct()
	{
		return Versioning.getProduct();
	}

	/**
	 * Returns an instance of the server database
	 *
	 * @return The server database engine
	 * @throws IllegalStateException thrown if the requested database is unconfigured
	 */
	public static DatabaseEngineLegacy getServerDatabase()
	{
		DatabaseEngineLegacy engine = AppConfig.get().getDatabase().getLegacy();

		if ( engine == null )
			throw new IllegalStateException( "The server database is unconfigured. It will need to be setup in order for you to use the getServerDatabase() method." );

		return engine;
	}

	/**
	 * Returns the current server version string
	 */
	public static String getVersion()
	{
		return Versioning.getVersion();
	}

	public static String implode( String joiner, Iterable<String> data )
	{
		return Joiner.on( joiner ).join( data );
	}

	public static String implode( String joiner, Iterator<String> data )
	{
		return Joiner.on( joiner ).join( data );
	}

	public static String implode( String joiner, Map<String, String> data )
	{
		return implode( joiner, "=", data );
	}

	public static String implode( String joiner, String separator, Map<String, String> data )
	{
		return Joiner.on( joiner ).withKeyValueSeparator( separator ).join( data );
	}

	public static String implode( String joiner, String[] data )
	{
		return Joiner.on( joiner ).join( data );
	}

	public static boolean is_array( Object obj )
	{
		return obj instanceof Collection || obj instanceof boolean[] || obj instanceof byte[] || obj instanceof short[] || obj instanceof char[] || obj instanceof int[] || obj instanceof long[] || obj instanceof float[] || obj instanceof double[] || obj instanceof Object[];
	}

	public static boolean is_bool( Object obj )
	{
		return obj.getClass() == boolean.class || obj.getClass() == Boolean.class;
	}

	public static boolean is_float( Object obj )
	{
		return obj.getClass() == Float.class || obj.getClass() == float.class;
	}

	public static boolean is_int( Object obj )
	{
		return obj.getClass() == Integer.class || obj.getClass() == int.class;
	}

	public static boolean is_null( Object obj )
	{
		return obj == null;
	}

	public static boolean is_string( Object obj )
	{
		return obj instanceof String;
	}

	/**
	 * Determines if the color hex is darker then 50%
	 *
	 * @param hexdec A hexdec color, e.g., #fff, #f3f3f3
	 * @return True if color is darker then 50%
	 */
	public static boolean isDarkColor( String hexdec )
	{
		return Integer.parseInt( hexdec, 16 ) > 0xffffff / 2;
	}

	public static boolean isNumeric( String str )
	{
		NumberFormat formatter = NumberFormat.getInstance();
		ParsePosition pos = new ParsePosition( 0 );
		formatter.parse( str, pos );
		return str.length() == pos.getIndex();
	}

	public static String md5( String str )
	{
		return SecureFunc.md5( str );
	}

	public static String money_format( Double amt )
	{
		if ( amt == null || amt == 0 )
			return "$0.00";

		// NumberFormat.getCurrencyInstance().format( amt );
		DecimalFormat df = new DecimalFormat( "$###,###,###.00" );
		return df.format( amt );
	}

	public static String money_format( Integer amt )
	{
		return money_format( amt.doubleValue() );
	}

	public static String money_format( String amt )
	{
		if ( amt == null || amt.isEmpty() )
			return "$0.00";

		return money_format( ObjectFunc.castToDouble( amt ) );
	}

	public static boolean notNull( Object o )
	{
		return o != null;
	}

	public static BigDecimal round( BigDecimal amt, int dec )
	{
		return amt.round( new MathContext( dec, RoundingMode.HALF_DOWN ) );
	}

	public static String str_replace( String needle, String replacement, String haystack )
	{
		return haystack.replaceAll( needle, replacement );
	}

	/**
	 * Deprecated because {@link #count(String)} makes for a more consistent replacement.<br>
	 * But remains since it's based on PHP's strlen() method.
	 */
	@Deprecated
	public static int strlen( String var )
	{
		return var == null ? 0 : var.length();
	}

	public static Integer strpos( String haystack, String needle )
	{
		return strpos( haystack, needle, 0 );
	}

	public static Integer strpos( String haystack, String needle, int offset )
	{
		if ( offset > 0 )
			haystack = haystack.substring( offset );

		if ( !haystack.contains( needle ) )
			return null;

		return haystack.indexOf( needle );
	}

	/**
	 * See {@link #toLowerCase(String)}<br>
	 * Based on PHP's strtolower() method
	 */
	public static String strtolower( String str )
	{
		return str == null ? null : str.toLowerCase();
	}

	/**
	 * See {@link #toUpperCase(String)}<br>
	 * Based on PHP's strtoupper() method
	 */
	public static String strtoupper( String str )
	{
		return str == null ? null : str.toUpperCase();
	}

	public static int time()
	{
		return Timings.epoch();
	}

	/**
	 * See {@link String#toLowerCase()} but is safe for use on a null {@link String}
	 */
	public static String toLowerCase( String str )
	{
		return str == null ? null : str.toLowerCase();
	}

	/**
	 * See {@link String#toUpperCase()} but is safe for use on a null {@link String}
	 */
	public static String toUpperCase( String str )
	{
		return str == null ? null : str.toUpperCase();
	}

	/**
	 * See {@link String#trim()} but is safe for use on a null {@link String}
	 */
	public static String trim( String str )
	{
		return str == null ? null : str.trim();
	}

	public static String urlDecode( String url ) throws UnsupportedEncodingException
	{
		return URLDecoder.decode( url, Charsets.UTF_8.displayName() );
	}

	public static String urlEncode( String url ) throws UnsupportedEncodingException
	{
		return URLEncoder.encode( url, Charsets.UTF_8.displayName() );
	}

	@SuppressWarnings( "unchecked" )
	public static String var_export( Object... objs )
	{
		StringBuilder sb = new StringBuilder();

		if ( objs == null )
			return "null";

		for ( Object obj : objs )
			if ( obj != null )
			{
				Map<String, Object> children = Maps.newLinkedHashMap();

				if ( obj instanceof Map )
					for ( Entry<Object, Object> e : ( ( Map<Object, Object> ) obj ).entrySet() )
					{
						String key = ObjectFunc.castToString( e.getKey() );
						if ( key == null )
							key = e.getKey().toString();
						children.put( key, e.getValue() );
					}
				else if ( obj instanceof Collection )
				{
					int i = 0;
					for ( Object o : ( Collection<Object> ) obj )
					{
						children.put( Integer.toString( i ), o );
						i++;
					}
				}
				else if ( obj instanceof Object[] )
					for ( int i = 0; i < ( ( Object[] ) obj ).length; i++ )
						children.put( Integer.toString( i ), ( ( Object[] ) obj )[i] );

				// boolean[], byte[], short[], char[], int[], long[], float[], double[], Object[]

				Object value = ObjectFunc.castToString( obj );
				if ( value == null )
					value = obj.toString();

				if ( !children.isEmpty() )
					value = children.size();

				sb.append( "\n" + obj.getClass().getName() + "(" + value + ")" );

				if ( !children.isEmpty() )
				{
					sb.append( " {" );
					for ( Entry<String, Object> c : children.entrySet() )
					{
						sb.append( "\n\t[" + c.getKey() + "]=>" );
						for ( String s : var_export( c.getValue() ).split( "\n" ) )
							sb.append( "\n\t" + s );
					}
					sb.append( "\n}" );
				}
			}
			else
				sb.append( "\nnull" );

		if ( sb.length() < 1 )
			return "";

		return sb.substring( 1 ).toString();
	}

	/**
	 * Alias for {@link #print(Object)}
	 */
	public void echo( Object obj )
	{
		try
		{
			print( WebFunc.escapeHTML( ( String ) obj ) );
		}
		catch ( ClassCastException e )
		{
			print( obj );
		}
	}

	public void exit() throws DiedException
	{
		die( null );
	}
}
