package com.chiorichan.command;

import com.chiorichan.Loader;
import com.chiorichan.permissions.Permissible;

public interface CommandSender extends Permissible
{
	
	/**
	 * Sends this sender a message
	 * 
	 * @param message
	 *           Message to be displayed
	 */
	public void sendMessage( String message );
	
	/**
	 * Sends this sender multiple messages
	 * 
	 * @param messages
	 *           An array of messages to be displayed
	 */
	public void sendMessage( String[] messages );
	
	/**
	 * Returns the server instance that this command is running on
	 * 
	 * @return Server instance
	 */
	public Loader getServer();
	
	/**
	 * Gets the name of this command sender
	 * 
	 * @return Name of the sender
	 */
	public String getName();
}
