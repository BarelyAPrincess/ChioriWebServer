/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Rights Reserved
 */
package com.chiorichan.event.site;

import com.chiorichan.event.Cancellable;
import com.chiorichan.event.application.ApplicationEvent;
import com.chiorichan.site.Site;

public class SiteLoadEvent extends ApplicationEvent implements Cancellable
{
	Site site;
	boolean cancelled;
	
	public SiteLoadEvent( Site site )
	{
		this.site = site;
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
