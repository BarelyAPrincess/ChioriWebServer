/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.http;

import java.util.HashMap;
import java.util.Map;

public class RouteResult
{
	private final Map<String, String> rewrites = new HashMap<>();
	private final String weight;
	private final Route route;

	public RouteResult( Route route, String weight, Map<String, String> rewrites )
	{
		this.rewrites.putAll( rewrites );
		this.weight = weight;
		this.route = route;
	}

	public Map<String, String> getRewrites()
	{
		return rewrites;
	}

	public String getWeight()
	{
		return weight;
	}

	public Route getRoute()
	{
		return route;
	}
}
