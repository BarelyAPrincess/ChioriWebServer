/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.account.auth;

import com.chiorichan.account.AccountMeta;
import com.chiorichan.account.AccountPermissible;
import com.chiorichan.account.lang.AccountException;
import com.chiorichan.account.lang.AccountResult;

/**
 * Provides login credentials to the {@link AccountAuthenticator}
 * 
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
public abstract class AccountCredentials
{
	protected final AccountAuthenticator authenticator;
	protected final AccountResult result;
	protected final AccountMeta meta;
	
	AccountCredentials( AccountAuthenticator authenticator, AccountResult result, AccountMeta meta )
	{
		this.authenticator = authenticator;
		this.result = result;
		this.meta = meta;
	}
	
	public final AccountAuthenticator getAuthenticator()
	{
		return authenticator;
	}
	
	public final AccountResult getResult()
	{
		return result;
	}
	
	public final AccountMeta getAccount()
	{
		return meta;
	}
	
	/**
	 * Saves persistent variables into the session for later resuming
	 * 
	 * @param perm
	 *            The AccountPermissible to store the login credentials
	 */
	public void makeResumable( AccountPermissible perm )
	{
		if ( perm.metadata() != meta )
			throw new AccountException( "You can't make an Account resumable on a Permissible it's not logged into." ).setAccount( meta );
		
		if ( result != AccountResult.LOGIN_SUCCESS )
			throw new AccountException( "You can't make a login resumable if it failed login." ).setAccount( meta );
		
		try
		{
			perm.setVariable( "auth", "token" );
			perm.setVariable( "acctId", meta.getAcctId() );
			perm.setVariable( "token", AccountAuthenticator.TOKEN.issueToken( meta ) );
		}
		catch ( AccountException e )
		{
			throw e;
		}
	}
}
