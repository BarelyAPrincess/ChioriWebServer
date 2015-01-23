/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.factory.preprocessors;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.chiorichan.Loader;
import com.chiorichan.exception.ShellExecuteException;
import com.chiorichan.factory.parsers.BasicParser;

public class LessImportParser extends BasicParser
{
	File rootDir = null;
	
	public LessImportParser()
	{
		super( "@import[: ]*(.*);", "(@import[: ]*.*;)" );
	}
	
	public String runParser( String source, File _rootDir ) throws ShellExecuteException
	{
		rootDir = _rootDir;
		return runParser( source );
	}
	
	@Override
	public String resolveMethod( String... args ) throws ShellExecuteException
	{
		File imp = ( args[0].startsWith( "/" ) || args[0].startsWith( "\\" ) || rootDir == null ) ? new File( args[0]) : new File( rootDir, args[0] );
		
		try
		{
			return FileUtils.readFileToString( imp );
		}
		catch ( IOException e )
		{
			Loader.getLogger().warning( "Attempted to import file '" + imp.getName() + "' but got error '" + e.getMessage() + "'" );
			return "/* Attempted to import file '" + imp.getName() + "' but got error '" + e.getMessage() + "' */";
		}
	}
}
