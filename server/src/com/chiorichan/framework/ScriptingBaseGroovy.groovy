package com.chiorichan.framework

import com.chiorichan.ConsoleLogManager
import com.chiorichan.Loader
import com.chiorichan.account.bases.Account
import com.chiorichan.database.DatabaseEngine
import com.chiorichan.http.HttpCode
import com.chiorichan.http.HttpRequest
import com.chiorichan.http.HttpResponse
import com.chiorichan.http.PersistenceManager
import com.chiorichan.http.PersistentSession
import com.chiorichan.plugin.PluginManager
import com.chiorichan.util.Versioning

abstract class ScriptingBaseGroovy extends ScriptingBaseJava
{
	void var_dump ( Object obj )
	{
		println var_export( obj )
	}
	
	HttpRequest getRequest()
	{
		return request;
	}
	
	HttpResponse getResponse()
	{
		return response;
	}
	
	PersistentSession getSession()
	{
		return request.getSession();
	}
	
	void echo( String var )
	{
		println var
	}
	
	String getVersion()
	{
		return Versioning.getVersion();
	}
	
	String getProduct()
	{
		return Versioning.getProduct();
	}
	
	String getCopyright()
	{
		return Versioning.getCopyright();
	}
	
	Account getAccount()
	{
		return request.getSession().getCurrentAccount();
	}
	
	ConfigurationManagerWrapper getConfigurationManager()
	{
		return new ConfigurationManagerWrapper( request.getSession() );
	}
	
	HttpUtilsWrapper getHttpUtils()
	{
		return new HttpUtilsWrapper( request.getSession() );
	}
	
	DatabaseEngine getServerDatabase()
	{
		return new DatabaseEngine( Loader.getPersistenceManager().getDatabase() );
	}
	
	DatabaseEngine getSiteDatabase()
	{
		return new DatabaseEngine( request.getSite().getDatabase() );
	}
	
	Site getSite()
	{
		return getRequest().getSite();
	}
	
	String getStatusDescription( int errNo )
	{
		return HttpCode.msg( errNo );
	}
	
	PersistenceManager getPersistenceManager()
	{
		return Loader.getPersistenceManager();
	}
}