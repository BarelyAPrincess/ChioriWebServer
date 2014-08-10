package com.chiorichan.factory.shells;

import com.chiorichan.exceptions.ShellExecuteException;
import com.chiorichan.factory.CodeEvalFactory;
import com.chiorichan.factory.CodeMetaData;

/**
 * Simple HTML SeaShell.
 * More of a dummy to handle simple html files. 
 * 
 * @author Chiori Greene
 */
public class HTMLSeaShell implements SeaShell
{
	@Override
	public String[] getHandledShells()
	{
		return new String[]{"text", "txt", "html", "htm"};
	}

	@Override
	public String eval( CodeMetaData meta, String code, CodeEvalFactory factory ) throws ShellExecuteException
	{
		return code;
	}	
}
