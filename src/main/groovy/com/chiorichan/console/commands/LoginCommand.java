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
import com.chiorichan.Loader;
import com.chiorichan.account.Account;
import com.chiorichan.account.AccountManager;
import com.chiorichan.account.LoginException;
import com.chiorichan.account.LoginExceptionReason;
import com.chiorichan.account.system.SystemAccounts;
import com.chiorichan.console.CommandDispatch;
import com.chiorichan.console.InteractiveConsole;
import com.chiorichan.console.Interviewer;


/**
 * Used to login an account to the console
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
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
					Account acct = Loader.getAccountManager().attemptLogin( handler.getPersistence(), user, pass );
					
					if ( !handler.getPersistence().checkPermission( "sys.query" ).isTrue() )
						throw new LoginException( LoginExceptionReason.notAuthorized, acct );
					
					AccountManager.getLogger().info( ConsoleColor.GREEN + "Successful Console Login [username='" + user + "',password='" + pass + "',userId='" + acct.getAcctId() + "',displayName='" + acct.getDisplayName() + "']" );
					
					handler.getPersistence().attachAccount( acct );
					handler.getPersistence().sendMessage( ConsoleColor.GREEN + "Welcome " + user + ", you have been successfully logged in." );
				}
			}
			catch ( LoginException l )
			{
				if ( l.getAccount() != null )
					AccountManager.getLogger().warning( ConsoleColor.GREEN + "Failed Console Login [username='" + user + "',password='" + pass + "',userId='" + l.getAccount().getAcctId() + "',displayName='" + l.getAccount().getDisplayName() + "',reason='" + l.getMessage() + "']" );
				
				handler.getPersistence().sendMessage( ConsoleColor.YELLOW + l.getMessage() );
				
				if ( handler.getPersistence().getAccount() == null || handler.getPersistence().getAccount() != SystemAccounts.noLogin )
				{
					handler.getPersistence().reset();
					handler.getPersistence().attachAccount( SystemAccounts.noLogin );
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
		if ( Loader.getAccountManager().isConfigured() )
		{
			CommandDispatch.addInterviewer( handler, new LoginInterviewerUser( handler ) );
			CommandDispatch.addInterviewer( handler, new LoginInterviewerPass( handler ) );
		}
		else
			handler.getPersistence().sendMessage( ConsoleColor.RED + "The Account Manager is unconfigured, we can not process logins until this problem is resolved." );
		
		return true;
	}
}
