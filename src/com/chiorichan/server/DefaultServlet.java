package com.chiorichan.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.chiorichan.Loader;
import com.chiorichan.framework.Framework;

public class DefaultServlet extends HttpServlet
{
	private static final long serialVersionUID = -4531330369021028992L;
	
	@Override
	public void service( HttpServletRequest request, HttpServletResponse response ) throws IOException, ServletException
	{
		Framework fw = new Framework( request, response, request.getServletContext() );
		fw.init();
	}
}
