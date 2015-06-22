/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.account.auth;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.chiorichan.Loader;
import com.chiorichan.account.AccountManager;
import com.chiorichan.account.AccountMeta;
import com.chiorichan.account.AccountPermissible;
import com.chiorichan.account.lang.AccountException;
import com.chiorichan.account.lang.AccountResult;
import com.chiorichan.database.DatabaseEngine;
import com.chiorichan.tasks.Timings;
import com.chiorichan.util.RandomFunc;

/**
 * Used to authenticate an account using an Account Id and Token combination
 */
public class OnetimeTokenAccountAuthenticator extends AccountAuthenticator
{
	private final DatabaseEngine db = Loader.getDatabase();
	
	OnetimeTokenAccountAuthenticator()
	{
		super( "token" );
		
		if ( !db.tableExist( "accounts_token" ) )
		{
			try
			{
				db.queryUpdate( "CREATE TABLE `accounts_token` ( `acctId` varchar(255) NOT NULL, `token` varchar(255) NOT NULL, `expires` int(12) NOT NULL);" );
			}
			catch ( SQLException e )
			{
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public AccountCredentials authorize( String acctId, AccountPermissible perm )
	{
		String token = perm.getVariable( "token" );
		
		if ( token == null )
			throw new AccountException( "The account, '" + acctId + "', was not made resumable using the token method." );
		
		return authorize( acctId, token );
	}
	
	@Override
	public AccountCredentials authorize( String acctId, Object... creds )
	{
		if ( creds.length < 1 || ! ( creds[0] instanceof String ) )
			throw AccountResult.INTERNAL_ERROR.exception();
		
		String token = ( String ) creds[0];
		
		try
		{
			// TODO Getting Account Meta is not always required. We should implement it that Meta is auto got before hand.
			AccountMeta meta = AccountManager.INSTANCE.getAccountWithException( acctId );
			
			if ( meta == null )
				throw AccountResult.INCORRECT_LOGIN.exception();
			
			ResultSet rs = db.query( "SELECT * FROM `accounts_token` WHERE `acctId` = '" + acctId + "' AND `token` = '" + token + "';" );
			
			if ( rs == null || db.getRowCount( rs ) < 1 )
				throw AccountResult.INCORRECT_LOGIN.exception();
			
			if ( rs.getInt( "expires" ) > 0 && rs.getInt( "expires" ) < Timings.epoch() )
				throw AccountResult.EXPIRED_LOGIN.exception();
			
			String token0 = rs.getString( "token" );
			
			if ( token0 == null || token0.isEmpty() )
				throw AccountResult.INCORRECT_LOGIN.exception();
			
			if ( token0.equals( token ) )
			{
				db.queryUpdate( "DELETE FROM `accounts_token` WHERE `acctId` = '" + acctId + "' AND `token` = '" + token + "';" );
				return new OnetimeTokenAccountCredentials( AccountResult.LOGIN_SUCCESS, meta, token );
			}
			else
				throw AccountResult.INCORRECT_LOGIN.exception();
		}
		catch ( SQLException e )
		{
			throw AccountResult.INTERNAL_ERROR.setThrowable( e ).exception( acctId );
		}
		
		
	}
	
	/**
	 * Used to issue new Login Tokens not only to resume our logins but to resume other Authenticator's logins.
	 * 
	 * @param acct
	 *            The Account to issue a Token to
	 * @return The issued token, be sure to save the token the authenticate using this Authenticator later
	 */
	public String issueToken( AccountMeta acct )
	{
		String token = RandomFunc.randomize( acct.getAcctId() ) + Timings.epoch();
		try
		{
			db.queryUpdate( "INSERT INTO `accounts_token` (`acctId`,`token`,`expires`) VALUES ('" + acct.getAcctId() + "','" + token + "','" + ( Timings.epoch() + ( 60 * 60 * 24 * 7 ) ) + "');" );
		}
		catch ( SQLException e )
		{
			e.printStackTrace();
			return null;
		}
		return token;
	}
	
	class OnetimeTokenAccountCredentials extends AccountCredentials
	{
		private String token;
		
		OnetimeTokenAccountCredentials( AccountResult result, AccountMeta meta, String token )
		{
			super( OnetimeTokenAccountAuthenticator.this, result, meta );
			this.token = token;
		}
		
		public String getToken()
		{
			return token;
		}
	}
}
