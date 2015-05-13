/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.account;

import com.chiorichan.account.auth.AccountCredentials;
import com.chiorichan.account.lang.AccountResult;

/**
 * Specifies which methods are required for a class to manage accounts
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public interface AccountCreator
{
	void save( AccountMeta account );
	
	void reload( AccountMeta account );
	
	boolean isEnabled();
	
	void failedLogin( AccountMeta meta, AccountResult result );
	
	void successLogin( AccountMeta meta, AccountResult result );
	
	void preLogin( AccountMeta meta, AccountPermissible via, AccountCredentials creds );
	
	String getDisplayName( AccountMeta meta );
}
