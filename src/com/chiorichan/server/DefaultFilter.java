package com.chiorichan.server;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.caucho.quercus.QuercusContext;
import com.caucho.quercus.QuercusRequestAdapter;
import com.caucho.vfs.FilePath;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;
import com.chiorichan.framework.Framework;

public class DefaultFilter implements Filter
{
	protected QuercusContext _quercus;
	protected ServletContext _servletContext;
	
	@Override
	public void destroy()
	{
		
	}
	
	@Override
	public void doFilter( ServletRequest request0, ServletResponse response0, FilterChain chain ) throws ServletException, IOException
	{
		HttpServletRequest request = (HttpServletRequest) request0;
		HttpServletResponse response = (HttpServletResponse) response0;
		
		Framework fw = new Framework( request, response, chain, getQuercus(), _servletContext );
		
		fw.init();
	}
	
	protected QuercusContext getQuercus()
	{
		if ( _quercus == null )
		{
			_quercus = new QuercusContext();
			
			Path pwd = new FilePath( _servletContext.getRealPath( "/" ) );
			Path webInfDir = new FilePath( _servletContext.getRealPath( "/WEB-INF" ) );
			
			_quercus.setPwd( pwd );
			_quercus.setWebInfDir( webInfDir );
			
			_quercus.init();
			_quercus.start();
		}
		
		return _quercus;
	}
	
	@Override
	public void init( FilterConfig config ) throws ServletException
	{
		_servletContext = config.getServletContext();
	}
}
