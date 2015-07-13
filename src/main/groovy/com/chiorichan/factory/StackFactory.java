/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.factory;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Sits as an interface between GroovyShell and Interpreters
 */
public class StackFactory
{
	Map<String, EvalContext> scriptStack = Maps.newLinkedHashMap();
	
	public List<ScriptTraceElement> examineStackTrace( StackTraceElement[] stackTrace )
	{
		Validate.notNull( stackTrace );
		
		List<ScriptTraceElement> scriptTrace = Lists.newLinkedList();
		
		for ( StackTraceElement ste : stackTrace )
			if ( ste.getFileName() != null && ste.getFileName().matches( "GroovyScript\\d*\\.chi" ) )
				scriptTrace.add( new ScriptTraceElement( scriptStack.get( ste.getFileName() ), ste ) );
		
		return scriptTrace;
	}
	
	public void stack( String scriptName, EvalContext context )
	{
		scriptStack.put( scriptName, context );
	}
	
	/**
	 * Removes the last stacked {@link EvalContext} from the stack
	 */
	public void unstack()
	{
		String[] keys = scriptStack.keySet().toArray( new String[0] );
		scriptStack.remove( keys[keys.length - 1] );
	}
}
