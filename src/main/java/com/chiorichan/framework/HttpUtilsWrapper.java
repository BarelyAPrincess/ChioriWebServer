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
