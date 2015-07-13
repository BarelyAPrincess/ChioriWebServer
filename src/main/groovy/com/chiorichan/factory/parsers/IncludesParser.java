/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.factory.parsers;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.chiorichan.Loader;
import com.chiorichan.factory.EvalContext;
import com.chiorichan.factory.EvalFactory;
import com.chiorichan.factory.EvalResult;
import com.chiorichan.lang.ErrorReporting;
import com.chiorichan.lang.EvalException;
import com.chiorichan.site.Site;

/**
 * Using the {@link HTMLCommentParser} we attempt to parse the source for include comments, i.e., {@literal <!-- include(com.chiorichan.widget.menu) -->}
 */
public class IncludesParser extends HTMLCommentParser
{
	EvalContext context;
	EvalFactory factory;
	Site site;
	
	public IncludesParser()
	{
		super( "include" );
	}
	
	@Override
	public String resolveMethod( String... args ) throws Exception
	{
		if ( args.length > 2 )
			Loader.getLogger().warning( "EvalFactory: include() method only accepts one argument, ignored." );
		
		// TODO Prevent infinite loops!
		EvalResult result = factory.eval( EvalContext.fromAuto( context.site(), args[1] ).request( context.request() ) );
		
		if ( result.hasNotIgnorableExceptions() )
			ErrorReporting.throwExceptions( result.getExceptions() );
		else if ( result.hasIgnorableExceptions() )
		{
			StringBuilder sb = new StringBuilder();
			for ( EvalException e : result.getExceptions() )
				sb.append( ExceptionUtils.getStackTrace( e ) + "\n" );
			return sb.toString();
		}
		
		return result.getString();
	}
	
	public String runParser( String source, Site site, EvalContext context, EvalFactory factory ) throws Exception
	{
		this.site = site;
		this.factory = factory;
		this.context = context;
		
		return runParser( source );
	}
}
