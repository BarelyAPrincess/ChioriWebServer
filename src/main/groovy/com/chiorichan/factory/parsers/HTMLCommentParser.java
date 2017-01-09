/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * <p>
 * Copyright 2016 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
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
