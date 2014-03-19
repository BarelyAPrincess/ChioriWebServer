package com.chiorichan;


public class StartupException extends RuntimeException
{
	private static final long serialVersionUID = 13L;
	
	public StartupException(String msg)
	{
		super( msg );
	}

	public StartupException(Throwable e)
	{
		super( e );
	}
	
	public StartupException(String msg, Throwable e)
	{
		super( msg, e );
	}
}
