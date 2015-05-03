/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.factory

import com.chiorichan.Loader
import com.chiorichan.account.Account
import com.chiorichan.database.DatabaseEngine
import com.chiorichan.framework.ConfigurationManagerWrapper
import com.chiorichan.framework.HttpUtilsWrapper
import com.chiorichan.http.HttpCode
import com.chiorichan.http.HttpRequestWrapper
import com.chiorichan.http.HttpResponseWrapper
import com.chiorichan.permission.Permission
import com.chiorichan.permission.PermissionResult
import com.chiorichan.session.SessionManager
import com.chiorichan.session.SessionProvider
import com.chiorichan.site.Site
import com.google.common.collect.Lists

/**
 * Used as the Groovy Scripting Base and provides scripts with custom builtin methods
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public abstract class ScriptingBaseGroovy extends ScriptingBaseJava
{
	List<ScriptTraceElement> getScriptTrace()
	{
		return getSession().getCodeFactory().getScriptTrace()
	}
	
	/**
	 * Holds history of included packages
	 * Used by include_once and require_once methods.
	 */
	private List<String> includedPackages = Lists.newArrayList()
	
	/**
	 * Same as @link ScriptingBaseJava:var_export(obj) but instead prints the result to the buffer
	 * Based on method of same name in PHP
	 * @param obj
	 *       The object you wish to dump
	 */
	void var_dump ( Object... obj )
	{
		println var_export( obj )
	}
	
	/**
	 * Returns the current HttpRequestWrapper instance
	 * @return
	 *      current instance
	 */
	HttpRequestWrapper getRequest()
	{
		return request
	}
	
	/**
	 * Returns the current HttpResponseWrapper instance
	 * @return
	 *      current instance
	 */
	HttpResponseWrapper getResponse()
	{
		return response
	}
	
	/**
	 * Return the current session for this request
	 * @return
	 *      current session
	 */
	SessionProvider getSession()
	{
		return request.getSession()
	}
	
	/**
	 * Alias for println
	 * Based on method of same name in PHP
	 * @param var
	 *       The string you wish to print
	 */
	void echo( String var )
	{
		println var
	}
	
	/**
	 * Get the account matching specified uid
	 * @param uid
	 *       The uid you wish to use
	 * @return
	 *       The found account, will return null if none found
	 */
	Account getAccount( String uid )
	{
		Account result =  Loader.getAccountManager().getAccount( uid )
		
		if ( result == null )
			result = Loader.getAccountManager().getAccountPartial( uid )
		
		return result
	}
	
	Account[] getAccounts( String query )
	{
		return Loader.getAccountManager().getAccounts( query )
	}
	
	Account[] getAccounts( String query, int limit )
	{
		return Loader.getAccountManager().getAccounts( query, limit )
	}
	
	/**
	 * Returns the current logged in account
	 * @return
	 *      The current account, will return null if no one is logged in
	 */
	Account getAccount()
	{
		return request.getSession().getAccount()
	}
	
	// XXX These two deprecated methods will soon be replaced with new static classes
	
	@Deprecated
	ConfigurationManagerWrapper getConfigurationManager()
	{
		return new ConfigurationManagerWrapper( request.getSession() )
	}
	
	@Deprecated
	HttpUtilsWrapper getHttpUtils()
	{
		return new HttpUtilsWrapper( request.getSession() )
	}
	
	/**
	 * Returns an instance of the server database
	 * @return
	 *       The server database engine
	 * @throws IllegalStateException if database is unconfigured
	 */
	DatabaseEngine getServerDatabase()
	{
		DatabaseEngine engine = Loader.getDatabase()
		
		if ( engine == null )
			throw new IllegalStateException( "The framework database is unconfigured. This will need to be setup in order for you to use the getServerDatabase() method." )
		
		return engine
	}
	
	/**
	 * Returns an instance of the current site database
	 * @return
	 *       The site database engine
	 * @throws IllegalStateException if database is unconfigured
	 */
	DatabaseEngine getSiteDatabase()
	{
		DatabaseEngine engine = getSite().getDatabase()
		
		if ( engine == null )
			throw new IllegalStateException( "The site database is unconfigured. This will need to be setup in order for you to use the getSiteDatabase() method." )
		
		return engine
	}
	
	/**
	 * Returns an instance of the current site
	 * @return
	 *       The current site
	 */
	Site getSite()
	{
		return getRequest().getSite()
	}
	
	/**
	 * Converts the specified http status code to a message
	 * @param errNo
	 *       The http status code
	 * @return
	 *       The http status message
	 */
	String getStatusDescription( int errNo )
	{
		return HttpCode.msg( errNo )
	}
	
	/**
	 * Returns the server session manager
	 * @return
	 *       Server session manager
	 */
	SessionManager getSessionManager()
	{
		return Loader.getSessionManager()
	}
	
	/**
	 * Same as @link url_to( null )
	 */
	String url_to()
	{
		return url_to( null )
	}
	
	/**
	 * Returns a valid http url address
	 * Used to produce absolute uri's within scripts
	 * ex: url_to( "css" ) + "stylesheet.css"
	 * @param subdomain
	 *       The subdomain
	 * @return
	 *       A valid formatted http uri
	 */
	String url_to( String subdomain )
	{
		String url = "http://"
		
		if ( subdomain != null && !subdomain.isEmpty() )
			url += subdomain + "."
		
		if ( request.getSite() != null )
			url += request.getSite().getDomain() + "/"
		else
			url += Loader.getSiteManager().getFrameworkSite().getDomain() + "/"
		
		return url
	}
	
	/**
	 * Returns the uri to the login page
	 * @return
	 *       The login uri
	 */
	String url_to_login()
	{
		if ( request.getSite() == null )
			return "/login"
		
		return request.getSite().getYaml().getString( "scripts.login-form", "/login" )
	}
	
	/**
	 * Returns the to log current account out
	 * @return
	 *       The logout uri
	 */
	String url_to_logout()
	{
		return url_to_login + "?logout"
	}
	
	/**
	 * Used to execute package file within script
	 * @param pack
	 *       The package, e.g, com.chiorichan.widgets.sidemenu
	 * @return
	 *       The object returned from the EvalFactory
	 */
	Object include( String pack )
	{
		EvalFactoryResult result = getHttpUtils().evalPackage( pack )
		print result.getString()
		return result.getObject()
	}
	
	/**
	 * Similar to @link include( pack ) but will only include once.
	 * Nothing happens if included more than once.
	 * @param pack
	 *       The package, e.g, com.chiorichan.widgets.sidemenu
	 * @return
	 *       The object returned from the EvalFactory
	 */
	Object include_once( String pack )
	{
		if ( !includedPackages.contains( pack ) )
		{
			includedPackages.add( pack )
			EvalFactoryResult result = getHttpUtils().evalPackage( pack )
			print result.getString()
			return result.getObject()
		}
	}
	
	/**
	 * Used to execute package files within script.
	 * Will throw an exception if there is a problem.
	 * @param pack
	 *       The package, e.g, com.chiorichan.widgets.sidemenu
	 * @return
	 *       The object returned from the EvalFactory
	 * @throws IOException if file not found
	 * @throws ShellExecuteException if there was a EvalFactory exception.
	 */
	Object require( String pack )
	{
		EvalFactoryResult result = getHttpUtils().evalPackageWithException( pack )
		print result.getString()
		return result.getObject()
	}
	
	/**
	 * Similar to @link require( pack ) but will only require once.
	 * Nothing happens if included more than once.
	 * @param pack
	 *       The package, e.g, com.chiorichan.widgets.sidemenu
	 * @return
	 *       The object returned from the EvalFactory
	 * @throws IOException if file not found
	 * @throws ShellExecuteException if there was a EvalFactory exception.
	 */
	Object require_once( String pack )
	{
		if ( !includedPackages.contains( pack ) )
		{
			includedPackages.add( pack )
			EvalFactoryResult result = getHttpUtils().evalPackageWithException( pack )
			print result.getString()
			return result.getObject()
		}
	}
	
	PermissionResult requirePermission( String perm )
	{
		getSession().getParentSession().requirePermission( perm )
	}
	
	PermissionResult requirePermission( Permission perm )
	{
		getSession().getParentSession().requirePermission( perm )
	}
}
