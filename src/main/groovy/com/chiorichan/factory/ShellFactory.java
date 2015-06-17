/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.factory;

import groovy.lang.GroovyShell;
import groovy.lang.Script;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import com.chiorichan.util.WebFunc;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Sits as an interface between GroovyShell and Interpreters
 * 
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
public class ShellFactory
{
	Map<String, EvalExecutionContext> scriptHistory = Maps.newLinkedHashMap();
	GroovyShell shell = null;
	
	ShellFactory setShell( GroovyShell shell )
	{
		this.shell = shell;
		return this;
	}
	
	/**
	 * This method is provided purely for convenience
	 * It is highly discouraged to use the parse, run or evaluate methods within
	 * as it will bypass the servers script stack tracing mechanism
	 * 
	 * @return The GroovyShell backing this ShellFactory, which changes with each script execute
	 */
	public GroovyShell getGroovyShell()
	{
		return shell;
	}
	
	public List<ScriptTraceElement> examineStackTrace( StackTraceElement[] stackTrace )
	{
		Validate.notNull( stackTrace );
		
		List<ScriptTraceElement> scriptTrace = Lists.newLinkedList();
		
		for ( StackTraceElement ste : stackTrace )
			if ( ste.getFileName() != null && ste.getFileName().matches( "GroovyScript\\d*\\.chi" ) )
				scriptTrace.add( new ScriptTraceElement( scriptHistory.get( ste.getFileName() ), ste ) );
		
		return scriptTrace;
	}
	
	public Script makeScript( EvalExecutionContext context )
	{
		return makeScript( context.readString(), context );
	}
	
	public Script makeScript( String source, EvalExecutionContext context )
	{
		String scriptName = "GroovyScript" + WebFunc.randomNum( 8 ) + ".chi";
		Script script = shell.parse( source, scriptName );
		
		context.script( scriptName, script );
		
		scriptHistory.put( scriptName, context );
		
		return script;
	}
	
	void onFinished()
	{
		scriptHistory.clear();
		shell = null;
	}
}
