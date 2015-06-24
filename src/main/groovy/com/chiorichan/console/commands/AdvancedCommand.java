/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.console.commands;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.chiorichan.ConsoleColor;
import com.chiorichan.console.Command;
import com.chiorichan.console.InteractiveConsole;
import com.chiorichan.console.commands.advanced.AutoCompleteChoicesException;
import com.chiorichan.console.commands.advanced.CommandBinding;
import com.chiorichan.console.commands.advanced.CommandHandler;
import com.chiorichan.console.commands.advanced.CommandListener;
import com.chiorichan.console.commands.advanced.CommandSyntax;
import com.chiorichan.permission.PermissionManager;
import com.google.common.base.Joiner;

public class AdvancedCommand extends Command
{
	protected Map<String, Map<CommandSyntax, CommandBinding>> listeners = new LinkedHashMap<String, Map<CommandSyntax, CommandBinding>>();
	
	public AdvancedCommand( String name )
	{
		super( name );
	}
	
	public AdvancedCommand( String name, String permission )
	{
		super( name, permission );
	}
	
	@Override
	public boolean execute( InteractiveConsole handler, String command, String[] args )
	{
		try
		{
			if ( args.length > 0 )
			{
				Map<CommandSyntax, CommandBinding> callMap = listeners.get( command );
				
				if ( callMap == null )
					return false;
				
				CommandBinding selectedBinding = null;
				int argumentsLength = 0;
				String arguments = Joiner.on( " " ).join( args );
				
				for ( Entry<CommandSyntax, CommandBinding> entry : callMap.entrySet() )
				{
					CommandSyntax syntax = entry.getKey();
					if ( !syntax.isMatch( arguments ) )
						continue;
					if ( selectedBinding != null && syntax.getRegexp().length() < argumentsLength )
						continue;
					
					CommandBinding binding = entry.getValue();
					binding.setParams( syntax.getMatchedArguments( arguments ) );
					selectedBinding = binding;
				}
				
				if ( selectedBinding == null )
				{
					handler.sendMessage( ConsoleColor.RED + "Error in command syntax. Check command help." );
					return true;
				}
				
				if ( !selectedBinding.checkPermissions( handler.getPersistence().getSession().getPermissibleEntity() ) )
				{
					PermissionManager.getLogger().warning( "Entity " + handler.getName() + " tried to access command \"" + command + " " + arguments + "\", but doesn't have permission to do this." );
					handler.sendMessage( ConsoleColor.RED + "Sorry, you don't have enough permissions." );
					return true;
				}
				
				try
				{
					selectedBinding.call( handler, selectedBinding.getParams() );
				}
				catch ( InvocationTargetException e )
				{
					if ( e.getTargetException() instanceof AutoCompleteChoicesException )
					{
						AutoCompleteChoicesException autocomplete = ( AutoCompleteChoicesException ) e.getTargetException();
						handler.sendMessage( "Autocomplete for <" + autocomplete.getArgName() + ">:" );
						handler.sendMessage( "    " + Joiner.on( "   " ).join( autocomplete.getChoices() ) );
					}
					else
						throw new RuntimeException( e.getTargetException() );
				}
				catch ( Exception e )
				{
					PermissionManager.getLogger().severe( "There is bogus command handler for " + command + " command. (Is appropriate plugin is update?)" );
					if ( e.getCause() != null )
						e.getCause().printStackTrace();
					else
						e.printStackTrace();
				}
				
				return true;
			}
			else
				return false;
		}
		catch ( Throwable t )
		{
			// TODO Make it so log messages can be sent to InteractiveConsoles, like a getLogger() for consoles
			handler.sendMessage( "Sorry, there was a severe exception encountered while executing this command. :(" );
			PermissionManager.getLogger().severe( String.format( "Exception encountered while %s was executing %s %s", handler.getName(), command, Joiner.on( " " ).join( args ) ), t );
			return true;
		}
	}
	
	public List<CommandBinding> getCommands()
	{
		List<CommandBinding> commands = new LinkedList<CommandBinding>();
		
		for ( Map<CommandSyntax, CommandBinding> map : listeners.values() )
			commands.addAll( map.values() );
		
		return commands;
	}
	
	public AdvancedCommand register( CommandListener listener )
	{
		for ( Method method : listener.getClass().getMethods() )
		{
			if ( !method.isAnnotationPresent( CommandHandler.class ) )
				continue;
			
			CommandHandler cmdAnnotation = method.getAnnotation( CommandHandler.class );
			
			Map<CommandSyntax, CommandBinding> commandListeners = listeners.get( cmdAnnotation.name() );
			if ( commandListeners == null )
			{
				commandListeners = new LinkedHashMap<CommandSyntax, CommandBinding>();
				listeners.put( cmdAnnotation.name(), commandListeners );
			}
			
			commandListeners.put( new CommandSyntax( cmdAnnotation.syntax() ), new CommandBinding( listener, method ) );
		}
		
		listener.onRegistered( this );
		
		return this;
	}
}
