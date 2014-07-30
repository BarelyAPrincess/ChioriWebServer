package com.chiorichan.bus.events.server;

import com.chiorichan.bus.bases.Cancellable;
import com.chiorichan.framework.Site;

public class SiteLoadEvent extends ServerEvent implements Cancellable
{
	Site site;
	boolean cancelled;
	
	public SiteLoadEvent(Site _site)
	{
		site = _site;
	}
	
	public Site getSite()
	{
		return site;
	}

	@Override
	public boolean isCancelled()
	{
		return cancelled;
	}

	@Override
	public void setCancelled( boolean cancel )
	{
		cancelled = cancel;
	}
}
