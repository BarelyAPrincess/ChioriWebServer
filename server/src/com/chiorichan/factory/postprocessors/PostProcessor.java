/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2014, Chiori Greene. All Right Reserved.
 */

package com.chiorichan.factory.postprocessors;

import com.chiorichan.factory.CodeMetaData;

/**
 * PostProcessors are commonly used to compress/optimize final files into something smaller in size
 * to improve load times and bandwidth load.
 *  
 * @author Chiori Greene
 */
public interface PostProcessor
{
	/**
	 * "all" will attempt to process any and everything. Don't abuse it!
	 * Returning null will continue to next available PreProcessor that handles type.
	 * @return String array of types this PreProcessor can handle. {"text/css", "css", "js", "application/javascript-x"}
	 */
	public String[] getHandledTypes();
	public String process( CodeMetaData meta, String code );
}