/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.permission;

import java.util.List;

import com.chiorichan.ConsoleColor;
import com.chiorichan.permission.lang.PermissionException;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

/**
 * Permission class for each permission node
 */
public final class Permission
{
	protected final List<Permission> children = Lists.newCopyOnWriteArrayList();
	protected final String localName;
	protected PermissionModelValue model;
	protected final Permission parent;
	
	public Permission( PermissionNamespace ns, PermissionType type )
	{
		this( ns.getLocalName(), type, ( ns.getNodeCount() <= 1 ) ? null : PermissionManager.INSTANCE.getNode( ns.getParent(), true ) );
	}
	
	Permission( String localName )
	{
		this( localName, PermissionType.DEFAULT );
	}
	
	Permission( String localName, Permission parent )
	{
		this( localName, PermissionType.DEFAULT, parent );
	}
	
	Permission( String localName, PermissionType type )
	{
		this( localName, type, null );
	}
	
	Permission( String localName, PermissionType type, Permission parent )
	{
		if ( localName.contains( "." ) )
			throw new PermissionException( String.format( "The permission local name can not contain periods, %s", localName ) );
		
		this.localName = localName.toLowerCase();
		this.parent = parent;
		
		model = new PermissionModelValue( localName, type, this );
		
		PermissionManager.INSTANCE.addPermission( this );
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
	
	void debugPermissionStack( int deepth )
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
	
	void setType( PermissionType type )
	{
		model = new PermissionModelValue( localName, type, this );
	}
	
	@Override
	public String toString()
	{
		return String.format( "Permission{name=%s,parent=%s,modelValue=%s}", getLocalName(), getParent(), model );
	}
}
