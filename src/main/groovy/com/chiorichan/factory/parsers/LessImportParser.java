/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.factory.parsers;

import java.io.File;
import java.io.IOException;

import com.chiorichan.zutils.ZIO;
import org.apache.commons.io.FileUtils;

import com.chiorichan.logger.Log;

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
		File imp = ZIO.isAbsolute( args[0] ) || args[0].startsWith( "\\" ) || rootDir == null ? new File( args[0] ) : new File( rootDir, args[0] );

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
