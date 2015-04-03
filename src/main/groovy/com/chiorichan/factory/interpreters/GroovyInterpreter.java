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

import groovy.lang.Script;

import java.io.ByteArrayOutputStream;

import com.chiorichan.factory.EvalMetaData;
import com.chiorichan.factory.ShellFactory;

/**
 * Groovy SeaShell.
 * More of another dummy SeaShell to evaluate groovy files.
 */
public class GroovyInterpreter implements Interpreter
{
	@Override
	public String[] getHandledTypes()
	{
		return new String[] {"groovy"};
	}
	
	@Override
	public Object eval( EvalMetaData meta, String scriptText, ShellFactory shell, ByteArrayOutputStream bs ) throws Exception
	{
		Script script = shell.makeScript( scriptText, meta );
		
		Object o = script.run();
		
		// Object o = shell.evaluate( code );
		return ( o == null ) ? "" : o;
	}
}
