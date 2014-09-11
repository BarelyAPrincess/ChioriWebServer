/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.account.adapter;

import java.util.List;

import com.chiorichan.account.bases.Account;
import com.chiorichan.account.helpers.AccountMetaData;
import com.chiorichan.account.helpers.LoginException;
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
	
	/**
	 * Nothing to reload.
	 */
	@Override
	public AccountMetaData reloadAccount( AccountMetaData account )
	{
		return account;
	}
	
	@Override
	public AccountMetaData loadAccount( String account ) throws LoginException
	{
		return new AccountMetaData( account, "9834h9fh3497ah4ea3a", account );
	}
	
	@Override
	public void preLoginCheck( Account account ) throws LoginException
	{
		
	}
	
	@Override
	public void postLoginCheck( Account account ) throws LoginException
	{
		
	}
	
	@Override
	public void failedLoginUpdate( Account account )
	{
		
	}
	
	@Override
	public boolean matchAccount( Account account, String username )
	{
		AccountMetaData meta = account.getMetaData();
		
		if ( meta.getString( "username" ).equals( username ) )
			return true;
		
		if ( meta.getString( "acctId" ).equals( username ) )
			return true;
		
		return false;
	}
}
