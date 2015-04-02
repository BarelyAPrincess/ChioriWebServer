/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.factory.interpreters;

import groovy.lang.GroovyShell;

import java.io.ByteArrayOutputStream;

import com.chiorichan.exception.ShellExecuteException;
import com.chiorichan.factory.EvalMetaData;

/**
 * Interpreters are commonly used to process commands and create dynamic content, most notably the groovy scripts.
 */
public interface Interpreter
{
	/**
	 * Response "all" will attempt to evaluate everything. Don't abuse it!
	 * Returning null will continue to next available Interpreter that handles type.
	 * 
	 * @return String array of types handled by the Interpreter
	 */
	String[] getHandledTypes();
	
	Object eval( EvalMetaData meta, String code, GroovyShell shell, ByteArrayOutputStream bs ) throws ShellExecuteException;
}
