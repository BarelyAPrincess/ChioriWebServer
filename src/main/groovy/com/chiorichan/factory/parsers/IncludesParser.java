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
import com.chiorichan.factory.EvalExecutionContext;
import com.chiorichan.factory.EvalFactory;
import com.chiorichan.factory.EvalFactoryResult;
import com.chiorichan.lang.ErrorReporting;
import com.chiorichan.site.Site;
import com.chiorichan.util.WebFunc;

/**
 * Using the {@link HTMLCommentParser} we attempt to parse the source for include comments, i.e., {@literal <!-- include(com.chiorichan.widget.menu) -->}
 */
public class IncludesParser extends HTMLCommentParser
{
	EvalExecutionContext context;
	EvalFactory factory;
	Site site;
	
	public IncludesParser()
	{
		super( "include" );
	}
	
	@Override
	public String resolveMethod( String... args ) throws Exception
	{
		if ( args.length > 1 )
			Loader.getLogger().warning( "CodeEvalFactory: include() method only accepts one argument, ignored." );
		
		// TODO Prevent infinite loops!
		EvalFactoryResult result = WebFunc.evalPackageWithException( context.request(), site, args[0] );
		
		if ( result.isSuccessful() )
			return result.getString();
		else if ( result.hasExceptions() )
			ErrorReporting.throwExceptions( result.getExceptions() );
		
		return "";
	}
	
	public String runParser( String source, Site site, EvalExecutionContext context, EvalFactory factory ) throws Exception
	{
		this.site = site;
		this.factory = factory;
		this.context = context;
		
		return runParser( source );
	}
}
