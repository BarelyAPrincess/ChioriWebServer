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

import java.util.Map;
import java.util.Map.Entry;

import com.chiorichan.Loader;
import com.chiorichan.framework.Site;

public class LinksParser extends HTMLCommentParser
{
	Site site;
	
	public LinksParser()
	{
		super( "url_to" );
	}
	
	public String runParser( String source, Site site ) throws Exception
	{
		this.site = site;
		Map<String, String> aliases = site.getAliases();
		
		if ( source.isEmpty() )
			return "";
		
		if ( aliases == null || aliases.size() < 1 )
			return source;
		
		for ( Entry<String, String> entry : aliases.entrySet() )
		{
			source = source.replace( "%" + entry.getKey() + "%", entry.getValue() );
		}
		
		return runParser( source );
	}
	
	/**
	 * args: [] = http://example.com/
	 * args: [subdomain] = http://subdomain.example.com/
	 * TODO: Expand this function.
	 */
	@Override
	public String resolveMethod( String... args ) throws Exception
	{
		String url = "http://";
		
		if ( args.length >= 1 && !args[0].isEmpty() )
			url += args[0] + ".";
		
		if ( site != null )
			url += site.getDomain() + "/";
		else
			url += Loader.getSiteManager().getFrameworkSite().getDomain() + "/";
		
		return url;
	}
	
}
