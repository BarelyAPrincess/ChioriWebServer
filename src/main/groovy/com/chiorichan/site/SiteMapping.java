/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2016 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.site;

import io.netty.handler.ssl.SslContext;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import com.chiorichan.util.Namespace;
import com.chiorichan.util.NetworkFunc;
import com.chiorichan.util.Pair;
import com.google.common.collect.Maps;

public class SiteMapping
{
	static class DomainNode
	{
		private final Map<String, DomainNode> children = Maps.newHashMap();
		private SiteMapping mapping = null;

		public SiteMapping getMapping()
		{
			return mapping;
		}

		/**
		 * Attempts to find a node by the provided name
		 */
		public DomainNode getNode( String name )
		{
			if ( !children.containsKey( name ) )
				children.put( name, new DomainNode() );
			return children.get( name );
		}

		public void setSite( Site site )
		{
			this.mapping = new SiteMapping( site );
		}
	}

	private static final DomainNode root = new DomainNode();

	public static Pair<String, SiteMapping> get( String domain )
	{
		Validate.notNull( domain );

		if ( NetworkFunc.isValidIPv4( domain ) )
			throw new IllegalArgumentException( "Can't match domain by IPv4 address" );
		if ( NetworkFunc.isValidIPv6( domain ) )
			throw new IllegalArgumentException( "Can't match domain by IPv6 address" );

		Namespace ns = new Namespace( domain.toLowerCase() );
		ns = ns.reverseOrder();

		DomainNode currentNode = root;
		String lastDomain = null;
		SiteMapping lastSite = null;

		for ( int i = 0; i < ns.getNodeCount(); i++ )
		{
			currentNode = currentNode.getNode( ns.getNode( i ) );
			if ( currentNode.getMapping() != null )
			{
				lastSite = currentNode.getMapping();
				lastDomain = ns.subNamespace( 0, i + 1 ).reverseOrder().getNamespace();
			}
		}

		if ( lastDomain == null || lastSite == null )
			return null;

		return new Pair<String, SiteMapping>( lastDomain, lastSite );
	}

	public static void put( String domain, Site site )
	{
		Validate.notEmpty( domain );
		Validate.notNull( site );

		Namespace ns = new Namespace( domain.toLowerCase() );
		ns.reverseOrder();

		DomainNode currentNode = root;
		for ( String node : ns.getNodes() )
			currentNode = currentNode.getNode( node );

		if ( currentNode.getMapping() != null )
			throw new IllegalStateException( String.format( "Can't override the site '%s' with site '%s' for domain '%s', each site must have a unique domain, subdomains included.", currentNode.getMapping().getSite().getId(), site.getId(), domain ) );

		currentNode.setSite( site );
	}

	private final Site site;

	public SiteMapping( Site site )
	{
		this.site = site;
	}

	public SslContext getDefaultSslContext()
	{
		return site.getDefaultSslContext();
	}

	public Site getSite()
	{
		return site;
	}

	public SslContext getSslContext( String subdomain )
	{
		Validate.notNull( subdomain );

		subdomain = subdomain.toLowerCase();

		if ( "*".equals( subdomain ) )
			return getDefaultSslContext();

		return site.getSslContext( subdomain );
	}

	public Map<String, Map<String, SslContext>> getSubdomains()
	{
		Map<String, Map<String, SslContext>> results = Maps.newHashMap();
		for ( Entry<String, Set<String>> e : site.getDomains().entrySet() )
			results.put( e.getKey(), getSubdomains( e.getKey() ) );
		return results;
	}

	public Map<String, SslContext> getSubdomains( String domain )
	{
		Validate.notNull( domain );
		Map<String, SslContext> sub = Maps.newHashMap();
		if ( !site.getDomains().containsKey( domain ) )
			return sub;
		for ( String subdomain : site.getDomains().get( domain ) )
			sub.put( subdomain, site.getSslContext( domain, subdomain ) );
		return sub;
	}
}
