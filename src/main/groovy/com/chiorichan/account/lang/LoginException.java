/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.account;

/**
 * Used to pass login errors to the requester.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class LoginException extends Exception
{
	private static final long serialVersionUID = 5522301956671473324L;
	
	private Account acct = null;
	private LoginExceptionReason reason = null;
	
	public LoginException( Exception e )
	{
		super( e );
		reason = LoginExceptionReason.customReason.setReason( e.getMessage() );
	}
	
	public LoginException( LoginExceptionReason reason )
	{
		this( reason, null );
	}
	
	public LoginException( LoginExceptionReason reason, Account acct )
	{
		super( reason.getReason() );
		
		this.reason = reason;
		this.acct = acct;
	}
	
	public static LoginExceptionReason customExceptionReason( String reason )
	{
		return LoginExceptionReason.customReason.setReason( reason );
	}
	
	public LoginExceptionReason getReason()
	{
		return reason;
	}
	
	public Account getAccount()
	{
		return acct;
	}
	
	public void setAccount( Account acct )
	{
		this.acct = acct;
	}
}
