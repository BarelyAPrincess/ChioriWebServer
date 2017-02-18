/*
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
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
	 * Same as {@link com.chiorichan.factory.api.Builtin#var_export()} but instead prints the result to the buffer
	 * Based on method of same name in PHP
	 * @param obj The object you wish to dump
	 */
	void var_dump( Object... obj )
	{
		println var_export( obj )
	}

	/**
	 * Returns the current HttpRequestWrapper instance
	 * XXX This is set inside the {@link HttpRequestWrapper#sessionStarted} and {@link com.chiorichan.session.SessionWrapper#startSession}, this needs looking over for other types
	 *
	 * @return current instance
	 */
	HttpRequestWrapper getRequest()
	{
		return getBindingProperty( "request" ) as HttpRequestWrapper
	}

	/**
	 * Returns the current HttpResponseWrapper instance
	 * XXX This is set inside the {@link HttpRequestWrapper#sessionStarted} and {@link com.chiorichan.session.SessionWrapper#startSession}, this needs looking over for other types
	 *
	 * @return current instance
	 */
	HttpResponseWrapper getResponse()
	{
		return getBindingProperty( "response" ) as HttpResponseWrapper
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
	 *
	 * @param var The string you wish to print
	 */
	void echo( String var )
	{
		println var
	}

	/**
	 * Determine if a variable is set and is not NULL.
	 * <p>
	 * If a variable has been unset with unset(), it will no longer be set. isset() will return FALSE if testing a variable that has been set to NULL. Also note that a null character ("\0") is not equivalent to the PHP NULL constant.
	 * <p>
	 * If multiple parameters are supplied then isset() will return TRUE only if all of the parameters are set. Evaluation goes from left to right and stops as soon as an unset variable is encountered.
	 *
	 * @param names The variables to be checked
	 * @return Returns TRUE if var exists and has value other than NULL. FALSE otherwise.
	 */
	boolean isset( String... names )
	{
		for ( String name : names )
		{
			if ( getPropertySafe( name ) == null )
			{
				return false
			}
		}
		return true;
	}

	void unset( String name )
	{
		setProperty( name, null );
	}

	def last( Collection<?> collection )
	{
		return collection.last();
	}

	def first( Collection<?> collection )
	{
		return collection.first();
	}

	boolean hasProperty( String name )
	{
		return getPropertySafe( name ) != null;
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
	 * @param uid The uid you wish to use
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
		return AccountManager.instance().getAccounts( query ).stream().limit( limit ).toArray();
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
	 * @return The current site
	 */
	Site getSite()
	{
		return getRequest().getSite()
	}

	/**
	 * Return the unique Id for the current site
	 * @return The current site Id
	 */
	String getSiteId()
	{
		return getSite().getSiteId()
	}

	/**
	 * Converts the specified http status code to a message
	 * @param errNo The http status code
	 * @return The http status message
	 */
	String getStatusDescription( int errNo )
	{
		return HttpCode.msg( errNo )
	}

	/**
	 * Returns the uri to the login page
	 * @return The login uri
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
	 * @return The logout uri
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
		return Server.packageContextWithException( pack ).eval()
	}

	private int stackLevel = -1;

	void obStart()
	{
		stackLevel = getRequest().getEvalFactory().obStart();
	}

	void obFlush()
	{
		if ( stackLevel == -1 )
		{
			throw new IllegalStateException( "obStart() must be called first." );
		};
		getRequest().getEvalFactory().obFlush( stackLevel );
	}

	String obEnd()
	{
		if ( stackLevel == -1 )
		{
			throw new IllegalStateException( "obStart() must be called first." );
		};
		return getRequest().getEvalFactory().obEnd( stackLevel );
	}

	void section( String key )
	{
		getRequest().getEvalFactory().getYieldBuffer().set( key, obEnd() );
	}

	void section( String key, String value )
	{
		getRequest().getEvalFactory().getYieldBuffer().set( key, value );
	}

	String yield( String key )
	{
		return getRequest().getEvalFactory().getYieldBuffer().get( key );
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
