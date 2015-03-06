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
import com.chiorichan.factory.BindingProvider;
import com.chiorichan.framework.Site;
import com.chiorichan.http.Candy;
import com.chiorichan.http.HttpRequestWrapper;
import com.chiorichan.http.HttpResponseWrapper;

public interface SessionProvider extends BindingProvider
{
	Session getParentSession();
	
	void handleUserProtocols();
	
	void onFinished();
	
	void setGlobal( String key, Object val );
	
	Object getGlobal( String key );
	
	void setVariable( String key, String value );
	
	String getVariable( String key );
	
	HttpRequestWrapper getRequest();
	
	HttpResponseWrapper getResponse();
	
	Account getAccount();
	
	Candy getCandy( String key );
	
	boolean isStale();
	
	String getSessId();
	
	boolean isSet( String key );
	
	void setCookieExpiry( int valid );
	
	void destroy() throws SessionException;
	
	long getTimeout();
	
	void infiniTimeout();
	
	boolean getUserState();
	
	void logoutAccount();
	
	Site getSite();
	
	void saveSession( boolean force );

	void onNotify();
}
