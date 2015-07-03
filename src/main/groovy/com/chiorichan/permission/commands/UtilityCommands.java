/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.permission.commands;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.chiorichan.ConsoleColor;
import com.chiorichan.Loader;
import com.chiorichan.configuration.file.FileConfiguration;
import com.chiorichan.permission.PermissibleEntity;
import com.chiorichan.permission.PermissibleGroup;
import com.chiorichan.permission.Permission;
import com.chiorichan.permission.PermissionBackend;
import com.chiorichan.permission.PermissionManager;
import com.chiorichan.permission.lang.PermissionBackendException;
import com.chiorichan.terminal.TerminalEntity;
import com.chiorichan.terminal.commands.advanced.CommandBinding;
import com.chiorichan.terminal.commands.advanced.CommandHandler;

public class UtilityCommands extends PermissionsCommand
{
	private static int tryGetInt( TerminalEntity sender, Map<String, String> args, String key, int def )
	{
		if ( !args.containsKey( key ) )
			return def;
		
		try
		{
			return Integer.parseInt( args.get( key ) );
		}
		catch ( NumberFormatException e )
		{
			sender.sendMessage( ConsoleColor.RED + "Invalid " + key + " entered; must be an integer but was '" + args.get( key ) + "'" );
			return Integer.MIN_VALUE;
		}
	}
	
	@CommandHandler( name = "pex", syntax = "commit [type] [id]", permission = "permissions.manage.commit", description = "Commit all permission changes to the backend" )
	public void commit( TerminalEntity sender, Map<String, String> args )
	{
		if ( args.containsKey( "type" ) && args.containsKey( "id" ) )
			switch ( args.get( "type" ) )
			{
				case "entity":
					PermissibleEntity entity = PermissionManager.INSTANCE.getEntity( args.get( "id" ) );
					if ( entity == null )
						sender.sendMessage( ConsoleColor.RED + "We could not find an entity with id `" + args.get( "id" ) + "`!" );
					else
					{
						entity.save();
						sender.sendMessage( ConsoleColor.AQUA + "Wonderful news, we successfully committed changes made to entity `" + entity.getId() + "` successfully!" );
					}
					break;
				case "group":
					PermissibleGroup group = PermissionManager.INSTANCE.getGroup( args.get( "id" ) );
					if ( group == null )
						sender.sendMessage( ConsoleColor.RED + "We could not find a group with id `" + args.get( "id" ) + "`!" );
					else
					{
						group.save();
						sender.sendMessage( ConsoleColor.AQUA + "Wonderful news, we successfully committed changes made to group `" + group.getId() + "` successfully!" );
					}
					break;
				case "permission":
					Permission perm = PermissionManager.INSTANCE.getNode( args.get( "id" ), false );
					if ( perm == null )
						sender.sendMessage( ConsoleColor.RED + "We could not find a permission with namespace `" + args.get( "id" ) + "`!" );
					else
					{
						perm.commit();
						sender.sendMessage( ConsoleColor.AQUA + "Wonderful news, we successfully committed changes made to permission `" + perm.getNamespace() + "` successfully!" );
					}
					break;
			}
		else
		{
			PermissionManager.INSTANCE.saveData();
			sender.sendMessage( ConsoleColor.AQUA + "Wonderful news, we successfully committed any changes to the backend successfully!" );
		}
		
		// Force backend to finally flush changes
		PermissionManager.INSTANCE.getBackend().commit();
	}
	
	@SuppressWarnings( "unchecked" )
	@CommandHandler( name = "pex", syntax = "config <node> [value]", permission = "permissions.manage.config", description = "Print or set <node> [value]" )
	public void config( TerminalEntity sender, Map<String, String> args )
	{
		String nodeName = args.get( "node" );
		if ( nodeName == null || nodeName.isEmpty() )
			return;
		
		FileConfiguration config = Loader.getConfig();
		
		if ( args.get( "value" ) != null )
		{
			config.set( nodeName, parseValue( args.get( "value" ) ) );
			Loader.saveConfig();
		}
		
		Object node = config.get( nodeName );
		if ( node instanceof Map )
		{
			sender.sendMessage( "Node \"" + nodeName + "\": " );
			for ( Map.Entry<String, Object> entry : ( ( Map<String, Object> ) node ).entrySet() )
				sender.sendMessage( "  " + entry.getKey() + " = " + entry.getValue() );
		}
		else if ( node instanceof List )
		{
			sender.sendMessage( "Node \"" + nodeName + "\": " );
			for ( String item : ( ( List<String> ) node ) )
				sender.sendMessage( " - " + item );
		}
		else
			sender.sendMessage( "Node \"" + nodeName + "\" = \"" + node + "\"" );
	}
	
	@CommandHandler( name = "pex", syntax = "dump <backend> <filename>", permission = "permissions.dump", description = "Dump users/groups to selected <backend> format" )
	public void dumpData( TerminalEntity sender, Map<String, String> args )
	{
		try
		{
			PermissionBackend backend = PermissionBackend.getBackendWithException( args.get( "backend" ) );
			
			File dstFile = new File( args.get( "filename" ) );
			
			FileOutputStream outStream = new FileOutputStream( dstFile );
			
			backend.dumpData( new OutputStreamWriter( outStream, "UTF-8" ) );
			
			outStream.close();
			
			sender.sendMessage( ConsoleColor.WHITE + "[Permissions] Data dumped in \"" + dstFile.getName() + "\" " );
		}
		catch ( IOException e )
		{
			sender.sendMessage( ConsoleColor.RED + "IO Error: " + e.getMessage() );
		}
		catch ( ClassNotFoundException e )
		{
			sender.sendMessage( ConsoleColor.RED + "Specified backend not found!" );
		}
		catch ( Throwable t )
		{
			sender.sendMessage( ConsoleColor.RED + "Error: " + t.getMessage() );
			PermissionManager.getLogger().severe( "Error: " + t.getMessage(), t );
			// t.printStackTrace();
		}
	}
	
	@CommandHandler( name = "pex", syntax = "backend", permission = "permissions.manage.backend", description = "Print currently used backend" )
	public void getBackend( TerminalEntity sender, Map<String, String> args )
	{
		sender.sendMessage( "Current backend: " + PermissionManager.INSTANCE.getBackend() );
	}
	
	@CommandHandler( name = "pex", syntax = "hierarchy [world]", permission = "permissions.manage.users", description = "Print complete user/group hierarchy" )
	public void printHierarchy( TerminalEntity sender, Map<String, String> args )
	{
		sender.sendMessage( "User/Group inheritance hierarchy:" );
		sendMessage( sender, this.printHierarchy( null, autoCompleteRef( args.get( "world" ) ), 0 ) );
	}
	
	@CommandHandler( name = "pex", syntax = "reload", permission = "permissions.manage.reload", description = "Reload environment" )
	public void reload( TerminalEntity sender, Map<String, String> args )
	{
		try
		{
			PermissionManager.INSTANCE.reload();
			sender.sendMessage( ConsoleColor.WHITE + "Permissions reloaded" );
		}
		catch ( PermissionBackendException e )
		{
			sender.sendMessage( ConsoleColor.RED + "Failed to reload permissions! Check configuration!\n" + ConsoleColor.RED + "Error (see console for full): " + e.getMessage() );
			PermissionManager.getLogger().log( Level.WARNING, "Failed to reload permissions when " + sender.getDisplayName() + " ran `pex reload`", e );
		}
	}
	
	@CommandHandler( name = "pex", syntax = "report", permission = "permissions.manage.reportbug", description = "Create an issue template to report an issue" )
	public void report( TerminalEntity sender, Map<String, String> args )
	{
		/*
		 * ErrorReport report = ErrorReport.withException( "User-requested report", new Exception().fillInStackTrace() );
		 * sender.sendMessage( "Fill in the information at " + report.getShortURL() + " to report an issue" );
		 * sender.sendMessage( ConsoleColor.RED + "NOTE: A GitHub account is necessary to report issues. Create one at https://github.com/" );
		 */
	}
	
	@CommandHandler( name = "pex", syntax = "backend <backend>", permission = "permissions.manage.backend", description = "Change permission backend on the fly (Use with caution!)" )
	public void setBackend( TerminalEntity sender, Map<String, String> args )
	{
		if ( args.get( "backend" ) == null )
			return;
		
		try
		{
			PermissionManager.INSTANCE.setBackend( args.get( "backend" ) );
			sender.sendMessage( ConsoleColor.WHITE + "Permission backend changed!" );
		}
		catch ( RuntimeException e )
		{
			if ( e.getCause() instanceof ClassNotFoundException )
				sender.sendMessage( ConsoleColor.RED + "Specified backend not found." );
			else
			{
				sender.sendMessage( ConsoleColor.RED + "Error during backend initialization." );
				e.printStackTrace();
			}
		}
		catch ( PermissionBackendException e )
		{
			sender.sendMessage( ConsoleColor.RED + "Backend initialization failed! Fix your configuration!\n" + ConsoleColor.RED + "Error (see console for more): " + e.getMessage() );
			PermissionManager.getLogger().log( Level.WARNING, "Backend initialization failed when " + sender.getDisplayName() + " was initializing " + args.get( "backend" ), e );
		}
	}
	
	@CommandHandler( name = "pex", syntax = "help [page] [count]", permission = "permissions.manage", description = "PermissionManager commands help" )
	public void showHelp( TerminalEntity sender, Map<String, String> args )
	{
		List<CommandBinding> commands = command.getCommands();
		
		int count = tryGetInt( sender, args, "count", 8 );
		int page = tryGetInt( sender, args, "page", 1 );
		
		if ( page == Integer.MIN_VALUE || count == Integer.MIN_VALUE )
			return; // method already prints error message
			
		if ( page < 1 )
		{
			sender.sendMessage( ConsoleColor.RED + "Page couldn't be lower than 1" );
			return;
		}
		
		int totalPages = ( int ) Math.ceil( commands.size() / count );
		
		sender.sendMessage( ConsoleColor.BLUE + "PermissionManager" + ConsoleColor.WHITE + " commands (page " + ConsoleColor.GOLD + page + "/" + totalPages + ConsoleColor.WHITE + "): " );
		
		int base = count * ( page - 1 );
		
		for ( int i = base; i < base + count; i++ )
		{
			if ( i >= commands.size() )
				break;
			
			CommandHandler command = commands.get( i ).getMethodAnnotation();
			String commandName = String.format( "/%s %s", command.name(), command.syntax() ).replace( "<", ConsoleColor.BOLD.toString() + ConsoleColor.RED + "<" ).replace( ">", ">" + ConsoleColor.RESET + ConsoleColor.GOLD.toString() ).replace( "[", ConsoleColor.BOLD.toString() + ConsoleColor.BLUE + "[" ).replace( "]", "]" + ConsoleColor.RESET + ConsoleColor.GOLD.toString() );
			
			
			sender.sendMessage( ConsoleColor.GOLD + commandName );
			sender.sendMessage( ConsoleColor.AQUA + "    " + command.description() );
		}
	}
	
	@CommandHandler( name = "pex", syntax = "toggle debug", permission = "permissions.debug", description = "Enable/disable debug mode" )
	public void toggleFeature( TerminalEntity sender, Map<String, String> args )
	{
		PermissionManager.setDebug( !PermissionManager.isDebug() );
		
		String debugStatusMessage = "[Permissions] Debug mode " + ( PermissionManager.isDebug() ? "enabled" : "disabled" );
		
		sender.sendMessage( debugStatusMessage );
		PermissionManager.getLogger().warning( debugStatusMessage );
	}
}
