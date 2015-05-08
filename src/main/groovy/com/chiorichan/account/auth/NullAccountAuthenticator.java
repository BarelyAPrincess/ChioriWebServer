/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.account.auth;

import com.chiorichan.account.AccountInstance;
import com.chiorichan.account.AccountMeta;
import com.chiorichan.account.AccountType;
import com.chiorichan.account.lang.AccountException;
import com.chiorichan.account.lang.AccountResult;

/**
 * Usually only used to authenticate the NONE login
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public final class NullAccountAuthenticator extends AccountAuthenticator
{
	NullAccountAuthenticator()
	{
		
	}
	
	public AccountCredentials credentials( AccountMeta meta )
	{
		if ( meta != AccountType.ACCOUNT_NONE )
			throw new AccountException( AccountResult.INCORRECT_LOGIN );
		
		return new NullAccountCredentials();
	}
	
	class NullAccountCredentials extends AccountCredentials
	{
		NullAccountCredentials()
		{
			super( NullAccountAuthenticator.this );
		}
		
		@Override
		public AccountInstance authenticate() throws AccountException
		{
			return AccountType.ACCOUNT_NONE.instance();
		}
	}
}
