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

import java.io.ByteArrayOutputStream;

import groovy.lang.GroovyShell;

import com.chiorichan.exception.ShellExecuteException;
import com.chiorichan.factory.CodeMetaData;

/**
 * Groovy SeaShell.
 * More of another dummy SeaShell to evaluate groovy files.
 * 
 * @author Chiori Greene
 */
public class GroovyInterpreter implements Interpreter
{
	@Override
	public String[] getHandledTypes()
	{
		return new String[] { "groovy" };
	}
	
	@Override
	public String eval( CodeMetaData meta, String code, GroovyShell shell, ByteArrayOutputStream bs ) throws ShellExecuteException
	{
		try
		{
			shell.setVariable( "__FILE__", meta.fileName );
			
			Object o = shell.evaluate( code );
			return ( o != null ) ? o.toString() : "";
		}
		catch ( Throwable e )
		{
			if ( e instanceof ShellExecuteException )
				throw (ShellExecuteException) e;
			else
				throw new ShellExecuteException( e, meta );
		}
	}
}
