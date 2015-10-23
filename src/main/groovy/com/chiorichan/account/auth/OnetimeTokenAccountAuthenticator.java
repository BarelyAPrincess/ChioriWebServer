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

import com.chiorichan.LogColor;
import com.chiorichan.Loader;
import com.chiorichan.account.AccountManager;
import com.chiorichan.account.AccountMeta;
import com.chiorichan.account.AccountPermissible;
import com.chiorichan.account.lang.AccountException;
import com.chiorichan.account.lang.AccountDescriptiveReason;
import com.chiorichan.datastore.sql.bases.SQLDatastore;
import com.chiorichan.datastore.sql.query.SQLQuerySelect;
import com.chiorichan.lang.ReportingLevel;
import com.chiorichan.tasks.TaskManager;
import com.chiorichan.tasks.Timings;
import com.chiorichan.util.RandomFunc;

/**
 * Used to authenticate an account using an Account Id and Token combination
 */
public class OnetimeTokenAccountAuthenticator extends AccountAuthenticator
{
	class OnetimeTokenAccountCredentials extends AccountCredentials
	{
		private String token;
		
		OnetimeTokenAccountCredentials( AccountDescriptiveReason reason, AccountMeta meta, String token )
		{
			super( OnetimeTokenAccountAuthenticator.this, reason, meta );
			this.token = token;
		}
		
		public String getToken()
		{
			return token;
		}
	}
	
	private final SQLDatastore db = Loader.getDatabase();
	
	OnetimeTokenAccountAuthenticator()
	{
		super( "token" );
		
		try
		{
			if ( !db.table( "account_token" ).exists() )
				db.table( "account_token" ).addColumnVar( "acctId", 255 ).addColumnVar( "token", 255 ).addColumnInt( "expires", 12 );
			// db.queryUpdate( "CREATE TABLE `accounts_token` ( `acctId` varchar(255) NOT NULL, `token` varchar(255) NOT NULL, `expires` int(12) NOT NULL);" );
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
					// int deleted = db.queryUpdate( "DELETE FROM `accounts_token` WHERE `expires` > 0 AND `expires` < ?", Timings.epoch() );
					int deleted = db.table( "accounts_token" ).delete().where( "expires" ).moreThan( 0 ).and().where( "expires" ).lessThan( Timings.epoch() ).execute().rowCount();
					if ( deleted > 0 )
						AccountManager.getLogger().info( LogColor.DARK_AQUA + "The cleanup task deleted " + deleted + " expired login token(s)." );
				}
				catch ( SQLException e )
				{
					e.printStackTrace();
				}
			}
		} );
	}
	
	@Override
	public AccountCredentials authorize( AccountMeta acct, AccountPermissible perm ) throws AccountException
	{
		if ( acct == null )
			throw new AccountException( AccountDescriptiveReason.INCORRECT_LOGIN, acct );
		
		String token = perm.getVariable( "token" );
		
		if ( token == null )
			throw new AccountException( new AccountDescriptiveReason( "The account '" + acct.getId() + "' has no resumable login using the token method.", ReportingLevel.L_ERROR ), acct );
		
		return authorize( acct, token );
	}
	
	@Override
	public AccountCredentials authorize( AccountMeta acct, Object... creds ) throws AccountException
	{
		if ( acct == null )
			throw new AccountException( AccountDescriptiveReason.INCORRECT_LOGIN, acct );
		
		if ( creds[0] instanceof AccountPermissible )
			return authorize( acct, ( AccountPermissible ) creds[0] );
		
		if ( creds.length == 0 || ! ( creds[0] instanceof String ) )
			throw new AccountException( AccountDescriptiveReason.INTERNAL_ERROR, acct );
		
		String acctId = acct.getId();
		String token = ( String ) creds[0];
		
		try
		{
			if ( token == null || token.isEmpty() )
				throw new AccountException( AccountDescriptiveReason.EMPTY_CREDENTIALS, acct );
			
			SQLQuerySelect select = db.table( "accounts_token" ).select().where( "acctId" ).matches( acctId ).and().where( "token" ).matches( token ).limit( 1 ).execute();
			
			if ( select.rowCount() == 0 )
				throw new AccountException( AccountDescriptiveReason.INCORRECT_LOGIN, acct );
			// throw AccountResult.INCORRECT_LOGIN.setMessage( "The provided token did not match any saved tokens" + ( Versioning.isDevelopment() ? ", token: " + token : "." ) ).exception();
			
			ResultSet rs = select.result();
			
			if ( rs.getInt( "expires" ) >= 0 && rs.getInt( "expires" ) < Timings.epoch() )
				throw new AccountException( AccountDescriptiveReason.EXPIRED_LOGIN, acct );
			
			// deleteToken( acctId, token );
			expireToken( acctId, token );
			return new OnetimeTokenAccountCredentials( AccountDescriptiveReason.LOGIN_SUCCESS, acct, token );
		}
		catch ( SQLException e )
		{
			throw new AccountException( AccountDescriptiveReason.INTERNAL_ERROR, e, acct );
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
			return db.table( "accounts_token" ).delete().where( "acctId" ).matches( acctId ).and().where( "token" ).matches( token ).execute().rowCount() > 0;
		}
		catch ( SQLException e )
		{
			return false;
		}
	}
	
	/**
	 * Expires the provided token from database
	 * 
	 * @param acctId
	 *            The acctId associated with Token
	 * @param token
	 *            The login token
	 */
	private boolean expireToken( String acctId, String token )
	{
		Validate.notNull( acctId );
		Validate.notNull( token );
		
		try
		{
			return db.table( "accounts_token" ).update().value( "expires", 0 ).where( "acctId" ).matches( acctId ).and().where( "token" ).matches( token ).execute().rowCount() > 0;
		}
		catch ( SQLException e )
		{
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
			// if ( db.queryUpdate( "INSERT INTO `accounts_token` (`acctId`,`token`,`expires`) VALUES (?,?,?);", acct.getId(), token, ( Timings.epoch() + ( 60 * 60 * 24 * 7 ) ) ) < 1 )
			if ( db.table( "accounts_token" ).insert().value( "acctId", acct.getId() ).value( "token", token ).value( "expires", ( Timings.epoch() + ( 60 * 60 * 24 * 7 ) ) ).execute().rowCount() < 0 )
			{
				AccountManager.getLogger().severe( "We had an unknown issue inserting token '" + token + "' into the database!" );
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
