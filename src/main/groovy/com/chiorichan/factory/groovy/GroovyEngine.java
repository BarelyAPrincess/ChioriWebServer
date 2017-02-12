/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.factory.groovy;

import com.chiorichan.factory.ScriptBinding;
import com.chiorichan.factory.ScriptingContext;
import com.chiorichan.factory.ScriptingEngine;
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
