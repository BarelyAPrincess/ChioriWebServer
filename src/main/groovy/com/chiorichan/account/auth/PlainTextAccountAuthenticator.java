/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.account.auth;

import org.apache.commons.codec.digest.DigestUtils;

import com.chiorichan.account.AccountInstance;
import com.chiorichan.account.AccountManager;
import com.chiorichan.account.AccountMeta;
import com.chiorichan.account.lang.AccountException;
import com.chiorichan.account.lang.AccountResult;

/**
 * Used to authenticate an account using a Username and Password combination
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public final class PlainTextAccountAuthenticator extends AccountAuthenticator
{
	PlainTextAccountAuthenticator()
	{
		
	}
	
	public AccountCredentials credentials( String user, String pass )
	{
		if ( user == null || user.isEmpty() )
			throw new AccountException( AccountResult.EMPTY_USERNAME );
		
		if ( pass == null || pass.isEmpty() )
			throw new AccountException( AccountResult.EMPTY_PASSWORD );
		
		return new PlainTextAccountCredentials( user, pass );
	}
	
	class PlainTextAccountCredentials extends AccountCredentials
	{
		private String user;
		private String pass;
		private AccountInstance acct;
		
		PlainTextAccountCredentials( String user, String pass )
		{
			super( PlainTextAccountAuthenticator.this );
			this.user = user;
			this.pass = pass;
		}
		
		@Override
		public String getToken() throws AccountException
		{
			if ( acct == null )
				throw new AccountException( "You can't getToken() until you authenticate()" );
			
			return AccountAuthenticator.TOKEN.issueToken( acct );
		}
		
		@Override
		public AccountInstance authenticate() throws AccountException
		{
			AccountMeta meta = AccountManager.INSTANCE.getAccountWithException( user );
			
			if ( meta == null )
				throw AccountResult.INCORRECT_LOGIN.exception();
			
			String password = meta.getString( "password" );
			
			if ( password == null )
				throw new AccountException( AccountResult.UNCONFIGURED );
			
			if ( password.equals( pass ) || password.equals( DigestUtils.md5Hex( pass ) ) || DigestUtils.md5Hex( password ).equals( pass ) )
				acct = meta.instance();
			else
				throw new AccountException( AccountResult.INCORRECT_LOGIN );
			
			return acct;
		}
	}
}
