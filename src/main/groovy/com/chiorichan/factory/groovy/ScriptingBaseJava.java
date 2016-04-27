/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2016 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.factory.groovy;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;

import com.chiorichan.database.DatabaseEngineLegacy;
import com.chiorichan.datastore.sql.bases.SQLDatastore;
import com.chiorichan.factory.api.Builtin;
import com.chiorichan.http.HttpRequestWrapper;
import com.chiorichan.http.HttpResponseWrapper;
import com.chiorichan.http.Nonce;
import com.chiorichan.lang.PluginNotFoundException;
import com.chiorichan.logger.Log;
import com.chiorichan.plugin.PluginManager;
import com.chiorichan.plugin.loader.Plugin;
import com.chiorichan.session.Session;
import com.chiorichan.site.Site;
import com.chiorichan.util.ObjectFunc;
import com.google.common.base.Joiner;

/*
 * XXX This deprecated class has already been ported to ScriptApiBase class
 */
@Deprecated
public abstract class ScriptingBaseJava extends Builtin
{
	public void define( String key, Object val )
	{
		getSession().setGlobal( key, val );
	}

	public File dirname()
	{
		File file = getRequest().getInterpreter().getFile();
		return file == null ? null : file.getParentFile();
	}

	public String domain( String subdomain )
	{
		String url = subdomain != null && !subdomain.isEmpty() ? subdomain + "." : "";
		url += getRequest().getDomain() + "/";
		return url;
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

	public Log getLogger()
	{
		return Log.get( getClass().getSimpleName() );
	}

	public Plugin getPluginbyClassname( String search ) throws PluginNotFoundException
	{
		return PluginManager.instance().getPluginByClassname( search );
	}

	public Plugin getPluginbyClassnameWithoutException( String search )
	{
		return PluginManager.instance().getPluginByClassnameWithoutException( search );
	}

	public Plugin getPluginByName( String search ) throws PluginNotFoundException
	{
		return PluginManager.instance().getPluginByName( search );
	}

	public Plugin getPluginByNameWithoutException( String search )
	{
		return PluginManager.instance().getPluginByNameWithoutException( search );
	}

	public abstract HttpRequestWrapper getRequest();

	public abstract HttpResponseWrapper getResponse();

	public abstract Session getSession();

	public abstract Site getSite();

	public SQLDatastore getSql()
	{
		SQLDatastore sql = getSite().getDatastore();

		if ( sql == null )
			throw new IllegalStateException( "The site database is unconfigured. It will need to be setup in order for you to use the getSql() method." );

		return sql;
	}

	public void header( String header )
	{
		if ( header.startsWith( "HTTP" ) )
		{
			Matcher m = Pattern.compile( "HTTP[^ ]* (\\d*) (.*)" ).matcher( header );
			if ( m.find() )
				getResponse().setStatus( Integer.parseInt( m.group( 1 ) ) );
		}
		else if ( header.startsWith( "Location:" ) )
			getResponse().sendRedirect( header.substring( header.indexOf( ':' ) + 1 ).trim() );
		else if ( header.contains( ":" ) )
			header( header.substring( 0, header.indexOf( ':' ) ), header.substring( header.indexOf( ':' ) + 1 ).trim() );
		else
			throw new IllegalArgumentException( "The header argument is malformed!" );
	}

	public void header( String key, String val )
	{
		getResponse().setHeader( key, val );
	}

	public Nonce nonce()
	{
		return getSession().getNonce();
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

	public String url_get_append( Map<String, Object> map )
	{
		return url_get_append( null, map );
	}

	public String url_get_append( String subdomain, Map<String, Object> map )
	{
		Validate.notNull( map, "Map can not be null" );

		String url = getRequest().getFullUrl( subdomain );

		Map<String, String> getMap = new HashMap<String, String>( getRequest().getGetMapRaw() );

		for ( Entry<String, Object> e : map.entrySet() )
			getMap.put( e.getKey(), ObjectFunc.castToString( e.getValue() ) );

		if ( getMap.size() > 0 )
			url += "?" + Joiner.on( "&" ).withKeyValueSeparator( "=" ).join( getMap );

		return url;
	}

	public String url_get_append( String key, Object val )
	{
		return url_get_append( null, key, val );
	}

	public String url_get_append( String subdomain, String key, Object val )
	{
		Validate.notNull( key );
		Validate.notNull( val );

		return url_get_append( subdomain, new HashMap<String, Object>()
		{
			{
				put( key, val );
			}
		} );
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
}
