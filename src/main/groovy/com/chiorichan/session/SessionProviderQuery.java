/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.session;

import groovy.lang.Binding;

import com.chiorichan.ConsoleColor;
import com.chiorichan.account.Account;
import com.chiorichan.factory.CodeEvalFactory;
import com.chiorichan.framework.Site;
import com.chiorichan.http.Candy;
import com.chiorichan.http.HttpRequestWrapper;
import com.chiorichan.http.HttpResponseWrapper;
import com.chiorichan.net.query.QueryServerHandler;
import com.chiorichan.permission.Permission;
import com.chiorichan.permission.PermissionResult;
import com.chiorichan.util.Common;

/**
 * Session Provider for Query Connections
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class SessionProviderQuery implements SessionProvider
{
	protected final Binding binding = new Binding();
	protected CodeEvalFactory factory = null;
	protected Session parentSession;
	protected QueryServerHandler handler;
	protected int created = Common.getEpoch();
	
	public SessionProviderQuery( QueryServerHandler handler )
	{
		parentSession = SessionManager.createSession();
		parentSession.sessionProviders.add( this );
		
		this.handler = handler;
	}
	
	@Override
	public CodeEvalFactory getCodeFactory()
	{
		return factory;
	}
	
	@Override
	public Session getParentSession()
	{
		return parentSession;
	}
	
	@Override
	public void handleUserProtocols()
	{
		if ( !parentSession.pendingMessages.isEmpty() )
		{
			handler.println( parentSession.pendingMessages.toArray( new String[0] ) );
		}
	}
	
	@Override
	public void onFinished()
	{
		
	}
	
	@Override
	public void setGlobal( String key, Object val )
	{
		binding.setVariable( key, val );
	}
	
	@Override
	public Object getGlobal( String key )
	{
		return binding.getVariable( key );
	}
	
	@Override
	public void setVariable( String key, String value )
	{
		parentSession.setVariable( key, value );
	}
	
	@Override
	public String getVariable( String key )
	{
		return parentSession.getVariable( key );
	}
	
	protected Binding getBinding()
	{
		return binding;
	}
	
	@Override
	public HttpRequestWrapper getRequest()
	{
		return null; // XXX Not implemented
	}
	
	@Override
	public HttpResponseWrapper getResponse()
	{
		return null; // XXX Not implemented
	}
	
	@Override
	public Account getAccount()
	{
		return parentSession.getAccount();
	}
	
	@Override
	public String toString()
	{
		return parentSession.toString();
	}
	
	@Override
	public Candy getCandy( String key )
	{
		return parentSession.getCandy( key );
	}
	
	@Override
	public boolean isStale()
	{
		return parentSession.isStale();
	}
	
	@Override
	public String getSessId()
	{
		return parentSession.getSessId();
	}
	
	@Override
	public boolean isSet( String key )
	{
		return parentSession.isSet( key );
	}
	
	@Override
	public void setCookieExpiry( int valid )
	{
		parentSession.setCookieExpiry( valid );
	}
	
	@Override
	public void destroy() throws SessionException
	{
		parentSession.destroy();
	}
	
	@Override
	public long getTimeout()
	{
		return parentSession.getTimeout();
	}
	
	@Override
	public void infiniTimeout()
	{
		parentSession.infiniTimeout();
	}
	
	@Override
	public boolean getUserState()
	{
		return parentSession.getUserState();
	}
	
	@Override
	public void logoutAccount()
	{
		parentSession.logoutAccount();
	}
	
	@Override
	public Site getSite()
	{
		return parentSession.getSite();
	}
	
	@Override
	public void saveSession( boolean force )
	{
		parentSession.saveSession( force );
	}
	
	public final boolean isBanned()
	{
		return parentSession.isBanned();
	}
	
	public final boolean isWhitelisted()
	{
		return parentSession.isWhitelisted();
	}
	
	public final boolean isAdmin()
	{
		return parentSession.isAdmin();
	}
	
	public final boolean isOp()
	{
		return parentSession.isOp();
	}
	
	public final PermissionResult checkPermission( String perm )
	{
		return parentSession.checkPermission( perm );
	}
	
	public final PermissionResult checkPermission( Permission perm )
	{
		return parentSession.checkPermission( perm );
	}
}
