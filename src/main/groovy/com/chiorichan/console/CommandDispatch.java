/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.console;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;

import com.chiorichan.ConsoleColor;
import com.chiorichan.Loader;
import com.chiorichan.account.InteractivePermissible;
import com.chiorichan.event.server.CommandIssuedEvent;
import com.chiorichan.util.Versioning;
import com.google.common.base.Joiner;

/**
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public final class CommandDispatch
{
	private static List<CommandRef> pendingCommands = Collections.synchronizedList( new ArrayList<CommandRef>() );
	private static List<Command> registeredCommands = Collections.synchronizedList( new ArrayList<Command>() );
	private static final Pattern PATTERN_ON_SPACE = Pattern.compile( " ", Pattern.LITERAL );
	
	static
	{
		reg( new BuiltinCommand( "version" )
		{
			@Override
			public boolean execute( InteractivePermissible handler, String command, String[] args )
			{
				handler.sendMessage( ConsoleColor.AQUA + Versioning.getProduct() + " is running version " + Versioning.getVersion() + ( ( Versioning.getBuildNumber().equals( "0" ) ) ? " (dev)" : " (build #" + Versioning.getBuildNumber() + ")" ) );
				return true;
			}
		} );
		
		reg( new BuiltinCommand( "echo" )
		{
			@Override
			public boolean execute( InteractivePermissible handler, String command, String[] args )
			{
				handler.sendMessage( Joiner.on( " " ).join( args ) );
				return true;
			}
		} );
		
		reg( new BuiltinCommand( "help" )
		{
			@Override
			public boolean execute( InteractivePermissible handler, String command, String[] args )
			{
				handler.sendMessage( ConsoleColor.YELLOW + "We're sorry, help has not been implemented as of yet, try again in a later version." );
				return true;
			}
		} );
	}
	
	protected static void reg( Command command )
	{
		registeredCommands.add( command );
	}
	
	public static void issueCommand( InteractivePermissible handler, String command )
	{
		Validate.notNull( handler, "Handler cannot be null" );
		Validate.notNull( command, "CommandLine cannot be null" );
		
		Loader.getLogger().fine( "The remote connection '" + handler + "' issued the command '" + command + "'." );
		
		pendingCommands.add( new CommandRef( handler, command ) );
	}
	
	public static void handleCommands()
	{
		while ( !pendingCommands.isEmpty() )
		{
			CommandRef command = pendingCommands.remove( 0 );
			
			try
			{
				CommandIssuedEvent event = new CommandIssuedEvent( command.command, command.permissible );
				
				Loader.getEventBus().callEvent( event );
				
				if ( event.isCancelled() )
				{
					command.permissible.sendMessage( ConsoleColor.RED + "Your entry was cancelled by the event system." );
					return;
				}
				
				String[] args = PATTERN_ON_SPACE.split( command.command );
				
				if ( args.length > 0 )
				{
					String sentCommandLabel = args[0].toLowerCase();
					Command target = getCommand( sentCommandLabel );
					
					if ( target != null )
					{
						try
						{
							if ( target.testPermission( command.permissible ) )
								target.execute( command.permissible, sentCommandLabel, Arrays.copyOfRange( args, 1, args.length ) );
							
							return;
						}
						catch ( CommandException ex )
						{
							throw ex;
						}
						catch ( Throwable ex )
						{
							throw new CommandException( "Unhandled exception executing '" + command.command + "' in " + target, ex );
						}
					}
				}
				
				command.permissible.sendMessage( ConsoleColor.YELLOW + "Your entry was unrecognized, type \"help\" for help." );
			}
			catch ( Exception ex )
			{
				Loader.getLogger().warning( "Unexpected exception while parsing console command \"" + command.command + '"', ex );
			}
		}
	}
	
	private static Command getCommand( String sentCommandLabel )
	{
		for ( Command command : registeredCommands )
		{
			if ( command.getName().equals( sentCommandLabel ) )
				return command;
		}
		
		return null;
	}
}
