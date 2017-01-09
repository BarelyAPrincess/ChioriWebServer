/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Rights Reserved
 */
package com.chiorichan.factory.parsers;

import com.google.common.base.Joiner;

public abstract class HTMLCommentParser extends BasicParser
{
	// TODO Check method names are a-z, A-Z, and 0-9.

	public HTMLCommentParser( String... methods )
	{
		this( Joiner.on( "|" ).join( methods ) );
	}

	public HTMLCommentParser( String methods )
	{
		super( "<!--[\\t ]*(?:" + methods + ")\\((.*)\\);*[\\t ]*-->", "(<!--[\\t ]*(?:" + methods + ")\\((.*)\\);*[\\t ]*-->)" );
		// super( "<!-- *" + argumentName + "\\((.*)\\) *-->", "(<!-- *" + argumentName + "\\(.*\\) *-->)" );
	}
}
