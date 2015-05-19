/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.permission;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.chiorichan.ConsoleColor;
import com.chiorichan.Loader;
import com.chiorichan.permission.backend.memory.MemoryPermission;
import com.chiorichan.permission.lang.PermissionException;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public abstract class Permission
{
	protected static final Set<Permission> allPerms = Sets.newConcurrentHashSet();
	protected final List<Permission> children = Lists.newCopyOnWriteArrayList();
	protected PermissionValue<?> value;
	protected String description = "";
	protected Permission parent = null;
	protected String localName;
	protected boolean changesMade = false;
	
	/**
	 * Used only for creating dummy permission nodes that have no real connection to actual permission nodes.
	 * 
	 * @param localName
	 *            The name used for this dummy node
	 * @param value
	 *            The value used for this dummy node
	 * @param desc
	 *            The description used for this dummy node
	 */
	protected Permission( String localName, PermissionValue<?> value, String desc )
	{
		if ( localName.contains( "." ) )
			throw new RuntimeException( "The permission local name can not contain periods, localName: " + localName + "" );
		
		this.localName = localName.toLowerCase();
		this.value = value;
		description = desc;
	}
	
	protected Permission( String localName )
	{
		this( localName, null );
		allPerms.add( this );
	}
	
	protected Permission( String localName, Permission parentNode )
	{
		if ( localName.contains( "." ) )
			throw new RuntimeException( "The permission local name can not contain periods!" );
		
		this.localName = localName.toLowerCase();
		parent = parentNode;
		allPerms.add( this );
	}
	
	public void setValue( PermissionValue<?> val )
	{
		setValue( val, true );
	}
	
	public void setValue( PermissionValue<?> val, boolean saveChanges )
	{
		value = val;
		changesMade = true;
		
		if ( saveChanges )
			saveNode();
	}
	
	public boolean hasValue()
	{
		return value != null;
	}
	
	public PermissionValue<?> getValue()
	{
		if ( value == null )
			return PermissionDefault.DEFAULT.getNode().getValue();
		
		return value;
	}
	
	public Object getObject()
	{
		return ( value == null ) ? PermissionDefault.DEFAULT.getNode().getObject() : value.getValue();
	}
	
	public Object getDefaultValue()
	{
		return ( value == null ) ? PermissionDefault.DEFAULT.getNode().getDefaultValue() : value.getDefault();
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
	
	public void setDescription( String value )
	{
		setDescription( value, true );
	}
	
	/**
	 * Sets the description of this permission.
	 * <p>
	 * This will not be saved to disk, and is a temporary operation until the server reloads permissions.
	 * 
	 * @param value
	 *            The new description to set
	 * @param saveChanges
	 *            shall we make the call to the backend to save these changes?
	 */
	public void setDescription( String value, boolean saveChanges )
	{
		if ( value == null )
			description = "";
		else
			description = value;
		
		changesMade = true;
		
		if ( saveChanges )
			saveNode();
	}
	
	public boolean hasDescription()
	{
		return description != null && !description.isEmpty();
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
			if ( node.getLocalName().equals( name ) )
				return node;
		}
		return null;
	}
	
	public void addChild( Permission node )
	{
		addChild( node, true );
	}
	
	public void addChild( Permission node, boolean saveChanges )
	{
		children.add( node );
		
		changesMade = true;
		
		if ( saveChanges )
			saveNode();
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
	
	protected static Permission getRootNode( String name )
	{
		for ( Permission perm : allPerms )
			if ( perm.parent == null && perm.getLocalName().equalsIgnoreCase( name ) )
				return perm;
		return null;
	}
	
	protected static Permission getNodeByLocalName( String name )
	{
		for ( Permission perm : allPerms )
			if ( perm.getLocalName().equalsIgnoreCase( name ) )
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
	public static Permission getNode( String namespace, PermissionValue<?> val, String desc )
	{
		Permission perm = getNode( namespace, false );
		
		if ( perm == null )
			return createNode( namespace, val, desc );
		
		return perm;
	}
	
	public static Permission getNode( String namespace )
	{
		return getNode( namespace, true );
	}
	
	public static List<Permission> getNodes( String ns )
	{
		return getNodes( new PermissionNamespace( ns ) );
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
	
	public List<Permission> getChildrenRecursive( boolean includeAll )
	{
		return getChildrenRecursive( this, includeAll );
	}
	
	public static List<Permission> getChildrenRecursive( Permission parent, boolean includeAll )
	{
		List<Permission> result = Lists.newArrayList();
		
		if ( includeAll || !parent.hasChildren() )
			result.add( parent );
		
		for ( Permission p : parent.getChildren() )
		{
			result.addAll( getChildrenRecursive( p, includeAll ) );
		}
		
		return result;
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
	
	/**
	 * Finds a registered permission node in the stack by crawling.
	 * 
	 * @param namespace
	 *            The full name space we need to crawl for.
	 * @param createChildren
	 *            Indicates if we should create the child node if not existent.
	 * @return The child node based on the namespace. Will return NULL if non-existent and createChildren is false.
	 */
	public static Permission getNode( String namespace, boolean createChildren )
	{
		String[] nodes = namespace.split( "\\." );
		
		if ( nodes.length < 1 )
			return null;
		
		Permission curr = getRootNode( nodes[0] );
		
		if ( curr == null )
			if ( createChildren )
			{
				try
				{
					curr = PermissionManager.INSTANCE.getBackend().createNode( nodes[0] );
				}
				catch ( PermissionException e )
				{
					PermissionManager.getLogger().warning( "Failed to create node '" + nodes[0] + "':" + e.getMessage() );
					curr = new MemoryPermission( nodes[0] );
				}
				allPerms.add( curr );
			}
			else
				return null;
		
		if ( nodes.length == 1 )
			return curr;
		
		for ( String node : Arrays.copyOfRange( nodes, 1, nodes.length ) )
		{
			Permission child = curr.getChild( node.toLowerCase() );
			if ( child == null )
			{
				if ( createChildren )
				{
					try
					{
						child = PermissionManager.INSTANCE.getBackend().createNode( node, curr );
					}
					catch ( PermissionException e )
					{
						PermissionManager.getLogger().warning( "Failed to create node '" + nodes[0] + "':" + e.getMessage() );
						child = new MemoryPermission( node, curr );
					}
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
	
	public static Permission createNode( String namespace, PermissionValue<?> val, String desc )
	{
		Permission perm = getNode( namespace, true );
		perm.setValue( val );
		perm.setDescription( desc );
		return perm;
	}
	
	public String toString()
	{
		return "Permission{name=" + getLocalName() + ",parent=" + getParent() + ( ( value != null ) ? ",value=" + value.toString() : "" ) + "}";
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
	
	public void debugPermissionStack( int deepth )
	{
		String spacing = ( deepth > 0 ) ? Strings.repeat( "      ", deepth - 1 ) + "|---> " : "";
		
		if ( value == null )
			PermissionManager.getLogger().info( ConsoleColor.YELLOW + spacing + getLocalName() );
		else
			PermissionManager.getLogger().info( ConsoleColor.YELLOW + spacing + getLocalName() + "=" + value.toString() );
		
		deepth++;
		for ( Permission p : children )
		{
			p.debugPermissionStack( deepth );
		}
	}
	
	public abstract void saveNode();
	
	public abstract void reloadNode();
	
	public abstract void destroyNode();
}
