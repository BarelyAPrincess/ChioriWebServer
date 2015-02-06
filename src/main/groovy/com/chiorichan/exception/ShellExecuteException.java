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

import com.chiorichan.factory.CodeMetaData;

/**
 *
 * @author Chiori Greene
 */
public class ShellExecuteException extends Exception
{
	private static final long serialVersionUID = -1611181613618341914L;
	
	CodeMetaData meta = new CodeMetaData();
	
	public ShellExecuteException( CodeMetaData meta )
	{
		super();
		this.meta = meta;
	}
	
	public ShellExecuteException( String message, CodeMetaData meta )
	{
		super( message );
		this.meta = meta;
	}
	
	public ShellExecuteException( String message, Throwable cause, CodeMetaData meta )
	{
		super( message, cause );
		this.meta = meta;
	}
	
	public ShellExecuteException( Throwable cause, CodeMetaData meta )
	{
		super( cause );
		this.meta = meta;
	}
	
	protected ShellExecuteException( String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, CodeMetaData meta )
	{
		super( message, cause, enableSuppression, writableStackTrace );
		this.meta = meta;
	}
	
	public CodeMetaData getCodeMetaData()
	{
		return meta;
	}
}
