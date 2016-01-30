/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com> All Right Reserved.
 */
package com.chiorichan.factory.groovy;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.Validate;

import com.chiorichan.APILogger;
import com.chiorichan.Loader;
import com.chiorichan.database.DatabaseEngineLegacy;
import com.chiorichan.datastore.sql.bases.SQLDatastore;
import com.chiorichan.factory.api.Builtin;
import com.chiorichan.http.HttpRequestWrapper;
import com.chiorichan.http.HttpResponseWrapper;
import com.chiorichan.http.Nonce;
import com.chiorichan.plugin.PluginManager;
import com.chiorichan.plugin.lang.PluginNotFoundException;
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

	public abstract HttpRequestWrapper getRequest();

	public abstract HttpResponseWrapper getResponse();

	public abstract Session getSession();

	public abstract Site getSite();

	public SQLDatastore getSql()
	{
		SQLDatastore sql = getSite().getDatastore();

		if ( sql == null )
			throw new IllegalStateException( "The site database is unconfigured. It will need to be setup in order for you to use the getDatabase() method." );

		return sql;
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
