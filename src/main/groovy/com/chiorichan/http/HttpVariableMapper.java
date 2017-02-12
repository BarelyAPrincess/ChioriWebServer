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

import com.chiorichan.zutils.ZObjects;
import org.apache.commons.lang3.Validate;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class HttpVariableMapper implements Map<String, Object>
{
	class HeaderEntry extends SimpleEntry<String, Object>
	{
		HeaderEntry( String key, Object value )
		{
			super( key, value );
		}

		public String getValueAsString()
		{
			return getValue() == null ? null : ZObjects.castToString( getValue() );
		}
	}

	private final List<HeaderEntry> entries = new ArrayList<>();

	public Map<String, Object> asMap()
	{
		return new HashMap<String, Object>()
		{
			{
				for ( HeaderEntry entry : entries )
					put( entry.getKey(), entry.getValue() );
			}
		};
	}

	@Override
	public void clear()
	{
		entries.clear();
	}

	@Override
	public boolean containsKey( Object key )
	{
		for ( HeaderEntry entry : entries )
			if ( entry.getKey().equals( key ) )
				return true;
		return false;
	}

	public boolean containsKey( ServerVars key )
	{
		for ( HeaderEntry entry : entries )
			if ( entry.getKey().equals( key.toString() ) )
				return true;
		return false;
	}

	@Override
	public boolean containsValue( Object value )
	{
		for ( HeaderEntry entry : entries )
			if ( entry.getValue().equals( value ) )
				return true;
		return false;
	}

	@Override
	public Set<Entry<String, Object>> entrySet()
	{
		return new HashSet<>( entries );
	}

	@Override
	public Object get( Object key )
	{
		for ( HeaderEntry entry : entries )
			if ( entry.getKey().equals( key ) )
				return entry.getValue();
		return null;
	}

	public Object get( ServerVars key )
	{
		for ( HeaderEntry entry : entries )
			if ( entry.getKey().equals( key.name() ) )
				return entry;
		return null;
	}

	@Override
	public boolean isEmpty()
	{
		return entries.isEmpty();
	}

	@Override
	public Set<String> keySet()
	{
		return entries.stream().map( Map.Entry::getKey ).collect( Collectors.toSet() );
	}

	public Object put( ServerVars key, Object value )
	{
		Validate.notNull( key );
		return put( key.name(), value );
	}

	@Override
	public Object put( String key, Object value )
	{
		Validate.notNull( key );
		Object obj = remove( key );
		entries.add( new HeaderEntry( key, value ) );
		return obj;
	}

	@Override
	public void putAll( Map<? extends String, ? extends Object> map )
	{
		for ( Entry<? extends String, ? extends Object> entry : map.entrySet() )
			put( entry.getKey(), entry.getValue() );
	}

	@Override
	public Object remove( Object key )
	{
		for ( HeaderEntry entry : entries )
			if ( entry.getKey().equals( key ) )
			{
				entries.remove( entry );
				return entry;
			}
		return null;
	}

	public Object remove( ServerVars key )
	{
		for ( HeaderEntry entry : entries )
			if ( entry.getKey().equals( key.name() ) )
			{
				entries.remove( entry );
				return entry;
			}
		return null;
	}

	@Override
	public int size()
	{
		return entries.size();
	}

	@Override
	public Collection<Object> values()
	{
		return new HashSet<Object>()
		{
			{
				for ( HeaderEntry entry : entries )
					add( entry.getValue() );
			}
		};
	}
}
