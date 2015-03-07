/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.account;

public enum LoginExceptionReason
{
	unconfigured( "The Accounts Manager is unconfigured." ), accountNotActivated( "Account is not activated." ), underAttackPleaseWait( "Max fail login tries reached. Account locked for 30 minutes." ), emptyUsername( "The specified username was empty. Please try again." ), emptyPassword( "The specified password was empty. Please try again." ), incorrectLogin( "Username and Password provided did not match any accounts on file." ), successLogin( "Your login has been successfully authenticated." ), unknownError( "Your login has failed due to an unknown internal error, Please try again." ), permissionsError( "Fatal error was detected with your account permissions. Please notify an administrator ASAP." ), banned( "You are banned on this server. THE BAN HAMMER HAS SPOKEN!" ), notWhiteListed( "You are not whitelisted on this server." ), customReason( "Someone forget to set my custom exception reason. :(" ), internalError( "Internal Service Error was encountered while attempting to login account." ), accountExists( "The username specified is already in use. Please try a different username." ), cancelledByEvent( "Your login has been cancelled by an internal event and no reason was returned, check logs." );
	
	String reason;
	
	LoginExceptionReason( String reason )
	{
		this.reason = reason;
	}
	
	public LoginExceptionReason setReason( String message )
	{
		reason = message;
		return this;
	}
	
	public String getReason()
	{
		return reason;
	}
}
