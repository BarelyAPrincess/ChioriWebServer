package com.chiorichan.auth;

import com.chiorichan.util.StringUtil;

public class ConsoleAuth extends AuthHandler
{
	private String consolePassword = null;
	
	@Override
	public String getPrompt()
	{
		return "&dThis console is password protected. Please enter the password to unlock it.";
	}
	
	@Override
	public AuthResponse processResponse( String line )
	{
		if ( StringUtil.md5( line ).equals( consolePassword ) || line.equals( consolePassword ) )
		{
			return new AuthResponse( "&aConsole Unlocked! Thank you for using Console Security Systems. :P", true );
		}
		else
		{
			return new AuthResponse( "&4Incorrect Password!", false );
		}
	}
	
	public void setPassword( String _consolePassword )
	{
		consolePassword = _consolePassword.trim();
	}
}
