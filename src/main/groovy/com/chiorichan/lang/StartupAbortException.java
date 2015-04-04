/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.lang;


public class StartupAbortException extends StartupException
{
	private static final long serialVersionUID = -4937198089020390887L;
	
	public StartupAbortException()
	{
		super( "STARTUP ABORTED!" );
	}
}
