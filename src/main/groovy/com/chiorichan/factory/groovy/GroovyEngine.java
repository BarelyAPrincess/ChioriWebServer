/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * <p>
 * Copyright 2016 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.factory.groovy;

import com.chiorichan.logger.Log;
import com.google.common.base.Joiner;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import com.chiorichan.factory.ScriptBinding;
import com.chiorichan.factory.ScriptingContext;
import com.chiorichan.factory.ScriptingEngine;

/**
 * Groovy Script Engine
 */
public class GroovyEngine implements ScriptingEngine
{
	private Binding binding = new Binding();
	private GroovyRegistry registry;

	public GroovyEngine( GroovyRegistry registry )
	{
		this.registry = registry;
	}

	@Override
	public boolean eval( ScriptingContext context ) throws Exception
	{
		try
		{
			Script script = GroovyRegistry.getCachedScript( context, binding );

			if ( script == null )
			{
				GroovyShell shell = registry.getNewShell( context, binding );
				script = registry.makeScript( shell, context );
			}

			context.result().setScript( script );
			context.result().setObject( script.run() );
		}
		catch ( Throwable t )
		{
			// Clear the input source code and replace it with the exception stack trace
			// context.resetAndWrite( ExceptionUtils.getStackTrace( t ) );
			context.reset();
			throw t;
		}

		return true;
	}

	@Override
	public List<String> getTypes()
	{
		return Arrays.asList( "groovy" );
	}

	@Override
	public void setBinding( ScriptBinding binding )
	{
		// Groovy Binding will keep the original EvalBinding map updated automatically. YAY!
		this.binding = new Binding( binding.getVariables() );
	}

	@Override
	public void setOutput( ByteBuf buffer, Charset charset )
	{
		try
		{
			binding.setProperty( "out", new PrintStream( new ByteBufOutputStream( buffer ), true, charset.name() ) );
		}
		catch ( UnsupportedEncodingException e )
		{
			e.printStackTrace();
		}
	}
}
