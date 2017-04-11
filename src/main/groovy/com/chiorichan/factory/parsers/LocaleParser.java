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

import com.chiorichan.factory.localization.LocalizationException;
import com.chiorichan.logger.Log;
import com.chiorichan.site.Site;

/**
 * Using the {@link HTMLCommentParser} we attempt to parse the source for langTrans methods, i.e., {@literal <!-- langTrans(general.welcomeText) -->}
 */
public class LocaleParser extends HTMLCommentParser
{
	Site site;

	public LocaleParser()
	{
		super( "localeTrans" );
	}

	@Override
	public String resolveMethod( String... args ) throws Exception
	{
		if ( args.length > 2 )
			Log.get().warning( "localeTrans() method only accepts one argument, ignored." );

		try
		{
			return site.getLocalization().localeTrans( args[1] );
		}
		catch ( LocalizationException e )
		{
			return args[1];
		}
	}

	public String runParser( String source, Site site ) throws Exception
	{
		this.site = site;

		return runParser( source );
	}
}
