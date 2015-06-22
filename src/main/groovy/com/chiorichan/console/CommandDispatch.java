/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.console;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.chiorichan.ConsoleColor;
import com.chiorichan.Loader;
import com.chiorichan.account.AccountPermissible;
import com.chiorichan.console.commands.BuiltinCommand;
import com.chiorichan.event.EventBus;
import com.chiorichan.event.server.CommandIssuedEvent;
import com.google.common.collect.Maps;

/**
 * This is the Command Dispatch for executing a command from a console.
 */
public final class CommandDispatch
{
	private static List<CommandRef> pendingCommands = Collections.synchronizedList( new ArrayList<CommandRef>() );
	private static List<Command> registeredCommands = Collections.synchronizedList( new ArrayList<Command>() );
	private static final Pattern PATTERN_ON_SPACE = Pattern.compile( " ", Pattern.LITERAL );
	private static Map<InteractiveConsole, List<Interviewer>> interviewers = Maps.newConcurrentMap();
	private static Map<InteractiveConsole, Interviewer> activeInterviewer = Maps.newConcurrentMap();
	
	static
	{
		BuiltinCommand.registerBuiltinCommands();
	}
	
	protected static void reg( Command command )
	{
		registerCommand( command );
	}
	
	public static void registerCommand( Command command )
	{
		if ( getCommand( command.getName() ) == null )
			registeredCommands.add( command );
	}
	
	public static void issueCommand( InteractiveConsole handler, String command )
	{
		Validate.notNull( handler, "Handler cannot be null" );
		Validate.notNull( command, "CommandLine cannot be null" );
		
		Loader.getLogger().fine( "The remote connection '" + handler + "' issued the command '" + command + "'." );
		
		pendingCommands.add( new CommandRef( handler, command ) );
	}
	
	public static void handleCommands()
	{
		for ( Entry<InteractiveConsole, List<Interviewer>> entry : interviewers.entrySet() )
		{
			if ( activeInterviewer.get( entry.getKey() ) == null )
			{
				if ( entry.getValue().isEmpty() )
				{
					interviewers.remove( entry.getKey() );
					entry.getKey().resetPrompt();
				}
				else
				{
					Interviewer i = entry.getValue().remove( 0 );
					activeInterviewer.put( entry.getKey(), i );
					entry.getKey().setPrompt( i.getPrompt() );
				}
			}
		}
		
		while ( !pendingCommands.isEmpty() )
		{
			CommandRef command = pendingCommands.remove( 0 );
			
			try
			{
				Interviewer i = activeInterviewer.get( command.handler );
				AccountPermissible permissible = command.handler.getPersistence().getSession();
				
				if ( i != null )
				{
					if ( i.handleInput( command.command ) )
						activeInterviewer.remove( command.handler );
					else
						command.handler.prompt();
				}
				else
				{
					CommandIssuedEvent event = new CommandIssuedEvent( command.command, permissible );
					
					EventBus.INSTANCE.callEvent( event );
					
					if ( event.isCancelled() )
					{
						permissible.send( ConsoleColor.RED + "Your entry was cancelled by the event system." );
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
								if ( target.testPermission( permissible ) )
									target.execute( command.handler, sentCommandLabel, Arrays.copyOfRange( args, 1, args.length ) );
								
								return;
							}
							catch ( CommandException ex )
							{
								throw ex;
							}
							catch ( Throwable ex )
							{
								command.handler.sendMessage( ConsoleColor.RED + "Unhandled exception executing '" + command.command + "' in " + target + "\n" + ExceptionUtils.getStackTrace( ex ) );
								
								throw new CommandException( "Unhandled exception executing '" + command.command + "' in " + target, ex );
							}
						}
					}
					
					permissible.send( ConsoleColor.YELLOW + "Your entry was unrecognized, type \"help\" for help." );
				}
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
	
	public static void addInterviewer( InteractiveConsole handler, Interviewer interviewer )
	{
		if ( interviewers.get( handler ) == null )
		{
			interviewers.put( handler, new ArrayList<Interviewer>( Arrays.asList( interviewer ) ) );
		}
		else
		{
			interviewers.get( handler ).add( interviewer );
		}
	}
}
