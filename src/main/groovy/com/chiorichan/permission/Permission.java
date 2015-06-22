/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.permission;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.chiorichan.ConsoleColor;
import com.chiorichan.permission.lang.PermissionException;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Permission class for each permission node
 * 
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
public final class Permission
{
	private static final Set<Permission> allPerms = Sets.newConcurrentHashSet();
	protected final List<Permission> children = Lists.newCopyOnWriteArrayList();
	protected final String localName;
	protected PermissionModelValue model;
	protected final Permission parent;
	
	public Permission( String localName )
	{
		this( localName, PermissionType.DEFAULT );
	}
	
	public Permission( String localName, Permission parent )
	{
		this( localName, PermissionType.DEFAULT, parent );
	}
	
	public Permission( String localName, PermissionType type )
	{
		this( localName, type, null );
	}
	
	public Permission( String localName, PermissionType type, Permission parent )
	{
		if ( localName.contains( "." ) )
			throw new PermissionException( String.format( "The permission local name can not contain periods, %s", localName ) );
		
		this.localName = localName.toLowerCase();
		this.parent = parent;
		
		model = new PermissionModelValue( localName, type, this );
		
		allPerms.add( this );
	}
	
	public static List<Permission> getChildrenRecursive( Permission parent, boolean includeAll )
	{
		List<Permission> result = Lists.newArrayList();
		
		if ( includeAll || !parent.hasChildren() )
			result.add( parent );
		
		for ( Permission p : parent.getChildren() )
			result.addAll( getChildrenRecursive( p, includeAll ) );
		
		return result;
	}
	
	/**
	 * Attempts to find a Permission Node.
	 * Will not create the node if non-existent.
	 * 
	 * @param namespace
	 *            The namespace to find, e.g., com.chiorichan.user
	 * @return The found permission, null if non-existent
	 */
	public static Permission getNode( String namespace )
	{
		String[] nodes = namespace.split( "\\." );
		
		if ( nodes.length < 1 )
			return null;
		
		Permission curr = getRootNode( nodes[0] );
		
		if ( curr == null )
			return null;
		
		if ( nodes.length == 1 )
			return curr;
		
		for ( String node : Arrays.copyOfRange( nodes, 1, nodes.length ) )
		{
			Permission child = curr.getChild( node.toLowerCase() );
			if ( child == null )
				return null;
			else
				curr = child;
		}
		
		return curr;
	}
	
	public static Permission getNode( String namespace, boolean createNode )
	{
		if ( createNode )
			return getNode( namespace, PermissionType.DEFAULT );
		else
			return getNode( namespace );
	}
	
	/**
	 * Finds a registered permission node in the stack by crawling.
	 * 
	 * @param namespace
	 *            The full name space we need to crawl for.
	 * @param createChildren
	 *            Indicates if we should create the child node if non-existent.
	 * @return The child node based on the namespace. Will return NULL if non-existent and createChildren is false.
	 */
	public static Permission getNode( String namespace, PermissionType type )
	{
		String[] nodes = namespace.split( "\\." );
		
		if ( nodes.length < 1 )
			return null;
		
		Permission curr = getRootNode( nodes[0] );
		
		if ( curr == null )
			curr = new Permission( nodes[0] );
		curr.setType( type );
		allPerms.add( curr );
		
		if ( nodes.length == 1 )
			return curr;
		
		for ( String node : Arrays.copyOfRange( nodes, 1, nodes.length ) )
		{
			Permission child = curr.getChild( node.toLowerCase() );
			if ( child == null )
			{
				child = new Permission( node, curr );
				child.setType( type );
				curr.addChild( child );
				curr = child;
			}
			else
				curr = child;
		}
		
		return curr;
	}
	
	protected static Permission getNodeByLocalName( String name )
	{
		for ( Permission perm : allPerms )
			if ( perm.getLocalName().equalsIgnoreCase( name ) )
				return perm;
		return null;
	}
	
	/**
	 * Finds registered permission nodes.
	 * 
	 * @param namespace
	 *            The full name space we need to crawl for.
	 * @return A list of permissions that matched the namespace. Will return more then one if namespace contained asterisk.
	 */
	public static List<Permission> getNodes( PermissionNamespace ns )
	{
		if ( ns == null )
			return Lists.newArrayList();
		
		if ( ns.getNodeCount() < 1 )
			return Lists.newArrayList();
		
		List<Permission> matches = Lists.newArrayList();
		
		for ( Permission p : allPerms )
			if ( ns.matches( p ) )
				matches.add( p );
		
		return matches;
	}
	
	public static List<Permission> getNodes( String ns )
	{
		return getNodes( new PermissionNamespace( ns ) );
	}
	
	protected static Permission getRootNode( String name )
	{
		for ( Permission perm : allPerms )
			if ( perm.parent == null && perm.getLocalName().equalsIgnoreCase( name ) )
				return perm;
		return null;
	}
	
	public static List<Permission> getRootNodes()
	{
		return getRootNodes( true );
	}
	
	public static List<Permission> getRootNodes( boolean ignoreSysNode )
	{
		List<Permission> rootNodes = Lists.newArrayList();
		for ( Permission p : allPerms )
			if ( p.parent == null && !p.getNamespace().startsWith( "sys" ) && ignoreSysNode )
				rootNodes.add( p );
		return rootNodes;
	}
	
	/**
	 * Attempts to parse if a permission string is actually a reference to the EVERYBODY (-1, everybody, everyone), OP (0, op, root) or ADMIN (admin) permission nodes;
	 * 
	 * @param perm
	 *            The permission string to parse
	 * @return A string for the permission node, will return the original string if no match was found.
	 */
	public static String parseNode( String perm )
	{
		// Everyone
		if ( perm == null || perm.isEmpty() || perm.equals( "-1" ) || perm.equals( "everybody" ) || perm.equals( "everyone" ) )
			perm = PermissionDefault.EVERYBODY.getNameSpace();
		
		// OP Only
		if ( perm.equals( "0" ) || perm.equalsIgnoreCase( "op" ) || perm.equalsIgnoreCase( "root" ) )
			perm = PermissionDefault.OP.getNameSpace();
		
		if ( perm.equalsIgnoreCase( "admin" ) )
			perm = PermissionDefault.ADMIN.getNameSpace();
		return perm;
	}
	
	public void addChild( Permission node )
	{
		addChild( node, true );
	}
	
	public void addChild( Permission node, boolean saveChanges )
	{
		children.add( node );
		if ( saveChanges )
			PermissionManager.INSTANCE.getBackend().nodeCommit( this );
	}
	
	public void commit()
	{
		PermissionManager.INSTANCE.getBackend().nodeCommit( this );
	}
	
	public void debugPermissionStack( int deepth )
	{
		String spacing = ( deepth > 0 ) ? Strings.repeat( "      ", deepth - 1 ) + "|---> " : "";
		
		PermissionManager.getLogger().info( String.format( "%s%s%s=%s", ConsoleColor.YELLOW, spacing, getLocalName(), model ) );
		
		deepth++;
		for ( Permission p : children )
			p.debugPermissionStack( deepth );
	}
	
	public Permission getChild( String name )
	{
		for ( Permission node : children )
			if ( node.getLocalName().equals( name ) )
				return node;
		return null;
	}
	
	public Permission[] getChildren()
	{
		return children.toArray( new Permission[0] );
	}
	
	public List<Permission> getChildrenRecursive( boolean includeAll )
	{
		return getChildrenRecursive( this, includeAll );
	}
	
	/**
	 * Returns the unique fully qualified name of this Permission
	 * 
	 * @return Fully qualified name
	 */
	public String getLocalName()
	{
		return localName.toLowerCase();
	}
	
	public PermissionModelValue getModel()
	{
		return model;
	}
	
	public String getNamespace()
	{
		String namespace = "";
		Permission curr = this;
		
		do
		{
			namespace = curr.getLocalName() + "." + namespace;
			curr = curr.getParent();
		}
		while ( curr != null );
		
		namespace = namespace.substring( 0, namespace.length() - 1 );
		return namespace;
	}
	
	public PermissionNamespace getNamespaceObj()
	{
		return new PermissionNamespace( getNamespace() );
	}
	
	public Permission getParent()
	{
		return parent;
	}
	
	public PermissionType getType()
	{
		return model.getType();
	}
	
	public boolean hasChildren()
	{
		return children.size() > 0;
	}
	
	/**
	 * Attempts to move a permission from one namespace to another.
	 * e.g., com.chiorichan.oldspace1.same.oldname -> com.chiorichan.newspace2.same.newname.
	 * 
	 * @param newNamespace
	 *            The new namespace you wish to use.
	 * @param appendLocalName
	 *            Pass true if you wish the method to append the LocalName to the new namespace.
	 *            If the localname of the new namespace is different then this permission will be renamed.
	 * @return true is move/rename was successful.
	 */
	public boolean refactorNamespace( String newNamespace, boolean appendLocalName )
	{
		// PermissionNamespace ns = getNamespaceObj();
		// TODO THIS!
		return false;
	}
	
	private void setType( PermissionType type )
	{
		model = new PermissionModelValue( localName, type, this );
	}
	
	@Override
	public String toString()
	{
		return String.format( "Permission{name=%s,parent=%s,modelValue=%s}", getLocalName(), getParent(), model );
	}
}
