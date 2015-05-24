/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.account;

import java.util.List;

import com.chiorichan.account.lang.AccountResult;

/**
 * Specifies which methods are required for a class to manage accounts
 * 
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
public interface AccountCreator
{
	void save( AccountMeta account );
	
	void reload( AccountMeta account );
	
	boolean isEnabled();
	
	void failedLogin( AccountMeta meta, AccountResult result );
	
	void successLogin( AccountMeta meta );
	
	void preLogin( AccountMeta meta, AccountPermissible via, String acctId, Object... creds );
	
	String getDisplayName( AccountMeta meta );
	
	List<String> getLoginKeys();
}
