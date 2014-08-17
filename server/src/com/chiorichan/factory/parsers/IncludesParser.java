package com.chiorichan.factory.parsers;

import java.io.File;

import com.chiorichan.Loader;
import com.chiorichan.exceptions.ShellExecuteException;
import com.chiorichan.factory.CodeEvalFactory;
import com.chiorichan.factory.HTMLCommentParser;
import com.chiorichan.framework.Site;

public class IncludesParser extends HTMLCommentParser
{
	Site site;
	CodeEvalFactory factory;
	
	public IncludesParser()
	{
		super( "include" );
	}
	
	public String runParser( String source, Site _site, CodeEvalFactory _factory ) throws ShellExecuteException
	{
		site = _site;
		factory = _factory;
		
		return runParser( source );
	}

	@Override
	public String resolveMethod( String... args ) throws ShellExecuteException
	{
		if ( args.length > 1 )
			Loader.getLogger().warning( "CodeEvalFactory: include() method only accepts one argument, ignored." );
		
		File res = site.getResource( args[0] );
		
		if ( res == null )
			res = Loader.getSiteManager().getFrameworkSite().getResource( args[0] );
		
		String result = "";
		
		if ( res != null && res.exists() )
		{
			// TODO Prevent this from going into an infinite loop!
			result = factory.eval( res, site );
		}
		else if ( !res.exists() )
		{
			Loader.getLogger().warning( "We had a problem finding the include file `" + res.getAbsolutePath() + "`" );
		}
		
		return result;
	}
}