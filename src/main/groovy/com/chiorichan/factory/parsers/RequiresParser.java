/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2016 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.factory.parsers;

import com.chiorichan.factory.ScriptingContext;
import com.chiorichan.factory.ScriptingFactory;
import com.chiorichan.factory.ScriptingResult;
import com.chiorichan.lang.ExceptionReport;
import com.chiorichan.logger.Log;
import com.chiorichan.site.Site;

/**
 * Using the {@link HTMLCommentParser} we attempt to parse the source for require methods, i.e., {@literal <!-- require(com.chiorichan.widget.menu) -->}
 */
public class RequiresParser extends HTMLCommentParser
{
	ScriptingContext context;
	ScriptingFactory factory;
	Site site;

	public RequiresParser()
	{
		super( "require" );
	}

	@Override
	public String resolveMethod( String... args ) throws Exception
	{
		if ( args.length > 2 )
			Log.get( factory ).warning( "EvalFactory: require() method only accepts one argument, ignored." );

		// TODO Prevent infinite loops!
		ScriptingResult result = factory.eval( ScriptingContext.fromPackage( context.site(), args[1] ).request( context.request() ).require() );

		if ( result.hasExceptions() )
			ExceptionReport.throwExceptions( result.getExceptions() );

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
