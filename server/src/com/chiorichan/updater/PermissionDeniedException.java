package com.chiorichan.updater;

public class PermissionDeniedException extends DownloadException
{
	private static final long serialVersionUID = 2L;
	
	public PermissionDeniedException(String message, Throwable cause)
	{
		super( message, cause );
	}
	
	public PermissionDeniedException(Throwable cause)
	{
		this( null, cause );
	}
	
	public PermissionDeniedException(String message)
	{
		this( message, null );
	}
	
	public PermissionDeniedException()
	{
		this( null, null );
	}
}
