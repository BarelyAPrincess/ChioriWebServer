/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.console.commands;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.chiorichan.ConsoleColor;
import com.chiorichan.account.AccountManager;
import com.chiorichan.account.AccountType;
import com.chiorichan.account.auth.AccountAuthenticator;
import com.chiorichan.account.lang.AccountException;
import com.chiorichan.account.lang.AccountResult;
import com.chiorichan.console.CommandDispatch;
import com.chiorichan.console.InteractiveConsole;
import com.chiorichan.console.Interviewer;

/**
 * Used to login an account to the console
 * 
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
class LoginCommand extends BuiltinCommand
{
	class LoginInterviewerUser implements Interviewer
	{
		private InteractiveConsole handler;
		
		public LoginInterviewerUser( InteractiveConsole handler )
		{
			this.handler = handler;
		}
		
		@Override
		public boolean handleInput( String input )
		{
			if ( input == null || input.isEmpty() )
			{
				handler.sendMessage( "Username can't be empty!" );
				return true;
			}
			
			handler.setMetadata( "user", input );
			return true;
		}
		
		@Override
		public String getPrompt()
		{
			try
			{
				return InetAddress.getLocalHost().getHostName() + " login: ";
			}
			catch ( UnknownHostException e )
			{
				return "login: ";
			}
		}
	}
	
	class LoginInterviewerPass implements Interviewer
	{
		private InteractiveConsole handler;
		
		public LoginInterviewerPass( InteractiveConsole handler )
		{
			this.handler = handler;
		}
		
		@Override
		public boolean handleInput( String input )
		{
			String user = handler.getMetadata( "user" );
			String pass = input;
			
			try
			{
				if ( user != null && pass != null )
				{
					AccountResult result = handler.getPersistence().getSession().login( handler.getPersistence().getSession(), AccountAuthenticator.PASSWORD.credentials( user, pass ) );
					
					if ( result != AccountResult.LOGIN_SUCCESS )
						throw new AccountException( result );
					
					// if ( !handler.getPersistence().checkPermission( "sys.query" ).isTrue() )
					// throw new LoginException( LoginExceptionReason.notAuthorized, acct );
					
					AccountManager.getLogger().info( ConsoleColor.GREEN + "Successful Console Login [username='" + user + "',password='" + pass + "',userId='" + result.getAccount().getAcctId() + "',displayName='" + result.getAccount().getDisplayName() + "']" );
					
					handler.getPersistence().send( ConsoleColor.GREEN + "Welcome " + user + ", you have been successfully logged in." );
				}
			}
			catch ( AccountException l )
			{
				if ( l.getAccount() != null )
					AccountManager.getLogger().warning( ConsoleColor.GREEN + "Failed Console Login [username='" + user + "',password='" + pass + "',userId='" + l.getAccount().getAcctId() + "',displayName='" + l.getAccount().getDisplayName() + "',reason='" + l.getMessage() + "']" );
				
				handler.getPersistence().send( ConsoleColor.YELLOW + l.getMessage() );
				
				if ( handler.getPersistence().getSession().account() == null || handler.getPersistence().getSession().account().metadata() != AccountType.ACCOUNT_NONE )
				{
					handler.getPersistence().getSession().login( handler.getPersistence().getSession(), AccountAuthenticator.NULL.credentials( AccountType.ACCOUNT_NONE ) );
				}
				
				return true;
			}
			
			handler.setMetadata( "user", null );
			return true;
		}
		
		@Override
		public String getPrompt()
		{
			return "Password for " + handler.getMetadata( "user" ) + ": ";
		}
	}
	
	public LoginCommand()
	{
		super( "login" );
	}
	
	@Override
	public boolean execute( InteractiveConsole handler, String command, String[] args )
	{
		CommandDispatch.addInterviewer( handler, new LoginInterviewerUser( handler ) );
		CommandDispatch.addInterviewer( handler, new LoginInterviewerPass( handler ) );
		
		return true;
	}
}
