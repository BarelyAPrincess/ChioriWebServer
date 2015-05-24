/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.net;

import java.util.Set;

import com.chiorichan.account.Account;
import com.chiorichan.account.AccountManager;
import com.chiorichan.account.AccountType;
import com.chiorichan.account.auth.AccountAuthenticator;
import com.chiorichan.account.lang.AccountException;
import com.chiorichan.account.lang.AccountResult;
import com.chiorichan.http.HttpCookie;
import com.chiorichan.net.query.QueryServerHandler;
import com.chiorichan.session.SessionException;
import com.chiorichan.session.SessionWrapper;
import com.chiorichan.site.Site;
import com.chiorichan.site.SiteManager;
import com.chiorichan.util.ObjectFunc;
import com.google.common.collect.Sets;

/**
 * This class is used to make a connection between a TCP connection and it's Permissible.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class NetworkWrapper extends SessionWrapper
{
	protected QueryServerHandler handler;
	
	public NetworkWrapper( QueryServerHandler handler )
	{
		this.handler = handler;
		try
		{
			startSession();
		}
		catch ( SessionException e )
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public String getIpAddr()
	{
		return handler.getIpAddr();
	}
	
	@Override
	public String toString()
	{
		return "NetworkPersistence{ipAddr=" + getIpAddr() + "}";
	}
	
	@Override
	public HttpCookie getCookie( String key )
	{
		return null;
	}
	
	@Override
	public Set<HttpCookie> getCookies()
	{
		return Sets.newHashSet();
	}
	
	@Override
	protected void sessionStarted()
	{
		try
		{
			throw new AccountException( getSession().login( AccountAuthenticator.NULL, AccountType.ACCOUNT_NONE.getAcctId() ) );
		}
		catch ( AccountException e )
		{
			if ( e.getResult() != AccountResult.LOGIN_SUCCESS )
			{
				if ( e.getResult() == AccountResult.INTERNAL_ERROR )
					e.getResult().getThrowable().printStackTrace();
				AccountManager.getLogger().severe( e.getMessage() );
			}
			else
				send( e.getResult().getMessage() );
		}
	}
	
	@Override
	protected Site getSite()
	{
		/*
		 * The NetworkWrapper dosn't really tie down to any one site, so we just use the default one
		 */
		return SiteManager.INSTANCE.getDefaultSite();
	}
	
	@Override
	protected void finish0()
	{
		handler.disconnect();
	}
	
	@Override
	public void send( Object obj )
	{
		handler.println( ObjectFunc.castToString( obj ) );
	}
	
	@Override
	public void send( Account sender, Object obj )
	{
		handler.println( "Message from " + sender.getAcctId() + ": " + ObjectFunc.castToString( obj ) );
	}
	
	@Override
	protected HttpCookie getServerCookie( String key )
	{
		return null; // Do Nothing
	}
	
	/*
	 * @Override
	 * public boolean kick( String kickMessage )
	 * {
	 * handler.disconnect( kickMessage );
	 * return true;
	 * }
	 */
}
