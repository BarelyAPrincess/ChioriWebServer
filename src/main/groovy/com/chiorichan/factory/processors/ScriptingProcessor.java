/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.factory.processors;

import com.chiorichan.factory.EvalContext;

/**
 * Interpreters are commonly used to process commands and create dynamic content, most notably the groovy scripts.
 *
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
public interface ScriptingProcessor
{
	/**
	 * "all" will attempt to process any and everything. Don't abuse it!
	 * Returning null will continue to next available ScriptingProcessor available.
	 * 
	 * @return String array of types this PreProcessor can handle. {"text/css", "css", "js", "application/javascript-x"}
	 */
	String[] getHandledTypes();
	
	boolean eval( EvalContext context ) throws Exception;
}
