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

public class DefaultDomainNode extends DomainNode
{
	protected DefaultDomainNode()
	{
		super( "" );
		site = SiteManager.instance().getDefaultSite();
	}

	@Override
	public DomainNode getChild( String domain, boolean create )
	{
		if ( create )
			throw new IllegalStateException( "Operation Not Permitted" );
		return null;
	}

	@Override
	protected DomainNode setSite( Site site, boolean override )
	{
		throw new IllegalStateException( "Operation Not Permitted" );
	}
}
