/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.site;

import com.chiorichan.utils.UtilObjects;

import java.util.stream.Stream;

/**
 * Implements the builtin server site with site id 'default'
 */
public final class DefaultSite extends Site
{
	private DomainMapping defaultMapping;

	public DefaultSite( SiteManager mgr )
	{
		super( mgr, "default" );

		defaultMapping = new DefaultDomainMapping( this );
	}

	public DomainMapping getDefaultMapping()
	{
		return defaultMapping;
	}

	public Stream<DomainMapping> getMappings()
	{
		return Stream.concat( Stream.of( defaultMapping ), super.getMappings() );
	}

	@Override
	public Stream<DomainMapping> getMappings( String fullDomain )
	{
		if ( UtilObjects.isEmpty( fullDomain ) )
			return Stream.of( defaultMapping );
		return super.getMappings( fullDomain );
	}
}
