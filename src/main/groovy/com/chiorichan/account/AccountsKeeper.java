/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.account;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.Validate;

import com.chiorichan.account.adapter.AccountLookupAdapter;
import com.chiorichan.account.adapter.memory.MemoryAccount;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class AccountsKeeper
{
	protected Map<Account, AccountsKeeperOptions> accounts = Maps.newConcurrentMap();
	protected AccountLookupAdapter adapter = null;
	
	public Account accountConstruct( AccountLookupAdapter adapter, String userId, Object... params ) throws LoginException
	{
		List<Class<?>> paramsClass = Lists.newLinkedList();
		
		for ( Object o : params )
			paramsClass.add( o.getClass() );
		
		try
		{
			Constructor<? extends Account> constructor = adapter.getAccountClass().getConstructor( paramsClass.toArray( new Class<?>[0] ) );
			return constructor.newInstance( params );
		}
		catch ( InvocationTargetException e )
		{
			throw ( LoginException ) e.getTargetException();
		}
		catch ( NoSuchMethodException | InstantiationException | IllegalAccessException | IllegalArgumentException e )
		{
			e.printStackTrace();
			AccountManager.getLogger().severe( "We had a problem constructing a new instance of '" + adapter.getAccountClass().getCanonicalName() + "' account class. We are not sure of the reason but this is most likely a SEVERE error. For the time being, we constructed a MemoryAccount but this is only temporary and logins will fail." );
			return new MemoryAccount( userId, AccountLookupAdapter.MEMORY_ADAPTER );
		}
	}
	
	public IAccountsKeeperOptions putAccount( Account acct, boolean keepInMemory )
	{
		AccountsKeeperOptions options = new AccountsKeeperOptions( keepInMemory );
		accounts.put( acct, options );
		return ( IAccountsKeeperOptions ) options;
	}
	
	public void clearAll()
	{
		accounts.clear();
	}
	
	public IAccountsKeeperOptions getAccountOptions( Account acct )
	{
		if ( !accounts.containsKey( acct ) )
			return null;
		
		return accounts.get( acct );
	}
	
	public Account isLoaded( String acctId )
	{
		for ( Account acct : accounts.keySet() )
			if ( acct.getAcctId().equals( acctId ) )
				return acct;
		
		return null;
	}
	
	public boolean isLoaded( Account acct )
	{
		return accounts.containsKey( acct );
	}
	
	public ArrayList<Account> getAccounts()
	{
		ArrayList<Account> accts = Lists.newArrayList();
		
		for ( Entry<Account, AccountsKeeperOptions> entry : accounts.entrySet() )
		{
			accts.add( entry.getKey() );
		}
		
		return accts;
	}
	
	public Account getAccountPartial( final String partial )
	{
		Validate.notNull( partial, "Partial Name cannot be null" );
		
		Account found = null;
		String lowerName = partial.toLowerCase();
		int delta = Integer.MAX_VALUE;
		for ( Entry<Account, AccountsKeeperOptions> entry : accounts.entrySet() )
		{
			if ( entry.getKey().getAcctId().toLowerCase().startsWith( lowerName ) )
			{
				int curDelta = entry.getKey().getAcctId().length() - lowerName.length();
				if ( curDelta < delta )
				{
					found = entry.getKey();
					delta = curDelta;
				}
				if ( curDelta == 0 )
					break;
			}
		}
		return found;
	}
	
	public Account getAccount( String s ) throws LoginException
	{
		Validate.notNull( s, "Partial Name cannot be null" );
		
		if ( adapter == null )
			throw new LoginException( LoginExceptionReason.unconfigured );
		
		Account acct = null;
		
		for ( Entry<Account, AccountsKeeperOptions> entry : accounts.entrySet() )
			if ( entry.getKey().isYou( s ) )
				acct = entry.getKey();
		
		if ( acct == null )
		{
			AccountMetaData meta = adapter.readAccount( s );
			
			if ( meta != null )
			{
				acct = accountConstruct( adapter, s, meta, adapter );
				putAccount( acct, false );
			}
		}
		
		return acct;
	}
	
	public List<Account> getOnlineAccounts()
	{
		ArrayList<Account> accts = Lists.newArrayList();
		
		for ( Entry<Account, AccountsKeeperOptions> entry : accounts.entrySet() )
		{
			if ( entry.getKey().hasHandler() )
				accts.add( entry.getKey() );
		}
		
		return accts;
	}
	
	public List<Account> getOfflineAccounts()
	{
		List<AccountMetaData> metas = adapter.getAccounts();
		List<Account> accts = Lists.newArrayList();
		
		if ( adapter == null )
			return accts;
		
		for ( AccountMetaData meta : metas )
		{
			Account acct = isLoaded( meta.getAcctId() );
			
			if ( acct == null )
				try
				{
					accts.add( accountConstruct( adapter, meta.getAcctId(), meta, adapter ) );
				}
				catch ( LoginException e )
				{
					e.printStackTrace();
				}
			else if ( !acct.hasHandler() )
				accts.add( acct );
		}
		
		return accts;
	}
	
	public void saveAccounts()
	{
		if ( adapter == null )
			return;
		
		for ( Account acct : accounts.keySet() )
		{
			if ( acct != null )
				adapter.saveAccount( acct.getMetaData() );
		}
	}
	
	public void setAdapter( AccountLookupAdapter adapter )
	{
		this.adapter = adapter;
	}
	
	public interface IAccountsKeeperOptions
	{
		boolean keepInMemory();
		
		boolean isOp();
		
		boolean isWhitelisted();
		
		boolean isBanned();
	}
	
	public class AccountsKeeperOptions implements IAccountsKeeperOptions
	{
		// Will not unload from memory. Used for system accounts.
		protected boolean keepInMemory = false;
		
		// Does this account bypass permissions systems and given an all exclusive backdoor access.
		protected boolean isOp = false;
		
		// Is this account permitted to connect to this server. Great for servers that share a users database.
		protected boolean isWhitelisted = true;
		
		// Is this account banned from this server.
		protected boolean isBanned = false;
		
		public AccountsKeeperOptions( boolean keepInMemory )
		{
			this.keepInMemory = keepInMemory;
		}
		
		public boolean keepInMemory()
		{
			return keepInMemory();
		}
		
		public boolean isOp()
		{
			return isOp;
		}
		
		public boolean isWhitelisted()
		{
			return isWhitelisted;
		}
		
		public boolean isBanned()
		{
			return isBanned;
		}
		
		public void setKeepInMemory( boolean value )
		{
			keepInMemory = value;
		}
		
		public void setOp( boolean value )
		{
			isOp = value;
		}
		
		public void setWhitelisted( boolean value )
		{
			isWhitelisted = value;
		}
		
		public void setBanned( boolean value )
		{
			isBanned = value;
		}
	}
	
	public boolean isConfigured()
	{
		return adapter != null;
	}
}
