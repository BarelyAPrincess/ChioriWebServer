/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Joel Greene <joel.greene@penoaks.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.site;

import com.chiorichan.helpers.Namespace;
import com.chiorichan.utils.UtilLists;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public final class DomainTree
{
	private static Map<String, List<DomainRoot>> domains = new ConcurrentHashMap<>();
	private static DomainNode defaultDomainNode = new DefaultDomainNode();

	public static DomainNode parseDomain( String fullDomain )
	{
		return parseDomain( new DomainParser( fullDomain ) );
	}

	public static DomainNode parseDomain( DomainParser fullDomain )
	{
		Namespace root = fullDomain.getTld();
		Namespace child = fullDomain.getSub().reverseOrder();

		if ( root.isEmpty() && child.isEmpty() )
			return defaultDomainNode;

		if ( child.isEmpty() )
			throw new IllegalArgumentException( String.format( "Something went wrong, the tld \"%s\" has no child domains.", root ) );

		List<DomainRoot> list = domains.compute( root.getString( "_", true ), ( k, v ) -> v == null ? new ArrayList<>() : v );

		String first = child.getFirst();
		DomainNode node = UtilLists.findOrNew( list, v -> first.equals( v.getNodeName() ), new DomainRoot( root.getString(), first ) );

		if ( child.getNodeCount() > 1 )
			for ( String s : child.subNodes( 1 ) )
				node = node.getChild( s, true );

		return node;
	}

	public static Stream<DomainNode> getChildren()
	{
		return domains.values().stream().flatMap( l -> l.stream() ).flatMap( DomainNode::getChildrenRecursive0 );
	}

	public static Stream<DomainRoot> getDomains( String tld )
	{
		return domains.values().stream().flatMap( l -> l.stream() ).filter( n -> tld.matches( n.getTld() ) );
	}

	public static Stream<String> getTLDsInuse()
	{
		return domains.keySet().stream();
	}

	private DomainTree()
	{

	}
}
