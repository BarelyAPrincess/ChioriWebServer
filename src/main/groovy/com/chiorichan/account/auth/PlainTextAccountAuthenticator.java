/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.account.auth;

import java.sql.SQLException;

import org.apache.commons.codec.digest.DigestUtils;

import com.chiorichan.Loader;
import com.chiorichan.account.AccountManager;
import com.chiorichan.account.AccountMeta;
import com.chiorichan.account.AccountPermissible;
import com.chiorichan.account.lang.AccountException;
import com.chiorichan.account.lang.AccountResult;
import com.chiorichan.datastore.sql.SQLExecute;
import com.chiorichan.datastore.sql.bases.SQLDatastore;
import com.chiorichan.datastore.sql.query.SQLQuerySelect;
import com.chiorichan.tasks.Timings;

/**
 * Used to authenticate an account using a Username and Password combination
 */
public final class PlainTextAccountAuthenticator extends AccountAuthenticator
{
	class PlainTextAccountCredentials extends AccountCredentials
	{
		PlainTextAccountCredentials( AccountResult result, AccountMeta meta )
		{
			super( PlainTextAccountAuthenticator.this, result, meta );
		}
	}
	
	private final SQLDatastore db = Loader.getDatabase();
	
	PlainTextAccountAuthenticator()
	{
		super( "plaintext" );
		
		try
		{
			if ( !db.table( "accounts_plaintext" ).exists() )
				db.table( "accounts_plaintext" ).addColumnVar( "acctId", 255 ).addColumnVar( "password", 255 ).addColumnInt( "expires", 12 );
		}
		catch ( SQLException e )
		{
			e.printStackTrace();
		}
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
			throw AccountResult.EMPTY_USERNAME.exception();
		
		if ( pass == null || pass.isEmpty() )
			throw AccountResult.EMPTY_PASSWORD.exception();
		
		AccountMeta meta = AccountManager.INSTANCE.getAccountWithException( acctId );
		
		if ( meta == null )
			throw AccountResult.INCORRECT_LOGIN.exception();
		
		String password = null;
		try
		{
			SQLExecute<SQLQuerySelect> select = db.table( "accounts_plaintext" ).select().where( "acctId" ).matches( acctId ).limit( 1 ).execute().result();
			// ResultSet rs = db.query( "SELECT * FROM `accounts_plaintext` WHERE `acctId` = '" + acctId + "' LIMIT 1;" );
			
			if ( select.rowCount() < 1 )
				throw AccountResult.PASSWORD_UNSET.format( meta ).exception();
			
			if ( select.getInt( "expires" ) > -1 && select.getInt( "expires" ) < Timings.epoch() )
				throw AccountResult.EXPIRED_LOGIN.format( meta ).exception();
			
			password = select.getString( "password" );
		}
		catch ( AccountException e )
		{
			if ( meta.getString( "password" ) != null && !meta.getString( "password" ).isEmpty() )
			{
				password = meta.getString( "password" );
				setPassword( meta, password, -1 );
				meta.set( "password", null );
			}
			else
				throw e;
		}
		catch ( SQLException e )
		{
			throw AccountResult.INTERNAL_ERROR.setThrowable( e ).format( meta ).exception();
		}
		
		// TODO Encrypt all passwords
		if ( password.equals( pass ) || password.equals( DigestUtils.md5Hex( pass ) ) || DigestUtils.md5Hex( password ).equals( pass ) )
			return new PlainTextAccountCredentials( AccountResult.LOGIN_SUCCESS, meta );
		else
			throw new AccountException( AccountResult.INCORRECT_LOGIN );
	}
	
	/**
	 * Similar to {@link #setPassword(AccountMeta, String, int)} except password never expires
	 */
	public void setPassword( AccountMeta acct, String password )
	{
		setPassword( acct, password, -1 );
	}
	
	/**
	 * Sets the Account Password which is stored in a separate table for security
	 * 
	 * @param acct
	 *            The Account to set password for
	 * @param password
	 *            The password to set
	 * @param expires
	 *            The password expiration. Use -1 for no expiration
	 * @return True if we successfully set the password
	 */
	public boolean setPassword( AccountMeta acct, String password, int expires )
	{
		try
		{
			if ( db.table( "accounts_plaintext" ).insert().value( "acctId", acct.getId() ).value( "password", password ).value( "expires", expires ).execute().rowCount() < 0 )
			{
				AccountManager.getLogger().severe( "We had an unknown issue inserting password for acctId '" + acct.getId() + "' into the database!" );
				return false;
			}
			
			// db.queryUpdate( "INSERT INTO `accounts_plaintext` (`acctId`,`password`,`expires`) VALUES ('" + acct.getId() + "','" + password + "','" + expires + "');" );
		}
		catch ( SQLException e )
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
