/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.factory.groovy;

import groovy.lang.Binding;
import groovy.lang.Script;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
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
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.ocpsoft.prettytime.PrettyTime;

import com.chiorichan.tasks.Timings;
import com.chiorichan.util.ObjectFunc;
import com.chiorichan.util.SecureFunc;
import com.chiorichan.util.StringFunc;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * The base Groovy API for Chiori-chan's Web Server
 */
public abstract class ScriptApiBase extends Script
{
	public ScriptApiBase()
	{
		super();
	}
	
	public ScriptApiBase( Binding binding )
	{
		super( binding );
	}
	
	/**
	 * Provides an easy array collections method.
	 * Not sure if this is counterintuitive since technically Groovy provides an easier array of [], so what, i.e., def val = array( "obj1", "ovj2", "obj3" );
	 * Comparable to PHP's array function, http://www.w3schools.com/php/func_array.asp
	 * 
	 * @param vals
	 *            Elements of the array separated as an argument
	 * @return The array of elements
	 */
	@SuppressWarnings( "unchecked" )
	public <T> T[] array( T... vals )
	{
		return vals;
	}
	
	public int count( Collection<Object> list )
	{
		return ( list == null ) ? 0 : list.size();
	}
	
	public int count( Map<Object, Object> maps )
	{
		return ( maps == null ) ? 0 : maps.size();
	}
	
	public int count( Object[] var )
	{
		return ( var == null ) ? 0 : var.length;
	}
	
	public int count( String var )
	{
		return ( var == null ) ? 0 : var.length();
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
		
		int colLength = ( headerArray != null ) ? headerArray.size() : tableData.size();
		for ( Object row : tableData.values() )
			if ( row instanceof Map )
				colLength = Math.max( ( ( Map<String, Object> ) row ).size(), colLength );
		
		for ( Object row : tableData.values() )
		{
			String clss = ( x % 2 == 0 ) ? "evenrowcolor" : "oddrowcolor";
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
							String subclass = ( col instanceof String && ( ( String ) col ).isEmpty() ) ? " emptyCol" : "";
							sb.append( "<td id=\"col_" + cc + "\" class=\"" + subclass + "\">" + col + "</td>\n" );
							cc++;
						}
				}
				sb.append( "</tr>\n" );
			}
			else if ( row instanceof String )
				sb.append( "<tr><td class=\"" + clss + "\" colspan=\"" + colLength + "\"><b><center>" + ( ( String ) row ) + "</b></center></td></tr>\n" );
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
			format = format.replaceAll( "U", ( date.getTime() / 1000 ) + "" );
		
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
	
	public String date_ago( Date date )
	{
		PrettyTime p = new PrettyTime();
		return p.format( date );
	}
	
	/**
	 * Default format is M/d/yyyy
	 * 
	 * @param Date
	 *            you wish to convert
	 * @return Long containing the epoch of provided date
	 */
	
	public Long date_epoch( String date )
	{
		return date_epoch( date, null );
	}
	
	public Long date_epoch( String date, String format )
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
	
	public boolean empty( Collection<Object> list )
	{
		return ( list == null || list.size() < 1 );
	}
	
	@SuppressWarnings( "rawtypes" )
	public boolean empty( Iterator list )
	{
		return ( list == null || list.hasNext() );
	}
	
	public boolean empty( Map<Object, Object> maps )
	{
		return ( maps == null || maps.size() < 1 );
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
	 * Filters a map for the specified list of keys, removing keys that are not contained in the list.
	 * Groovy example: def filteredMap = getHttpUtils().filter( unfilteredMap, ["keyA", "keyB", "someKey"], false );
	 * 
	 * @param data
	 *            The map that needs checking
	 * @param allowedKeys
	 *            A list of keys allowed
	 * @param caseSensitive
	 *            Will the key match be case sensitive or not
	 * @return The resulting map of filtered data
	 */
	public Map<String, Object> filter( Map<String, Object> data, Collection<String> allowedKeys, boolean caseSensitive )
	{
		Map<String, Object> newArray = new LinkedHashMap<String, Object>();
		
		if ( !caseSensitive )
			allowedKeys = StringFunc.toLowerCaseList( allowedKeys );
		
		for ( Entry<String, Object> e : data.entrySet() )
			if ( ( !caseSensitive && allowedKeys.contains( e.getKey().toLowerCase() ) ) || allowedKeys.contains( e.getKey() ) )
				newArray.put( e.getKey(), e.getValue() );
		
		return newArray;
	}
	
	public String guid() throws UnsupportedEncodingException
	{
		return guid( Timings.epoch() + "-guid" );
	}
	
	public String guid( String seed )
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
	
	/**
	 * Determines if the color hex is darker then 50%
	 * 
	 * @param hexdec
	 *            A hexdec color, e.g., #fff, #f3f3f3
	 * @return True if color is darker then 50%
	 */
	public boolean is_darkcolor( String hexdec )
	{
		return Integer.parseInt( hexdec, 16 ) > ( 0xffffff / 2 );
	}
	
	public boolean is_null( Object obj )
	{
		return ( obj == null );
	}
	
	public boolean is_numeric( String str )
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
	
	public boolean notNull( Object o )
	{
		return o != null;
	}
	
	public String random()
	{
		return random( 8, true, false, new String[0] );
	}
	
	public String random( int length )
	{
		return random( length, true, false, new String[0] );
	}
	
	public String random( int length, boolean numbers )
	{
		return random( length, numbers, false, new String[0] );
	}
	
	public String random( int length, boolean numbers, boolean letters )
	{
		return random( length, numbers, letters, new String[0] );
	}
	
	public String random( int length, boolean numbers, boolean letters, String[] allowedChars )
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
		return ( var == null ) ? 0 : var.length();
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
		return ( str == null ) ? null : str.toLowerCase();
	}
	
	/**
	 * See {@link #toUpperCase(String)}<br>
	 * Based on PHP's strtoupper() method
	 */
	public String strtoupper( String str )
	{
		return ( str == null ) ? null : str.toUpperCase();
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
		return ( str == null ) ? null : str.toLowerCase();
	}
	
	/**
	 * See {@link String#toUpperCase()} but is safe for use on a null {@link String}
	 */
	public String toUpperCase( String str )
	{
		return ( str == null ) ? null : str.toUpperCase();
	}
	
	/**
	 * See {@link String#trim()} but is safe for use on a null {@link String}
	 */
	public String trim( String str )
	{
		return ( str == null ) ? null : str.trim();
	}
	
	public String uuid() throws UnsupportedEncodingException
	{
		return uuid( Timings.epoch() + "-uuid" );
	}
	
	public String uuid( String seed ) throws UnsupportedEncodingException
	{
		return DigestUtils.md5Hex( guid( seed ) );
	}
	
	/**
	 * Same as {@link ScriptingBaseJava#var_export(obj)} but instead prints the result to the buffer
	 * Based on method of same name in PHP
	 * 
	 * @param obj
	 *            The object you wish to dump
	 */
	public void var_dump( Object... obj )
	{
		println( var_export( obj ) );
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
