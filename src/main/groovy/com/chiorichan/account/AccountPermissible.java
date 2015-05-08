/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.account;

import com.chiorichan.ConsoleColor;
import com.chiorichan.Loader;
import com.chiorichan.account.auth.AccountAuthenticator;
import com.chiorichan.account.auth.AccountCredentials;
import com.chiorichan.account.event.AccountFailedLoginEvent;
import com.chiorichan.account.event.AccountPreLoginEvent;
import com.chiorichan.account.event.AccountSuccessfulLoginEvent;
import com.chiorichan.account.lang.AccountException;
import com.chiorichan.account.lang.AccountResult;
import com.chiorichan.event.EventBus;
import com.chiorichan.permission.Permissible;
import com.chiorichan.session.SessionManager;
import com.chiorichan.util.CommonFunc;

/**
 * Used on classes that can support Account Logins, e.g., {@link Sessions}
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public abstract class AccountPermissible extends Permissible implements Account
{
	/**
	 * The logged in account associated with this session
	 */
	protected AccountInstance account = null;
	
	/**
	 * Attempts to authenticate the {@link AccountCredentials} onto the {@link AccountPermissible}
	 * 
	 * @param via
	 *            The {@link AccountPermissible}
	 * @param creds
	 *            The {@link AccountCredentials}
	 * @return The authenticated {@link AccountResult}
	 */
	public AccountResult login( AccountPermissible via, AccountCredentials creds )
	{
		AccountResult result;
		AccountInstance acct = null;
		
		try
		{
			acct = creds.authenticate();
			
			acct.metadata().getContext().creator().preLogin( acct.metadata(), via, creds );
			AccountPreLoginEvent event = new AccountPreLoginEvent( acct.metadata(), via, creds );
			
			EventBus.INSTANCE.callEvent( event );
			
			result = event.getAccountResult();
			
			if ( result == AccountResult.LOGIN_SUCCESS )
			{
				// TODO Single login per via method checks?
				if ( acct.countPermissibles() > 1 && Loader.getConfig().getBoolean( "accounts.singleLogin" ) )
					for ( AccountPermissible ap : acct.getPermissibles() )
						ap.kick( Loader.getConfig().getString( "accounts.singleLoginMessage", "You logged in from another location." ) );
				
				acct.metadata().set( "lastLoginTime", CommonFunc.getEpoch() );
				
				// acct.metadata().set( "lastLoginIp", getIpAddresses().toArray( new String[0] )[0] );
				
				acct.registerPermissible( via );
				
				acct.metadata().getContext().creator().successLogin( acct.metadata(), AccountResult.LOGIN_SUCCESS );
				EventBus.INSTANCE.callEvent( new AccountSuccessfulLoginEvent( acct.metadata() ) );
				
				setVariable( "acctId", acct.getAcctId() );
				account = acct;
				
				try
				{
					/**
					 * We try and get a relogin token, but not all authenticators support them.
					 */
					setVariable( "token", creds.getToken() );
				}
				catch ( AccountException e )
				{
					if ( e.getResult() != AccountResult.FEATURE_NOT_IMPLEMENTED )
						throw e;
				}
			}
			
			return result.setAccount( acct );
		}
		catch ( AccountException e )
		{
			if ( acct != null )
			{
				acct.metadata().getContext().creator().failedLogin( acct.metadata(), e.getResult() );
				EventBus.INSTANCE.callEvent( new AccountFailedLoginEvent( acct.metadata(), e.getResult() ) );
			}
			account = null;
			return e.getResult().setAccount( acct );
		}
		catch ( Throwable t )
		{
			return AccountResult.INTERNAL_ERROR.setAccount( acct ).setThrowable( t );
		}
	}
	
	/**
	 * Called from subclass once subclass is finished loading
	 */
	protected void initialized()
	{
		String acctId = getVariable( "acctId" );
		String token = getVariable( "token" );
		
		AccountResult result;
		try
		{
			result = login( this, AccountAuthenticator.TOKEN.credentials( acctId, token ) );
		}
		catch ( AccountException e )
		{
			result = e.getResult();
		}
		
		if ( AccountManager.INSTANCE.isDebug() )
			if ( result == AccountResult.LOGIN_SUCCESS )
			{
				Account acct = result.getAccount();
				SessionManager.getLogger().info( ConsoleColor.GREEN + "Restored Login: [id='" + acct.getAcctId() + "',siteId='" + acct.getSiteId() + "',displayName='" + acct.getHumanReadableName() + "',ipAddrs='" + acct.getIpAddresses() + "']" );
			}
			else
				SessionManager.getLogger().info( ConsoleColor.YELLOW + "Failed Login: [id='" + acctId + "',reason='" + result.getMessage( acctId ) + "']" );
	}
	
	public AccountResult logout()
	{
		if ( account != null )
			SessionManager.getLogger().info( ConsoleColor.GREEN + "Successful Logout: [id='" + account.getAcctId() + "',siteId='" + account.getSiteId() + "',displayName='" + account.getHumanReadableName() + "',ipAddrs='" + account.getIpAddresses() + "']" );
		
		setVariable( "acctId", null );
		setVariable( "token", null );
		account = null;
		
		return AccountResult.LOGOUT_SUCCESS;
	}
	
	protected abstract void successfulLogin();
	
	protected abstract void failedLogin( AccountResult result );
	
	public abstract void setVariable( String key, String value );
	
	public abstract String getVariable( String key );
	
	@Override
	public boolean kick( String msg )
	{
		return instance().kick( msg );
	}
	
	@Override
	public AccountMeta metadata()
	{
		return instance().metadata();
	}
	
	@Override
	public AccountInstance instance()
	{
		if ( account == null )
			throw new AccountException( AccountResult.ACCOUNT_NOT_INITIALIZED );
		return account;
	}
	
	@Override
	public String getAcctId()
	{
		return instance().getAcctId();
	}
	
	@Override
	public String getEntityId()
	{
		return instance().getAcctId();
	}
}
