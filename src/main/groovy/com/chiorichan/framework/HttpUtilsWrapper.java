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

import com.chiorichan.factory.EvalFactoryResult;
import com.chiorichan.lang.EvalFactoryException;
import com.chiorichan.session.SessionProvider;
import com.chiorichan.util.WebUtils;

@Deprecated
public class HttpUtilsWrapper extends WebUtils
{
	SessionProvider sess;
	
	public HttpUtilsWrapper( SessionProvider sess )
	{
		this.sess = sess;
	}
	
	// TODO Improve and differ what eval and read do
	
	public EvalFactoryResult evalFile( String file ) throws IOException, EvalFactoryException
	{
		return evalFile( sess.getEvalFactory(), sess.getParentSession().getSite(), file );
	}
	
	public EvalFactoryResult evalPackage( String pack ) throws EvalFactoryException
	{
		return evalPackage( sess.getEvalFactory(), sess.getParentSession().getSite(), pack );
	}
	
	public EvalFactoryResult evalPackageWithException( String pack, Object... global ) throws IOException, EvalFactoryException
	{
		return evalPackageWithException( sess.getEvalFactory(), sess.getParentSession().getSite(), pack );
	}
	
	public EvalFactoryResult evalPackageWithException( String pack ) throws IOException, EvalFactoryException
	{
		return evalPackageWithException( sess.getEvalFactory(), sess.getParentSession().getSite(), pack );
	}
	
	public String readFile( String file ) throws IOException, EvalFactoryException
	{
		return evalFile( sess.getEvalFactory(), sess.getParentSession().getSite(), file ).getString();
	}
	
	public String readPackage( String pack ) throws EvalFactoryException
	{
		return evalPackage( sess.getEvalFactory(), sess.getParentSession().getSite(), pack ).getString();
	}
	
	public String readPackageWithException( String pack, Object... global ) throws IOException, EvalFactoryException
	{
		return evalPackageWithException( sess.getEvalFactory(), sess.getParentSession().getSite(), pack ).getString();
	}
	
	public String readPackageWithException( String pack ) throws IOException, EvalFactoryException
	{
		return evalPackageWithException( sess.getEvalFactory(), sess.getParentSession().getSite(), pack ).getString();
	}
}
