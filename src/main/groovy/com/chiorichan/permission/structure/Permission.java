/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.permission.structure;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.chiorichan.permission.PermissionManager;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public final class Permission
{
	protected static final Set<Permission> allPerms = Sets.newConcurrentHashSet();
	protected final List<Permission> children = Lists.newCopyOnWriteArrayList();
	protected PermissionValue<?> value = null;
	protected boolean isRootNode = false;
	protected String description = "";
	protected Permission parent = null;
	protected String name;
	
	public static final String DEFAULT = "default";
	public static final String OP = "sys.op";
	public static final String ADMIN = "sys.admin";
	public static final String BANNED = "sys.banned";
	public static final String WHITELISTED = "sys.whitelisted";
	
	/**
	 * Used only for creating dummy permission nodes that have no real connection to actual permission nodes.
	 * 
	 * @param name
	 *            The name used for this dummy node
	 * @param value
	 *            The value used for this dummy node
	 * @param desc
	 *            The description used for this dummy node
	 */
	public Permission( String name, PermissionValue<?> value, String desc )
	{
		this.name = name;
		this.value = value;
		description = desc;
	}
	
	public Permission( String name, Permission parentNode, boolean rootNode )
	{
		this( name, parentNode );
		isRootNode = rootNode;
		allPerms.add( this );
	}
	
	public Permission( String name, Permission parentNode )
	{
		this.name = name.toLowerCase();
		parent = parentNode;
		allPerms.add( this );
	}
	
	public void setValue( PermissionValue<?> val )
	{
		value = val;
	}
	
	public PermissionValue<?> getValue()
	{
		return value;
	}
	
	public Object getObject()
	{
		return value.getValue();
	}
	
	public String getString()
	{
		if ( value.getType() == PermissionValue.PermissionType.ENUM || value.getType() == PermissionValue.PermissionType.VAR )
			return ( String ) value.getValue();
		
		return null;
	}
	
	public Integer getInt()
	{
		if ( value.getType() == PermissionValue.PermissionType.INT )
			return ( Integer ) value.getValue();
		
		return null;
	}
	
	public Boolean getBoolean()
	{
		if ( value.getType() == PermissionValue.PermissionType.BOOL )
			return ( Boolean ) value.getValue();
		
		return null;
	}
	
	/**
	 * Gets a brief description of this permission, if set
	 * 
	 * @return Brief description of this permission
	 */
	public String getDescription()
	{
		return description;
	}
	
	/**
	 * Sets the description of this permission.
	 * <p>
	 * This will not be saved to disk, and is a temporary operation until the server reloads permissions.
	 * 
	 * @param value
	 *            The new description to set
	 */
	public void setDescription( String value )
	{
		if ( value == null )
		{
			description = "";
		}
		else
		{
			description = value;
		}
	}
	
	public boolean hasChildren()
	{
		return children.size() > 0;
	}
	
	public Permission[] getChildren()
	{
		return children.toArray( new Permission[0] );
	}
	
	public Permission getChild( String name )
	{
		for ( Permission node : children )
		{
			if ( node.getName().equals( name ) )
				return node;
		}
		return null;
	}
	
	public void addChild( Permission node )
	{
		children.add( node );
	}
	
	/**
	 * Returns the unique fully qualified name of this Permission
	 * 
	 * @return Fully qualified name
	 */
	public String getName()
	{
		return name;
	}
	
	public Permission getParent()
	{
		return parent;
	}
	
	public String getNamespace()
	{
		String namespace = "";
		Permission curr = this;
		
		do
		{
			namespace = getName() + "." + namespace;
			curr = curr.getParent();
		}
		while ( curr != null );
		
		namespace = namespace.substring( 0, namespace.length() - 1 );
		return namespace;
	}
	
	protected static Permission getRootNode( String name )
	{
		for ( Permission perm : allPerms )
			if ( perm.isRootNode && perm.getName().equals( name ) )
				return perm;
		return null;
	}
	
	protected static Permission getNode( String name )
	{
		for ( Permission perm : allPerms )
			if ( perm.getName().equals( name ) )
				return perm;
		return null;
	}
	
	/**
	 * Similar to {@link #createPermissionNode(String, PermissionValue, String)} as in it will
	 * try to create non-existent nodes but will not override existing values.
	 * 
	 * @param namespace
	 *            The namespace to use, e.g., com.chiorichan.permission.node
	 * @param val
	 *            The value to set if it needed to be created
	 * @param desc
	 *            The description to set if it needed to be created
	 * @return
	 *         The Permission
	 */
	public static Permission getPermissionNode( String namespace, PermissionValue<?> val, String desc )
	{
		Permission perm = getPermissionNode( namespace, false );
		
		if ( perm == null )
			return createPermissionNode( namespace, val, desc );
		
		return perm;
	}
	
	public static Permission getPermissionNode( String namespace )
	{
		return getPermissionNode( namespace, true );
	}
	
	/**
	 * Finds a registered permission node in the stack by crawling.
	 * 
	 * @param namespace
	 *            The full node path we need to crawl for.
	 * @param createChildren
	 *            Indicates if we should create the child node if not existent.
	 * @return The child node based on the namespace.
	 */
	public static Permission getPermissionNode( String namespace, boolean createChildren )
	{
		String[] nodes = namespace.split( "\\." );
		
		if ( nodes.length < 1 )
			return null;
		
		Permission curr = getRootNode( nodes[0] );
		
		if ( curr == null )
			if ( createChildren )
			{
				curr = new Permission( nodes[0], null, true );
				allPerms.add( curr );
			}
			else
				return null;
		
		if ( nodes.length == 1 )
			return curr;
		
		for ( String node : Arrays.copyOfRange( nodes, 1, nodes.length ) )
		{
			Permission child = curr.getChild( node );
			if ( child == null )
			{
				if ( createChildren )
				{
					child = new Permission( node, curr );
					curr.addChild( child );
					curr = child;
				}
				else
					return null;
			}
			else
				curr = child;
		}
		
		return curr;
	}
	
	public static Permission createPermissionNode( String namespace, PermissionValue<?> val, String desc )
	{
		Permission perm = getPermissionNode( namespace, true );
		perm.setValue( val );
		perm.setDescription( desc );
		return perm;
	}
	
	public String toString()
	{
		return "Permission[name=" + getName() + ",parent=" + getParent() + ( ( value != null ) ? ",value=" + value.toString() : "" ) + "]";
	}
	
	public static Set<Permission> getRootNodes()
	{
		Set<Permission> rootNodes = Sets.newHashSet();
		for ( Permission p : allPerms )
			if ( p.isRootNode )
				rootNodes.add( p );
		return rootNodes;
	}
	
	public void debugPermissionStack( int deepth )
	{
		String spacing = ( deepth > 0 ) ? Strings.repeat( "      ", deepth - 1 ) + "|---> " : "";
		
		if ( value == null )
			PermissionManager.getLogger().info( "[DEBUG] " + spacing + getName() );
		else
			PermissionManager.getLogger().info( "[DEBUG] " + spacing + getName() + "=" + value.toString() );
		
		deepth++;
		for ( Permission p : children )
		{
			p.debugPermissionStack( deepth );
		}
	}
}
