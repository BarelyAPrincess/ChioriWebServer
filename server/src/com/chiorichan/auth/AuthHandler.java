package com.chiorichan.auth;

import java.io.IOException;

import jline.console.ConsoleReader;

import com.chiorichan.command.CommandSender;
import com.chiorichan.command.ConsoleCommandSender;

public abstract class AuthHandler
{
	/**
	 * Will keep getting called until it returns true, which indicates that auth prompting was successfully satisfied.
	 * 
	 * @param reader
	 * @param sender
	 * @return
	 * @throws IOException
	 */
	public boolean promptForAuth( ConsoleCommandSender sender, ConsoleReader reader ) throws IOException
	{
		String prompt = getPrompt();
		if ( prompt != null && !prompt.isEmpty() )
			sender.sendMessage( prompt );
		
		String response = reader.readLine( "?>", '*' );
		
		if ( response != null )
		{
			AuthResponse answer = processResponse( response );
			
			if ( answer != null )
			{
				sender.sendMessage( answer.getReason() );
				return answer.getSuccess();
			}
			
			return false;
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * CURRENTLY NOT IMPLEMENTED
	 * 
	 * @param sender
	 * @return
	 */
	public boolean promptForAuth( CommandSender sender )
	{
		return true;
	}
	
	/**
	 * Will get called to retrive what the prompt should read.
	 * 
	 * @return
	 */
	public abstract String getPrompt();
	
	/**
	 * Called with the response the user made to the AuthHandler request.
	 * Return null for correct response.
	 * 
	 * @param line
	 * @return
	 */
	public abstract AuthResponse processResponse( String line );
}
