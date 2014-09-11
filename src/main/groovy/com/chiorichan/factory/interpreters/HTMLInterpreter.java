/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.factory.interpreters;

import groovy.lang.GroovyShell;

import java.io.ByteArrayOutputStream;

import com.chiorichan.exceptions.ShellExecuteException;
import com.chiorichan.factory.CodeMetaData;

/**
 * Simple HTML SeaShell.
 * More of a dummy to handle simple html files.
 * 
 * @author Chiori Greene
 */
public class HTMLInterpreter implements Interpreter
{
	@Override
	public String[] getHandledTypes()
	{
		return new String[] { "plain", "text", "txt", "html", "htm" };
	}
	
	@Override
	public String eval( CodeMetaData meta, String code, GroovyShell shell, ByteArrayOutputStream bs ) throws ShellExecuteException
	{
		return code;
	}
}
