/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.session;

import com.chiorichan.account.Account;
import com.chiorichan.factory.CodeEvalFactory;
import com.chiorichan.framework.Site;
import com.chiorichan.http.Candy;
import com.chiorichan.http.HttpRequestWrapper;
import com.chiorichan.http.HttpResponseWrapper;

public class SessionProviderNet implements SessionProvider
{
	@Override
	public CodeEvalFactory getCodeFactory()
	{
		return null;
	}
	
	@Override
	public Session getParentSession()
	{
		return null;
	}
	
	@Override
	public void handleUserProtocols()
	{
		
	}
	
	@Override
	public void onFinished()
	{
		
	}
	
	@Override
	public void setGlobal( String key, Object val )
	{
		
	}
	
	@Override
	public Object getGlobal( String key )
	{
		return null;
	}
	
	@Override
	public HttpRequestWrapper getRequest()
	{
		return null;
	}
	
	@Override
	public HttpResponseWrapper getResponse()
	{
		return null;
	}
	
	@Override
	public Account getAccount()
	{
		return null;
	}
	
	@Override
	public Candy getCandy( String key )
	{
		return null;
	}
	
	@Override
	public boolean isStale()
	{
		return false;
	}
	
	@Override
	public String getSessId()
	{
		return null;
	}
	
	@Override
	public boolean isSet( String key )
	{
		return false;
	}
	
	@Override
	public void setCookieExpiry( int valid )
	{
		
	}
	
	@Override
	public void destroy() throws SessionException
	{
		
	}
	
	@Override
	public long getTimeout()
	{
		return 0;
	}
	
	@Override
	public void infiniTimeout()
	{
		
	}
	
	@Override
	public boolean getUserState()
	{
		return false;
	}
	
	@Override
	public void logoutAccount()
	{
		
	}
	
	@Override
	public Site getSite()
	{
		return null;
	}
	
	@Override
	public void setVariable( String key, String value )
	{
		
	}
	
	@Override
	public String getVariable( String key )
	{
		return null;
	}
	
	@Override
	public void saveSession( boolean force )
	{
		
	}
}
