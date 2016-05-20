/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2016 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.factory.parsers;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.chiorichan.logger.Log;
import com.chiorichan.util.FileFunc;

public class LessImportParser extends BasicParser
{
	File rootDir = null;

	public LessImportParser()
	{
		super( "@import[: ]*(.*);", "(@import[: ]*.*;)" );
	}

	@Override
	public String resolveMethod( String... args ) throws Exception
	{
		File imp = FileFunc.isAbsolute( args[0] ) || args[0].startsWith( "\\" ) || rootDir == null ? new File( args[0] ) : new File( rootDir, args[0] );

		try
		{
			return FileUtils.readFileToString( imp );
		}
		catch ( IOException e )
		{
			Log.get( "ScriptFactory" ).warning( "Attempted to import file '" + imp.getName() + "' but got error '" + e.getMessage() + "'" );
			return "/* Attempted to import file '" + imp.getName() + "' but got error '" + e.getMessage() + "' */";
		}
	}

	public String runParser( String source, File rootDir ) throws Exception
	{
		this.rootDir = rootDir;
		return runParser( source );
	}
}