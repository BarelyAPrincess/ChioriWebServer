package com.chiorichan.permission.structure;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.gradle.jarjar.com.google.common.collect.Sets;

import com.google.common.collect.Lists;

public class Permission
{
	protected static final Set<Permission> allPerms = Sets.newConcurrentHashSet();
	protected final List<Permission> children = Lists.newCopyOnWriteArrayList();
	protected Set<String> acceptableSites = Sets.newConcurrentHashSet();
	private PermissionValue value = null;
	private String name;
	private String description;
	private boolean isRootNode = false;
	
	public Permission( String name, boolean rootNode )
	{
		this( name );
		isRootNode = rootNode;
	}
	
	public Permission( String permName )
	{
		name = permName.toLowerCase();
		allPerms.add( this );
	}
	
	public void setValue( PermissionValue val )
	{
		value = val;
	}
	
	public PermissionValue getValue()
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
			return (String) value.getValue();
		
		return null;
	}
	
	public Integer getInt()
	{
		if ( value.getType() == PermissionValue.PermissionType.INT )
			return (Integer) value.getValue();
		
		return null;
	}
	
	public Boolean getBoolean()
	{
		if ( value.getType() == PermissionValue.PermissionType.BOOLEAN )
			return (Boolean) value.getValue();
		
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
	 *             The new description to set
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
	
	public Permission[] getChildren()
	{
		return children.toArray( new Permission[0] );
	}
	
	public Permission getChild( String name )
	{
		for ( Permission node : children )
		{
			if ( node.getName() == name )
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
	
	/**
	 * Finds a registered permission node in the stack by crawling.
	 * 
	 * @param fullNode
	 * @return
	 */
	public static Permission nodeWalker( String fullNode )
	{
		String[] nodes = fullNode.split( "\\." );
		
		if ( nodes.length < 1 )
			return null;
		
		Permission curr = getRootNode( nodes[0] );
		
		if ( curr == null )
			return null;
		
		for ( String node : Arrays.copyOfRange( nodes, 1, nodes.length ) )
		{
			curr = curr.getChild( node );
			if ( curr == null )
				return null;
		}
		
		return curr;
	}
	
	protected static Permission getRootNode( String name )
	{
		for ( Permission perm : allPerms )
			if ( perm.isRootNode && perm.getName() == name )
				return perm;
		return null;
	}
	
	protected static Permission getNode( String name )
	{
		for ( Permission perm : allPerms )
			if ( perm.getName() == name )
				return perm;
		return null;
	}
}
