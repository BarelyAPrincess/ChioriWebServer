/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.framework;

import java.io.IOException;

import com.chiorichan.exceptions.ShellExecuteException;
import com.chiorichan.http.PersistentSession;

public class HttpUtilsWrapper extends WebUtils
{
	PersistentSession sess;
	
	public HttpUtilsWrapper(PersistentSession _sess)
	{
		sess = _sess;
	}
	
	public String evalFile( String file ) throws IOException, ShellExecuteException
	{
		return evalFile( sess.getCodeFactory(), sess.getSite(), file );
	}
	
	public String evalPackage( String pack ) throws IOException, ShellExecuteException
	{
		return evalPackage( sess.getCodeFactory(), sess.getSite(), pack );
	}
}
