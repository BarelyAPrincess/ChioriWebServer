package com.chiorichan.framework;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bsh.EvalError;

public class CodeParsingException extends Exception
{
	private static final long serialVersionUID = 6622374555743321786L;
	
	protected int lineNumber = -1;
	protected int columnNumber = -1;
	protected String fileName = null;
	protected String sourceCode = "";
	protected String message = "";
	
	public CodeParsingException( EvalError e, String code )
	{
		super();
		
		message = e.getMessage();
		
		Pattern p = Pattern.compile( "line ([0-9]+), column ([0-9]+).*? in (.*)" );
		Matcher m = p.matcher( e.getMessage() );
		
		if ( m.find() )
		{
			lineNumber = Integer.parseInt( m.group( 1 ) );
			columnNumber = Integer.parseInt( m.group( 2 ) );
			fileName = m.group( 3 );
		}
		else
		{
			p = Pattern.compile( "line ([0-9]+), column ([0-9]+)" );
			m = p.matcher( e.getMessage() );
			
			if ( m.find() )
			{
				lineNumber = Integer.parseInt( m.group( 1 ) );
				columnNumber = Integer.parseInt( m.group( 2 ) );
			}
		}
		
		System.out.println( lineNumber + " -- " + columnNumber + " -- " + fileName );
		
		sourceCode = code;
	}
	
	public void setMessage( String msg )
	{
		message = msg;
	}
	
	public String getMessage()
	{
		return message;
	}
	
	public String getSourceCode()
	{
		return sourceCode;
	}
	
	public int getLineNumber()
	{
		return lineNumber;
	}
	
	public int getColumnNumber()
	{
		return columnNumber;
	}
	
	public String getSourceFile()
	{
		return fileName;
	}
}
