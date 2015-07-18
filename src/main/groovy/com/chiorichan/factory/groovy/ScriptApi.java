/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.factory.groovy;

import groovy.lang.Binding;
import io.netty.buffer.ByteBufOutputStream;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import com.chiorichan.http.HttpRequestWrapper;

/**
 * Implements the Groovy API by being extent by groovy nested classes
 */
public class ScriptApi extends ScriptApiBase
{
	@SuppressWarnings( "unchecked" )
	public ScriptApi()
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
