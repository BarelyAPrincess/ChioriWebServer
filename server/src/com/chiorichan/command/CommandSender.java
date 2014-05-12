package com.chiorichan.command;

import java.io.IOException;

import com.chiorichan.auth.AuthHandler;
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
	
	/**
	 * Just a recommended way to ping the CommandSender to try and prompt if a AuthHandler is pending.
	 */
	public boolean promptForAuth() throws IOException;
	
	/**
	 * Called when an AuthHandler needs to be satisfied.
	 * Method can either cache the AuthHandler for later or try and satisfy on the spot.
	 * Recommended that you try and not block the IO since the requester might not respond properly.
	 */
	public void addAuthHandler( AuthHandler ah );
}
