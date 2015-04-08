/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.event.server;

import com.chiorichan.event.Cancellable;
import com.chiorichan.framework.Site;

public class SiteLoadEvent extends ServerEvent implements Cancellable
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
