/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.account.adapter;

import java.util.List;

import com.chiorichan.account.Account;
import com.chiorichan.account.AccountMetaData;
import com.chiorichan.account.adapter.memory.MemoryAdapter;
import com.chiorichan.account.lang.LoginException;
import com.chiorichan.account.lang.LoginExceptionReason;

/**
 * Used to lookup Account MetaData from the adapters datastore
 */
public interface AccountLookupAdapter
{
	MemoryAdapter MEMORY_ADAPTER = new MemoryAdapter();
	
	/**
	 * Returns all accounts maintained by this adapter.
	 * 
	 * @return List of loaded accounts by this adapter
	 */
	List<AccountMetaData> getAccounts();
	
	/**
	 * Attempt to serialize provided account.
	 * Use of the account instance may continue.
	 */
	void saveAccount( AccountMetaData account ) throws Exception;
	
	/**
	 * Attempt to reload details regarding this account.
	 */
	void reloadAccount( AccountMetaData account ) throws Exception;
	
	/**
	 * Attempt to load a account.
	 * 
	 * @throws LoginException
	 */
	AccountMetaData readAccount( String account ) throws LoginException;
	
	/**
	 * @return Class extends Account
	 *         The class that should be used to create the Account Object
	 */
	Class<? extends Account> getAccountClass();
	
	/**
	 * Informs the Adapater of a failed login
	 * 
	 * @param meta
	 *            The account that failed the login.
	 * @param reason
	 *            The reason the login failed.
	 */
	void failedLoginUpdate( AccountMetaData meta, LoginExceptionReason reason );
}
