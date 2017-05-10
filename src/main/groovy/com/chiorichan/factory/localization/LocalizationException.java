/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Joel Greene <joel.greene@penoaks.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
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
