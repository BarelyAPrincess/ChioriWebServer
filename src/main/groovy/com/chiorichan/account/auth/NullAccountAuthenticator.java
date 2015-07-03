/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.account.auth;

import com.chiorichan.account.AccountManager;
import com.chiorichan.account.AccountMeta;
import com.chiorichan.account.AccountPermissible;
import com.chiorichan.account.AccountType;
import com.chiorichan.account.lang.AccountException;
import com.chiorichan.account.lang.AccountResult;

/**
 * Usually only used to authenticate the NONE login
 */
public final class NullAccountAuthenticator extends AccountAuthenticator
{
	class NullAccountCredentials extends AccountCredentials
	{
		NullAccountCredentials( AccountMeta meta )
		{
			super( NullAccountAuthenticator.this, AccountResult.LOGIN_SUCCESS, meta );
		}
	}
	
	NullAccountAuthenticator()
	{
		super( "null" );
	}
	
	@Override
	public AccountCredentials authorize( String acctId, AccountPermissible perm )
	{
		AccountMeta meta = AccountManager.INSTANCE.getAccountWithException( acctId );
		
		if ( meta != AccountType.ACCOUNT_NONE )
			throw new AccountException( AccountResult.INCORRECT_LOGIN );
		
		return new NullAccountCredentials( meta );
	}
	
	@Override
	public AccountCredentials authorize( String acctId, Object... creds )
	{
		AccountMeta meta = AccountManager.INSTANCE.getAccountWithException( acctId );
		
		if ( meta != AccountType.ACCOUNT_NONE )
			throw new AccountException( AccountResult.INCORRECT_LOGIN );
		
		return new NullAccountCredentials( meta );
	}
}
