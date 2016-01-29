package com.chiorichan.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;

public class ObjectStacker<T>
{
	private final String key;
	private T value;
	private final List<ObjectStacker<T>> children = new ArrayList<>();

	public ObjectStacker()
	{
		this( "", null );
	}

	public ObjectStacker( String key )
	{
		this( key, null );
	}

	public ObjectStacker( String key, T value )
	{
		Validate.notNull( key );

		this.key = key;
		this.value = value;
	}

	public ObjectStacker<T> addChild( ObjectStacker<T> child )
	{
		if ( !hasChild( key ) )
		{
			children.add( child );
			return child;
		}
		return null;
	}

	public Set<T> allValues()
	{
		return new HashSet<T>()
		{
			{
				for ( ObjectStacker<T> child : children )
				{
					addAll( child.allValues() );
					add( child.value() );
				}
			}
		};
	}

	private ObjectStacker<T> child( String key, boolean create )
	{
		for ( ObjectStacker<T> child : children )
			if ( child.key() == null )
				children.remove( child );
			else if ( child.key().equals( key ) )
				return child;
		return create ? createChild( key ) : null;
	}

	public void clear()
	{
		children.clear();
		value = null;
	}

	public ObjectStacker<T> createChild( String key )
	{
		return addChild( new ObjectStacker<T>( key ) );
	}

	public ObjectStacker<T> getChild( Namespace nodes, boolean create )
	{
		Validate.notNull( nodes, "nodes can not be null" );
		if ( nodes.getNodeCount() == 0 )
			return this;
		String key = nodes.getFirst();
		ObjectStacker<T> child = child( key, create );
		return child == null ? null : nodes.getNodeCount() <= 1 ? child : child.getChild( nodes.subNamespace( 1 ), create );
	}

	public ObjectStacker<T> getChild( String nodes )
	{
		return getChild( nodes, false );
	}

	public ObjectStacker<T> getChild( String nodes, boolean create )
	{
		return getChild( new Namespace( nodes ), create );
	}

	public boolean hasChild( String nodes )
	{
		return getChild( nodes ) != null;
	}

	public boolean hasChildren()
	{
		return children.size() > 0;
	}

	public String key()
	{
		return key;
	}

	public T value()
	{
		return value;
	}

	public void value( Namespace ns, T value )
	{
		ObjectStacker<T> child = getChild( ns, true );
		child.value( value );
	}

	public T value( String nodes )
	{
		ObjectStacker<T> child = getChild( nodes );
		return child == null ? null : child.value();
	}

	public void value( String nodes, T value )
	{
		value( new Namespace( nodes ), value );
	}

	public void value( T value )
	{
		this.value = value;
	}
}
