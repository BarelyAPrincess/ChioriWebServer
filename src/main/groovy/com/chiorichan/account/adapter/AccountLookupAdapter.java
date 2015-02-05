/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.account.adapter;

import java.util.List;

import com.chiorichan.account.Account;
import com.chiorichan.account.AccountMetaData;
import com.chiorichan.account.LoginException;
import com.chiorichan.account.LoginExceptionReason;
import com.chiorichan.account.adapter.memory.MemoryAdapter;

/**
 * Used to lookup Account MetaData from the adapters datastore
 */
public interface AccountLookupAdapter
{
	public MemoryAdapter MEMORY_ADAPTER = new MemoryAdapter();
	
	/**
	 * Returns all accounts maintained by this adapter.
	 * 
	 * @return
	 */
	public List<AccountMetaData> getAccounts();
	
	/**
	 * Attempt to serialize provided account.
	 * Use of the account instance may continue.
	 */
	public void saveAccount( AccountMetaData account );
	
	/**
	 * Attempt to reload details regarding this account.
	 * 
	 * @return
	 */
	public AccountMetaData reloadAccount( AccountMetaData account );
	
	/**
	 * Attempt to load a account.
	 * 
	 * @throws LoginException
	 */
	public AccountMetaData readAccount( String account ) throws LoginException;
	
	/**
	 * @return Class extends Account
	 *         The class that should be used to create the Account Object
	 */
	public Class<? extends Account<? extends AccountLookupAdapter>> getAccountClass();
	
	/**
	 * Informs the Adapater of a failed login
	 * 
	 * @param account
	 *             The account that failed the login.
	 * @param reason
	 *             The reason the login failed.
	 * 
	 */
	public void failedLoginUpdate( AccountMetaData meta, LoginExceptionReason reason );
}
