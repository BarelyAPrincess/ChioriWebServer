/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.factory.processors;

import groovy.lang.GroovyShell;
import groovy.lang.Script;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.chiorichan.factory.EvalContext;
import com.chiorichan.factory.groovy.GroovyRegistry;

/**
 * Groovy Script Processor
 */
public class GroovyScriptProcessor implements ScriptingProcessor
{
	@Override
	public boolean eval( EvalContext context ) throws Exception
	{
		try
		{
			GroovyShell shell = GroovyRegistry.getNewShell( context );
			Script script = GroovyRegistry.makeScript( shell, context );
			
			context.result().object( script.run() );
			
		}
		catch ( Throwable t )
		{
			// Clear the input source code and replace it with the exception stack trace
			context.resetAndWrite( ExceptionUtils.getStackTrace( t ) );
			throw t;
		}
		
		return true;
	}
	
	@Override
	public String[] getHandledTypes()
	{
		return new String[] {"groovy"};
	}
}
