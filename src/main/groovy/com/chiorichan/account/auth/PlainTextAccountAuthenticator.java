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

import org.apache.commons.codec.digest.DigestUtils;

import com.chiorichan.Loader;
import com.chiorichan.account.AccountManager;
import com.chiorichan.account.AccountMeta;
import com.chiorichan.account.AccountPermissible;
import com.chiorichan.account.lang.AccountException;
import com.chiorichan.account.lang.AccountResult;
import com.chiorichan.database.DatabaseEngine;
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
	
	private final DatabaseEngine db = Loader.getDatabase();
	
	PlainTextAccountAuthenticator()
	{
		super( "plaintext" );
		
		if ( !db.tableExist( "accounts_plaintext" ) )
			try
			{
				db.queryUpdate( "CREATE TABLE `accounts_plaintext` ( `acctId` varchar(255) NOT NULL, `password` varchar(255) NOT NULL, `expires` int(12) NOT NULL);" );
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
			ResultSet rs = db.query( "SELECT * FROM `accounts_plaintext` WHERE `acctId` = '" + acctId + "' LIMIT 1;" );
			
			if ( rs == null || db.getRowCount( rs ) < 1 )
				throw AccountResult.PASSWORD_UNSET.exception( meta );
			
			if ( rs.getInt( "expires" ) > -1 && rs.getInt( "expires" ) < Timings.epoch() )
				throw AccountResult.EXPIRED_LOGIN.exception( meta );
			
			password = rs.getString( "password" );
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
			throw AccountResult.INTERNAL_ERROR.setThrowable( e ).exception( meta );
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
			db.queryUpdate( "INSERT INTO `accounts_plaintext` (`acctId`,`password`,`expires`) VALUES ('" + acct.getId() + "','" + password + "','" + expires + "');" );
			return true;
		}
		catch ( SQLException e )
		{
			e.printStackTrace();
			return false;
		}
	}
}
