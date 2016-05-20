/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2016 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.factory;

import java.util.Collections;
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
	Map<String, ScriptingContext> scriptStack = Maps.newLinkedHashMap();
	Map<String, ScriptingContext> scriptStackHistory = Maps.newLinkedHashMap();

	public List<ScriptTraceElement> examineStackTrace( StackTraceElement[] stackTrace )
	{
		Validate.notNull( stackTrace );

		List<ScriptTraceElement> scriptTrace = Lists.newLinkedList();

		for ( StackTraceElement ste : stackTrace )
			if ( ste.getFileName() != null && ste.getFileName().matches( "EvalScript\\d*\\.chi" ) )
				scriptTrace.add( new ScriptTraceElement( scriptStackHistory.get( ste.getFileName() ), ste ) );

		ScriptingContext context = scriptStack.values().toArray( new ScriptingContext[0] )[scriptStack.size() - 1];
		if ( context != null )
		{
			boolean contains = false;
			for ( ScriptTraceElement ste : scriptTrace )
				if ( ste.context().filename().equals( context.filename() ) )
					contains = true;
			if ( !contains )
				scriptTrace.add( 0, new ScriptTraceElement( context, "" ) );
		}

		return scriptTrace;
	}

	public Map<String, ScriptingContext> getScriptTrace()
	{
		return Collections.unmodifiableMap( scriptStack );
	}

	public Map<String, ScriptingContext> getScriptTraceHistory()
	{
		return Collections.unmodifiableMap( scriptStackHistory );
	}

	public void stack( String scriptName, ScriptingContext context )
	{
		scriptStackHistory.put( scriptName, context );
		scriptStack.put( scriptName, context );
	}

	/**
	 * Removes the last stacked {@link ScriptingContext} from the stack
	 */
	public void unstack()
	{
		if ( scriptStack.size() == 0 )
			return;
		String[] keys = scriptStack.keySet().toArray( new String[0] );
		scriptStack.remove( keys[keys.length - 1] );
	}
}