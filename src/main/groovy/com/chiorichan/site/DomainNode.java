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
import com.chiorichan.utils.UtilHttp;
import com.chiorichan.utils.UtilLists;
import com.chiorichan.utils.UtilObjects;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class DomainNode
{
	protected final List<DomainNode> children = new ArrayList<>();
	protected final DomainNode parent;
	protected final String nodeName;
	protected Site site;

	protected DomainNode( DomainNode parent, String nodeName )
	{
		this.parent = parent;
		this.nodeName = nodeName;
	}

	protected DomainNode( String nodeName )
	{
		this.parent = null;
		this.nodeName = nodeName;
	}

	public Stream<DomainNode> getParents()
	{
		return Stream.of( parent ).flatMap( DomainNode::getParents0 );
	}

	protected Stream<DomainNode> getParents0()
	{
		return Stream.concat( Stream.of( this ), Stream.of( parent ).flatMap( DomainNode::getParents0 ) );
	}

	public DomainNode getParent()
	{
		return parent;
	}

	public boolean hasParent()
	{
		return parent != null;
	}

	public boolean hasChildren()
	{
		return children.size() > 0;
	}

	public Stream<DomainNode> getChildren()
	{
		return children.stream();
	}

	public Stream<DomainNode> getChildrenRecursive()
	{
		return children.stream().flatMap( DomainNode::getChildrenRecursive0 );
	}

	protected Stream<DomainNode> getChildrenRecursive0()
	{
		return Stream.concat( Stream.of( this ), children.stream().flatMap( DomainNode::getChildrenRecursive0 ) );
	}

	public Namespace getNamespace()
	{
		return hasParent() ? getParent().getNamespace().append( getNodeName() ) : Namespace.parseString( getNodeName() );
	}

	public String getRootDomain()
	{
		return new DomainParser( getFullDomain() ).getTld().getString();
	}

	public String getChildDomain()
	{
		return new DomainParser( getFullDomain() ).getSub().getString();
	}

	public String getFullDomain()
	{
		return getNamespace().reverseOrder().getString();
	}

	public String getNodeName()
	{
		return nodeName;
	}

	public DomainNode getChild( String domain )
	{
		return getChild( domain, false );
	}

	/**
	 * Supports RegEx for each domain node
	 *
	 * @param domain The path to the child requested
	 * @param create Shall we create the child if it's missing or fail gracefully
	 * @return
	 */
	public DomainNode getChild( String domain, boolean create )
	{
		UtilObjects.notEmpty( domain );

		if ( UtilHttp.isValidIPv4( domain ) )
			throw new IllegalArgumentException( "Can't match child by IPv4 address" );
		if ( UtilHttp.isValidIPv6( domain ) )
			throw new IllegalArgumentException( "Can't match child by IPv6 address" );

		// XXX Can this inner method be replaced by the Java 8 Stream feature?

		Namespace ns = Namespace.parseString( domain ).reverseOrder();
		DomainNode domainNode = this;

		for ( final String node : ns.getNodes() )
		{
			Optional<DomainNode> results = domainNode.children.stream().filter( c -> node.matches( c.getNodeName() ) ).findFirst();
			if ( results.isPresent() )
				domainNode = results.get();
			else
			{
				if ( create )
					domainNode = UtilLists.add( domainNode.children, new DomainNode( this, node ) );
				else
					return null;
			}
		}

		return domainNode;
	}

	protected DomainNode setSite( Site site )
	{
		return setSite( site, false );
	}

	protected DomainNode setSite( Site site, boolean override )
	{
		UtilObjects.notNull( site );

		if ( this.site != null && this.site != site && !override )
			throw new IllegalStateException( String.format( "You can not override the site set on domain node [%s], it was already assigned to site [%s]", site.getId(), this.site.getId() ) );
		this.site = site;
		return this;
	}

	public Site getSite()
	{
		return site;
	}

	public DomainMapping getDomainMapping()
	{
		return getSite() == null ? null : getSite().getMappings( getFullDomain() ).findFirst().orElse( null );
	}
}
