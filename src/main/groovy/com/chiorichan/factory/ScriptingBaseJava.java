/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.factory;

import groovy.lang.Script;

import java.io.File;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.chiorichan.ConsoleLogger;
import com.chiorichan.Loader;
import com.chiorichan.database.DatabaseEngine;
import com.chiorichan.lang.PluginNotFoundException;
import com.chiorichan.plugin.PluginManager;
import com.chiorichan.plugin.loader.Plugin;
import com.chiorichan.site.Site;
import com.chiorichan.util.CommonFunc;
import com.chiorichan.util.ObjectFunc;
import com.chiorichan.util.StringFunc;
import com.chiorichan.util.Versioning;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public abstract class ScriptingBaseJava extends Script
{
	@SuppressWarnings( "unchecked" )
	String var_export( Object... objs )
	{
		StringBuilder sb = new StringBuilder();
		
		for ( Object obj : objs )
		{
			if ( obj != null )
			{
				Map<String, Object> children = Maps.newLinkedHashMap();
				
				if ( obj instanceof Map )
				{
					for ( Entry<Object, Object> e : ( ( Map<Object, Object> ) obj ).entrySet() )
					{
						String key = ObjectFunc.castToString( e.getKey() );
						if ( key == null )
							key = e.getKey().toString();
						children.put( key, e.getValue() );
					}
				}
				else if ( obj instanceof List )
				{
					for ( int i = 0; i < ( ( List<Object> ) obj ).size(); i++ )
						children.put( "" + i, ( ( List<Object> ) obj ).get( i ) );
				}
				else if ( obj instanceof Object[] )
				{
					for ( int i = 0; i < ( ( Object[] ) obj ).length; i++ )
						children.put( "" + i, ( ( Object[] ) obj )[i] );
				}
				
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
			{
				sb.append( "\nnull" );
			}
		}
		
		if ( sb.length() < 1 )
			return "";
		
		return sb.substring( 1 ).toString();
	}
	
	/**
	 * See {@link String#trim()} but is safe for use on a null {@link String}
	 */
	String trim( String str )
	{
		return ( str == null ) ? null : str.trim();
	}
	
	/**
	 * See {@link String#toUpperCase()} but is safe for use on a null {@link String}
	 */
	String toUpperCase( String str )
	{
		return ( str == null ) ? null : str.toUpperCase();
	}
	
	/**
	 * See {@link #toUpperCase(String)}<br>
	 * Based on PHP's strtoupper() method
	 */
	String strtoupper( String str )
	{
		return ( str == null ) ? null : str.toUpperCase();
	}
	
	/**
	 * See {@link String#toLowerCase()} but is safe for use on a null {@link String}
	 */
	String toLowerCase( String str )
	{
		return ( str == null ) ? null : str.toLowerCase();
	}
	
	/**
	 * See {@link #toLowerCase(String)}<br>
	 * Based on PHP's strtolower() method
	 */
	String strtolower( String str )
	{
		return ( str == null ) ? null : str.toLowerCase();
	}
	
	int count( Map<Object, Object> maps )
	{
		return ( maps == null ) ? 0 : maps.size();
	}
	
	int count( List<Object> list )
	{
		return ( list == null ) ? 0 : list.size();
	}
	
	int count( Object[] var )
	{
		return ( var == null ) ? 0 : var.length;
	}
	
	int count( String var )
	{
		return ( var == null ) ? 0 : var.length();
	}
	
	/**
	 * Deprecated because {@link #count(String)} makes for a more consistent replacement.<br>
	 * But remains since it's based on PHP's strlen() method.
	 */
	@Deprecated
	int strlen( String var )
	{
		return ( var == null ) ? 0 : var.length();
	}
	
	boolean notNull( Object o )
	{
		return o != null;
	}
	
	boolean empty( Object o )
	{
		if ( o == null )
			return true;
		return false;
	}
	
	@SuppressWarnings( "rawtypes" )
	boolean empty( Iterator list )
	{
		return ( list == null || list.hasNext() );
	}
	
	boolean empty( List<Object> list )
	{
		return ( list == null || list.size() < 1 );
	}
	
	boolean empty( Map<Object, Object> maps )
	{
		return ( maps == null || maps.size() < 1 );
	}
	
	boolean empty( String var )
	{
		if ( var == null )
			return true;
		
		if ( var.isEmpty() )
			return true;
		
		return false;
	}
	
	int time()
	{
		return CommonFunc.getEpoch();
	}
	
	/**
	 * Default format is M/d/yyyy
	 * 
	 * @param Date
	 *            you wish to convert
	 * @return Long containing the epoch of provided date
	 */
	Long dateToEpoch( String date )
	{
		return dateToEpoch( date, null );
	}
	
	Long dateToEpoch( String date, String format )
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
	
	String date()
	{
		return date( "" );
	}
	
	String date( String format )
	{
		return date( format, "" );
	}
	
	String date( String format, Object data )
	{
		return date( format, data, null );
	}
	
	String date( String format, Object data, String def )
	{
		return date( format, ObjectFunc.castToString( data ), def );
	}
	
	String date( String format, Date date )
	{
		return date( format, date, null );
	}
	
	String date( String format, Date date, String def )
	{
		return date( format, ObjectFunc.castToString( date.getTime() / 1000 ), def );
	}
	
	String date( String format, String data )
	{
		return date( format, data, null );
	}
	
	String date( String format, String data, String def )
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
		{
			format = format.replaceAll( "U", ( date.getTime() / 1000 ) + "" );
		}
		
		if ( format.contains( "x" ) )
		{
			Calendar var1 = Calendar.getInstance();
			var1.setTime( date );
			int day = var1.get( Calendar.DAY_OF_MONTH );
			String suffix = "";
			
			if ( day >= 11 && day <= 13 )
			{
				suffix = "'th'";
			}
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
	
	String md5( String str )
	{
		return StringFunc.md5( str );
	}
	
	Integer strpos( String haystack, String needle )
	{
		return strpos( haystack, needle, 0 );
	}
	
	Integer strpos( String haystack, String needle, int offset )
	{
		if ( offset > 0 )
			haystack = haystack.substring( offset );
		
		if ( !haystack.contains( needle ) )
			return null;
		
		return haystack.indexOf( needle );
	}
	
	String str_replace( String needle, String replacement, String haystack )
	{
		return haystack.replaceAll( needle, replacement );
	}
	
	String money_format( String amt )
	{
		if ( amt == null || amt.isEmpty() )
			return "$0.00";
		
		return money_format( ObjectFunc.castToDouble( amt ) );
	}
	
	String money_format( Integer amt )
	{
		return money_format( amt.doubleValue() );
	}
	
	String money_format( Double amt )
	{
		if ( amt == null || amt == 0 )
			return "$0.00";
		
		// NumberFormat.getCurrencyInstance().format( amt );
		DecimalFormat df = new DecimalFormat( "$###,###,###.00" );
		return df.format( amt );
	}
	
	int getEpoch()
	{
		return CommonFunc.getEpoch();
	}
	
	List<String> explode( String limiter, String data )
	{
		if ( data == null || data.isEmpty() )
			return Lists.newArrayList();
		
		return Splitter.on( limiter ).splitToList( data );
	}
	
	Map<String, String> explode( String limiter, String separator, String data )
	{
		if ( data == null || data.isEmpty() )
			return Maps.newHashMap();
		
		return Splitter.on( limiter ).withKeyValueSeparator( separator ).split( data );
	}
	
	String implode( String joiner, Map<String, String> data )
	{
		return implode( joiner, "=", data );
	}
	
	String implode( String joiner, String separator, Map<String, String> data )
	{
		return Joiner.on( joiner ).withKeyValueSeparator( separator ).join( data );
	}
	
	String implode( String joiner, String[] data )
	{
		return Joiner.on( joiner ).join( data );
	}
	
	String implode( String joiner, Iterator<String> data )
	{
		return Joiner.on( joiner ).join( data );
	}
	
	String implode( String joiner, Iterable<String> data )
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
	boolean isDarkColor( String hexdec )
	{
		return Integer.parseInt( hexdec, 16 ) > ( 0xffffff / 2 );
	}
	
	public boolean isNumeric( String str )
	{
		NumberFormat formatter = NumberFormat.getInstance();
		ParsePosition pos = new ParsePosition( 0 );
		formatter.parse( str, pos );
		return str.length() == pos.getIndex();
	}
	
	boolean is_null( Object obj )
	{
		return ( obj == null );
	}
	
	boolean file_exists( File file )
	{
		return file.exists();
	}
	
	boolean file_exists( String file )
	{
		return new File( file ).exists();
	}
	
	String dirname( File path )
	{
		return path.getParent();
	}
	
	String dirname( String path )
	{
		return new File( path ).getParent();
	}
	
	BigDecimal round( BigDecimal amt, int dec )
	{
		return amt.round( new MathContext( dec, RoundingMode.HALF_DOWN ) );
	}
	
	Plugin getPluginbyClassname( String search ) throws PluginNotFoundException
	{
		return PluginManager.INSTANCE.getPluginByClassname( search );
	}
	
	Plugin getPluginbyClassnameWithoutException( String search )
	{
		return PluginManager.INSTANCE.getPluginByClassnameWithoutException( search );
	}
	
	Plugin getPluginByName( String search ) throws PluginNotFoundException
	{
		return PluginManager.INSTANCE.getPluginByName( search );
	}
	
	Plugin getPluginByNameWithoutException( String search )
	{
		return PluginManager.INSTANCE.getPluginByNameWithoutException( search );
	}
	
	ConsoleLogger getLogger()
	{
		return Loader.getLogger( getClass().getSimpleName() );
	}
	
	String apache_get_version()
	{
		return "THIS IS NOT APACHE YOU DUMMY!!!";
	}
	
	/**
	 * Returns the current server version string
	 */
	String getVersion()
	{
		return Versioning.getVersion();
	}
	
	/**
	 * Returns the current server product string
	 */
	String getProduct()
	{
		return Versioning.getProduct();
	}
	
	/**
	 * Returns the current server copyright string
	 * 
	 * @return
	 *         Copyright string
	 */
	String getCopyright()
	{
		return Versioning.getCopyright();
	}
	
	/**
	 * Returns an instance of the server database
	 * 
	 * @return
	 *         The server database engine
	 * @throws IllegalStateException
	 *             thrown if the requested database is unconfigured
	 */
	DatabaseEngine getServerDatabase()
	{
		DatabaseEngine engine = Loader.getDatabase();
		
		if ( engine == null )
			throw new IllegalStateException( "The server database is unconfigured. It will need to be setup in order for you to use the getServerDatabase() method." );
		
		return engine;
	}
	
	/**
	 * See {@link #getDatabase()}
	 */
	@Deprecated
	DatabaseEngine getSiteDatabase()
	{
		return getDatabase();
	}
	
	/**
	 * Returns an instance of the current site database
	 * 
	 * @return
	 *         The site database engine
	 * @throws IllegalStateException
	 *             thrown if the requested database is unconfigured
	 */
	DatabaseEngine getDatabase()
	{
		DatabaseEngine engine = getSite().getDatabase();
		
		if ( engine == null )
			throw new IllegalStateException( "The site database is unconfigured. It will need to be setup in order for you to use the getDatabase() method." );
		
		return engine;
	}
	
	/*
	 * Abstract Methods
	 */
	
	abstract Site getSite();
}
