/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.factory.interpreters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.chiorichan.factory.EvalMetaData;
import com.chiorichan.factory.ShellFactory;
import com.chiorichan.lang.EvalFactoryException;

/**
 * Simple HTML SeaShell.
 * More of a dummy to handle simple html files.
 */
public class HTMLInterpreter implements Interpreter
{
	@Override
	public String[] getHandledTypes()
	{
		return new String[] {"plain", "text", "txt", "html", "htm"};
	}
	
	@Override
	public String eval( EvalMetaData meta, String code, ShellFactory shellFactory, ByteArrayOutputStream bs ) throws EvalFactoryException
	{
		try
		{
			bs.write( code.getBytes() );
		}
		catch ( IOException e )
		{
			throw new EvalFactoryException( e, shellFactory );
		}
		return "";
	}
}
