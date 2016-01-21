/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com> All Right Reserved.
 */
package com.chiorichan.factory.groovy;

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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.commons.lang3.ArrayUtils;
import org.ocpsoft.prettytime.PrettyTime;

import com.chiorichan.APILogger;
import com.chiorichan.Loader;
import com.chiorichan.database.DatabaseEngineLegacy;
import com.chiorichan.datastore.sql.bases.SQLDatastore;
import com.chiorichan.http.HttpRequestWrapper;
import com.chiorichan.http.HttpResponseWrapper;
import com.chiorichan.http.Nonce;
import com.chiorichan.plugin.PluginManager;
import com.chiorichan.plugin.lang.PluginNotFoundException;
import com.chiorichan.plugin.loader.Plugin;
import com.chiorichan.session.Session;
import com.chiorichan.site.Site;
import com.chiorichan.tasks.Timings;
import com.chiorichan.util.ObjectFunc;
import com.chiorichan.util.SecureFunc;
import com.chiorichan.util.StringFunc;
import com.chiorichan.util.Versioning;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

/*
 * XXX This deprecated class has already been ported to ScriptApiBase class
 */
@Deprecated
public abstract class ScriptingBaseJava extends Script
{
	public String apache_get_version()
	{
		return "THIS IS NOT APACHE YOU DUMMY!!!";
	}

	/**
	 * Provides an easy array collections method. Not sure if this is counterintuitive since technically Groovy provides an easier array of [], so what, i.e., def val = array( "obj1", "ovj2", "obj3" ); Comparable to PHP's array function,
	 * http://www.w3schools.com/php/func_array.asp
	 *
	 * @param vals
	 *             Elements of the array separated as an argument
	 * @return The array of elements
	 */
	@SuppressWarnings( "unchecked" )
	public <T> T[] array( T... vals )
	{
		return vals;
	}

	public boolean asBool( Object obj )
	{
		return ObjectFunc.castToBool( obj );
	}

	public double asDouble( Object obj )
	{
		return ObjectFunc.castToDouble( obj );
	}

	public int asInt( Object obj )
	{
		return ObjectFunc.castToInt( obj );
	}

	public long asLong( Object obj )
	{
		return ObjectFunc.castToLong( obj );
	}

	public String asString( Object obj )
	{
		return ObjectFunc.castToString( obj );
	}

	public byte[] base64Decode( String str )
	{
		return SecureFunc.base64Decode( str );
	}

	public String base64DecodeString( String str )
	{
		return SecureFunc.base64DecodeString( str );
	}

	public String base64Encode( byte[] bytes )
	{
		return SecureFunc.base64Encode( bytes );
	}

	public String base64Encode( String str )
	{
		return SecureFunc.base64Encode( str );
	}

	public int count( Collection<Object> list )
	{
		return list == null ? 0 : list.size();
	}

	public int count( Map<Object, Object> maps )
	{
		return maps == null ? 0 : maps.size();
	}

	public int count( Object[] var )
	{
		return var == null ? 0 : var.length;
	}

	public int count( String var )
	{
		return var == null ? 0 : var.length();
	}

	public String createTable( Collection<Object> tableData )
	{
		return createTable( tableData, null, null );
	}

	public String createTable( Collection<Object> tableData, Collection<String> headerArray )
	{
		return createTable( tableData, headerArray, null, null );
	}

	public String createTable( Collection<Object> tableData, Collection<String> headerArray, String tableId )
	{
		return createTable( tableData, headerArray, tableId, null );
	}

	public String createTable( Collection<Object> tableData, Collection<String> headerArray, String tableId, String altTableClass )
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

	public String createTable( Map<Object, Object> tableData )
	{
		return createTable( tableData, null, "" );
	}

	public String createTable( Map<Object, Object> tableData, Collection<String> headerArray )
	{
		return createTable( tableData, headerArray, "" );
	}

	public String createTable( Map<Object, Object> tableData, Collection<String> headerArray, String tableId )
	{
		return createTable( tableData, headerArray, tableId, null );
	}

	@SuppressWarnings( "unchecked" )
	public String createTable( Map<Object, Object> tableData, Collection<String> headerArray, String tableId, String altTableClass )
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
							String subclass = col instanceof String && ( ( String ) col ).isEmpty() ? " emptyCol" : "";
							sb.append( "<td id=\"col_" + cc + "\" class=\"" + subclass + "\">" + col + "</td>\n" );
							cc++;
						}
				}
				sb.append( "</tr>\n" );
			}
			else if ( row instanceof String )
				sb.append( "<tr><td class=\"" + clss + "\" colspan=\"" + colLength + "\"><b><center>" + ( String ) row + "</b></center></td></tr>\n" );
			else
				sb.append( "<tr><td class=\"" + clss + "\" colspan=\"" + colLength + "\"><b><center>" + row.toString() + "</b></center></td></tr>\n" );
		}
		sb.append( "</table>\n" );

		return sb.toString();
	}

	public String date()
	{
		return date( "" );
	}

	public String date( String format )
	{
		return date( format, "" );
	}

	public String date( String format, Date date )
	{
		return date( format, date, null );
	}

	public String date( String format, Date date, String def )
	{
		return date( format, ObjectFunc.castToString( date.getTime() / 1000 ), def );
	}

	public String date( String format, Object data )
	{
		return date( format, data, null );
	}

	public String date( String format, Object data, String def )
	{
		return date( format, ObjectFunc.castToString( data ), def );
	}

	public String date( String format, String data )
	{
		return date( format, data, null );
	}

	public String date( String format, String data, String def )
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
	 * @param Date
	 *             you wish to convert
	 * @return Long containing the epoch of provided date
	 */
	public Long dateToEpoch( String date )
	{
		return dateToEpoch( date, null );
	}

	public Long dateToEpoch( String date, String format )
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

	public String dirname( File path )
	{
		return path.getParent();
	}

	public String dirname( String path )
	{
		return new File( path ).getParent();
	}

	public String domain( String subdomain )
	{
		String url = subdomain != null && !subdomain.isEmpty() ? subdomain + "." : "";
		url += getRequest().getDomain() + "/";
		return url;
	}

	public boolean empty( Collection<Object> list )
	{
		return list == null || list.size() < 1;
	}

	@SuppressWarnings( "rawtypes" )
	public boolean empty( Iterator list )
	{
		return list == null || list.hasNext();
	}

	public boolean empty( Map<Object, Object> maps )
	{
		return maps == null || maps.size() < 1;
	}

	public boolean empty( Object o )
	{
		if ( o == null )
			return true;
		return false;
	}

	public boolean empty( String var )
	{
		if ( var == null )
			return true;

		if ( var.isEmpty() )
			return true;

		return false;
	}

	public int epoch()
	{
		return Timings.epoch();
	}

	public Collection<String> explode( String limiter, String data )
	{
		if ( data == null || data.isEmpty() )
			return Lists.newArrayList();

		return new ArrayList<String>( Splitter.on( limiter ).splitToList( data ) );
	}

	public Map<String, String> explode( String limiter, String separator, String data )
	{
		if ( data == null || data.isEmpty() )
			return Maps.newHashMap();

		return new HashMap<String, String>( Splitter.on( limiter ).withKeyValueSeparator( separator ).split( data ) );
	}

	public boolean file_exists( File file )
	{
		return file.exists();
	}

	public boolean file_exists( String file )
	{
		return new File( file ).exists();
	}

	public Map<String, Object> filter( Map<String, Object> data, Collection<String> allowedKeys )
	{
		return filter( data, allowedKeys, false );
	}

	/**
	 * Filters a map for the specified list of keys, removing keys that are not contained in the list. Groovy example: def filteredMap = getHttpUtils().filter( unfilteredMap, ["keyA", "keyB", "someKey"], false );
	 *
	 * @param data
	 *             The map that needs checking
	 * @param allowedKeys
	 *             A list of keys allowed
	 * @param caseSensitive
	 *             Will the key match be case sensitive or not
	 * @return The resulting map of filtered data
	 */
	public Map<String, Object> filter( Map<String, Object> data, Collection<String> allowedKeys, boolean caseSensitive )
	{
		Map<String, Object> newArray = new LinkedHashMap<String, Object>();

		if ( !caseSensitive )
			allowedKeys = StringFunc.toLowerCaseList( allowedKeys );

		for ( Entry<String, Object> e : data.entrySet() )
			if ( !caseSensitive && allowedKeys.contains( e.getKey().toLowerCase() ) || allowedKeys.contains( e.getKey() ) )
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

	// This might change
	public String formatTimeAgo( Date date )
	{
		PrettyTime p = new PrettyTime();
		return p.format( date );
	}

	/**
	 * Returns the current server copyright string
	 *
	 * @return Copyright string
	 */
	public String getCopyright()
	{
		return Versioning.getCopyright();
	}

	/**
	 * Returns an instance of the current site database
	 *
	 * @return The site database engine
	 * @throws IllegalStateException
	 *              thrown if the requested database is unconfigured
	 */
	public DatabaseEngineLegacy getDatabase()
	{
		SQLDatastore engine = getSite().getDatastore();

		if ( engine == null )
			throw new IllegalStateException( "The site database is unconfigured. It will need to be setup in order for you to use the getSql() method." );

		return engine.getLegacy();
	}

	public APILogger getLogger()
	{
		return Loader.getLogger( getClass().getSimpleName() );
	}

	public Plugin getPluginbyClassname( String search ) throws PluginNotFoundException
	{
		return PluginManager.INSTANCE.getPluginByClassname( search );
	}

	public Plugin getPluginbyClassnameWithoutException( String search )
	{
		return PluginManager.INSTANCE.getPluginByClassnameWithoutException( search );
	}

	public Plugin getPluginByName( String search ) throws PluginNotFoundException
	{
		return PluginManager.INSTANCE.getPluginByName( search );
	}

	public Plugin getPluginByNameWithoutException( String search )
	{
		return PluginManager.INSTANCE.getPluginByNameWithoutException( search );
	}

	/**
	 * Returns the current server product string
	 */
	public String getProduct()
	{
		return Versioning.getProduct();
	}

	public abstract HttpRequestWrapper getRequest();

	public abstract HttpResponseWrapper getResponse();

	/**
	 * Returns an instance of the server database
	 *
	 * @return The server database engine
	 * @throws IllegalStateException
	 *              thrown if the requested database is unconfigured
	 */
	public DatabaseEngineLegacy getServerDatabase()
	{
		DatabaseEngineLegacy engine = Loader.getDatabase().getLegacy();

		if ( engine == null )
			throw new IllegalStateException( "The server database is unconfigured. It will need to be setup in order for you to use the getServerDatabase() method." );

		return engine;
	}

	public abstract Session getSession();

	public abstract Site getSite();

	/**
	 * See {@link #getDatabase()}
	 */
	@Deprecated
	public DatabaseEngineLegacy getSiteDatabase()
	{
		return getDatabase();
	}

	public SQLDatastore getSql()
	{
		SQLDatastore sql = getSite().getDatastore();

		if ( sql == null )
			throw new IllegalStateException( "The site database is unconfigured. It will need to be setup in order for you to use the getDatabase() method." );

		return sql;
	}

	/**
	 * Returns the current server version string
	 */
	public String getVersion()
	{
		return Versioning.getVersion();
	}

	public String implode( String joiner, Iterable<String> data )
	{
		return Joiner.on( joiner ).join( data );
	}

	public String implode( String joiner, Iterator<String> data )
	{
		return Joiner.on( joiner ).join( data );
	}

	public String implode( String joiner, Map<String, String> data )
	{
		return implode( joiner, "=", data );
	}

	public String implode( String joiner, String separator, Map<String, String> data )
	{
		return Joiner.on( joiner ).withKeyValueSeparator( separator ).join( data );
	}

	public String implode( String joiner, String[] data )
	{
		return Joiner.on( joiner ).join( data );
	}

	public boolean is_null( Object obj )
	{
		return obj == null;
	}

	/**
	 * Determines if the color hex is darker then 50%
	 *
	 * @param hexdec
	 *             A hexdec color, e.g., #fff, #f3f3f3
	 * @return True if color is darker then 50%
	 */
	public boolean isDarkColor( String hexdec )
	{
		return Integer.parseInt( hexdec, 16 ) > 0xffffff / 2;
	}

	public boolean isNumeric( String str )
	{
		NumberFormat formatter = NumberFormat.getInstance();
		ParsePosition pos = new ParsePosition( 0 );
		formatter.parse( str, pos );
		return str.length() == pos.getIndex();
	}

	public String md5( String str )
	{
		return SecureFunc.md5( str );
	}

	public String money_format( Double amt )
	{
		if ( amt == null || amt == 0 )
			return "$0.00";

		// NumberFormat.getCurrencyInstance().format( amt );
		DecimalFormat df = new DecimalFormat( "$###,###,###.00" );
		return df.format( amt );
	}

	public String money_format( Integer amt )
	{
		return money_format( amt.doubleValue() );
	}

	public String money_format( String amt )
	{
		if ( amt == null || amt.isEmpty() )
			return "$0.00";

		return money_format( ObjectFunc.castToDouble( amt ) );
	}

	public Nonce nonce()
	{
		return getSession().getNonce();
	}

	public boolean notNull( Object o )
	{
		return o != null;
	}

	/*
	 * Old WebFunc Methods
	 */
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
			allowedChars = ArrayUtils.addAll( allowedChars, new String[] {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"} );

		if ( letters )
			allowedChars = ArrayUtils.addAll( allowedChars, new String[] {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"} );

		String rtn = "";
		for ( int i = 0; i < length; i++ )
			rtn += allowedChars[new Random().nextInt( allowedChars.length )];

		return rtn;
	}

	public BigDecimal round( BigDecimal amt, int dec )
	{
		return amt.round( new MathContext( dec, RoundingMode.HALF_DOWN ) );
	}

	public String str_replace( String needle, String replacement, String haystack )
	{
		return haystack.replaceAll( needle, replacement );
	}

	/**
	 * Deprecated because {@link #count(String)} makes for a more consistent replacement.<br>
	 * But remains since it's based on PHP's strlen() method.
	 */
	@Deprecated
	public int strlen( String var )
	{
		return var == null ? 0 : var.length();
	}

	public Integer strpos( String haystack, String needle )
	{
		return strpos( haystack, needle, 0 );
	}

	public Integer strpos( String haystack, String needle, int offset )
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
	public String strtolower( String str )
	{
		return str == null ? null : str.toLowerCase();
	}

	/**
	 * See {@link #toUpperCase(String)}<br>
	 * Based on PHP's strtoupper() method
	 */
	public String strtoupper( String str )
	{
		return str == null ? null : str.toUpperCase();
	}

	public int time()
	{
		return Timings.epoch();
	}

	/**
	 * See {@link String#toLowerCase()} but is safe for use on a null {@link String}
	 */
	public String toLowerCase( String str )
	{
		return str == null ? null : str.toLowerCase();
	}

	/**
	 * See {@link String#toUpperCase()} but is safe for use on a null {@link String}
	 */
	public String toUpperCase( String str )
	{
		return str == null ? null : str.toUpperCase();
	}

	/**
	 * See {@link String#trim()} but is safe for use on a null {@link String}
	 */
	public String trim( String str )
	{
		return str == null ? null : str.trim();
	}

	public String uri_to()
	{
		return getRequest().getFullUrl();
	}

	public String uri_to( boolean secure )
	{
		return getRequest().getFullUrl( secure );
	}

	public String uri_to( String subdomain )
	{
		return getRequest().getFullUrl( subdomain );
	}

	public String uri_to( String subdomain, boolean secure )
	{
		return getRequest().getFullUrl( subdomain, secure );
	}

	public String url_get_append( String key, Object val )
	{
		return url_get_append( null, key, val );
	}

	public String url_get_append( String subdomain, String key, Object val )
	{
		String url = getRequest().getFullUrl( subdomain );

		Map<String, String> getMap = new HashMap<String, String>( getRequest().getGetMapRaw() );

		if ( getMap.containsKey( key ) )
			getMap.remove( key );

		if ( getMap.isEmpty() )
			url += "?" + key + "=" + ObjectFunc.castToString( val );
		else
		{
			url += "?" + Joiner.on( "&" ).withKeyValueSeparator( "=" ).join( getMap );
			url += "&" + key + "=" + ObjectFunc.castToString( val );
		}

		return url;
	}

	/**
	 * Same as @link url_to( null )
	 */
	public String url_to()
	{
		return url_to( null );
	}

	public String url_to( String subdomain )
	{
		return getRequest().getFullDomain( subdomain );
	}

	/**
	 * Returns a fresh built URL based on the current domain Used to produce absolute uri's within scripts, e.g., url_to( "css" ) + "stylesheet.css"
	 *
	 * @param subdomain
	 *             The subdomain
	 * @return A valid formatted URI
	 */
	public String url_to( String subdomain, boolean secure )
	{
		return getRequest().getFullDomain( subdomain, secure );
	}

	public String urlDecode( String url ) throws UnsupportedEncodingException
	{
		return URLDecoder.decode( url, Charsets.UTF_8.displayName() );
	}

	public String urlEncode( String url ) throws UnsupportedEncodingException
	{
		return URLEncoder.encode( url, Charsets.UTF_8.displayName() );
	}

	@SuppressWarnings( "unchecked" )
	public String var_export( Object... objs )
	{
		StringBuilder sb = new StringBuilder();

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
}
