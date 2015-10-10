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
import com.chiorichan.lang.ReportingLevel;

/**
 * Provides easy exception reasons for account and session issues.
 */
public class AccountResult
{
	public static final AccountResult LOGOUT_SUCCESS = new AccountResult( "You have been successfully logged out.", ReportingLevel.L_SUCCESS );
	public static final AccountResult LOGIN_SUCCESS = new AccountResult( "Your login has been successfully authenticated.", ReportingLevel.L_SUCCESS );
	
	public static final AccountResult IP_BANNED = new AccountResult( "You are not authorized to connect to this server using this method of entry!", ReportingLevel.L_SECURITY );
	
	public static final AccountResult UNKNOWN_ERROR = new AccountResult( "Your login has failed due to an unknown internal error, please try again or contact an administrator ASAP.", ReportingLevel.E_ERROR );
	public static final AccountResult PERMISSION_ERROR = new AccountResult( "Fatal level was detected with your account permissions. Please notify an administrator ASAP.", ReportingLevel.L_PERMISSION );
	public static final AccountResult INTERNAL_ERROR = new AccountResult( "Internal Server Error was encountered while attempting to process login.", ReportingLevel.E_ERROR );
	
	public static final AccountResult ACCOUNT_NOT_INITIALIZED = new AccountResult( "That Account was not initialized, i.e., no logins are present in this state.", ReportingLevel.L_ERROR );
	public static final AccountResult ACCOUNT_NOT_ACTIVATED = new AccountResult( "That account is not activated.", ReportingLevel.L_DENIED );
	public static final AccountResult ACCOUNT_NOT_WHITELISTED = new AccountResult( "You are not whitelisted on this server.", ReportingLevel.L_SECURITY );
	public static final AccountResult ACCOUNT_BANNED = new AccountResult( "You are banned on this server. THE BAN HAMMER HAS SPOKEN!", ReportingLevel.L_SECURITY );
	public static final AccountResult ACCOUNT_EXISTS = new AccountResult( "The username specified is already in use. Please try a different username.", ReportingLevel.L_DENIED );
	
	public static final AccountResult FEATURE_DISABLED = new AccountResult( "The requested feature is disallowed on this server!", ReportingLevel.L_ERROR );
	public static final AccountResult FEATURE_NOT_IMPLEMENTED = new AccountResult( "The requested feature has not been implemented. Try a different version.", ReportingLevel.L_ERROR );
	
	public static final AccountResult UNCONFIGURED = new AccountResult( "The Accounts Manager is unconfigured.", ReportingLevel.L_ERROR );
	public static final AccountResult UNDER_ATTACK = new AccountResult( "Max fail login tries reached. Account temporarily locked.", ReportingLevel.L_SECURITY );
	public static final AccountResult CANCELLED_BY_EVENT = new AccountResult( "Your login has been cancelled by an internal event for unknown reason, check logs.", ReportingLevel.L_ERROR );
	
	public static final AccountResult EMPTY_USERNAME = new AccountResult( "The specified username was empty or null.", ReportingLevel.L_ERROR );
	public static final AccountResult EMPTY_PASSWORD = new AccountResult( "The specified password was empty or null.", ReportingLevel.L_ERROR );
	public static final AccountResult EMPTY_ACCTID = new AccountResult( "The specified Account Id was empty or null.", ReportingLevel.L_ERROR );
	
	public static final AccountResult INCORRECT_LOGIN = new AccountResult( "There were no accounts that matched the provided credentials.", ReportingLevel.L_DENIED );
	public static final AccountResult EXPIRED_LOGIN = new AccountResult( "The provided login credentials were marked as expired.", ReportingLevel.L_EXPIRED );
	public static final AccountResult PASSWORD_UNSET = new AccountResult( "The specified Account has no password set, either the password was never set or the account uses another form of authentication.", ReportingLevel.L_DENIED );
	
	public static final AccountResult SUCCESS = new AccountResult( "The requested action was completed successfully!", ReportingLevel.L_SUCCESS );
	
	public static final AccountResult DEFAULT = new AccountResult( "There was no result returned." );
	
	private final String msg;
	private final ReportingLevel level;
	private final Account acct;
	private final Throwable cause;
	private final AccountResult orig;
	
	AccountResult( String msg )
	{
		this( msg, ReportingLevel.L_DEFAULT );
	}
	
	/**
	 * @param msg
	 *            The Result Message
	 * @param level
	 *            Is this AccountResult critical?
	 */
	AccountResult( String msg, ReportingLevel level )
	{
		this.msg = msg;
		this.level = level;
		acct = null;
		cause = null;
		orig = this;
	}
	
	AccountResult( String msg, ReportingLevel level, Account acct, Throwable cause, AccountResult orig )
	{
		this.msg = msg;
		this.level = level;
		this.acct = acct;
		this.cause = cause;
		this.orig = orig;
	}
	
	@Override
	public boolean equals( Object result )
	{
		return orig == result;
	}
	
	public AccountException exception()
	{
		return new AccountException( this );
	}
	
	public AccountResult format( AccountMeta acct )
	{
		String ip = ( acct.instance().getIpAddresses().size() > 0 ) ? acct.instance().getIpAddresses().toArray( new String[0] )[0] : null;
		format( acct.getDisplayName(), ip );
		return this;
	}
	
	public AccountResult format( Object... args )
	{
		return new AccountResult( String.format( msg, args ), level, acct, cause, orig );
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
	
	public boolean isIgnorable()
	{
		return level.isIgnorable();
	}
	
	public boolean isSuccess()
	{
		return level == ReportingLevel.L_SUCCESS;
	}
	
	public ReportingLevel level()
	{
		return level;
	}
	
	public AccountResult setAccount( Account acct )
	{
		return new AccountResult( msg, level, acct, cause, orig );
	}
	
	public AccountResult setError( ReportingLevel level )
	{
		return new AccountResult( msg, level, acct, cause, orig );
	}
	
	public AccountResult setMessage( String msg )
	{
		return new AccountResult( msg, level, acct, cause, orig );
	}
	
	public AccountResult setThrowable( Throwable cause )
	{
		return new AccountResult( msg, level, acct, cause, orig );
	}
	
	@Override
	public String toString()
	{
		return msg;
	}
}
