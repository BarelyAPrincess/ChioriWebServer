/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
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
