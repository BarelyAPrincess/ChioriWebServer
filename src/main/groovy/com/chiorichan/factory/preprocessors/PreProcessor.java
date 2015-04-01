/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.factory.preprocessors;

import com.chiorichan.factory.EvalMetaData;

/**
 * PreProcessors are commonly used to convert files into something readable by either an Interpreter and/or Browser,
 * i.e., Convert CoffeeScripts to Javascript, Less and Sass to CSS.
 */
public interface PreProcessor
{
	/**
	 * "all" will attempt to process any and everything. Don't abuse it!
	 * Returning null will continue to next available PreProcessor that handles type.
	 * 
	 * @return String array of types this PreProcessor can handle. {"text/css", "css", "js", "application/javascript-x"}
	 */
	String[] getHandledTypes();
	
	String process( EvalMetaData meta, String code );
}
