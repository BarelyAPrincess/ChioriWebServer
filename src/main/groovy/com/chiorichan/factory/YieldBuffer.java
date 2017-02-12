/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.factory;

import java.util.HashMap;
import java.util.Map;

public class YieldBuffer
{
	// TODO Expand for more practical uses

	private Map<String, String> yields = new HashMap<>();

	public void set( String key, String value )
	{
		yields.put( key, value );
	}

	public String get( String key )
	{
		return yields.get( key );
	}
}
