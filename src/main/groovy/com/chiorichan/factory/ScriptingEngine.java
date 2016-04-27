/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2016 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.factory;

import io.netty.buffer.ByteBuf;

import java.nio.charset.Charset;
import java.util.List;

import com.chiorichan.lang.ScriptingException;

/**
 * Interpreters are commonly used to process commands and create dynamic content, most notably the groovy scripts.
 * ScriptingEngines are kept persistent for exactly one full request.
 */
public interface ScriptingEngine
{
	/**
	 * Called to evaluate the provided context based on registered content types and extensions
	 * Returning false} will continue to next available ScriptingProcessor.
	 * 
	 * @param context
	 *            The EvalContext
	 * @return
	 *         Context finished execution
	 * @throws Exception
	 *             Provided simply for convenience, keep in mind that if any unique exceptions need special handling
	 *             when thrown, you will need to register them with {@link ScriptingException#registerException(EvalCallback, Class...)}.
	 */
	boolean eval( ScriptingContext context ) throws Exception;
	
	/**
	 * Called on each instance to register what types this engine will handle
	 *
	 * @return types Array of content types and file extensions that provided {@link ScriptingEngine} can handle, e.g., "text/css", "css", "js", or "application/javascript-x".
	 *         Leaving this field empty will catch everything, please don't abuse the power.
	 */
	List<String> getTypes();
	
	/**
	 * Called to provide output stream to ScriptingEngine
	 * 
	 * @param buffer
	 *            The ByteBuf output stream
	 * @param charset
	 *            The current EvalFactory character set
	 */
	void setOutput( ByteBuf buffer, Charset charset );
	
	/**
	 * Called to provide the EvalFactory bindings
	 * 
	 * @param binding
	 *            The EvalFactory binding
	 */
	void setBinding( ScriptBinding binding );
}
