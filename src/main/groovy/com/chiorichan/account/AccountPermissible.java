/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.account;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;

import com.chiorichan.LogColor;
import com.chiorichan.Loader;
import com.chiorichan.account.auth.AccountAuthenticator;
import com.chiorichan.account.auth.AccountCredentials;
import com.chiorichan.account.event.AccountFailedLoginEvent;
import com.chiorichan.account.event.AccountPreLoginEvent;
import com.chiorichan.account.event.AccountSuccessfulLoginEvent;
import com.chiorichan.account.lang.AccountException;
import com.chiorichan.account.lang.AccountResult;
import com.chiorichan.account.lang.AccountDescriptiveReason;
import com.chiorichan.event.EventBus;
import com.chiorichan.lang.ReportingLevel;
import com.chiorichan.permission.Permissible;
import com.chiorichan.permission.PermissibleEntity;
import com.chiorichan.session.SessionManager;
import com.chiorichan.tasks.Timings;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;

/**
 * Used on classes that can support Account Logins, e.g., {@link com.chiorichan.session.Session}
 */
public abstract class AccountPermissible extends Permissible implements Account
{
	/**
	 * The logged in account associated with this session
	 */
	protected AccountInstance account = null;
	
	protected abstract void failedLogin( AccountResult result );
	
	@Override
	public String getDisplayName()
	{
		return instance().getDisplayName();
	}
	
	@Override
	public PermissibleEntity getEntity()
	{
		return meta().getEntity();
	}
	
	/**
	 * Reports if there is an Account logged in
	 * 
	 * @return True is there is
	 */
	public boolean isLoginPresent()
	{
		return account != null;
	}
	
	@Override
	public boolean isInitialized()
	{
		return account != null && account.isInitialized();
	}
	
	@Override
	public String getId()
	{
		if ( !isInitialized() )
			return null;
		
		return instance().getId();
	}
	
	/**
	 * Used by {@link #login()} and {@link #login(AccountAuthenticator, String, Object...)} to save persistent login information
	 * 
	 * @param key
	 *            The key to get
	 * @return
	 *         The String result
	 */
	public abstract String getVariable( String key );
	
	/**
	 * See {@link #getVariable(String)}
	 * 
	 * @param def
	 *            Specifies a default value to return if the requested key is null
	 */
	public abstract String getVariable( String key, String def );
	
	/**
	 * Called from subclass once subclass has finished loading
	 */
	protected void initialized()
	{
		login();
	}
	
	@Override
	public AccountInstance instance()
	{
		// if ( account == null )
		// throw new AccountException( LoginDescriptiveReason.ACCOUNT_NOT_INITIALIZED, AccountType.ACCOUNT_NONE );
		
		return account;
	}
	
	@Override
	public String getSiteId()
	{
		return getSite().getSiteId();
	}
	
	/**
	 * Attempts to authenticate using saved Account Credentials
	 */
	public void login()
	{
		String authName = getVariable( "auth" );
		String acctId = getVariable( "acctId" );
		
		if ( authName != null && !authName.isEmpty() )
		{
			AccountAuthenticator auth = AccountAuthenticator.byName( authName );
			login( auth, acctId, this );
		}
	}
	
	public AccountResult loginWithException( AccountAuthenticator auth, String acctId, Object... credObjs ) throws AccountException
	{
		AccountResult result = login( auth, acctId, credObjs );
		
		if ( !result.isSuccess() )
			throw new AccountException( result.getDescriptiveReason(), result );
		
		return result;
	}
	
	/**
	 * Attempts to authenticate the Account Id using the specified {@link AccountAuthenticator} and Credentials
	 * 
	 * @param auth
	 *            The {@link AccountAuthenticator}
	 * @param acctId
	 *            The Account Id
	 * @param credObjs
	 *            The Account Credentials. Exact credentials depend on what AccountAuthenticator was provided.
	 * @return
	 *         The {@link AccountResult}
	 */
	public AccountResult login( AccountAuthenticator auth, String acctId, Object... credObjs )
	{
		AccountResult result = new AccountResult( acctId );
		AccountMeta meta = null;
		
		try
		{
			if ( auth != null )
			{
				meta = result.getAccountWithException();
				
				if ( meta == null )
				{
					result.setReason( AccountDescriptiveReason.INCORRECT_LOGIN );
					return result;
				}
				
				meta.context().creator().preLogin( meta, this, acctId, credObjs );
				AccountPreLoginEvent event = new AccountPreLoginEvent( meta, this, acctId, credObjs );
				
				EventBus.INSTANCE.callEvent( event );
				
				if ( !event.getDescriptiveReason().getReportingLevel().isIgnorable() )
				{
					result.setReason( event.getDescriptiveReason() );
					return result;
				}
				
				AccountCredentials creds = auth.authorize( meta, credObjs );
				meta.context().credentials = creds;
				
				if ( creds.getDescriptiveReason().getReportingLevel().isSuccess() )
				{
					result.setReason( AccountDescriptiveReason.LOGIN_SUCCESS );
					
					AccountInstance acct = meta.instance();
					
					// TODO Single login per via method checks?
					if ( acct.countAttachments() > 1 && Loader.getConfig().getBoolean( "accounts.singleLogin" ) )
						for ( AccountAttachment ap : acct.getAttachments() )
							if ( ap instanceof Kickable )
								( ( Kickable ) ap ).kick( Loader.getConfig().getString( "accounts.singleLoginMessage", "You logged in from another location." ) );
					
					meta.set( "lastLoginTime", Timings.epoch() );
					
					// XXX Should we track all past IPs or only the current ones and what about local logins?
					Set<String> ips = Sets.newLinkedHashSet();
					if ( meta.getString( "lastLoginIp" ) != null )
						ips.addAll( Splitter.on( "|" ).splitToList( meta.getString( "lastLoginIp" ) ) );
					ips.addAll( getIpAddresses() );
					
					if ( ips.size() > 5 )
						meta.set( "lastLoginIp", Joiner.on( "|" ).join( new LinkedList<String>( ips ).subList( ips.size() - 5, ips.size() ) ) );
					else if ( ips.size() > 0 )
						meta.set( "lastLoginIp", Joiner.on( "|" ).join( ips ) );
					setVariable( "acctId", meta.getId() );
					
					meta.save();
					
					account = acct;
					
					successfulLogin();
					meta.context().creator().successLogin( meta );
					EventBus.INSTANCE.callEvent( new AccountSuccessfulLoginEvent( meta, this, result ) );
				}
				else
					result.setReason( creds.getDescriptiveReason() );
			}
			else
				result.setReason( new AccountDescriptiveReason( "The Authenticator was null!", ReportingLevel.L_ERROR ) );
		}
		catch ( AccountException e )
		{
			if ( e.getResult() == null )
			{
				result.setReason( e.getReason() );
				if ( e.hasCause() )
					result.setCause( e.getCause() );
			}
			else
				result = e.getResult();
		}
		catch ( Throwable t )
		{
			result.setReason( AccountDescriptiveReason.INTERNAL_ERROR );
			result.setCause( t );
		}
		
		if ( !result.isSuccess() )
		{
			failedLogin( result );
			if ( meta != null )
				meta.context().creator().failedLogin( meta, result );
			EventBus.INSTANCE.callEvent( new AccountFailedLoginEvent( meta, result ) );
		}
		
		if ( AccountManager.INSTANCE.isDebug() )
		{
			if ( !result.isIgnorable() && result.hasCause() )
				result.getCause().printStackTrace();
			
			SessionManager.getLogger().info( ( ( result.isSuccess() ) ? LogColor.GREEN : LogColor.YELLOW ) + "Session Login: [id='" + acctId + "',reason='" + result.getFormattedMessage() + "']" );
		}
		
		return result;
	}
	
	protected void registerAttachment( AccountAttachment attachment )
	{
		if ( account != null )
			account.registerAttachment( attachment );
	}
	
	protected void unregisterAttachment( AccountAttachment attachment )
	{
		if ( account != null )
			account.unregisterAttachment( attachment );
	}
	
	public abstract Collection<String> getIpAddresses();
	
	public AccountResult logout()
	{
		AccountResult result = new AccountResult( account == null ? AccountType.ACCOUNT_NONE : account.meta(), AccountDescriptiveReason.LOGOUT_SUCCESS );
		
		if ( account != null )
		{
			SessionManager.getLogger().info( LogColor.GREEN + "Successful Logout: [id='" + account.getId() + "',siteId='" + account.getSiteId() + "',displayName='" + account.getDisplayName() + "',ipAddrs='" + account.getIpAddresses() + "']" );
			account = null;
		}
		
		setVariable( "auth", null );
		setVariable( "acctId", null );
		setVariable( "token", null );
		
		return result;
	}
	
	@Override
	public AccountMeta meta()
	{
		return instance().meta();
	}
	
	public abstract void setVariable( String key, String value );
	
	protected abstract void successfulLogin() throws AccountException;
}
