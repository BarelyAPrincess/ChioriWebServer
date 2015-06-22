/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.http;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.chiorichan.Loader;
import com.chiorichan.database.DatabaseEngine;
import com.chiorichan.http.Routes.RouteType;
import com.chiorichan.site.Site;
import com.chiorichan.util.StringFunc;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class Route
{
	protected RouteType type = RouteType.NOTSET;
	protected Map<String, String> params = Maps.newLinkedHashMap();
	protected Map<String, String> rewrites = Maps.newHashMap();
	protected Site site;
	
	protected Route( ResultSet rs, Site site ) throws SQLException
	{
		this.site = site;
		type = RouteType.SQL;
		params = DatabaseEngine.toStringsMap( rs );
	}
	
	public String toString()
	{
		return "Type: " + type + ", Params: " + params;
	}
	
	/**
	 * 
	 * @param args
	 *            Line input in the format of "pattern '/dir/[cat=]/[id=]', to '/dir/view_item.gsp'"
	 * @throws IOException
	 *             Thrown if input string is not valid
	 */
	public Route( String args, Site site ) throws IOException
	{
		if ( args == null || args.isEmpty() )
			throw new IOException( "args can't be null or empty" );
		
		this.site = site;
		type = RouteType.FILE;
		
		for ( String o : args.split( "," ) )
		{
			String key = null;
			String val = null;
			
			o = o.trim();
			
			if ( o.contains( ":" ) )
			{
				key = o.substring( 0, o.indexOf( ":" ) );
				val = o.substring( o.indexOf( ":" ) + 1 );
			}
			else if ( ( !o.contains( "\"" ) && !o.contains( "'" ) ) || ( o.contains( "\"" ) && o.indexOf( " " ) < o.indexOf( "\"" ) ) || ( o.contains( "'" ) && o.indexOf( " " ) < o.indexOf( "'" ) ) )
			{
				key = o.substring( 0, o.indexOf( " " ) );
				val = o.substring( o.indexOf( " " ) + 1 );
			}
			
			if ( key != null && val != null )
			{
				key = StringUtils.trimToEmpty( key.toLowerCase() );
				val = StringUtils.trimToEmpty( val );
				
				val = StringUtils.removeStart( val, "\"" );
				val = StringUtils.removeStart( val, "'" );
				
				val = StringUtils.removeEnd( val, "\"" );
				val = StringUtils.removeEnd( val, "'" );
				
				params.put( key, val );
			}
		}
		
		params.put( "domain", site.getDomain() );
	}
	
	public RouteType getRouteType()
	{
		return type;
	}
	
	public Map<String, String> getRewrites()
	{
		return rewrites;
	}
	
	public Map<String, String> getParams()
	{
		return params;
	}
	
	public File getFile()
	{
		if ( params.get( "file" ) != null && !params.get( "file" ).isEmpty() )
			return new File( site.getSourceDirectory(), params.get( "file" ) );
		
		return null;
	}
	
	public String getHTML()
	{
		if ( params.get( "html" ) != null && !params.get( "html" ).isEmpty() )
			return params.get( "html" );
		
		return null;
	}
	
	public String match( String domain, String subdomain, String uri )
	{
		String prop = params.get( "pattern" );
		
		if ( prop == null )
			prop = params.get( "page" );
		
		if ( prop == null )
		{
			Loader.getLogger().warning( "The `pattern` attribute was null for route '" + this + "'. Unusable!" );
			return null;
		}
		
		prop = StringUtils.trimToEmpty( prop );
		uri = StringUtils.trimToEmpty( uri );
		
		if ( prop.startsWith( "/" ) )
		{
			prop = prop.substring( 1 );
			params.put( "pattern", prop );
		}
		
		if ( !StringUtils.trimToEmpty( params.get( "subdomain" ) ).equals( "*" ) && !subdomain.equals( params.get( "subdomain" ) ) )
		{
			Loader.getLogger().fine( "The subdomain does not match for " + uri + " on route " + this );
			return null;
		}
		
		String[] propsRaw = prop.split( "[.//]" );
		String[] urisRaw = uri.split( "[.//]" );
		
		ArrayList<String> props = Lists.newArrayList();
		ArrayList<String> uris = Lists.newArrayList();
		
		for ( String s : propsRaw )
			if ( s != null && !s.isEmpty() )
				props.add( s );
		
		for ( String s : urisRaw )
			if ( s != null && !s.isEmpty() )
				uris.add( s );
		
		if ( uris.isEmpty() )
			uris.add( "" );
		
		if ( props.isEmpty() )
			props.add( "" );
		
		if ( props.size() > uris.size() )
		{
			Loader.getLogger().fine( "The length of elements in route " + this + " is LONGER then the length of elements on the uri; " + uris );
			return null;
		}
		
		if ( props.size() < uris.size() )
		{
			Loader.getLogger().fine( "The length of elements in route " + this + " is SHORTER then the length of elements on the uri; " + uris );
			return null;
		}
		
		String weight = StringUtils.repeat( "?", Math.max( props.size(), uris.size() ) );
		
		boolean match = true;
		for ( int i = 0; i < Math.max( props.size(), uris.size() ); i++ )
		{
			try
			{
				Loader.getLogger().fine( prop + " --> " + props.get( i ) + " == " + uris.get( i ) );
				
				if ( props.get( i ).matches( "\\[([a-zA-Z0-9]+)=\\]" ) )
				{
					weight = StringFunc.replaceAt( weight, i, "Z" );
					
					String key = props.get( i ).replaceAll( "[\\[\\]=]", "" );
					String value = uris.get( i );
					
					rewrites.put( key, value );
					
					// PREG MATCH
					Loader.getLogger().fine( "Found a PREG match for " + prop + " on route " + this );
				}
				else if ( props.get( i ).equals( uris.get( i ) ) )
				{
					weight = StringFunc.replaceAt( weight, i, "A" );
					
					Loader.getLogger().fine( "Found a match for " + prop + " on route " + this );
					// MATCH
				}
				else
				{
					match = false;
					Loader.getLogger().fine( "Found no match for " + prop + " on route " + this );
					break;
					// NO MATCH
				}
			}
			catch ( ArrayIndexOutOfBoundsException e )
			{
				match = false;
				break;
			}
		}
		
		return ( match ) ? weight : null;
	}
}
