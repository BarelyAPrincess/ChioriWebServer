/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.factory.postprocessors;

import com.chiorichan.factory.EvalMetaData;

/**
 * PostProcessors are commonly used to compress/optimize final files into something smaller in size
 * to improve load times and bandwidth load.
 */
public interface PostProcessor
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
