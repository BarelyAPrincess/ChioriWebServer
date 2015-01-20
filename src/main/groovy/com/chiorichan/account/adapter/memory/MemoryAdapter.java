/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
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
import com.google.common.collect.Lists;

public class MemoryAdapter implements AccountLookupAdapter
{
	/**
	 * Accounts are created programmably.
	 */
	@Override
	public List<AccountMetaData> getAccounts()
	{
		return Lists.newArrayList();
	}
	
	/**
	 * Can't save programmably created accounts.
	 */
	@Override
	public void saveAccount( AccountMetaData account )
	{
		
	}
	
	@Override
	public void failedLoginUpdate( AccountMetaData account, LoginExceptionReason reason )
	{
		
	}
	
	/**
	 * Nothing to reload.
	 */
	@Override
	public AccountMetaData reloadAccount( AccountMetaData account )
	{
		return account;
	}
	
	@Override
	public AccountMetaData readAccount( String account ) throws LoginException
	{
		return new AccountMetaData( account, "9834h9fh3497ah4ea3a", account );
	}

	@Override
	public Class<? extends Account<AccountLookupAdapter>> getAccountClass()
	{
		return MemoryAccount.class;
	}
}
