/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.account.auth;

import org.apache.commons.codec.digest.DigestUtils;

import com.chiorichan.account.AccountManager;
import com.chiorichan.account.AccountMeta;
import com.chiorichan.account.AccountPermissible;
import com.chiorichan.account.lang.AccountException;
import com.chiorichan.account.lang.AccountResult;

/**
 * Used to authenticate an account using a Username and Password combination
 * 
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
public final class PlainTextAccountAuthenticator extends AccountAuthenticator
{
	PlainTextAccountAuthenticator()
	{
		super( "plaintext" );
	}
	
	@Override
	public AccountCredentials authorize( String acctId, AccountPermissible perm )
	{
		/**
		 * Session Logins are not resumed using plain text. See {@link AccountCredentials#makeResumable}
		 */
		throw AccountResult.FEATURE_NOT_IMPLEMENTED.exception();
	}
	
	@Override
	public AccountCredentials authorize( String acctId, Object... creds )
	{
		if ( creds.length < 1 || ! ( creds[0] instanceof String ) )
			throw AccountResult.INTERNAL_ERROR.exception();
		
		String pass = ( String ) creds[0];
		
		if ( acctId == null || acctId.isEmpty() )
			throw new AccountException( AccountResult.EMPTY_USERNAME );
		
		if ( pass == null || pass.isEmpty() )
			throw new AccountException( AccountResult.EMPTY_PASSWORD );
		
		AccountMeta meta = AccountManager.INSTANCE.getAccountWithException( acctId );
		
		if ( meta == null )
			throw AccountResult.INCORRECT_LOGIN.exception();
		
		// TODO Save passwords elsewhere
		String password = meta.getString( "password" );
		
		if ( password == null )
			throw new AccountException( AccountResult.UNCONFIGURED );
		
		// TODO Encrypt all passwords
		if ( password.equals( pass ) || password.equals( DigestUtils.md5Hex( pass ) ) || DigestUtils.md5Hex( password ).equals( pass ) )
			return new PlainTextAccountCredentials( AccountResult.LOGIN_SUCCESS, meta );
		else
			throw new AccountException( AccountResult.INCORRECT_LOGIN );
	}
	
	class PlainTextAccountCredentials extends AccountCredentials
	{
		PlainTextAccountCredentials( AccountResult result, AccountMeta meta )
		{
			super( PlainTextAccountAuthenticator.this, result, meta );
		}
	}
}
