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

import org.apache.commons.lang3.Validate;

import com.chiorichan.ConsoleColor;
import com.chiorichan.Loader;
import com.chiorichan.account.AccountManager;
import com.chiorichan.account.AccountMeta;
import com.chiorichan.account.AccountPermissible;
import com.chiorichan.account.lang.AccountException;
import com.chiorichan.account.lang.AccountResult;
import com.chiorichan.database.DatabaseEngine;
import com.chiorichan.tasks.TaskManager;
import com.chiorichan.tasks.Timings;
import com.chiorichan.util.RandomFunc;
import com.chiorichan.util.Versioning;

/**
 * Used to authenticate an account using an Account Id and Token combination
 */
public class OnetimeTokenAccountAuthenticator extends AccountAuthenticator
{
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
	
	private final DatabaseEngine db = Loader.getDatabase();
	
	OnetimeTokenAccountAuthenticator()
	{
		super( "token" );
		
		if ( !db.tableExist( "accounts_token" ) )
			try
			{
				db.queryUpdate( "CREATE TABLE `accounts_token` ( `acctId` varchar(255) NOT NULL, `token` varchar(255) NOT NULL, `expires` int(12) NOT NULL);" );
			}
			catch ( SQLException e )
			{
				e.printStackTrace();
			}
		
		TaskManager.INSTANCE.scheduleAsyncRepeatingTask( AccountManager.INSTANCE, 0L, Timings.TICK_MINUTE * Loader.getConfig().getInt( "sessions.cleanupInterval", 5 ), new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					int deleted = db.queryUpdate( "DELETE FROM `accounts_token` WHERE `expires` > 0 AND `expires` < ?", Timings.epoch() );
					if ( deleted > 0 )
						AccountManager.getLogger().info( ConsoleColor.DARK_AQUA + "The cleanup task deleted " + deleted + " expired login token(s)." );
				}
				catch ( SQLException e )
				{
					e.printStackTrace();
				}
			}
		} );
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
		if ( creds.length == 0 || ! ( creds[0] instanceof String ) )
			throw AccountResult.INTERNAL_ERROR.exception();
		
		String token = ( String ) creds[0];
		
		try
		{
			if ( token == null || token.isEmpty() )
				throw AccountResult.INTERNAL_ERROR.format( "There was an internal error authorizing the provided token." ).exception();
			
			AccountMeta meta = AccountManager.INSTANCE.getAccountWithException( acctId );
			
			if ( meta == null )
				throw AccountResult.INCORRECT_LOGIN.exception();
			
			ResultSet rs = db.query( "SELECT * FROM `accounts_token` WHERE `acctId` = '" + acctId + "' AND `token` = '" + token + "' LIMIT 1;" );
			
			if ( rs == null || db.getRowCount( rs ) < 1 )
				throw AccountResult.INCORRECT_LOGIN.setMessage( "The provided token did not match any saved tokens" + ( Versioning.isDevelopment() ? ", token: " + token : "." ) ).exception();
			
			if ( rs.getInt( "expires" ) > 0 && rs.getInt( "expires" ) < Timings.epoch() )
				throw AccountResult.EXPIRED_LOGIN.exception();
			
			deleteToken( acctId, token );
			return new OnetimeTokenAccountCredentials( AccountResult.LOGIN_SUCCESS, meta, token );
		}
		catch ( SQLException e )
		{
			throw AccountResult.INTERNAL_ERROR.setThrowable( e ).format( acctId ).exception();
		}
	}
	
	/**
	 * Deletes provided token from database
	 * 
	 * @param acctId
	 *            The acctId associated with Token
	 * @param token
	 *            The login token
	 */
	public boolean deleteToken( String acctId, String token )
	{
		Validate.notNull( acctId );
		Validate.notNull( token );
		
		try
		{
			return db.queryUpdate( "DELETE FROM `accounts_token` WHERE `acctId` = ? AND `token` = ? LIMIT 1;", acctId, token ) > 0;
		}
		catch ( SQLException e )
		{
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Issues a new login token not only to resume logins performed by the {@link OnetimeTokenAccountAuthenticator} but to resume other {@link AccountAuthenticator} logins.
	 * 
	 * @param acct
	 *            The Account to issue a Token to
	 * @return The issued token, be sure to save the token, authenticate with the token later. Token is valid for 7 days.
	 */
	public String issueToken( AccountMeta acct )
	{
		Validate.notNull( acct );
		
		String token = RandomFunc.randomize( acct.getId() ) + Timings.epoch();
		try
		{
			if ( db.queryUpdate( "INSERT INTO `accounts_token` (`acctId`,`token`,`expires`) VALUES (?,?,?);", acct.getId(), token, ( Timings.epoch() + ( 60 * 60 * 24 * 7 ) ) ) < 1 )
			{
				AccountManager.getLogger().severe( "We had an unknown issue inserting token '" + token + "' in the database!" );
				return null;
			}
		}
		catch ( SQLException e )
		{
			e.printStackTrace();
			return null;
		}
		return token;
	}
}
