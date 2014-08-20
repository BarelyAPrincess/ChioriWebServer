package com.chiorichan.factory.interpreters;

import groovy.lang.GroovyShell;

import java.io.ByteArrayOutputStream;

import com.chiorichan.exceptions.ShellExecuteException;
import com.chiorichan.factory.CodeMetaData;

/**
 * Simple HTML SeaShell.
 * More of a dummy to handle simple html files.
 * 
 * @author Chiori Greene
 */
public class HTMLInterpreter implements Interpreter
{
	@Override
	public String[] getHandledTypes()
	{
		return new String[] { "plain", "text", "txt", "html", "htm" };
	}
	
	@Override
	public String eval( CodeMetaData meta, String code, GroovyShell shell, ByteArrayOutputStream bs ) throws ShellExecuteException
	{
		return code;
	}
}
