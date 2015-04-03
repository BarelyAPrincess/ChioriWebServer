/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.framework;

import java.io.IOException;

import com.chiorichan.exception.ShellExecuteException;
import com.chiorichan.factory.EvalFactoryResult;
import com.chiorichan.session.SessionProvider;

public class HttpUtilsWrapper extends WebUtils
{
	SessionProvider sess;
	
	public HttpUtilsWrapper( SessionProvider sess )
	{
		this.sess = sess;
	}
	
	// TODO Improve and differ what eval and read do
	
	public EvalFactoryResult evalFile( String file ) throws IOException, ShellExecuteException
	{
		return evalFile( sess.getCodeFactory(), sess.getParentSession().getSite(), file );
	}
	
	public EvalFactoryResult evalPackage( String pack ) throws ShellExecuteException
	{
		return evalPackage( sess.getCodeFactory(), sess.getParentSession().getSite(), pack );
	}
	
	public EvalFactoryResult evalPackageWithException( String pack, Object... global ) throws IOException, ShellExecuteException
	{
		return evalPackageWithException( sess.getCodeFactory(), sess.getParentSession().getSite(), pack );
	}
	
	public EvalFactoryResult evalPackageWithException( String pack ) throws IOException, ShellExecuteException
	{
		return evalPackageWithException( sess.getCodeFactory(), sess.getParentSession().getSite(), pack );
	}
	
	public String readFile( String file ) throws IOException, ShellExecuteException
	{
		return evalFile( sess.getCodeFactory(), sess.getParentSession().getSite(), file ).getString();
	}
	
	public String readPackage( String pack ) throws ShellExecuteException
	{
		return evalPackage( sess.getCodeFactory(), sess.getParentSession().getSite(), pack ).getString();
	}
	
	public String readPackageWithException( String pack, Object... global ) throws IOException, ShellExecuteException
	{
		return evalPackageWithException( sess.getCodeFactory(), sess.getParentSession().getSite(), pack ).getString();
	}
	
	public String readPackageWithException( String pack ) throws IOException, ShellExecuteException
	{
		return evalPackageWithException( sess.getCodeFactory(), sess.getParentSession().getSite(), pack ).getString();
	}
}
