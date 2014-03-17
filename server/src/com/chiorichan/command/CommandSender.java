package com.chiorichan.command;

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
	 * Gets the name of this command sender
	 * 
	 * @return Name of the sender
	 */
	public String getName();
	
	/**
	 * Allows the pausing of command processing
	 * 
	 * @param b
	 */
	public void pauseInput( boolean b );
}
