package com.chiorichan.event.server;

import com.chiorichan.event.Cancellable;
import com.chiorichan.framework.Site;
import com.chiorichan.user.builtin.UserLookupAdapter;

public class SiteLoadEvent extends ServerEvent implements Cancellable
{
	UserLookupAdapter userLookupAdapter = null;
	Site site;
	boolean cancelled;
	
	public SiteLoadEvent(Site _site)
	{
		site = _site;
	}

	public void setUserLookupAdapter( UserLookupAdapter adapter )
	{
		userLookupAdapter = adapter;
	}
	
	public UserLookupAdapter getUserLookupAdapter()
	{
		return userLookupAdapter;
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
