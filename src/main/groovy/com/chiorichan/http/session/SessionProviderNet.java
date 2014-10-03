package com.chiorichan.http.session;

import java.sql.SQLException;

import com.chiorichan.account.bases.Account;
import com.chiorichan.account.bases.Sentient;
import com.chiorichan.account.bases.SentientHandler;
import com.chiorichan.factory.BindingProvider;
import com.chiorichan.factory.CodeEvalFactory;
import com.chiorichan.framework.Site;
import com.chiorichan.http.Candy;
import com.chiorichan.http.HttpRequest;
import com.chiorichan.http.HttpResponse;

public class SessionProviderNet implements SessionProvider, BindingProvider, SentientHandler
{
	@Override
	public boolean kick( String kickMessage )
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void sendMessage( String... msg )
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void attachSentient( Sentient sentient )
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeSentient()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isValid()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Sentient getSentient()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getIpAddr()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CodeEvalFactory getCodeFactory()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Session getParentSession()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void handleUserProtocols()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onFinished()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setGlobal( String key, Object val )
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public Object getGlobal( String key )
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpRequest getRequest()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpResponse getResponse()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Account getAccount()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Candy getCandy( String key )
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isStale()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getId()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isSet( String key )
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setCookieExpiry( int valid )
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void destroy() throws SQLException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public long getTimeout()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void infiniTimeout()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean getUserState()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void logoutAccount()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public Site getSite()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setVariable( String key, String value )
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getVariable( String key )
	{
		// TODO Auto-generated method stub
		return null;
	}
}
