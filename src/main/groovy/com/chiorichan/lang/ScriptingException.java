/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2016 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.lang;

import java.util.List;

import com.chiorichan.factory.ScriptTraceElement;
import com.chiorichan.factory.ScriptingContext;
import com.chiorichan.factory.StackFactory;

/**
 * Carries extra information for debugging when an {@link Exception} is thrown by the ScriptingFactory
 */
public class ScriptingException extends ApplicationException
{
	private List<ScriptTraceElement> scriptTrace = null;

	public ScriptingException( ReportingLevel level )
	{
		super( level );
	}

	public ScriptingException( ReportingLevel level, String message )
	{
		super( level, message );
	}

	public ScriptingException( ReportingLevel level, String message, Throwable cause )
	{
		super( level, message, cause );
	}

	public ScriptingException( ReportingLevel level, Throwable cause )
	{
		super( level, cause );
	}

	@Override
	public String getMessage()
	{
		if ( isScriptingException() )
		{
			ScriptTraceElement element = getScriptTrace()[0];
			Throwable t = getCause() == null ? this : getCause();
			return String.format( "Exception %s thrown in file '%s' at line %s:%s, message '%s'", t.getClass().getName(), element.context().filename(), element.getLineNumber(), element.getColumnNumber() > 0 ? element.getColumnNumber() : 0, super.getMessage() );
		}
		else
		{
			Throwable t = getCause() == null ? this : getCause();
			return String.format( "Exception %s thrown in file '%s' at line %s, message '%s'", t.getClass().getName(), t.getStackTrace()[0].getFileName(), t.getStackTrace()[0].getLineNumber(), super.getMessage() );
		}
	}

	public ScriptTraceElement[] getScriptTrace()
	{
		return scriptTrace == null ? null : scriptTrace.toArray( new ScriptTraceElement[0] );
	}

	@Override
	public boolean handle( ExceptionReport report, ExceptionContext context )
	{
		/**
		 * Forward this type of exception to the report
		 */
		if ( context instanceof ScriptingContext )
			populateScriptTrace( ( ( ScriptingContext ) context ).factory().stack() );
		report.addException( level, this );
		return isIgnorable() ? false : true;
	}

	public boolean hasScriptTrace()
	{
		return scriptTrace != null && scriptTrace.size() > 0;
	}

	public boolean isScriptingException()
	{
		return getCause() != null && getCause().getStackTrace().length > 0 && getCause().getStackTrace()[0].getClassName().startsWith( "org.codehaus.groovy.runtime" );
	}

	public ScriptingException populateScriptTrace( StackFactory factory )
	{
		scriptTrace = factory.examineStackTrace( getCause() == null ? getStackTrace() : getCause().getStackTrace() );
		return this;
	}
}
