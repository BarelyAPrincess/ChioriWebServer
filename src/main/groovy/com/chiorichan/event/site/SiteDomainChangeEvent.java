/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.event.site;

import com.chiorichan.event.AbstractEvent;
import com.chiorichan.site.DomainNode;
import com.chiorichan.site.Site;

/**
 * Called when a domain and/or subdomain is updated for a {@link Site}
 */
public class SiteDomainChangeEvent extends AbstractEvent
{
	public enum SiteDomainChangeEventType
	{
		ADD, REMOVE
	}

	private final SiteDomainChangeEventType type;
	private final Site site;
	private final DomainNode node;
	private final String domain;

	public SiteDomainChangeEvent( SiteDomainChangeEventType type, Site site, String domain, DomainNode mapping )
	{
		this.type = type;
		this.site = site;
		this.domain = domain;
		this.node = mapping;
	}

	public String getDomain()
	{
		return domain;
	}

	public Site getSite()
	{
		return site;
	}

	public DomainNode getSiteMapping()
	{
		return node;
	}

	public SiteDomainChangeEventType getType()
	{
		return type;
	}
}
