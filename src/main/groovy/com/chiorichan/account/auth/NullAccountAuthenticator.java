/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.account.auth;

import com.chiorichan.account.AccountMeta;
import com.chiorichan.account.AccountPermissible;
import com.chiorichan.account.AccountType;
import com.chiorichan.account.lang.AccountException;
import com.chiorichan.account.lang.AccountDescriptiveReason;

/**
 * Usually only used to authenticate the NONE login
 */
public final class NullAccountAuthenticator extends AccountAuthenticator
{
	class NullAccountCredentials extends AccountCredentials
	{
		NullAccountCredentials( AccountMeta meta )
		{
			super( NullAccountAuthenticator.this, AccountDescriptiveReason.LOGIN_SUCCESS, meta );
		}
	}
	
	NullAccountAuthenticator()
	{
		super( "null" );
	}
	
	@Override
	public AccountCredentials authorize( AccountMeta acct, AccountPermissible perm ) throws AccountException
	{
		if ( acct != AccountType.ACCOUNT_NONE )
			throw new AccountException( AccountDescriptiveReason.INCORRECT_LOGIN, acct );
		
		return new NullAccountCredentials( acct );
	}
	
	@Override
	public AccountCredentials authorize( AccountMeta acct, Object... creds ) throws AccountException
	{
		if ( acct != AccountType.ACCOUNT_NONE )
			throw new AccountException( AccountDescriptiveReason.INCORRECT_LOGIN, acct );
		
		return new NullAccountCredentials( acct );
	}
}
