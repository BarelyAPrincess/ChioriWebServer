package com.chiorichan.factory.preprocessors;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.chiorichan.Loader;
import com.chiorichan.exceptions.ShellExecuteException;
import com.chiorichan.factory.BasicParser;

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
