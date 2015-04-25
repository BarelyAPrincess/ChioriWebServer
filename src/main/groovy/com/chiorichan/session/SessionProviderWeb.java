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

import groovy.lang.Binding;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;

import com.chiorichan.ConsoleColor;
import com.chiorichan.Loader;
import com.chiorichan.account.Account;
import com.chiorichan.account.AccountManager;
import com.chiorichan.account.LoginException;
import com.chiorichan.factory.EvalFactory;
import com.chiorichan.framework.ConfigurationManagerWrapper;
import com.chiorichan.http.Candy;
import com.chiorichan.http.HttpCode;
import com.chiorichan.http.HttpRequestWrapper;
import com.chiorichan.http.HttpResponseWrapper;
import com.chiorichan.permission.Permission;
import com.chiorichan.permission.PermissionResult;
import com.chiorichan.site.Site;
import com.chiorichan.util.CommonFunc;
import com.google.common.collect.Sets;

@SuppressWarnings( "deprecation" )
public class SessionProviderWeb implements SessionProvider
{
	private static final List<String> disallowedKeys = Arrays.asList( new String[] {"user", "pass", "remember", "out", "request", "response", "_REQUEST", "__FILE__", "_SESSION", "_REWRITE", "_GET", "_POST", "_SERVER", "_FILES"} );
	
	protected Set<String> lastChangeHistory = Sets.newHashSet();
	protected final Binding binding = new Binding();
	protected EvalFactory factory = null;
	protected HttpRequestWrapper request;
	protected Session parentSession;
	protected int created = CommonFunc.getEpoch();
	
	protected SessionProviderWeb( HttpRequestWrapper request )
	{
		parentSession = SessionManager.createSession();
		parentSession.sessionProviders.add( this );
		setRequest( request, false );
	}
	
	protected SessionProviderWeb( Session session, HttpRequestWrapper request )
	{
		parentSession = session;
		parentSession.sessionProviders.add( this );
		for ( Entry<String, Object> e : session.bindingMap.entrySet() )
			binding.setVariable( e.getKey(), e.getValue() );
		
		binding.setVariable( "_SESSION", new LinkedHashMap<String, String>( session.data ) );
		lastChangeHistory = session.getChangeHistory();
		
		setRequest( request, true );
	}
	
	@Override
	public Session getParentSession()
	{
		return parentSession;
	}
	
	protected void setRequest( HttpRequestWrapper request, Boolean stale )
	{
		this.request = request;
		parentSession.stale = stale;
		
		parentSession.setSite( request.getSite() );
		parentSession.lastIpAddr = request.getRemoteAddr();
		
		Map<String, Candy> pulledCandies = SessionUtils.poleCandies( request );
		pulledCandies.putAll( parentSession.candies );
		parentSession.candies = pulledCandies;
		
		if ( request.getSite().getYaml() != null )
		{
			String candyName = request.getSite().getYaml().getString( "sessions.cookie-name", parentSession.candyName );
			
			if ( !candyName.equals( parentSession.candyName ) )
				if ( parentSession.candies.containsKey( parentSession.candyName ) )
				{
					parentSession.candies.put( candyName, parentSession.candies.get( parentSession.candyName ) );
					parentSession.candies.remove( parentSession.candyName );
					parentSession.candyName = candyName;
				}
				else
				{
					parentSession.candyName = candyName;
				}
		}
		
		parentSession.sessionCandy = parentSession.candies.get( parentSession.candyName );
		
		try
		{
			parentSession.initSession( request.getParentDomain() );
		}
		catch ( SessionException e )
		{
			e.printStackTrace();
		}
		
		if ( request != null )
		{
			binding.setVariable( "request", request );
			binding.setVariable( "response", request.getResponse() );
		}
		binding.setVariable( "__FILE__", new File( "" ) );
	}
	
	@Override
	public void handleUserProtocols()
	{
		if ( !parentSession.pendingMessages.isEmpty() )
		{
			// TODO Figure out a way that messages can be delivered to web users
			// request.getResponse().sendMessage( pendingMessages.toArray( new String[0] ) );
		}
		
		if ( request == null )
		{
			SessionManager.getLogger().severe( "Request was empty for an unknown reason." );
			return;
		}
		
		String username = request.getArgument( "user" );
		String password = request.getArgument( "pass" );
		String remember = request.getArgumentBoolean( "remember" ) ? "true" : "false";
		String target = request.getArgument( "target" );
		
		if ( request.getArgument( "logout", "", true ) != null )
		{
			parentSession.logoutAccount();
			
			// TODO Make a server login page for sites without a login page
			if ( target.isEmpty() )
				target = request.getSite().getYaml().getString( "scripts.login-form", "/login" );
			
			request.getResponse().sendRedirect( target + "?ok=You have been successfully logged out." );
			return;
		}
		
		if ( !username.isEmpty() && !password.isEmpty() )
		{
			try
			{
				Account acct = Loader.getAccountManager().attemptLogin( parentSession, username, password );
				parentSession.currentAccount = acct;
				
				parentSession.setVariable( "user", acct.getAcctId() );
				parentSession.setVariable( "pass", DigestUtils.md5Hex( acct.getPassword() ) );
				
				String loginPost = ( target.isEmpty() ) ? request.getSite().getYaml().getString( "scripts.login-post", "" ) : target;
				
				parentSession.setVariable( "remember", remember );
				
				AccountManager.getLogger().info( ConsoleColor.GREEN + "Successful Login [acctId='" + username + "',hasPassword='" + ( password != null && !password.isEmpty() ) + "',displayName='" + acct.getDisplayName() + "']" );
				request.getResponse().sendRedirect( loginPost );
			}
			catch ( LoginException l )
			{
				String loginForm = request.getSite().getYaml().getString( "scripts.login-form", "/login" );
				
				if ( l.getAccount() != null )
					AccountManager.getLogger().warning( ConsoleColor.GREEN + "Failed Login [acctId='" + username + "',hasPassword='" + ( password != null && !password.isEmpty() ) + "',displayName='" + l.getAccount().getDisplayName() + "',reason='" + l.getMessage() + "']" );
				
				parentSession.currentAccount = null;
				parentSession.setVariable( "user", "" );
				parentSession.setVariable( "pass", "" );
				parentSession.setVariable( "remember", "false" );
				
				request.getResponse().sendRedirect( loginForm + "?ok=" + l.getMessage() + "&target=" + target );
			}
		}
		else if ( parentSession.currentAccount == null )
		{
			username = parentSession.getVariable( "user" );
			password = parentSession.getVariable( "pass" );
			
			if ( username != null && !username.isEmpty() && password != null && !password.isEmpty() )
			{
				try
				{
					Account acct = Loader.getAccountManager().attemptLogin( parentSession, username, password );
					parentSession.currentAccount = acct;
					
					AccountManager.getLogger().info( ConsoleColor.GREEN + "Successful Login [acctId='" + username + "',hasPassword='" + ( !password.isEmpty() ) + "',displayName='" + acct.getDisplayName() + "']" );
				}
				catch ( LoginException l )
				{
					AccountManager.getLogger().warning( ConsoleColor.RED + "Failed Login [acctId='" + username + "',hasPassword='" + ( !password.isEmpty() ) + "',reason='" + l.getMessage() + "']" );
				}
			}
			else
				parentSession.currentAccount = null;
		}
		else
		{
			/*
			 * XXX
			 * try
			 * {
			 * parentSession.currentAccount.reloadAndValidate(); // <- Is this being overly redundant?
			 * Loader.getLogger().info( ChatColor.GREEN + "Current Login `Username \"" + parentSession.currentAccount.getName() + "\", Password \"" + parentSession.currentAccount.getMetaData().getPassword() + "\", UserId \"" +
			 * parentSession.currentAccount.getAccountId() + "\", Display Name \"" + parentSession.currentAccount.getDisplayName() + "\"`" );
			 * }
			 * catch ( LoginException e )
			 * {
			 * parentSession.currentAccount = null;
			 * Loader.getLogger().warning( ChatColor.GREEN + "Login Failed `There was a login present but it failed validation with error: " + e.getMessage() + "`" );
			 * }
			 */
		}
		
		if ( parentSession.currentAccount != null )
			parentSession.currentAccount.putHandler( parentSession );
		
		if ( !parentSession.stale || Loader.getConfig().getBoolean( "sessions.rearmTimeoutWithEachRequest" ) )
			parentSession.rearmTimeout();
	}
	
	@Override
	public EvalFactory getEvalFactory()
	{
		return getEvalFactory( true );
	}
	
	@Override
	public EvalFactory getEvalFactory( boolean createIfNull )
	{
		if ( factory == null && createIfNull )
			factory = EvalFactory.create( binding );
		
		return factory;
	}
	
	@Override
	public void setGlobal( String key, Object val )
	{
		binding.setVariable( key, val );
	}
	
	@Override
	public Object getGlobal( String key )
	{
		return binding.getVariable( key );
	}
	
	public void setVariable( String key, String value )
	{
		parentSession.setVariable( key, value );
	}
	
	public String getVariable( String key )
	{
		return parentSession.getVariable( key );
	}
	
	protected Binding getBinding()
	{
		return binding;
	}
	
	@Override
	public HttpRequestWrapper getRequest()
	{
		return request;
	}
	
	@Override
	public HttpResponseWrapper getResponse()
	{
		return request.getResponse();
	}
	
	public void requireLogin() throws IOException
	{
		requireLogin( null );
	}
	
	/**
	 * First checks in an account is present, sends to login page if not.
	 * Second checks if the present accounts has the specified permission.
	 * 
	 * @param permission
	 * @throws IOException
	 */
	public void requireLogin( String permission ) throws IOException
	{
		if ( parentSession.currentAccount == null )
			request.getResponse().sendLoginPage();
		
		if ( permission != null )
			if ( !parentSession.checkPermission( permission ).isTrue() )
				request.getResponse().sendError( HttpCode.HTTP_FORBIDDEN, "You must have the permission `" + permission + "` in order to view this page!" );
	}
	
	public ConfigurationManagerWrapper getConfigurationManager()
	{
		return new ConfigurationManagerWrapper( this );
	}
	
	@SuppressWarnings( "unchecked" )
	@Override
	public void onFinished()
	{
		request = null;
		Map<String, Object> bindingMap = parentSession.bindingMap;
		Map<String, Object> variables = binding.getVariables();
		
		if ( bindingMap != null && variables != null )
		{
			// Copy all keys besides those in disallowedKeys list into the parent binding map
			for ( Entry<String, Object> e : variables.entrySet() )
				if ( !disallowedKeys.contains( e.getKey() ) )
					bindingMap.put( e.getKey(), e.getValue() );
			
			// Merge our _SESSION into parentSession's data map
			if ( variables.containsKey( "_SESSION" ) && variables.get( "_SESSION" ) instanceof Map )
			{
				synchronized ( parentSession )
				{
					Set<String> changeHistory = parentSession.getChangeHistory();
					
					/*
					 * Update only if values don't match and key was not updated since we copied the data
					 */
					for ( Entry<String, String> e : ( ( Map<String, String> ) variables.get( "_SESSION" ) ).entrySet() )
						if ( parentSession.data.get( e.getKey() ) != e.getValue() ) // We have a value different from parent session
							if ( ! ( changeHistory.contains( e.getKey() ) && !lastChangeHistory.contains( e.getKey() ) ) )
								if ( !disallowedKeys.contains( e.getKey() ) )
									parentSession.setVariable( e.getKey(), e.getValue() );
				}
			}
		}
		
		parentSession.sessionProviders.remove( this );
	}
	
	@Override
	public Account getAccount()
	{
		return parentSession.getAccount();
	}
	
	@Override
	public String toString()
	{
		return parentSession.toString();
	}
	
	@Override
	public Candy getCandy( String key )
	{
		return parentSession.getCandy( key );
	}
	
	@Override
	public boolean isStale()
	{
		return parentSession.isStale();
	}
	
	@Override
	public String getSessId()
	{
		return parentSession.getSessId();
	}
	
	@Override
	public boolean isSet( String key )
	{
		return parentSession.isSet( key );
	}
	
	@Override
	public void setCookieExpiry( int valid )
	{
		parentSession.setCookieExpiry( valid );
	}
	
	@Override
	public void destroy() throws SessionException
	{
		parentSession.destroy();
	}
	
	@Override
	public long getTimeout()
	{
		return parentSession.getTimeout();
	}
	
	@Override
	public void infiniTimeout()
	{
		parentSession.infiniTimeout();
	}
	
	@Override
	public boolean getUserState()
	{
		return parentSession.getUserState();
	}
	
	@Override
	public void logoutAccount()
	{
		parentSession.logoutAccount();
	}
	
	@Override
	public Site getSite()
	{
		return parentSession.getSite();
	}
	
	@Override
	public void saveSession( boolean force )
	{
		parentSession.saveSession( force );
	}
	
	// TODO: Future add of setDomain, setCookieName, setSecure (http verses https)
	
	public final boolean isBanned()
	{
		return parentSession.isBanned();
	}
	
	public final boolean isWhitelisted()
	{
		return parentSession.isWhitelisted();
	}
	
	public final boolean isAdmin()
	{
		return parentSession.isAdmin();
	}
	
	public final boolean isOp()
	{
		return parentSession.isOp();
	}
	
	public final PermissionResult checkPermission( String perm )
	{
		return parentSession.checkPermission( perm );
	}
	
	public final PermissionResult checkPermission( Permission perm )
	{
		return parentSession.checkPermission( perm );
	}
	
	@Override
	public void onNotify()
	{
		// Do Nothing
	}
	
	@Override
	public void sendMessage( String... msgs )
	{
		// Do Nothing
	}
}
