/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.account;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import com.chiorichan.ConsoleLogger;
import com.chiorichan.Loader;
import com.chiorichan.ServerManager;
import com.chiorichan.account.lang.AccountException;
import com.chiorichan.account.lang.AccountResult;
import com.chiorichan.event.EventBus;
import com.chiorichan.plugin.PluginDescriptionFile;
import com.chiorichan.scheduler.TaskCreator;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;

/**
 * Provides Account Management to the Server
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public final class AccountManager extends AccountEvents implements ServerManager, TaskCreator
{
	/**
	 * Holds an instance of this Account Manager
	 */
	public static final AccountManager INSTANCE = new AccountManager();
	
	/**
	 * Has this manager already been initialized?
	 */
	private static boolean isInitialized = false;
	
	/**
	 * References accounts meta data. We try and populate this list at load with all available accounts but this is not always guaranteed.
	 */
	final AccountList accounts = new AccountList();
	
	boolean isDebug = false;
	int maxAccounts = -1;
	
	public static void init()
	{
		if ( isInitialized )
			throw new IllegalStateException( "The Account Manager has already been initialized." );
		
		assert INSTANCE != null;
		
		INSTANCE.init0();
		
		isInitialized = true;
	}
	
	private AccountManager()
	{
		
	}
	
	private void init0()
	{
		isDebug = Loader.getConfig().getBoolean( "accounts.debug" );
		maxAccounts = Loader.getConfig().getInt( "accounts.maxLogins", -1 );
		
		EventBus.INSTANCE.registerEvents( AccountType.MEMORY.getCreator(), this );
		EventBus.INSTANCE.registerEvents( AccountType.SQL.getCreator(), this );
		EventBus.INSTANCE.registerEvents( AccountType.FILE.getCreator(), this );
	}
	
	public AccountMeta getAccount( String acctId ) throws AccountException
	{
		AccountMeta acct = accounts.get( acctId );
		
		if ( acct == null )
		{
			acct = fireAccountLookup( acctId );
			
			if ( acct == null )
				throw AccountResult.INCORRECT_LOGIN.exception();
			
			accounts.put( acct );
		}
		
		return acct;
	}
	
	public AccountMeta getAccountPartial( String partial ) throws AccountException
	{
		Validate.notNull( partial );
		
		AccountMeta found = null;
		String lowerName = partial.toLowerCase();
		int delta = Integer.MAX_VALUE;
		for ( AccountMeta meta : getAccounts() )
		{
			if ( meta.getAcctId().toLowerCase().startsWith( lowerName ) )
			{
				int curDelta = meta.getAcctId().length() - lowerName.length();
				if ( curDelta < delta )
				{
					found = meta;
					delta = curDelta;
				}
				if ( curDelta == 0 )
					break;
			}
		}
		return found;
	}
	
	public Set<Account> getInitializedAccounts()
	{
		Set<Account> accts = Sets.newHashSet();
		for ( AccountMeta meta : accounts )
			if ( meta.isInitialized() )
				accts.add( meta );
		return accts;
	}
	
	/**
	 * Gets all Account Permissibles by crawling the {@link AccountMeta} and {@link AccountInstance}
	 * 
	 * @return
	 *         A set of AccountPermissibles
	 */
	public Set<AccountPermissible> getAccountPermissibles()
	{
		Set<AccountPermissible> accts = Sets.newHashSet();
		for ( AccountMeta meta : accounts )
			if ( meta.isInitialized() )
				accts.addAll( Arrays.asList( meta.instance().getPermissibles() ) );
		return accts;
	}
	
	Set<AccountMeta> getAccounts0()
	{
		return accounts.toSet();
	}
	
	public Set<AccountMeta> getAccounts()
	{
		return Collections.unmodifiableSet( getAccounts0() );
	}
	
	public Set<AccountMeta> getAccounts( String query )
	{
		Validate.notNull( query );
		
		Set<AccountMeta> results = Sets.newHashSet();
		
		if ( query.contains( "|" ) )
		{
			for ( String s : Splitter.on( "|" ).split( query ) )
				if ( s != null && !s.isEmpty() )
					results.addAll( getAccounts( s ) );
			
			return results;
		}
		
		boolean isLower = query.toLowerCase().equals( query ); // Is query string all lower case?
		
		for ( AccountMeta meta : accounts.toSet() )
		{
			String id = ( isLower ) ? meta.getAcctId().toLowerCase() : meta.getAcctId();
			
			if ( !id.isEmpty() && id.contains( query ) )
			{
				results.add( meta );
				continue;
			}
			
			id = ( isLower ) ? meta.getDisplayName().toLowerCase() : meta.getDisplayName();
			
			if ( !id.isEmpty() && id.contains( query ) )
			{
				results.add( meta );
				continue;
			}
			
			// TODO Figure out how to further check these values.
			// Maybe send the check into the Account Creator
		}
		
		return results;
	}
	
	public Set<Account> getBanned()
	{
		Set<Account> accts = Sets.newHashSet();
		for ( AccountMeta meta : accounts )
			if ( meta.isBanned() )
				accts.add( meta );
		return accts;
	}
	
	public Set<Account> getWhitelisted()
	{
		Set<Account> accts = Sets.newHashSet();
		for ( AccountMeta meta : accounts )
			if ( meta.isWhitelisted() )
				accts.add( meta );
		return accts;
	}
	
	public Set<Account> getOperators()
	{
		Set<Account> accts = Sets.newHashSet();
		for ( AccountMeta meta : accounts )
			if ( meta.isOp() )
				accts.add( meta );
		return accts;
	}
	
	public void save()
	{
		for ( AccountMeta meta : accounts )
			meta.save();
	}
	
	public void reload()
	{
		save();
		accounts.clear();
	}
	
	@Override
	public boolean isEnabled()
	{
		return true;
	}
	
	@Override
	public String getName()
	{
		return "AccountManager";
	}
	
	@Override
	public PluginDescriptionFile getDescription()
	{
		return null;
	}
	
	public static ConsoleLogger getLogger()
	{
		return Loader.getLogger( "AcctMgr" );
	}
	
	public boolean isDebug()
	{
		return isDebug;
	}
	
	/**
	 * Attempts to kick all logins of account
	 * 
	 * @param acct
	 *            The Account to kick
	 * @param msg
	 *            The reason for kick
	 * @return Was the kick successful
	 */
	public boolean kick( AccountInstance acct, String msg )
	{
		Validate.notNull( acct );
		
		return fireKick( acct, msg );
	}
	
	/**
	 * See {@link #kick(AccountInstance, String)}
	 */
	public boolean kick( AccountMeta acct, String msg )
	{
		Validate.notNull( acct );
		
		if ( !acct.isInitialized() )
			throw AccountResult.ACCOUNT_NOT_INITIALIZED.exception( acct.getDisplayName() );
		
		return kick( acct.instance(), msg );
	}
	
	/**
	 * Attempts to only kick the provided instance of login
	 * 
	 * @param acct
	 *            The instance to kick
	 * @param msg
	 *            The reason to kick
	 * @return Was the kick successful
	 */
	public boolean kick( AccountPermissible acct, String msg )
	{
		Validate.notNull( acct );
		
		return fireKick( acct, msg );
	}
}
