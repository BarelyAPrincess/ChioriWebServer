/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.account.bases;

import java.util.Set;

import com.chiorichan.Loader;
import com.chiorichan.framework.Site;
import com.chiorichan.http.session.Session;
import com.chiorichan.permissions.Permissible;
import com.google.common.collect.Sets;

public abstract class Sentient extends Permissible
{
	protected Set<SentientHandler> handlers = Sets.newCopyOnWriteArraySet();
	
	public void sendMessage( String... msgs )
	{
		for ( String msg : msgs )
			sendMessage( msg );
	}
	
	public void sendMessage( String string )
	{
		for ( SentientHandler h : handlers )
		{
			h.sendMessage( string );
		}
	}
	
	public abstract String getName();
	
	public String getAddress()
	{
		for ( SentientHandler h : handlers )
			if ( h.getIpAddr() != null || !h.getIpAddr().isEmpty() )
				return ( h.getIpAddr() );
		
		return null;
	}
	
	public Site getSite()
	{
		Site site = Loader.getSiteManager().getFrameworkSite();
		
		for ( SentientHandler h : handlers )
			if ( h instanceof Session )
				site = ( (Session) h ).getSite();
		
		return site;
	}
	
	public void putHandler( SentientHandler handler )
	{
		if ( !handlers.contains( handler ) )
			handlers.add( handler );
	}
	
	public void removeHandler( SentientHandler handler )
	{
		checkHandlers();
		handlers.remove( handler );
	}
	
	public void clearHandlers()
	{
		checkHandlers();
		handlers.clear();
	}
	
	public boolean hasHandler()
	{
		checkHandlers();
		return !handlers.isEmpty();
	}
	
	public int countHandlers()
	{
		checkHandlers();
		return handlers.size();
	}
	
	public Set<SentientHandler> getHandlers()
	{
		checkHandlers();
		return handlers;
	}
	
	private void checkHandlers()
	{
		for ( SentientHandler h : handlers )
			if ( !h.isValid() )
				handlers.remove( h );
	}
}
