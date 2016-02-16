package com.chiorichan.lang;

public class DiedException extends EvalException
{
	public DiedException( String msg )
	{
		super( ReportingLevel.E_IGNORABLE, msg );
	}
}
