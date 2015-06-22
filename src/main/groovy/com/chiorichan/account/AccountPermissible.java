/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
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
import com.chiorichan.tasks.Timings;
import com.google.common.base.Joiner;

/**
 * Used on classes that can support Account Logins, e.g., {@link com.chiorichan.session.Session}
 */
public abstract class AccountPermissible extends Permissible implements Account
{
	/**
	 * The logged in account associated with this session
	 */
	protected AccountInstance account = null;
	
	/**
	 * Attempts to authenticate using saved Account Credentials
	 */
	public void login()
	{
		AccountResult result = AccountResult.DEFAULT;
		String authName = getVariable( "auth" );
		String acctId = getVariable( "acctId" );
		
		if ( authName != null && !authName.isEmpty() )
		{
			AccountAuthenticator auth = AccountAuthenticator.byName( authName );
			
			if ( auth == null )
				throw new AccountException( "The Authenticator is null" );
			
			try
			{
				AccountMeta meta = AccountManager.INSTANCE.getAccountWithException( acctId );
				
				if ( meta != null )
				{
					AccountCredentials creds = auth.authorize( acctId, this );
					meta.context().credentials = creds;
					
					if ( !creds.getResult().isError() )
					{
						result = AccountResult.LOGIN_SUCCESS;
						login0( meta );
					}
					
					result.setAccount( meta );
					
					if ( result.isError() )
					{
						failedLogin( result );
						meta.context().creator().failedLogin( meta, result );
						EventBus.INSTANCE.callEvent( new AccountFailedLoginEvent( meta, result ) );
					}
				}
			}
			catch ( AccountException e )
			{
				result = e.getResult();
			}
			catch ( Throwable t )
			{
				result = AccountResult.INTERNAL_ERROR.setThrowable( t );
			}
			
			if ( AccountManager.INSTANCE.isDebug() )
			{
				if ( result.isError() && result.getThrowable() != null )
					result.getThrowable().printStackTrace();
				
				SessionManager.getLogger().info( ( ( result == AccountResult.LOGIN_SUCCESS ) ? ConsoleColor.GREEN : ConsoleColor.YELLOW ) + "Session Login: [id='" + acctId + "',reason='" + result.getMessage( acctId ) + "']" );
			}
		}
	}
	
	/**
	 * Attempts to authenticate the Account Id using the specified {@link AccountAuthenticator} and Credentials
	 * 
	 * @param auth
	 *            The {@link AccountAuthenticator}
	 * @param acctId
	 *            The Account Id
	 * @param credObjs
	 *            The Account Credentials
	 * @return
	 *         The {@link AccountResult}
	 */
	public AccountResult login( AccountAuthenticator auth, String acctId, Object... credObjs )
	{
		try
		{
			AccountResult result = AccountResult.DEFAULT;
			AccountMeta meta = AccountManager.INSTANCE.getAccountWithException( acctId );
			
			if ( meta == null )
				return AccountResult.INCORRECT_LOGIN;
			
			try
			{
				meta.context().creator().preLogin( meta, this, acctId, credObjs );
				AccountPreLoginEvent event = new AccountPreLoginEvent( meta, this, acctId, credObjs );
				
				EventBus.INSTANCE.callEvent( event );
				
				if ( event.getAccountResult().isError() )
					return event.getAccountResult().setAccount( meta );
				
				AccountCredentials creds = auth.authorize( meta.getAcctId(), credObjs );
				meta.context().credentials = creds;
				
				if ( creds.getResult().isError() )
					return creds.getResult();
				
				result = AccountResult.LOGIN_SUCCESS;
				login0( meta );
			}
			catch ( AccountException e )
			{
				result = e.getResult();
			}
			catch ( Throwable t )
			{
				return AccountResult.INTERNAL_ERROR.setAccount( meta ).setThrowable( t );
			}
			
			result.setAccount( meta );
			
			if ( result.isError() )
			{
				failedLogin( result );
				meta.context().creator().failedLogin( meta, result );
				EventBus.INSTANCE.callEvent( new AccountFailedLoginEvent( meta, result ) );
			}
			
			return result;
		}
		catch ( AccountException e )
		{
			return e.getResult();
		}
	}
	
	/**
	 * Handles the common final login procedures
	 * 
	 * @param meta
	 *            The {@link AccountMeta}
	 */
	private void login0( AccountMeta meta )
	{
		AccountInstance acct = meta.instance();
		acct.registerPermissible( this );
		
		// TODO Single login per via method checks?
		if ( acct.countPermissibles() > 1 && Loader.getConfig().getBoolean( "accounts.singleLogin" ) )
			for ( AccountPermissible ap : acct.getPermissibles() )
				ap.kick( Loader.getConfig().getString( "accounts.singleLoginMessage", "You logged in from another location." ) );
		
		meta.set( "lastLoginTime", Timings.epoch() );
		meta.set( "lastLoginIp", Joiner.on( "|" ).join( getIpAddresses() ) );
		setVariable( "acctId", meta.getAcctId() );
		
		meta.save();
		
		account = acct;
		
		successfulLogin();
		meta.context().creator().successLogin( meta );
		EventBus.INSTANCE.callEvent( new AccountSuccessfulLoginEvent( meta ) );
	}
	
	/**
	 * Called from subclass once subclass has finished loading
	 */
	protected void initialized()
	{
		login();
	}
	
	public AccountResult logout()
	{
		if ( account != null )
			SessionManager.getLogger().info( ConsoleColor.GREEN + "Successful Logout: [id='" + account.getAcctId() + "',siteId='" + account.getSiteId() + "',displayName='" + account.getDisplayName() + "',ipAddrs='" + account.getIpAddresses() + "']" );
		
		setVariable( "auth", null );
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
