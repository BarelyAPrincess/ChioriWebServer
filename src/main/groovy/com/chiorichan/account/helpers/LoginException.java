/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.account.helpers;

import com.chiorichan.account.bases.Account;

public class LoginException extends Exception
{
	private static final long serialVersionUID = 5522301956671473324L;
	private Account acct = null;
	
	public LoginException(Exception e)
	{
		super( e );
	}
	
	public LoginException(LoginExceptionReasons reason, Account _acct)
	{
		this( reason );
		acct = _acct;
	}
	
	public LoginException(LoginExceptionReasons reason)
	{
		super( reason.getReason() );
	}
	
	public static LoginExceptionReasons customExceptionReason( String reason )
	{
		return LoginExceptionReasons.customReason.setReason( reason );
	}
	
	public Account getAccount()
	{
		return acct;
	}

	public LoginException setAccount( Account _acct )
	{
		acct = _acct;
		return this;
	}
}
