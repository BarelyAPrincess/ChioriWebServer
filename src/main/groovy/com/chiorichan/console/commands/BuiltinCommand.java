/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.console.commands;

import com.chiorichan.ConsoleColor;
import com.chiorichan.console.Command;
import com.chiorichan.console.CommandDispatch;
import com.chiorichan.console.InteractiveConsole;
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
				handler.getSession().sendMessage( ConsoleColor.AQUA + Versioning.getProduct() + " is running version " + Versioning.getVersion() + ( ( Versioning.getBuildNumber().equals( "0" ) ) ? " (dev)" : " (build #" + Versioning.getBuildNumber() + ")" ) );
				return true;
			}
		} );
		
		CommandDispatch.registerCommand( new BuiltinCommand( "echo" )
		{
			@Override
			public boolean execute( InteractiveConsole handler, String command, String[] args )
			{
				handler.getSession().sendMessage( Joiner.on( " " ).join( args ) );
				return true;
			}
		} );
		
		CommandDispatch.registerCommand( new BuiltinCommand( "help" )
		{
			@Override
			public boolean execute( InteractiveConsole handler, String command, String[] args )
			{
				handler.getSession().sendMessage( ConsoleColor.YELLOW + "We're sorry, help has not been implemented as of yet, try again in a later version." );
				return true;
			}
		} );
		
		CommandDispatch.registerCommand( new BuiltinCommand( "whoami" )
		{
			@Override
			public boolean execute( InteractiveConsole handler, String command, String[] args )
			{
				handler.getSession().sendMessage( handler.getSession().getAccount().getAcctId() );
				return true;
			}
		} );
		
		CommandDispatch.registerCommand( new BuiltinCommand( "nocolor" )
		{
			@Override
			public boolean execute( InteractiveConsole handler, String command, String[] args )
			{
				handler.setMetadata( "nocolor", "true" );
				return true;
			}
		} );
		
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
