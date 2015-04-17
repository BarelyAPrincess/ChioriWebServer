/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.account.adapter.memory;

import java.util.List;

import com.chiorichan.account.Account;
import com.chiorichan.account.AccountMetaData;
import com.chiorichan.account.LoginException;
import com.chiorichan.account.LoginExceptionReason;
import com.chiorichan.account.adapter.AccountLookupAdapter;
import com.chiorichan.util.StringUtil;
import com.google.common.collect.Lists;

/**
 * Works as the virtual adapter for virtual accounts.
 */
public class MemoryAdapter implements AccountLookupAdapter
{
	/**
	 * This is only so we can pretend to actually have accounts with the {@link #getAccounts()} method.
	 * Obviously if we did not initialize any account during runtime {@link #getAccounts()} will return empty.
	 */
	protected final List<AccountMetaData> cachedAccts = Lists.newCopyOnWriteArrayList();
	
	@Override
	public List<AccountMetaData> getAccounts()
	{
		return cachedAccts;
	}
	
	protected void cacheAcct( AccountMetaData acct )
	{
		cachedAccts.add( acct );
	}
	
	@Override
	public void saveAccount( AccountMetaData meta )
	{
		
	}
	
	@Override
	public void failedLoginUpdate( AccountMetaData meta, LoginExceptionReason reason )
	{
		
	}
	
	/**
	 * Nothing to reload.
	 */
	@Override
	public AccountMetaData reloadAccount( AccountMetaData meta )
	{
		return meta;
	}
	
	@Override
	public AccountMetaData readAccount( String account ) throws LoginException
	{
		return new AccountMetaData( account, StringUtil.md5( "9834h9fh3497ah4ea3a" + System.currentTimeMillis() ), account );
	}
	
	@Override
	public Class<? extends Account> getAccountClass()
	{
		return MemoryAccount.class;
	}
}
