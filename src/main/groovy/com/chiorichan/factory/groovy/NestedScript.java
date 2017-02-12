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

import groovy.lang.Binding;
import io.netty.buffer.ByteBufOutputStream;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import com.chiorichan.factory.api.Builtin;
import com.chiorichan.http.HttpRequestWrapper;

/**
 * Implements the builtin API by being extent by groovy nested classes
 */
public class NestedScript extends Builtin
{
	@SuppressWarnings( "unchecked" )
	public NestedScript()
	{
		HttpRequestWrapper request = HttpRequestWrapper.getRequest();
		Binding binding = new Binding( HttpRequestWrapper.getRequest().getBinding().getVariables() );
		try
		{
			binding.setProperty( "out", new PrintStream( new ByteBufOutputStream( request.getEvalFactory().getOutputStream() ), true, request.getEvalFactory().getCharset().name() ) );
		}
		catch ( UnsupportedEncodingException e )
		{
			e.printStackTrace();
		}
		// setBinding( binding );
		getBinding().getVariables().putAll( binding.getVariables() );
		// Setting the binding directly was causing an override of variable value from last class instance.
		// Possibly a link to the groovy MetaClass, hmm, thoughts on how to make use of this...
	}

	@Override
	public Object run()
	{
		return null;
	}
}
