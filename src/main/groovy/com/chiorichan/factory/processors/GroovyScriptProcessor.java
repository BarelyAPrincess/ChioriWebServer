/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.factory.processors;

import groovy.lang.Script;

import com.chiorichan.factory.EvalExecutionContext;
import com.chiorichan.factory.ShellFactory;

/**
 * Groovy SeaShell.
 * More of another dummy SeaShell to evaluate groovy files.
 */
public class GroovyScriptProcessor implements ScriptingProcessor
{
	@Override
	public String[] getHandledTypes()
	{
		return new String[] {"groovy"};
	}
	
	@Override
	public boolean eval( EvalExecutionContext context, ShellFactory shell ) throws Exception
	{
		Script script = shell.makeScript( context );
		context.result().object( script.run() );
		return true;
	}
}
