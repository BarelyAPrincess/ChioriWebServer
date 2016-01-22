package com.chiorichan.event.site;

import com.chiorichan.event.AbstractEvent;
import com.chiorichan.site.Site;
import com.chiorichan.site.SiteDomain;

/**
 * Called when a domain and/or subdomain is updated for a {@link Site}
 */
public class SiteDomainChangeEvent extends AbstractEvent
{
	public enum SiteDomainChangeEventType
	{
		ADD, REMOVE;
	}

	private final SiteDomainChangeEventType type;
	private final Site site;
	private final SiteDomain siteDomain;
	private final String domain;

	public SiteDomainChangeEvent( SiteDomainChangeEventType type, Site site, String domain, SiteDomain siteDomain )
	{
		this.type = type;
		this.site = site;
		this.domain = domain;
		this.siteDomain = siteDomain;
	}

	public String getDomain()
	{
		return domain;
	}

	public Site getSite()
	{
		return site;
	}

	public SiteDomain getSiteDomain()
	{
		return siteDomain;
	}

	public SiteDomainChangeEventType getType()
	{
		return type;
	}
}
