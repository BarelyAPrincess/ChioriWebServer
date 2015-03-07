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
import java.util.Map;

import com.chiorichan.exception.ShellExecuteException;
import com.chiorichan.session.SessionProvider;
import com.google.common.collect.Maps;

public class HttpUtilsWrapper extends WebUtils
{
	SessionProvider sess;
	
	public HttpUtilsWrapper( SessionProvider sess )
	{
		this.sess = sess;
	}
	
	public String evalFile( String file, Object... global ) throws IOException, ShellExecuteException
	{
		Map<String, Object> globals = Maps.newHashMap();
		
		for ( int i = 0; i < global.length; i++ )
			globals.put( "" + i, global[i] );
		
		return evalFile( sess.getCodeFactory(), sess.getParentSession().getSite(), file, globals );
	}
	
	public String evalFile( String file, Map<String, Object> global ) throws IOException, ShellExecuteException
	{
		return evalFile( sess.getCodeFactory(), sess.getParentSession().getSite(), file, global );
	}
	
	public String evalFile( String file ) throws IOException, ShellExecuteException
	{
		return evalFile( sess.getCodeFactory(), sess.getParentSession().getSite(), file );
	}
	
	public String evalPackage( String pack, Object... global ) throws ShellExecuteException
	{
		Map<String, Object> globals = Maps.newHashMap();
		
		for ( int i = 0; i < global.length; i++ )
			globals.put( "" + i, global[i] );
		
		return evalPackage( sess.getCodeFactory(), sess.getParentSession().getSite(), pack, globals );
	}
	
	public String evalPackage( String pack, Map<String, Object> global ) throws ShellExecuteException
	{
		return evalPackage( sess.getCodeFactory(), sess.getParentSession().getSite(), pack, global );
	}
	
	public String evalPackage( String pack ) throws ShellExecuteException
	{
		return evalPackage( sess.getCodeFactory(), sess.getParentSession().getSite(), pack );
	}
	
	public String evalPackageWithException( String pack, Object... global ) throws IOException, ShellExecuteException
	{
		Map<String, Object> globals = Maps.newHashMap();
		
		for ( int i = 0; i < global.length; i++ )
			globals.put( "" + i, global[i] );
		
		return evalPackageWithException( sess.getCodeFactory(), sess.getParentSession().getSite(), pack, globals );
	}
	
	public String evalPackageWithException( String pack, Map<String, Object> global ) throws IOException, ShellExecuteException
	{
		return evalPackageWithException( sess.getCodeFactory(), sess.getParentSession().getSite(), pack, global );
	}
	
	public String evalPackageWithException( String pack ) throws IOException, ShellExecuteException
	{
		return evalPackageWithException( sess.getCodeFactory(), sess.getParentSession().getSite(), pack );
	}
}
