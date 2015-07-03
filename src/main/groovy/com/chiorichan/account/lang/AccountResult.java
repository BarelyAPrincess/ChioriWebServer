/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.account.lang;

import com.chiorichan.account.Account;
import com.chiorichan.account.AccountMeta;

/**
 * Provides easy exception reasons for account and session issues.
 * TODO Format the messages for use with the format method
 */
public class AccountResult
{
	public static final AccountResult LOGOUT_SUCCESS = new AccountResult( "You have been successfully logged out." );
	public static final AccountResult LOGIN_SUCCESS = new AccountResult( "Your login has been successfully authenticated." );
	
	public static final AccountResult IP_BANNED = new AccountResult( "You are not authorized to connect to this server using this method of entry!", true );
	
	public static final AccountResult UNKNOWN_ERROR = new AccountResult( "Your login has failed due to an unknown internal error, Please try again.", true );
	public static final AccountResult PERMISSION_ERROR = new AccountResult( "Fatal error was detected with your account permissions. Please notify an administrator ASAP.", true );
	public static final AccountResult INTERNAL_ERROR = new AccountResult( "Internal Server Error was encountered while attempting to process login.", true );
	
	public static final AccountResult ACCOUNT_NOT_INITIALIZED = new AccountResult( "That Account was not initialized, i.e., no logins are present in this state." );
	public static final AccountResult ACCOUNT_NOT_ACTIVATED = new AccountResult( "That account is not activated.", true );
	public static final AccountResult ACCOUNT_NOT_WHITELISTED = new AccountResult( "You are not whitelisted on this server.", true );
	public static final AccountResult ACCOUNT_BANNED = new AccountResult( "You are banned on this server. THE BAN HAMMER HAS SPOKEN!", true );
	public static final AccountResult ACCOUNT_EXISTS = new AccountResult( "The username specified is already in use. Please try a different username." );
	
	public static final AccountResult FEATURE_DISABLED = new AccountResult( "The requested feature is disallowed on this server!" );
	public static final AccountResult FEATURE_NOT_IMPLEMENTED = new AccountResult( "The requested feature has not been implemented. Try a different version." );
	
	public static final AccountResult UNCONFIGURED = new AccountResult( "The Accounts Manager is unconfigured." );
	public static final AccountResult UNDER_ATTACK = new AccountResult( "Max fail login tries reached. Account temporarily locked.", true );
	public static final AccountResult CANCELLED_BY_EVENT = new AccountResult( "Your login has been cancelled by an internal event for unknown reason, check logs.", true );
	
	public static final AccountResult EMPTY_USERNAME = new AccountResult( "The specified username was empty or null.", true );
	public static final AccountResult EMPTY_PASSWORD = new AccountResult( "The specified password was empty or null.", true );
	public static final AccountResult EMPTY_ACCTID = new AccountResult( "The specified Account Id was empty or null.", true );
	
	public static final AccountResult INCORRECT_LOGIN = new AccountResult( "Username and/or Password provided did not match any accounts on file.", true );
	public static final AccountResult EXPIRED_LOGIN = new AccountResult( "The provided login credentials were marked as expired.", true );
	public static final AccountResult PASSWORD_UNSET = new AccountResult( "The specified Account has no password set, either the password was never set or the account uses another form of authentication." );
	
	public static final AccountResult SUCCESS = new AccountResult( "The requested action was completed successfully!" );
	
	public static final AccountResult DEFAULT = new AccountResult( "There was no offical result returned." );
	
	private String msg;
	private boolean error;
	private Account acct = null;
	private Throwable cause = null;
	
	AccountResult( String msg )
	{
		this( msg, false );
	}
	
	/**
	 * @param msg
	 *            The Result Message
	 * @param error
	 *            Should we stop when this result is returned?
	 */
	AccountResult( String msg, boolean error )
	{
		this.msg = msg;
		this.error = error;
	}
	
	public AccountException exception()
	{
		return new AccountException( this );
	}
	
	public AccountException exception( AccountMeta acct )
	{
		String ip = ( acct.instance().getIpAddresses().size() > 0 ) ? acct.instance().getIpAddresses().toArray( new String[0] )[0] : null;
		return exception( acct.getDisplayName(), ip );
	}
	
	public AccountException exception( Object... args )
	{
		msg = String.format( msg, args );
		return new AccountException( this );
	}
	
	public AccountException exception( String msg )
	{
		this.msg = msg;
		return new AccountException( this );
	}
	
	public String format( Object... args )
	{
		msg = String.format( msg, args );
		return msg;
	}
	
	public Account getAccount()
	{
		return acct;
	}
	
	public String getMessage()
	{
		return msg;
	}
	
	public String getMessage( Object... args )
	{
		return String.format( msg, args );
	}
	
	public Throwable getThrowable()
	{
		return cause;
	}
	
	public boolean hasCause()
	{
		return cause != null;
	}
	
	public boolean isError()
	{
		return error;
	}
	
	public AccountResult setAccount( Account acct )
	{
		this.acct = acct;
		return this;
	}
	
	public AccountResult setThrowable( Throwable cause )
	{
		this.cause = cause;
		return this;
	}
	
	@Override
	public String toString()
	{
		return msg;
	}
}
