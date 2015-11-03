/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.account.auth;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.Validate;

import com.chiorichan.account.AccountAttachment;
import com.chiorichan.account.AccountMeta;
import com.chiorichan.account.AccountPermissible;
import com.chiorichan.account.lang.AccountException;
import com.google.common.collect.Lists;

/**
 * References available Account Authenticators
 */
public abstract class AccountAuthenticator
{
	/**
	 * Holds reference to loaded Account Authenticators
	 */
	private static final List<AccountAuthenticator> authenticators = Lists.newArrayList();
	
	/**
	 * Typically only used for authenticating the NONE login
	 * This will fail for all other logins
	 */
	public static final NullAccountAuthenticator NULL = new NullAccountAuthenticator();
	
	/**
	 * Used to authenticate any Account that supports plain text passwords
	 */
	public static final PlainTextAccountAuthenticator PASSWORD = new PlainTextAccountAuthenticator();
	
	/**
	 * Typically only used to authenticate relogins, for security, token will change with each successful auth
	 */
	public static final OnetimeTokenAccountAuthenticator TOKEN = new OnetimeTokenAccountAuthenticator();
	
	public static List<AccountAuthenticator> getAuthenticators()
	{
		return Collections.unmodifiableList( authenticators );
	}
	
	@SuppressWarnings( "unchecked" )
	public static <T extends AccountAuthenticator> T byName( String name )
	{
		Validate.notEmpty( name );
		
		for ( AccountAuthenticator aa : authenticators )
			if ( name.equalsIgnoreCase( aa.name ) )
				return ( T ) aa;
		return null;
	}
	
	private String name;
	
	AccountAuthenticator( String name )
	{
		this.name = name;
		authenticators.add( this );
	}
	
	/**
	 * Used to resume a saved session login
	 * 
	 * @param acct
	 *            The Account Meta
	 * @param perm
	 *            An instance of the {@link AccountAttachment}
	 * @return
	 */
	public abstract AccountCredentials authorize( AccountMeta acct, AccountPermissible perm ) throws AccountException;
	
	/**
	 * Used to check Account Credentials prior to creating the Account Instance
	 * 
	 * @param acct
	 *            The Account Meta
	 * @param creds
	 *            The Credentials to use for authentication
	 * @return An instance of the Account Credentials
	 */
	public abstract AccountCredentials authorize( AccountMeta acct, Object... creds ) throws AccountException;
}
