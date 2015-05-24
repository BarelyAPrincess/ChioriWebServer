/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.console;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.chiorichan.ConsoleColor;
import com.chiorichan.account.Account;
import com.chiorichan.account.AccountPermissible;
import com.chiorichan.permission.Permissible;
import com.chiorichan.permission.Permission;
import com.chiorichan.permission.PermissionNamespace;
import com.google.common.collect.Sets;

/**
 * The base class for Console Commands
 * 
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
public abstract class Command
{
	private final String name;
	private Set<String> aliases = Sets.newHashSet();
	private String description = "";
	private String usageMessage = null;
	private String permission = null;
	private String permissionMessage = null;
	
	public Command( String name )
	{
		this.name = name;
	}
	
	public Command( String name, String permission )
	{
		if ( permission != null )
		{
			PermissionNamespace ns = new PermissionNamespace( permission );
			
			if ( !ns.containsOnlyValidChars() )
				throw new RuntimeException( "We detected that the required permission '" + ns.getNamespace() + "' for command '" + name + "' contains invalid characters, this is most likely a programmers bug." );
			
			this.permission = ns.getNamespace();
		}
		
		this.name = name;
	}
	
	/**
	 * Executes the command, returning its success
	 * 
	 * @param handler
	 *            Source object which is executing this command
	 * @param commandLabel
	 *            The alias of the command used
	 * @param args
	 *            All arguments passed to the command, split via ' '
	 * @return true if the command was successful, otherwise false
	 */
	public abstract boolean execute( InteractiveConsole handler, String command, String[] args );
	
	/**
	 * Returns the name of this command
	 * 
	 * @return Name of this command
	 */
	public String getName()
	{
		return name;
	}
	
	/**
	 * Gets the permission required by users to be able to perform this command
	 * 
	 * @return Permission name, or null if none
	 */
	public String getPermission()
	{
		return permission;
	}
	
	/**
	 * Gets the permission required by users to be able to perform this command
	 * 
	 * @return Permission name, or null if none
	 */
	public Permission getPermissionNode()
	{
		return Permission.getNode( permission );
	}
	
	/**
	 * Tests the given {@link InteractiveConsoleHandler} to see if they can perform this command.
	 * <p>
	 * If they do not have permission, they will be informed that they cannot do this.
	 * 
	 * @param target
	 *            InteractiveConsoleHandler to test
	 * @return {@link true} if they can use it, otherwise false
	 */
	public boolean testPermission( AccountPermissible target )
	{
		if ( target == null )
			return false;
		
		if ( testPermissionSilent( target ) )
			return true;
		
		if ( permissionMessage == null )
			target.send( ConsoleColor.RED + "I'm sorry, but you do not have permission to perform the command '" + name + "'." );
		else if ( permissionMessage.length() != 0 )
		{
			for ( String line : permissionMessage.replace( "<permission>", permission ).split( "\n" ) )
			{
				target.send( line );
			}
		}
		
		return false;
	}
	
	/**
	 * Tests the given {@link InteractiveConsoleHandler} to see if they can perform this command.
	 * <p>
	 * No error is sent to the sender.
	 * 
	 * @param target
	 *            User to test
	 * @return true if they can use it, otherwise false
	 */
	public boolean testPermissionSilent( Permissible target )
	{
		if ( ( permission == null ) || ( permission.length() == 0 ) )
		{
			return true;
		}
		
		// TODO split permissions
		for ( String p : permission.split( ";" ) )
		{
			if ( target.checkPermission( p ).isTrue() )
			{
				return true;
			}
		}
		
		return false;
	}
	
	public Set<String> getAliases()
	{
		return aliases;
	}
	
	/**
	 * Returns a message to be displayed on a failed permission check for this command
	 * 
	 * @return Permission check failed message
	 */
	public String getPermissionMessage()
	{
		return permissionMessage;
	}
	
	/**
	 * Gets a brief description of this command
	 * 
	 * @return Description of this command
	 */
	public String getDescription()
	{
		return description;
	}
	
	/**
	 * Gets an example usage of this command
	 * 
	 * @return One or more example usages
	 */
	public String getUsage()
	{
		return usageMessage;
	}
	
	/**
	 * Sets the list of aliases to request on registration for this command.
	 * 
	 * @param aliases
	 *            aliases to register
	 * @return this command object, for chaining
	 */
	public Command setAliases( Set<String> aliases )
	{
		this.aliases = aliases;
		return this;
	}
	
	/**
	 * Sets the list of aliases to request on registration for this command.
	 * 
	 * @param aliases
	 *            aliases to register
	 * @return this command object, for chaining
	 */
	public Command setAliases( List<String> aliases )
	{
		this.aliases = new HashSet<String>( aliases );
		return this;
	}
	
	/**
	 * Sets a brief description of this command.
	 * 
	 * @param description
	 *            new command description
	 * @return this command object, for chaining
	 */
	public Command setDescription( String description )
	{
		this.description = description;
		return this;
	}
	
	/**
	 * Sets the message sent when a permission check fails
	 * 
	 * @param permissionMessage
	 *            new permission message, null to indicate default message, or an empty string to indicate no message
	 * @return this command object, for chaining
	 */
	public Command setPermissionMessage( String permissionMessage )
	{
		this.permissionMessage = permissionMessage;
		return this;
	}
	
	/**
	 * Sets the example usage of this command
	 * 
	 * @param usage
	 *            new example usage
	 * @return this command object, for chaining
	 */
	public Command setUsage( String usage )
	{
		this.usageMessage = usage;
		return this;
	}
	
	public static void broadcastCommandMessage( Account source, String message )
	{
		broadcastCommandMessage( source, message, true );
	}
	
	public static void broadcastCommandMessage( Account source, String message, boolean sendToSource )
	{
		/*
		 * String result;
		 * if ( source.getSentient() == null )
		 * result = source + ": " + message;
		 * else
		 * result = source.getSentient().getName() + ": " + message;
		 * 
		 * Set<Permissible> subscribed = Loader.getPermissionManager().getPermissionSubscriptions( Loader.BROADCAST_CHANNEL_ADMINISTRATIVE );
		 * String colored = ChatColor.GRAY + "" + ChatColor.ITALIC + "[" + result + "]";
		 * 
		 * if ( sendToSource )
		 * {
		 * source.sendMessage( message );
		 * }
		 * 
		 * for ( Permissible obj : subscribed )
		 * {
		 * if ( obj instanceof InteractiveEntity )
		 * {
		 * InteractiveEntity target = ( InteractiveEntity ) obj;
		 * 
		 * if ( target != source )
		 * {
		 * target.sendMessage( colored );
		 * }
		 * }
		 * }
		 */
	}
	
	@Override
	public String toString()
	{
		return getClass().getName() + '(' + name + ')';
	}
}
