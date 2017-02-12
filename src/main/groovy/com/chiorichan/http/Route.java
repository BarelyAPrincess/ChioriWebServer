/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * <p>
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 * <p>
 * All Rights Reserved.
 */
package com.chiorichan.http;

import com.chiorichan.database.DatabaseEngineLegacy;
import com.chiorichan.http.Routes.RouteType;
import com.chiorichan.logger.Log;
import com.chiorichan.site.Site;
import com.chiorichan.zutils.ZObjects;
import com.chiorichan.zutils.ZStrings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

public class Route
{
	protected RouteType type = RouteType.NOTSET;
	protected Map<String, String> params = Maps.newLinkedHashMap();
	protected Map<String, String> rewrites = Maps.newHashMap();
	protected Site site;

	protected Route( Map<String, String> params, Site site ) throws SQLException
	{
		this.site = site;
		type = RouteType.SQL;
		this.params = params;
	}

	protected Route( ResultSet rs, Site site ) throws SQLException
	{
		this.site = site;
		type = RouteType.SQL;
		params = DatabaseEngineLegacy.toStringsMap( rs );
	}

	/**
	 * @param args Line input in the format of "pattern '/dir/[cat=]/[id=]', to '/dir/view_item.gsp'"
	 * @throws IOException Thrown if input string is not valid
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
			else if ( !o.contains( "\"" ) && !o.contains( "'" ) || o.contains( "\"" ) && o.indexOf( " " ) < o.indexOf( "\"" ) || o.contains( "'" ) && o.indexOf( " " ) < o.indexOf( "'" ) )
			{
				key = o.substring( 0, o.indexOf( " " ) );
				val = o.substring( o.indexOf( " " ) + 1 );
			}

			if ( key != null )
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

		// params.put( "domain", site.getTLD() );
	}

	public String getFile()
	{
		return params.get( "file" );
	}

	public String getHTML()
	{
		if ( params.get( "html" ) != null && !params.get( "html" ).isEmpty() )
			return params.get( "html" );
		return null;
	}

	public Map<String, String> getParams()
	{
		return params;
	}

	public String getRedirect()
	{
		return params.get( "redirect" );
	}

	public Map<String, String> getRewrites()
	{
		return rewrites;
	}

	public RouteType getRouteType()
	{
		return type;
	}

	public int httpCode()
	{
		return params.get( "status" ) == null || params.get( "status" ).isEmpty() ? 301 : Integer.parseInt( params.get( "status" ) );
	}

	public boolean isRedirect()
	{
		return params.get( "redirect" ) != null;
	}

	public String match( String uri, String host )
	{
		String prop = params.get( "pattern" );

		if ( prop == null )
			prop = params.get( "page" );

		if ( prop == null )
		{
			Log.get().warning( "The `pattern` attribute was null for route '" + this + "'. Unusable!" );
			return null;
		}

		prop = StringUtils.trimToEmpty( prop );
		uri = StringUtils.trimToEmpty( uri );

		if ( prop.startsWith( "/" ) )
		{
			prop = prop.substring( 1 );
			params.put( "pattern", prop );
		}

		if ( !ZObjects.isEmpty( params.get( "host" ) ) && !host.matches( params.get( "host" ) ) )
		{
			Log.get().finer( "The host failed validation for route " + this );
			return null;
		}

		if ( ZObjects.isEmpty( params.get( "host" ) ) )
			Log.get().warning( "The Route [" + params.entrySet().stream().map( e -> e.getKey() + "=\"" + e.getValue() + "\"" ).collect( Collectors.joining( "," ) ) + "] has no host (Uses RegEx, e.g., ^example.com$) defined, it's recommended that one is set so that the rule is not used unintentionally." );

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
			Log.get().finer( "The length of elements in route " + this + " is LONGER then the length of elements on the uri; " + uris );
			return null;
		}

		if ( props.size() < uris.size() )
		{
			Log.get().finer( "The length of elements in route " + this + " is SHORTER then the length of elements on the uri; " + uris );
			return null;
		}

		String weight = StringUtils.repeat( "?", Math.max( props.size(), uris.size() ) );

		boolean match = true;
		for ( int i = 0; i < Math.max( props.size(), uris.size() ); i++ )
			try
			{
				Log.get().finest( prop + " --> " + props.get( i ) + " == " + uris.get( i ) );

				if ( props.get( i ).matches( "\\[([a-zA-Z0-9]+)=\\]" ) )
				{
					weight = ZStrings.replaceAt( weight, i, "Z" );

					String key = props.get( i ).replaceAll( "[\\[\\]=]", "" );
					String value = uris.get( i );

					rewrites.put( key, value );

					// PREG MATCH
					Log.get().finer( "Found a PREG match for " + prop + " on route " + this );
				}
				else if ( props.get( i ).equals( uris.get( i ) ) )
				{
					weight = ZStrings.replaceAt( weight, i, "A" );

					Log.get().finer( "Found a match for " + prop + " on route " + this );
					// MATCH
				}
				else
				{
					match = false;
					Log.get().finer( "Found no match for " + prop + " on route " + this );
					break;
					// NO MATCH
				}
			}
			catch ( ArrayIndexOutOfBoundsException e )
			{
				match = false;
				break;
			}

		return match ? weight : null;
	}

	@Override
	public String toString()
	{
		return "Route{Type=\"" + type + "\",Params=[" + params.entrySet().stream().map( e -> e.getKey() + "=\"" + e.getValue() + "\"" ).collect( Collectors.joining( "," ) ) + "]}";
	}
}
