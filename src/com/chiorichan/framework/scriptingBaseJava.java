package com.chiorichan.framework;

import groovy.lang.Script;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.eclipse.jetty.util.security.Credential.MD5;

abstract public class scriptingBaseJava extends Script
{
	String strtoupper( String str )
	{
		return str.toUpperCase();
	}
	
	String strtolower( String str )
	{
		return str.toLowerCase();
	}
	
	int count( Map<Object, Object> maps )
	{
		return maps.size();
	}
	
	boolean empty( String var )
	{
		if ( var == null )
			return true;
		
		if ( var.isEmpty() )
			return true;
		
		return false;
	}
	
	String date( String format )
	{
		return new SimpleDateFormat( format ).format( new Date() );
	}
	
	String date( String format, String data )
	{
		return new SimpleDateFormat( format ).format( new Date( Long.parseLong( data ) * 1000 ) );
	}
	
	String md5( String str )
	{
		return MD5.digest( str ).substring( 4 );
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
	
	String money_format( Double amt )
	{
		DecimalFormat df = new DecimalFormat( "$###,###,###.00" );
		return df.format( amt );
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
		return new File(path).getParent();
	}
	
	String apache_get_version()
	{
		return "THIS IS NOT APACHE YOU DUMMY!!!";
	}
}
