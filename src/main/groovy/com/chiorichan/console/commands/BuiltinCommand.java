/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.console.commands;

import java.util.Arrays;

import com.chiorichan.ConsoleColor;
import com.chiorichan.Loader;
import com.chiorichan.account.AccountInstance;
import com.chiorichan.account.AccountManager;
import com.chiorichan.account.AccountMeta;
import com.chiorichan.console.Command;
import com.chiorichan.console.CommandDispatch;
import com.chiorichan.console.InteractiveConsole;
import com.chiorichan.session.Session;
import com.chiorichan.util.StringFunc;
import com.chiorichan.util.Versioning;
import com.google.common.base.Joiner;

/**
 * Used for builtin server commands
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public abstract class BuiltinCommand extends Command
{
	public static void registerBuiltinCommands()
	{
		CommandDispatch.registerCommand( new BuiltinCommand( "version" )
		{
			@Override
			public boolean execute( InteractiveConsole handler, String command, String[] args )
			{
				handler.getPersistence().send( ConsoleColor.AQUA + Versioning.getProduct() + " is running version " + Versioning.getVersion() + ( ( Versioning.getBuildNumber().equals( "0" ) ) ? " (dev)" : " (build #" + Versioning.getBuildNumber() + ")" ) );
				return true;
			}
		} );
		
		CommandDispatch.registerCommand( new BuiltinCommand( "echo" )
		{
			@Override
			public boolean execute( InteractiveConsole handler, String command, String[] args )
			{
				handler.getPersistence().send( Joiner.on( " " ).join( args ) );
				return true;
			}
		} );
		
		CommandDispatch.registerCommand( new BuiltinCommand( "help" )
		{
			@Override
			public boolean execute( InteractiveConsole handler, String command, String[] args )
			{
				// handler.getPersistence().send( ConsoleColor.YELLOW + "We're sorry, help has not been implemented as of yet, try again in a later version." );
				
				for ( Session s : Loader.getSessionManager().getSessions() )
					handler.getPersistence().send( "Session: " + s );
				
				return true;
			}
		} );
		
		CommandDispatch.registerCommand( new BuiltinCommand( "whoami" )
		{
			@Override
			public boolean execute( InteractiveConsole handler, String command, String[] args )
			{
				handler.getPersistence().send( handler.getPersistence().getSession().account().getAcctId() );
				return true;
			}
		} );
		
		CommandDispatch.registerCommand( new BuiltinCommand( "color" )
		{
			@Override
			public boolean execute( InteractiveConsole handler, String command, String[] args )
			{
				String color = "" + ( ( args.length < 1 ) ? !StringFunc.isTrue( handler.getMetadata( "color", "true" ) ) : StringFunc.isTrue( args[0] ) );
				handler.setMetadata( "color", color );
				handler.sendMessage( ConsoleColor.AQUA + "Console color has been " + ( ( StringFunc.isTrue( color ) ) ? "enabled" : "disabled" ) + "." );
				return true;
			}
		} );
		
		CommandDispatch.registerCommand( new BuiltinCommand( "stop" )
		{
			@Override
			public boolean execute( InteractiveConsole handler, String command, String[] args )
			{
				if ( handler.getPersistence().getSession().isOp() )
					Loader.stop( "The server is shutting down as requested by user " + handler.getPersistence().getSession().getAcctId() );
				else
					handler.sendMessage( ConsoleColor.RED + "Only server operators can request the server to stop." );
				
				return true;
			}
		} );
		
		CommandDispatch.registerCommand( new BuiltinCommand( "op" )
		{
			@Override
			public boolean execute( InteractiveConsole handler, String command, String[] args )
			{
				if ( handler.getPersistence().getSession().isOp() )
				{
					if ( args.length < 1 )
						handler.sendMessage( ConsoleColor.RED + "You must specify which account you wish to op." );
					else
					{
						AccountMeta acct = AccountManager.INSTANCE.getAccount( args[0] );
						acct.getPermissibleEntity().checkPermission( "sys.op" ).assign();
						handler.sendMessage( ConsoleColor.AQUA + "We successfully op'ed the account " + acct.getAcctId() );
					}
				}
				else
					handler.sendMessage( ConsoleColor.RED + "Only server operators can promote other accounts to server operator." );
				
				return true;
			}
		} );
		
		CommandDispatch.registerCommand( new BuiltinCommand( "aboutme" )
		{
			@Override
			public boolean execute( InteractiveConsole handler, String command, String[] args )
			{
				AccountInstance acct = handler.getPersistence().getSession().account();
				
				for ( String s : acct.metadata().getKeys() )
					handler.sendMessage( s + " => " + acct.metadata().getString( s ) );
				
				return true;
			}
		} );
		
		CommandDispatch.registerCommand( new BuiltinCommand( "exit" )
		{
			@Override
			public boolean execute( InteractiveConsole handler, String command, String[] args )
			{
				handler.getPersistence().send( ConsoleColor.AQUA + "Thank you for visiting, please come back again." );
				handler.getPersistence().finish();
				return true;
			}
		}.setAliases( Arrays.asList( new String[] {"quit", "end", "leave", "logout"} ) ) );
		
		CommandDispatch.registerCommand( new LoginCommand() );
	}
	
	BuiltinCommand( String name )
	{
		super( name );
	}
	
	BuiltinCommand( String name, String permission )
	{
		super( name, permission );
	}
}
