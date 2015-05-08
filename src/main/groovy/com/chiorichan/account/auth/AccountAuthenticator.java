/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.account.auth;


/**
 * References available Account Authenticators
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public abstract class AccountAuthenticator
{
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
}
