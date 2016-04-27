/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2016 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.site;

import java.io.File;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import com.chiorichan.configuration.ConfigurationSection;
import com.chiorichan.event.EventBus;
import com.chiorichan.event.site.SiteDomainChangeEvent;
import com.chiorichan.event.site.SiteDomainChangeEvent.SiteDomainChangeEventType;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

public class SiteDomain
{
	private final Site site;
	private final String subdomain;

	SiteDomain( Site site, String subdomain )
	{
		Validate.notNull( site );

		if ( subdomain == null || subdomain.length() == 0 )
			subdomain = "root";

		subdomain = subdomain.toLowerCase();

		this.site = site;
		this.subdomain = subdomain;
	}

	public File directory()
	{
		return new File( site.directoryPublic(), subdomain );
	}

	public File directory( String subdir )
	{
		return new File( directory(), subdir );
	}

	public boolean exists()
	{
		return site.domains.containsKey( subdomain ) || "root".equals( subdomain );
	}

	public ConfigurationSection getConfig( String domain )
	{
		if ( isMaped( domain ) )
			if ( site.yaml.isConfigurationSection( "site.domains." + domain.replace( ".", "_" ) + "." + subdomain.replace( ".", "_" ) ) )
				return site.yaml.getConfigurationSection( "site.domains." + domain.replace( ".", "_" ) + "." + subdomain.replace( ".", "_" ) );
			else
				return site.yaml.createSection( "site.domains." + domain.replace( ".", "_" ) + "." + subdomain.replace( ".", "_" ) );
		return null;
	}

	public Set<String> getMapped()
	{
		return new HashSet<String>()
		{
			{
				for ( Entry<String, Set<String>> e : site.domains.entrySet() )
					for ( String s : e.getValue() )
						if ( s.equalsIgnoreCase( subdomain ) )
						{
							add( e.getKey() );
							break;
						}
			}
		};
	}

	public String getSubdomain()
	{
		return subdomain;
	}

	public boolean isMaped()
	{
		for ( Set<String> subdomains : site.domains.values() )
			if ( subdomains.contains( subdomain ) )
				return true;
		return false;
	}

	public boolean isMaped( String domain )
	{
		Set<String> subdomains = site.domains.get( domain );
		if ( subdomains != null )
			if ( "root".equalsIgnoreCase( subdomain ) )
				return true;
			else if ( subdomains.contains( subdomain ) )
				return true;
		return false;
	}

	public void mapAll()
	{
		for ( String domain : site.domains.keySet() )
			mapDomain( domain );
	}

	public void mapDomain( String domain )
	{
		Validate.notEmpty( domain );
		domain = domain.toLowerCase();

		if ( "root".equalsIgnoreCase( subdomain ) )
		{
			if ( !site.domains.containsKey( domain ) )
				site.domains.put( domain, Sets.newHashSet() );
			site.yaml.createSection( "site.domains." + domain.replace( ".", "_" ) );
		}
		else
		{
			Set<String> subdomains = site.domains.get( domain );
			if ( subdomains == null )
			{
				subdomains = Sets.newHashSet();
				site.domains.put( domain, subdomains );
			}
			site.yaml.createSection( "site.domains." + domain.replace( ".", "_" ) + "." + subdomain.replace( ".", "_" ) );
			subdomains.add( subdomain );
		}

		EventBus.instance().callEvent( new SiteDomainChangeEvent( SiteDomainChangeEventType.ADD, site, domain, this ) );
	}

	public void unmapAll()
	{
		for ( String domain : site.domains.keySet() )
			unmapDomain( domain );
	}

	public void unmapDomain( String domain )
	{
		Validate.notEmpty( domain );
		domain = domain.toLowerCase();
		if ( "root".equalsIgnoreCase( subdomain ) )
		{
			Set<String> subdomains = site.domains.get( domain );
			if ( subdomains != null && subdomains.size() == 0 )
			{
				site.domains.remove( domain );
				site.yaml.set( "site.domains." + domain.replace( ".", "_" ), null );
			}
			else if ( subdomains != null )
				throw new IllegalStateException( "Can't unmap root domain from site while subdomains are maped, remove these first: " + Joiner.on( "," ).join( subdomains ) );
		}
		else
		{
			Set<String> subdomains = site.domains.get( domain );
			if ( subdomains != null )
				subdomains.remove( subdomain );
			if ( site.yaml.has( "site.domains." + domain.replace( ".", "_" ) ) )
				site.yaml.set( "site.domains." + domain.replace( ".", "_" ) + "." + subdomain.replace( ".", "_" ), null );
		}

		EventBus.instance().callEvent( new SiteDomainChangeEvent( SiteDomainChangeEventType.REMOVE, site, domain, this ) );
	}
}
