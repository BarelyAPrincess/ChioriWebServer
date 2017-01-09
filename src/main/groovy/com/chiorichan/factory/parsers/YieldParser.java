/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2016 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
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
