/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.factory.parsers;

import java.util.Map;
import java.util.Map.Entry;

import com.chiorichan.site.Site;
import com.chiorichan.site.SiteManager;

public class LinksParser extends HTMLCommentParser
{
	Site site;
	
	public LinksParser()
	{
		super( "url_to" );
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
			url += SiteManager.INSTANCE.getDefaultSite().getDomain() + "/";
		
		return url;
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
			source = source.replace( "%" + entry.getKey() + "%", entry.getValue() );
		
		return super.runParser( source );
	}
	
}
