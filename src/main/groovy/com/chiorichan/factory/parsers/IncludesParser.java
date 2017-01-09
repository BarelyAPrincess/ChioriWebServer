/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * <p>
 * Copyright 2016 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.factory.parsers;

import com.chiorichan.lang.IException;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.chiorichan.factory.ScriptingContext;
import com.chiorichan.factory.ScriptingFactory;
import com.chiorichan.factory.ScriptingResult;
import com.chiorichan.lang.ExceptionReport;
import com.chiorichan.lang.ScriptingException;
import com.chiorichan.logger.Log;
import com.chiorichan.site.Site;

/**
 * Using the {@link HTMLCommentParser} we attempt to parse the source for include methods, i.e., {@literal <!-- include(com.chiorichan.widget.menu) -->}
 */
public class IncludesParser extends HTMLCommentParser
{
	ScriptingContext context;
	ScriptingFactory factory;
	Site site;

	public IncludesParser()
	{
		super( "include" );
	}

	@Override
	public String resolveMethod( String... args ) throws Exception
	{
		if ( args.length > 2 )
			Log.get( factory ).warning( "EvalFactory: include() method only accepts one argument, ignored." );

		// TODO Prevent infinite loops!
		ScriptingResult result = factory.eval( ScriptingContext.fromAuto( context.site(), args[1] ).request( context.request() ) );

		if ( result.hasNonIgnorableExceptions() )
			ExceptionReport.throwExceptions( result.getExceptions() );
		else if ( result.hasIgnorableExceptions() )
		{
			StringBuilder sb = new StringBuilder();
			for ( IException e : result.getExceptions() )
				if ( e instanceof Throwable )
					sb.append( ExceptionUtils.getStackTrace( ( Throwable ) e ) + "\n" );
			return sb.toString();
		}

		return result.getString();
	}

	public String runParser( String source, Site site, ScriptingContext context, ScriptingFactory factory ) throws Exception
	{
		this.site = site;
		this.factory = factory;
		this.context = context;

		return runParser( source );
	}
}
