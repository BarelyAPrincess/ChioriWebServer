/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014, Atom Node LLC. All Right Reserved.
 */
package com.chiorichan.exceptions;

import com.chiorichan.factory.CodeMetaData;

/**
 *
 * @author Chiori Greene
 */
public class ShellExecuteException extends Exception
{
	private static final long serialVersionUID = -1611181613618341914L;
	
	CodeMetaData meta = new CodeMetaData();
	
	public ShellExecuteException(CodeMetaData _meta)
	{
		super();
		meta = _meta;
	}
	
	public ShellExecuteException(String message, CodeMetaData _meta)
	{
		super( message );
		meta = _meta;
	}
	
	public ShellExecuteException(String message, Throwable cause, CodeMetaData _meta)
	{
		super( message, cause );
		meta = _meta;
	}
	
	public ShellExecuteException(Throwable cause, CodeMetaData _meta)
	{
		super( cause );
		meta = _meta;
	}
	
	protected ShellExecuteException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, CodeMetaData _meta)
	{
		super( message, cause, enableSuppression, writableStackTrace );
		meta = _meta;
	}
	
	public CodeMetaData getCodeMetaData()
	{
		return meta;
	}
}
