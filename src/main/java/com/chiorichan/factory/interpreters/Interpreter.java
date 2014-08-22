/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2014, Atom Node LLC. All Right Reserved.
 */

package com.chiorichan.factory.interpreters;

import groovy.lang.GroovyShell;

import java.io.ByteArrayOutputStream;

import com.chiorichan.exceptions.ShellExecuteException;
import com.chiorichan.factory.CodeMetaData;

/**
 * Interpreters are commonly used to process commands and create dynamic content, most notably the groovy scripts.
 * 
 * @author Chiori Greene
 */
public interface Interpreter
{
	/**
	 * Response "all" will attempt to eval any and everything. Don't abuse it!
	 * Returning null will continue to next available Interpreter that handles type.
	 * @return
	 */
	public String[] getHandledTypes();
	public String eval( CodeMetaData meta, String code, GroovyShell shell, ByteArrayOutputStream bs ) throws ShellExecuteException;
}
