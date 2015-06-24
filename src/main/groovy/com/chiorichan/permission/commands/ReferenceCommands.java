/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.permission.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.chiorichan.ConsoleColor;
import com.chiorichan.console.InteractiveConsole;
import com.chiorichan.console.commands.advanced.CommandHandler;
import com.chiorichan.permission.PermissionManager;
import com.google.common.base.Joiner;

public class ReferenceCommands extends PermissionsCommand
{
	@CommandHandler( name = "pex", syntax = "ref <ref>", description = "Print <ref> inheritance info", permission = "permissions.manage.refs" )
	public void refPrintInheritance( InteractiveConsole sender, Map<String, String> args )
	{
		String refName = autoCompleteRef( args.get( "ref" ) );
		PermissionManager manager = PermissionManager.INSTANCE;
		Collection<String> parentReferences = manager.getRefInheritance( refName );
		if ( parentReferences == null )
		{
			sender.sendMessage( "Specified ref \"" + args.get( "ref" ) + "\" not found." );
			return;
		}
		
		sender.sendMessage( "Reference " + refName + " inherit:" );
		if ( parentReferences.size() == 0 )
		{
			sender.sendMessage( "nothing :3" );
			return;
		}
		
		for ( String parentReference : parentReferences )
		{
			// Collection<String> parents = manager.getRefInheritance( parentReference );
			String output = "  " + parentReference;
			if ( parentReferences.size() > 0 )
				output += ConsoleColor.GREEN + " [" + ConsoleColor.WHITE + Joiner.on( ", " ).join( parentReferences ) + ConsoleColor.GREEN + "]";
			
			sender.sendMessage( output );
		}
	}
	
	@CommandHandler( name = "pex", syntax = "ref <ref> inherit <parentReferences>", description = "Set <parentReferences> for <ref>", permission = "permissions.manage.refs.inheritance" )
	public void refSetInheritance( InteractiveConsole sender, Map<String, String> args )
	{
		String refName = autoCompleteRef( args.get( "ref" ) );
		PermissionManager manager = PermissionManager.INSTANCE;
		/*
		 * if ( ReferenceManager.INSTANCE.getReferenceById( refName ) == null )
		 * {
		 * sender.sendMessage( "Specified ref \"" + args.get( "ref" ) + "\" not found." );
		 * return;
		 * }
		 * 
		 * TODO Check for references once they are front loaded
		 */
		
		List<String> parents = new ArrayList<String>();
		String parentReferences = args.get( "parentReferences" );
		if ( parentReferences.contains( "," ) )
			for ( String ref : parentReferences.split( "," ) )
			{
				ref = autoCompleteRef( ref, "parentReferences" );
				if ( !parents.contains( ref ) )
					parents.add( ref.trim() );
			}
		else
			parents.add( parentReferences.trim() );
		
		manager.setRefInheritance( refName, parents );
		
		sender.sendMessage( "Reference " + refName + " inherits " + Joiner.on( ", " ).join( parents ) );
	}
	
	@CommandHandler( name = "pex", syntax = "refs", description = "Print loaded refs", isPrimary = true, permission = "permissions.manage.refs" )
	public void refsTree( InteractiveConsole sender, Map<String, String> args )
	{
		PermissionManager manager = PermissionManager.INSTANCE;
		
		sender.sendMessage( "References on server: " );
		for ( String ref : PermissionManager.INSTANCE.getReferences() )
		{
			Collection<String> parentReferences = manager.getRefInheritance( ref );
			String output = "  " + ref;
			if ( parentReferences.size() > 0 )
				output += ConsoleColor.GREEN + " [" + ConsoleColor.WHITE + Joiner.on( ", " ).join( parentReferences ) + ConsoleColor.GREEN + "]";
			
			sender.sendMessage( output );
		}
	}
}
