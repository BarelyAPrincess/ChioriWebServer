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

import com.chiorichan.factory.ScriptingFactory;
import com.chiorichan.logger.Log;

/**
 * Using the {@link HTMLCommentParser} we attempt to parse the source for yield methods, i.e., {@literal <!-- yield(com.chiorichan.widget.menu) -->}
 */
public class YieldParser extends HTMLCommentParser
{
	ScriptingFactory factory;

	public YieldParser()
	{
		super( "yield" );
	}

	@Override
	public String resolveMethod( String... args ) throws Exception
	{
		if ( args.length > 2 )
			Log.get( factory ).warning( "EvalFactory: yield() method only accepts one argument, ignored." );

		return factory.getYieldBuffer().get( args[1] );
	}

	public String runParser( String source, ScriptingFactory factory ) throws Exception
	{
		this.factory = factory;

		return runParser( source );
	}
}
