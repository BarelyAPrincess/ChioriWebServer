/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.factory.parsers;

import java.io.File;

import com.chiorichan.Loader;
import com.chiorichan.factory.EvalFactory;
import com.chiorichan.site.Site;

public class IncludesParser extends HTMLCommentParser
{
	Site site;
	EvalFactory factory;
	
	public IncludesParser()
	{
		super( "include" );
	}
	
	public String runParser( String source, Site site, EvalFactory factory ) throws Exception
	{
		this.site = site;
		this.factory = factory;
		
		return runParser( source );
	}
	
	@Override
	public String resolveMethod( String... args ) throws Exception
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
			result = factory.eval( res, site ).getString();
		}
		else if ( !res.exists() )
		{
			Loader.getLogger().warning( "We had a problem finding the include file `" + res.getAbsolutePath() + "`" );
		}
		
		return result;
	}
}
