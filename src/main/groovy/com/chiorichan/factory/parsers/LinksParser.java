/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2016 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.factory.parsers;

import com.chiorichan.http.HttpRequestWrapper;
import com.chiorichan.site.Site;

public class LinksParser extends HTMLCommentParser
{
	HttpRequestWrapper request;
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
		String url = request.isSecure() ? "https://" : "http://";

		if ( args.length >= 1 && !args[1].isEmpty() )
			url += args[1] + ".";

		url += request.getDomain() + "/";

		return url;
	}

	public String runParser( String source, HttpRequestWrapper request, Site site ) throws Exception
	{
		if ( request == null || site == null )
			return source;

		this.request = request;
		this.site = site;

		if ( source.isEmpty() )
			return "";

		// Technically Deprecated!!!
		for ( String subdomain : site.getSubdomains( request.getDomain() ) )
			source = source.replace( "%" + subdomain + "%", "http://" + subdomain + "." + request.getDomain() + "/" );

		return super.runParser( source );
	}

}
