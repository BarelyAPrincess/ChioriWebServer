/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.site;

import com.chiorichan.helpers.Namespace;

public class DomainRoot extends DomainNode
{
	protected final String tld;

	public DomainRoot( String tld, String nodeName )
	{
		super( nodeName );
		this.tld = tld;
	}

	@Override
	public Namespace getNamespace()
	{
		return Namespace.parseString( tld ).reverseOrder().append( getNodeName() );
	}

	public String getTld()
	{
		return tld;
	}
}
