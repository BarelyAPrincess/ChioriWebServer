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

import com.chiorichan.logger.Log;
import com.chiorichan.site.Site;
import com.chiorichan.zutils.ZObjects;
import com.chiorichan.zutils.ZStrings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

public class Route
{
	protected Map<String, String> params = Maps.newLinkedHashMap();
	protected Map<String, String> rewrites = Maps.newHashMap();
	protected Site site;

	protected Route( Map<String, String> params, Site site )
	{
		this.site = site;
		this.params = params;
	}

	public String getParam( String id )
	{
		return params.get( id );
	}

	public boolean hasParam( String id )
	{
		return params.containsKey( id );
	}

	public Map<String, String> getParams()
	{
		return params;
	}

	public Map<String, String> getRewrites()
	{
		return rewrites;
	}

	public int httpCode()
	{
		return ZObjects.isEmpty( params.get( "status" ) ) ? 301 : Integer.parseInt( params.get( "status" ) );
	}

	public boolean isRedirect()
	{
		return params.get( "redirect" ) != null;
	}

	public String match( String uri, String host )
	{
		String prop = params.get( "pattern" );

		if ( prop == null )
			return null; // Ignore, is likely a route url entry

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
		return "Route {params=[" + params.entrySet().stream().map( e -> e.getKey() + "=\"" + e.getValue() + "\"" ).collect( Collectors.joining( "," ) ) + "]}";
	}
}
