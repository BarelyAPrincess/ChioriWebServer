package com.chiorichan.account.bases;

import java.util.Set;

import com.chiorichan.Loader;
import com.chiorichan.framework.Site;
import com.chiorichan.http.PersistentSession;
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
			if ( h instanceof PersistentSession )
				site = ( (PersistentSession) h ).getSite();
		
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
