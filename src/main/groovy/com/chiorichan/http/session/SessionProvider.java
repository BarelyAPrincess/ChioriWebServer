/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.http.session;

import com.chiorichan.account.Account;
import com.chiorichan.factory.BindingProvider;
import com.chiorichan.framework.Site;
import com.chiorichan.http.Candy;
import com.chiorichan.http.HttpRequestWrapper;
import com.chiorichan.http.HttpResponseWrapper;

public interface SessionProvider extends BindingProvider
{
	public Session getParentSession();
	
	public void handleUserProtocols();
	
	public void onFinished();
	
	public void setGlobal( String key, Object val );
	
	public Object getGlobal( String key );
	
	public void setVariable( String key, String value );
	
	public String getVariable( String key );
	
	public HttpRequestWrapper getRequest();
	
	public HttpResponseWrapper getResponse();
	
	public Account<?> getAccount();
	
	Candy getCandy( String key );
	
	public boolean isStale();
	
	public String getId();
	
	public boolean isSet( String key );
	
	public void setCookieExpiry( int valid );
	
	public void destroy() throws SessionException;
	
	public long getTimeout();
	
	public void infiniTimeout();
	
	public boolean getUserState();
	
	public void logoutAccount();
	
	public Site getSite();
	
	public void saveSession( boolean force );
}
