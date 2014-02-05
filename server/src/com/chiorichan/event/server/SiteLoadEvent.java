package com.chiorichan.event.server;

import com.chiorichan.event.Cancellable;
import com.chiorichan.user.builtin.UserLookupAdapter;

public class SiteLoadEvent extends ServerEvent implements Cancellable
{
	UserLookupAdapter userLookupAdapter = null;
	boolean cancelled;
	
	public void setUserLookupAdapter( UserLookupAdapter adapter )
	{
		userLookupAdapter = adapter;
	}
	
	public UserLookupAdapter getUserLookupAdapter()
	{
		return userLookupAdapter;
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
