package com.chiorichan;

public class StartupException extends RuntimeException
{
	private static final long serialVersionUID = 13L;
	
	public StartupException(String string)
	{
		super( string );
	}
}
