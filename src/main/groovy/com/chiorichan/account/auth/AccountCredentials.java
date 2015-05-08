/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.account.auth;

import com.chiorichan.account.AccountInstance;
import com.chiorichan.account.lang.AccountException;
import com.chiorichan.account.lang.AccountResult;

/**
 * Provides login credentials to the {@link AccountAuthenticator}
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public abstract class AccountCredentials
{
	private final AccountAuthenticator authenticator;
	
	AccountCredentials( AccountAuthenticator authenticator )
	{
		this.authenticator = authenticator;
	}
	
	public AccountAuthenticator getAuthenticator()
	{
		return authenticator;
	}
	
	/**
	 * Implemented on an Authenticator by Authenticator bases,<br>
	 * ideally we try and get a relogin token from the {@link OneTimeTokenAuthentictor}.<br>
	 * Which means you would use said Authenticator for relogin.
	 * 
	 * @return
	 *         A relogin token
	 * @throws AccountException
	 *             {@link AccountResult.FEATURE_NOT_IMPLEMENTED} if not implemented by authenticator
	 */
	public String getToken() throws AccountException
	{
		throw new AccountException( AccountResult.FEATURE_NOT_IMPLEMENTED );
	}
	
	public abstract AccountInstance authenticate() throws AccountException;
}
