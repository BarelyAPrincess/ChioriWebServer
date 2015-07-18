/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.factory.parsers;

import com.chiorichan.Loader;
import com.chiorichan.factory.ScriptingContext;
import com.chiorichan.factory.ScriptingFactory;
import com.chiorichan.factory.ScriptingResult;
import com.chiorichan.lang.ErrorReporting;
import com.chiorichan.site.Site;

/**
 * Using the {@link HTMLCommentParser} we attempt to parse the source for include comments, i.e., {@literal <!-- include(com.chiorichan.widget.menu) -->}
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
			Loader.getLogger().warning( "EvalFactory: require() method only accepts one argument, ignored." );
		
		// TODO Prevent infinite loops!
		ScriptingResult result = factory.eval( ScriptingContext.fromAuto( context.site(), args[1] ).request( context.request() ) );
		
		if ( result.hasExceptions() )
			ErrorReporting.throwExceptions( result.getExceptions() );
		
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
