package com.chiorichan.http.session;

import java.sql.SQLException;

import com.chiorichan.account.bases.Account;
import com.chiorichan.factory.BindingProvider;
import com.chiorichan.framework.Site;
import com.chiorichan.http.Candy;
import com.chiorichan.http.HttpRequest;
import com.chiorichan.http.HttpResponse;

public interface SessionProvider extends BindingProvider
{
	public Session getParentSession();
	public void handleUserProtocols();
	public void onFinished();
	public void setGlobal( String key, Object val );
	public Object getGlobal( String key );
	public void setVariable( String key, String value );
	public String getVariable( String key );
	public HttpRequest getRequest();
	public HttpResponse getResponse();
	public Account getAccount();
	Candy getCandy( String key );
	public boolean isStale();
	public String getId();
	public boolean isSet( String key );
	public void setCookieExpiry( int valid );
	public void destroy() throws SQLException;
	public long getTimeout();
	public void infiniTimeout();
	public boolean getUserState();
	public void logoutAccount();
	public Site getSite();
}