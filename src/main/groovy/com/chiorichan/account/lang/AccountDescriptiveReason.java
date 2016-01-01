/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.account.lang;

import com.chiorichan.lang.ReportingLevel;

/**
 *
 */
public class AccountDescriptiveReason
{
	public static final AccountDescriptiveReason LOGOUT_SUCCESS = new AccountDescriptiveReason( "You have been successfully logged out.", ReportingLevel.L_SUCCESS );
	public static final AccountDescriptiveReason LOGIN_SUCCESS = new AccountDescriptiveReason( "Your login has been successfully authenticated.", ReportingLevel.L_SUCCESS );

	public static final AccountDescriptiveReason IP_BANNED = new AccountDescriptiveReason( "You are not authorized to connect to this server using this method of entry!", ReportingLevel.L_SECURITY );

	public static final AccountDescriptiveReason UNKNOWN_ERROR = new AccountDescriptiveReason( "Your login has failed due to an unknown internal error, please try again or contact an administrator ASAP.", ReportingLevel.E_ERROR );
	public static final AccountDescriptiveReason PERMISSION_ERROR = new AccountDescriptiveReason( "Fatal level was detected with your account permissions. Please notify an administrator ASAP.", ReportingLevel.L_PERMISSION );
	public static final AccountDescriptiveReason INTERNAL_ERROR = new AccountDescriptiveReason( "Internal Server Error was encountered while attempting to process login.", ReportingLevel.E_ERROR );
	public static final AccountDescriptiveReason UNAUTHORIZED = new AccountDescriptiveReason( "You are unauthorized to access this server resource.", ReportingLevel.L_DENIED );
	public static final AccountDescriptiveReason NONCE_REQUIRED = new AccountDescriptiveReason( "Your login failed the NONCE validation.", ReportingLevel.L_SECURITY );

	public static final AccountDescriptiveReason ACCOUNT_NOT_INITIALIZED = new AccountDescriptiveReason( "That Account was not initialized, i.e., no logins are present in this state.", ReportingLevel.L_ERROR );
	public static final AccountDescriptiveReason ACCOUNT_NOT_ACTIVATED = new AccountDescriptiveReason( "That account is not activated.", ReportingLevel.L_DENIED );
	public static final AccountDescriptiveReason ACCOUNT_NOT_WHITELISTED = new AccountDescriptiveReason( "You are not whitelisted on this server.", ReportingLevel.L_SECURITY );
	public static final AccountDescriptiveReason ACCOUNT_BANNED = new AccountDescriptiveReason( "You are banned on this server. THE BAN HAMMER HAS SPOKEN!", ReportingLevel.L_SECURITY );
	public static final AccountDescriptiveReason ACCOUNT_EXISTS = new AccountDescriptiveReason( "The username specified is already in use. Please try a different username.", ReportingLevel.L_DENIED );

	public static final AccountDescriptiveReason FEATURE_DISABLED = new AccountDescriptiveReason( "The requested feature is disallowed on this server!", ReportingLevel.L_ERROR );
	public static final AccountDescriptiveReason FEATURE_NOT_IMPLEMENTED = new AccountDescriptiveReason( "The requested feature has not been implemented per this version.", ReportingLevel.L_ERROR );

	public static final AccountDescriptiveReason UNCONFIGURED = new AccountDescriptiveReason( "The Accounts Manager is unconfigured.", ReportingLevel.L_ERROR );
	public static final AccountDescriptiveReason UNDER_ATTACK = new AccountDescriptiveReason( "Max fail login tries reached. Account temporarily locked.", ReportingLevel.L_SECURITY );
	public static final AccountDescriptiveReason CANCELLED_BY_EVENT = new AccountDescriptiveReason( "Your login has been cancelled by an internal event for unknown reason, check logs.", ReportingLevel.L_ERROR );

	public static final AccountDescriptiveReason EMPTY_USERNAME = new AccountDescriptiveReason( "The specified username was empty or null.", ReportingLevel.L_ERROR );
	public static final AccountDescriptiveReason EMPTY_CREDENTIALS = new AccountDescriptiveReason( "The specified password (or credentials) was empty or null.", ReportingLevel.L_ERROR );
	public static final AccountDescriptiveReason EMPTY_ACCTID = new AccountDescriptiveReason( "The specified Account Id was empty or null.", ReportingLevel.L_ERROR );

	public static final AccountDescriptiveReason INCORRECT_LOGIN = new AccountDescriptiveReason( "There were no accounts that matched the provided credentials.", ReportingLevel.L_DENIED );
	public static final AccountDescriptiveReason EXPIRED_LOGIN = new AccountDescriptiveReason( "The provided login credentials were marked as expired.", ReportingLevel.L_EXPIRED );
	public static final AccountDescriptiveReason PASSWORD_UNSET = new AccountDescriptiveReason( "The specified Account has no password set, either the password was never set or the account uses another form of authentication.", ReportingLevel.L_DENIED );

	public static final AccountDescriptiveReason SUCCESS = new AccountDescriptiveReason( "The requested action was completed successfully!", ReportingLevel.L_SUCCESS );

	public static final AccountDescriptiveReason DEFAULT = new AccountDescriptiveReason( "There was no result returned.", ReportingLevel.L_DEFAULT );

	private final String reason;
	private final ReportingLevel level;

	public AccountDescriptiveReason( String reason, ReportingLevel level )
	{
		this.reason = reason;
		this.level = level;
	}

	public String getMessage()
	{
		return reason;
	}

	public ReportingLevel getReportingLevel()
	{
		return level;
	}
}
