package com.chiorichan.factory.shells;

import groovy.lang.GroovyShell;

import com.chiorichan.exceptions.ShellExecuteException;
import com.chiorichan.factory.CodeEvalFactory;
import com.chiorichan.factory.CodeMetaData;

/**
 * Groovy SeaShell.
 * More of another dummy SeaShell to evaluate groovy files.
 * 
 * @author Chiori Greene
 */
public class GroovySeaShell implements SeaShell
{
	@Override
	public String[] getHandledShells()
	{
		return new String[] { "groovy" };
	}
	
	@Override
	public String eval( CodeMetaData meta, String code, CodeEvalFactory factory ) throws ShellExecuteException
	{
		try
		{
			GroovyShell shell = factory.getShell();
			shell.setVariable( "__FILE__", meta.fileName );
			
			Object o = shell.evaluate( code );
			return ( o != null ) ? o.toString() : "";
		}
		catch ( Throwable e )
		{
			throw new ShellExecuteException( e );
		}
	}
}
