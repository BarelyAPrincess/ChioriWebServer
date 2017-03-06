/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * <p>
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 * <p>
 * All Rights Reserved.
 */
package com.chiorichan.factory.groovy;

import com.chiorichan.database.DatabaseEngineLegacy;
import com.chiorichan.datastore.sql.bases.SQLDatastore;
import com.chiorichan.factory.api.Builtin;
import com.chiorichan.helpers.Pair;
import com.chiorichan.http.HttpRequestWrapper;
import com.chiorichan.http.HttpResponseWrapper;
import com.chiorichan.http.Nonce;
import com.chiorichan.http.Route;
import com.chiorichan.lang.PluginNotFoundException;
import com.chiorichan.lang.SiteConfigurationException;
import com.chiorichan.logger.Log;
import com.chiorichan.plugin.PluginManager;
import com.chiorichan.plugin.loader.Plugin;
import com.chiorichan.session.Session;
import com.chiorichan.site.DomainMapping;
import com.chiorichan.site.Site;
import com.chiorichan.site.SiteManager;
import com.chiorichan.zutils.ZHttp;
import com.chiorichan.zutils.ZMaps;
import com.chiorichan.zutils.ZObjects;
import com.chiorichan.zutils.ZStrings;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

	public String domain()
	{
		return domain( null );
	}

	public String domain( String subdomain )
	{
		StringBuilder domain = new StringBuilder();

		if ( subdomain != null && subdomain.trim().length() > 0 )
			domain.append( subdomain.trim() ).append( "." );

		domain.append( getRequest().getRootDomain() ).append( "/" );

		return domain.toString();
	}

	/**
	 * Returns an instance of the current site database
	 *
	 * @return The site database engine
	 * @throws IllegalStateException thrown if the requested database is unconfigured
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

	public Plugin getPluginByClassname( String search ) throws PluginNotFoundException
	{
		return PluginManager.instance().getPluginByClassname( search );
	}

	public Plugin getPluginByClassnameWithoutException( String search )
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

	public String base_url()
	{
		return getRequest().getBaseUrl();
	}

	public String route_id( String id ) throws SiteConfigurationException
	{
		return route_id( id, new HashMap<>() );
	}

	public String route_id( String id, List<String> params ) throws SiteConfigurationException
	{
		AtomicInteger inx = new AtomicInteger();
		return route_id( id, params.stream().map( l -> new Pair<>( Integer.toString( inx.getAndIncrement() ), l ) ).collect( Collectors.toMap( p -> p.getKey(), p -> p.getValue() ) ) );
	}

	public String route_id( String id, Map<String, String> params ) throws SiteConfigurationException
	{
		params = ZObjects.castMap( params, String.class, String.class );
		Route route = getSite().getRoutes().routeUrl( id );

		if ( ZObjects.isNull( route ) )
			throw new SiteConfigurationException( "Failed to find a route for id [" + id + "]" );

		if ( route.hasParam( "pattern" ) )
		{
			String url = route.getParam( "pattern" );
			Pattern p = Pattern.compile( "\\[([a-zA-Z0-9]*)\\=\\]" );
			Matcher m = p.matcher( url );
			AtomicInteger iteration = new AtomicInteger();

			if ( m.find() )
				do
				{
					String key = m.group( 1 );

					if ( !params.containsKey( key ) )
						if ( params.containsKey( Integer.toString( iteration.get() ) ) )
							key = Integer.toString( iteration.getAndIncrement() );
						else
							throw new SiteConfigurationException( "Route param [" + key + "] went unspecified for id [" + id + "], pattern [" + route.getParam( "pattern" ) + "]" );

					url = m.replaceFirst( params.get( key ) );
					m = p.matcher( url );
				}
				while ( m.find() );

			url = ZStrings.trimFront( url, '/' );

			if ( route.hasParam( "domain" ) )
			{
				String domain = ZHttp.normalize( route.getParam( "domain" ) );
				if ( ZObjects.isEmpty( domain ) )
					return getRequest().getFullDomain() + url;
				if ( domain.startsWith( "http" ) || domain.startsWith( "//" ) )
					return domain + url;
				return ( getRequest().isSecure() ? "https://" : "http://" ) + domain + "/" + url;
			}

			/* Validates if the host string could be used as the domain, meaning it's a simple (or not) regex string */
			if ( route.hasParam( "host" ) && !ZObjects.isEmpty( route.getParam( "host" ) ) )
			{
				String host = ZHttp.normalize( route.getParam( "host" ) );

				if ( host.startsWith( "^" ) )
					host = host.substring( 1 );
				if ( host.endsWith( "$" ) )
					host = host.substring( 0, host.length() - 1 );

				if ( host.matches( "[a-z0-9.]+" ) )
				{
					if ( host.startsWith( "http" ) || host.startsWith( "//" ) )
						return host + url;
					return ( getRequest().isSecure() ? "https://" : "http://" ) + host + "/" + url;
				}
			}

			return getRequest().getFullDomain() + url;
		}
		else if ( route.hasParam( "url" ) )
		{
			String url = route.getParam( "url" );
			return url.toLowerCase().startsWith( "http" ) || url.toLowerCase().startsWith( "//" ) ? url : getRequest().getFullDomain() + ZStrings.trimAll( url, '/' );
		}
		else
			throw new SiteConfigurationException( "The route with id [" + id + "] has no 'pattern' nor 'url' directive, we can not produce a route url without either one." );
	}

	public String url_id( String id ) throws SiteConfigurationException
	{
		return url_id( id, getRequest().isSecure() );
	}

	public String url_id( String id, boolean ssl ) throws SiteConfigurationException
	{
		return url_id( id, ssl ? "https://" : "http://" );
	}

	public String url_id( String id, String prefix ) throws SiteConfigurationException
	{
		Optional<DomainMapping> result = SiteManager.instance().getDomainMappingsById( id ).findFirst();
		if ( !result.isPresent() )
			throw new SiteConfigurationException( "Can't find a domain mapping with id [" + id + "] in site [" + getSite().getId() + "]" );
		return prefix + result.get().getFullDomain() + "/";
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
	 * @param subdomain The subdomain
	 * @return A valid formatted URI
	 */
	public String url_to( String subdomain, boolean secure )
	{
		return getRequest().getFullDomain( subdomain, secure );
	}

	/**
	 * Returns GET params as a String to appended after the full uri, e.g., {@code uri_to() + get_map()}
	 *
	 * @return
	 */
	public String get_map()
	{
		Map<String, String> getMap = getRequest().getGetMapRaw();
		return getMap.size() == 0 ? "" : "?" + getMap.entrySet().stream().map( e -> e.getKey() + "=" + e.getValue() ).collect( Collectors.joining( "&" ) );
	}

	/**
	 * Returns GET params as a String to be appended after the full uri, e.g., {@code uri_to() + get_append( "key", "value" )}
	 *
	 * @param map The additional get values
	 * @return The GET params as a string
	 */
	public String get_append( Map<String, Object> map )
	{
		ZObjects.notNull( map, "Map can not be null" );

		Map<String, String> getMap = new HashMap<>( getRequest().getGetMapRaw() );

		for ( Entry<String, Object> e : map.entrySet() )
			getMap.put( e.getKey(), ZObjects.castToString( e.getValue() ) );

		return getMap.size() == 0 ? "" : "?" + getMap.entrySet().stream().map( e -> e.getKey() + "=" + e.getValue() ).collect( Collectors.joining( "&" ) );
	}

	public String get_append( String key, Object val )
	{
		ZObjects.notNull( key );
		ZObjects.notNull( val );

		return get_append( ZMaps.newHashMap( key, val ) );
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

	public String uri_to( String subdomain, boolean ssl )
	{
		return getRequest().getFullUrl( subdomain, ssl );
	}

	public String uri_to( String subdomain, String prefix )
	{
		return getRequest().getFullUrl( subdomain, prefix );
	}
}
