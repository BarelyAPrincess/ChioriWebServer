package com.chiorichan.framework;

public class SiteException extends Exception
{
	private static final long serialVersionUID = 8856241361601633171L;
	
	public SiteException( String reason )
	{
		super( reason );
	}
	
	public SiteException( Exception e )
	{
		super( e );
	}
}
