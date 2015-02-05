package com.chiorichan.http.session;

import java.util.Set;

import com.chiorichan.account.Account;
import com.chiorichan.account.AccountHandler;
import com.chiorichan.factory.BindingProvider;
import com.chiorichan.factory.CodeEvalFactory;
import com.chiorichan.framework.Site;
import com.chiorichan.http.Candy;
import com.chiorichan.http.HttpRequestWrapper;
import com.chiorichan.http.HttpResponseWrapper;
import com.chiorichan.permission.Permissible;
import com.chiorichan.permission.PermissibleType;

public class SessionProviderNet extends AccountHandler implements SessionProvider, BindingProvider
{
	@Override
	public boolean kick( String kickMessage )
	{
		return false;
	}
	
	@Override
	public void sendMessage( String... msg )
	{
		
	}
	
	@Override
	public void sendMessage( String string )
	{
		
	}
	
	@Override
	public boolean isValid()
	{
		return false;
	}
	
	@Override
	public String getIpAddr()
	{
		return null;
	}
	
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
	public String getId()
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

	@Override
	public PermissibleType getType()
	{
		return null;
	}
}
