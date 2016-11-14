package com.chiorichan.lang;

/**
 * Thrown when two maps are joined and have conflicting keys
 */
public class MapCollisionException extends ApplicationException
{
	public MapCollisionException()
	{
		super( ReportingLevel.E_IGNORABLE );
	}

	@Override
	public boolean handle( ExceptionReport report, ExceptionContext context )
	{
		return false;
	}
}
