package com.chiorichan.exceptions;

public class UnhandledException extends Exception
{
	private static final long serialVersionUID = -7820557499294033093L;
	public String reason;
	
	public UnhandledException(String format, Throwable thrown)
	{
		super( thrown );
		reason = format;
	}
}
