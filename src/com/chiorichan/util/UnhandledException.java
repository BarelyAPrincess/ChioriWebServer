package com.chiorichan.util;

public class UnhandledException extends Exception
{
	public String reason;
	
	public UnhandledException(String format, Throwable thrown)
	{
		super( thrown );
		reason = format;
	}
}
