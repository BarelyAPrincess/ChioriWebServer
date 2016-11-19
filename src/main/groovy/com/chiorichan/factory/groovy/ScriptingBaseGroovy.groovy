/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2016 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.factory.groovy

import com.chiorichan.account.Account
import com.chiorichan.account.AccountManager
import com.chiorichan.factory.ScriptingFactory

import com.chiorichan.factory.api.Server
import com.chiorichan.factory.models.SQLQueryBuilder
import com.chiorichan.http.HttpCode
import com.chiorichan.http.HttpRequestWrapper
import com.chiorichan.http.HttpResponseWrapper
import com.chiorichan.logger.Log
import com.chiorichan.permission.PermissibleEntity
import com.chiorichan.permission.Permission
import com.chiorichan.permission.PermissionResult
import com.chiorichan.session.Session
import com.chiorichan.site.Site


/**
 * Used as the Groovy Scripting Base and provides scripts with custom builtin methods
 * */
@Deprecated
public abstract class ScriptingBaseGroovy extends ScriptingBaseJava
{
	/**
	 * Same as {@link Builtin#var_export(obj)} but instead prints the result to the buffer
	 * Based on method of same name in PHP
	 * @param obj
	 *       The object you wish to dump
	 */
	void var_dump( Object... obj )
	{
		println var_export( obj )
	}

	/**
	 * Returns the current HttpRequestWrapper instance
	 * XXX This is set inside the {@link HttpRequestWrapper#sessionStarted} and {@link SessionWrapper#startSession}, this needs looking over for other types
	 *
	 * @return current instance
	 */
	HttpRequestWrapper getRequest()
	{
		return getBindingProperty( "request" )
	}

	/**
	 * Returns the current HttpResponseWrapper instance
	 * XXX This is set inside the {@link HttpRequestWrapper#sessionStarted} and {@link SessionWrapper#startSession}, this needs looking over for other types
	 *
	 * @return current instance
	 */
	HttpResponseWrapper getResponse()
	{
		return getBindingProperty( "response" )
	}

	/**
	 * Return the current session for this request
	 *
	 * @return current session
	 */
	Session getSession()
	{
		return getRequest().getSession()
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

	Object getPropertySafe( String name )
	{
		try
		{
			return getProperty( name )
		}
		catch ( MissingPropertyException e )
		{
			return null
		}
	}

	Object getBindingProperty( String name )
	{
		try
		{
			return binding.getProperty( name )
		}
		catch ( MissingPropertyException e )
		{
			return null
		}
	}

	PermissibleEntity getEntity()
	{
		getAccount().getEntity()
	}

	/**
	 * Get the account matching specified uid
	 * @param uid
	 *       The uid you wish to use
	 * @return
	 * The found account, will return null if none found
	 */
	Account getAccount( String uid )
	{
		Account result = AccountManager.instance().getAccount( uid )

		if ( result == null )
		{
			result = AccountManager.instance().getAccountPartial( uid )
		}

		return result
	}

	Account[] getAccounts( String query )
	{
		return AccountManager.instance().getAccounts( query )
	}

	Account[] getAccounts( String query, int limit )
	{
		return AccountManager.instance().getAccounts( query, limit )
	}

	/**
	 * Returns the current logged in account
	 * @return
	 * The current account, will return null if no one is logged in
	 */
	Account getAccount()
	{
		return getRequest().getSession().account()
	}

	String getAcctId()
	{
		return isLoginPresent() ?: getAccount().getAcctId()
	}

	boolean isLoginPresent()
	{
		return getRequest().getSession().isLoginPresent()
	}

	@Deprecated
	boolean getAcctState()
	{
		return getRequest().getSession().isLoginPresent()
	}

	@Deprecated
	boolean getAccountState()
	{
		return getRequest().getSession().isLoginPresent()
	}

	/**
	 * Returns an instance of the current site
	 * @return
	 * The current site
	 */
	Site getSite()
	{
		return getRequest().getSite()
	}

	/**
	 * Return the unique Id for the current site
	 *
	 * @return The current site Id
	 */
	String getSiteId()
	{
		return getSite().getSiteId()
	}

	/**
	 * Converts the specified http status code to a message
	 * @param errNo
	 *       The http status code
	 * @return
	 * The http status message
	 */
	String getStatusDescription( int errNo )
	{
		return HttpCode.msg( errNo )
	}

	/**
	 * Returns the uri to the login page
	 * @return
	 * The login uri
	 */
	String url_to_login()
	{
		if ( getRequest().getSite() == null )
		{
			return "/login"
		}

		return getRequest().getSite().getYaml().getString( "scripts.login-form", "/login" )
	}

	/**
	 * Returns the to log current account out
	 * @return
	 * The logout uri
	 */
	String url_to_logout()
	{
		return url_to_login + "?logout"
	}

	Object include( String pack )
	{
		return Server.packageContext( pack ).eval()
	}

	Object require( String pack )
	{
		return Server.packageContext( pack ).require().eval()
	}

	SQLQueryBuilder model( String pack )
	{
		return Server.packageContext( pack ).model()
	}

	boolean isAdmin()
	{
		getSession().isAdmin()
	}

	boolean isOp()
	{
		getSession().isOp()
	}

	PermissionResult checkPermission( String perm )
	{
		getSession().checkPermission( perm )
	}

	PermissionResult checkPermission( Permission perm )
	{
		getSession().checkPermission( perm )
	}

	PermissionResult requirePermission( String perm )
	{
		getSession().requirePermission( perm )
	}

	PermissionResult requirePermission( Permission perm )
	{
		getSession().requirePermission( perm )
	}

	ScriptingFactory getEvalFactory()
	{
		return getRequest().getEvalFactory()
	}
}
