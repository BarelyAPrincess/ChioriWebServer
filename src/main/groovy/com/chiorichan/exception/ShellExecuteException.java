/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.exception;

import groovy.lang.Script;

import com.chiorichan.factory.EvalFactory;
import com.chiorichan.factory.EvalMetaData;
import com.chiorichan.factory.ScriptingBaseJava;

/**
 *
 * @author Chiori Greene
 */
public class ShellExecuteException extends Exception
{
	private static final long serialVersionUID = -1611181613618341914L;
	
	EvalMetaData meta = new EvalMetaData();
	
	public ShellExecuteException( EvalMetaData meta )
	{
		super();
		this.meta = meta;
	}
	
	public ShellExecuteException( String message, EvalMetaData meta )
	{
		super( message );
		this.meta = meta;
	}
	
	public ShellExecuteException( String message, Throwable cause, EvalMetaData meta )
	{
		super( message, cause );
		this.meta = meta;
	}
	
	public ShellExecuteException( Throwable cause, EvalMetaData meta, Class<?> cls )
	{
		super( cause );
		
		Script script = EvalFactory.getScript( cls );
		
		if ( script != null && script instanceof ScriptingBaseJava )
		{
			ScriptingBaseJava base = ( ScriptingBaseJava ) script;
			if ( base.getMeta() != null )
				meta = base.getMeta();
		}
		
		this.meta = meta;
	}
	
	public ShellExecuteException( Throwable cause, EvalMetaData meta )
	{
		super( cause );
		this.meta = meta;
	}
	
	protected ShellExecuteException( String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, EvalMetaData meta )
	{
		super( message, cause, enableSuppression, writableStackTrace );
		this.meta = meta;
	}
	
	public EvalMetaData getMetaData()
	{
		return meta;
	}
}
