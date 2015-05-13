/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.account.auth;

import com.chiorichan.account.AccountInstance;
import com.chiorichan.account.AccountManager;
import com.chiorichan.account.AccountMeta;
import com.chiorichan.account.lang.AccountException;
import com.chiorichan.account.lang.AccountResult;
import com.chiorichan.util.CommonFunc;
import com.chiorichan.util.RandomFunc;


public class OnetimeTokenAccountAuthenticator extends AccountAuthenticator
{
	OnetimeTokenAccountAuthenticator()
	{
		
	}
	
	public AccountCredentials credentials( String acctId, String token )
	{
		return new OnetimeTokenAccountCredentials( acctId, token );
	}
	
	class OnetimeTokenAccountCredentials extends AccountCredentials
	{
		private String acctId;
		private String token;
		
		OnetimeTokenAccountCredentials( String acctId, String token )
		{
			super( OnetimeTokenAccountAuthenticator.this );
			this.acctId = acctId;
			this.token = token;
		}
		
		@Override
		public String getToken()
		{
			return null;
		}
		
		@Override
		public AccountInstance authenticate() throws AccountException
		{
			AccountMeta meta = AccountManager.INSTANCE.getAccountWithException( acctId );
			
			if ( meta == null )
				throw AccountResult.INCORRECT_LOGIN.exception();
			
			String token0 = meta.getString( "token" );
			
			if ( token0 == null )
				throw new AccountException( AccountResult.UNCONFIGURED );
			
			if ( token0.equals( token ) )
			{
				meta.set( "token", null );
				return meta.instance();
			}
			else
				throw new AccountException( AccountResult.INCORRECT_LOGIN );
		}
	}
	
	public String issueToken( AccountInstance acct )
	{
		return RandomFunc.randomize( acct.getAcctId() ) + CommonFunc.getEpoch();
	}
}
