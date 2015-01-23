/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.exception;

public class UnhandledException extends Exception
{
	private static final long serialVersionUID = -7820557499294033093L;
	public String reason;
	
	public UnhandledException(String format, Throwable thrown)
	{
		super( thrown );
		reason = format;
	}
}
