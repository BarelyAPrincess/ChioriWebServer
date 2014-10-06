/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.framework

import com.chiorichan.Loader
import com.chiorichan.account.bases.Account
import com.chiorichan.database.DatabaseEngine
import com.chiorichan.http.HttpCode
import com.chiorichan.http.HttpRequest
import com.chiorichan.http.HttpResponse
import com.chiorichan.http.session.SessionManager
import com.chiorichan.http.session.SessionProvider
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
	
	SessionProvider getSession()
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
		return request.getSession().getAccount();
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
		return Loader.getDatabase();
	}
	
	DatabaseEngine getSiteDatabase()
	{
		return request.getSite().getDatabase();
	}
	
	Site getSite()
	{
		return getRequest().getSite();
	}
	
	String getStatusDescription( int errNo )
	{
		return HttpCode.msg( errNo );
	}
	
	SessionManager getSessionManager()
	{
		return Loader.getSessionManager();
	}
	
	String url_to()
	{
		return url_to( null );
	}
	
	String url_to( String subdomain )
	{
		String url = "http://";
		
		if ( subdomain != null && !subdomain.isEmpty() )
			url += subdomain + ".";
		
		if ( request.getSite() != null )
			url += request.getSite().getDomain() + "/";
		else
			url += Loader.getSiteManager().getFrameworkSite().getDomain() + "/";
		
		return url;
	}
	
	String url_to_login()
	{
		if ( request.getSite() == null )
			return "/login";
		
		return request.getSite().getYaml().getString( "scripts.login-form", "/login" );;
	}
	
	String url_to_logout()
	{
		return url_to_login + "?logout";
	}
}