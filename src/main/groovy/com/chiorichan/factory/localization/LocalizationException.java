package com.chiorichan.factory.localization;

import com.chiorichan.lang.ReportingLevel;
import com.chiorichan.lang.ScriptingException;

public class LocalizationException extends ScriptingException
{
	public LocalizationException( String message )
	{
		super( ReportingLevel.E_WARNING, message );
	}

	public LocalizationException( String message, Throwable cause )
	{
		super( ReportingLevel.E_WARNING, message, cause );
	}
}
