/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * <p>
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 * <p>
 * All Rights Reserved.
 */
package com.chiorichan.http;

import com.chiorichan.logger.Log;
import com.chiorichan.site.Site;
import com.chiorichan.zutils.ZObjects;
import io.netty.util.internal.ConcurrentSet;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Keeps track of routes
 */
public class Routes
{
	protected final Set<Route> routes = new ConcurrentSet<>();
	private RouteWatcher jsonWatcher;
	private RouteWatcher yamlWatcher;
	protected final Site site;

	public Routes( Site site )
	{
		this.site = site;

		File routesJson = new File( site.directory(), "routes.json" );
		File routesYaml = new File( site.directory(), "routes.yaml" );

		jsonWatcher = new RouteWatcher( this, routesJson );
		yamlWatcher = new RouteWatcher( this, routesYaml );
	}

	public Route routeUrl( String id )
	{
		ZObjects.notEmpty( id );

		jsonWatcher.reviveTask();
		yamlWatcher.reviveTask();

		return routes.stream().filter( r -> id.equalsIgnoreCase( r.getId() ) || id.matches( r.getId() ) ).findFirst().orElse( null );
	}

	public boolean hasRoute( String id )
	{
		return routes.stream().filter( r -> id.equalsIgnoreCase( r.getId() ) || id.matches( r.getId() ) ).findAny().isPresent();
	}

	public RouteResult searchRoutes( String uri, String host ) throws IOException
	{
		jsonWatcher.reviveTask();
		yamlWatcher.reviveTask();

		AtomicInteger keyInteger = new AtomicInteger();

		Map<String, RouteResult> matches = routes.stream().map( route -> route.match( uri, host ) ).filter( result -> result != null ).collect( Collectors.toMap( result -> result.getWeight() + keyInteger.getAndIncrement(), result -> result ) );

		if ( matches.size() > 0 )
			return ( RouteResult ) matches.values().toArray()[0];
		else
			Log.get().fine( String.format( "Failed to find route for... {host=%s,uri=%s}", host, uri ) );

		return null;
	}
}
