/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Joel Greene <joel.greene@penoaks.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.factory;

import io.netty.buffer.ByteBuf;

import java.nio.charset.Charset;
import java.util.List;

/**
 * ScriptingEngines are commonly used to process commands and create dynamic content, most notably the groovy scripts.
 * ScriptingEngines are kept persistent for exactly one full request.
 */
public interface ScriptingEngine
{
	/**
	 * Called to evaluate the provided context based on registered content types and extensions
	 * Returning false} will continue to next available ScriptingProcessor.
	 *
	 * @param context The EvalContext
	 * @return Context finished execution
	 * @throws Exception Provided simply for convenience, keep in mind that if any unique exceptions need special handling when thrown.
	 */
	boolean eval( ScriptingContext context ) throws Exception;

	/**
	 * Called on each instance to register what types this engine will handle
	 *
	 * @return types Array of content types and file extensions that provided {@link ScriptingEngine} can handle, e.g., "text/css", "css", "js", or "application/javascript-x".
	 * Leaving this field empty will catch everything, please don't abuse the power.
	 */
	List<String> getTypes();

	/**
	 * Called to provide output stream to ScriptingEngine
	 *
	 * @param buffer  The ByteBuf output stream
	 * @param charset The current EvalFactory character set
	 */
	void setOutput( ByteBuf buffer, Charset charset );

	/**
	 * Called to provide the EvalFactory bindings
	 *
	 * @param binding The EvalFactory binding
	 */
	void setBinding( ScriptBinding binding );
}
