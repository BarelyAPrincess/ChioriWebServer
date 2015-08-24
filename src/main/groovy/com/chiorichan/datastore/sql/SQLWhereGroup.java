/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.datastore.sql;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

/**
 * 
 */
@SuppressWarnings( "rawtypes" )
public class SQLWhereGroup implements SQLWhereElement
{
	private SQLWhereGroup back = null;
	private SQLQuery parent = null;
	private SQLWhereSeperator pre = SQLWhereSeperator.NONE;
	private List<SQLWhereElement> elements = new LinkedList<SQLWhereElement>();
	
	public SQLWhereGroup( SQLQuery parent )
	{
		this.parent = parent;
	}
	
	public SQLWhereGroup( SQLWhereSeperator pre, SQLWhereGroup back, SQLQuery parent )
	{
		this.pre = pre;
		this.back = back;
	}
	
	public SQLWhereGroup and()
	{
		return putGroup( SQLWhereSeperator.AND );
	}
	
	public SQLWhereGroup and( Map<String, Object> map )
	{
		return put( SQLWhereSeperator.AND, map );
	}
	
	public SQLWhereGroup and( String key, Object value )
	{
		return put( SQLWhereSeperator.AND, key, value );
	}
	
	public SQLWhereGroup back()
	{
		return back;
	}
	
	public SQLWhereGroup or()
	{
		return putGroup( SQLWhereSeperator.OR );
	}
	
	public SQLWhereGroup or( Map<String, Object> map )
	{
		return put( SQLWhereSeperator.OR, map );
	}
	
	public SQLWhereGroup or( String key, Object value )
	{
		return put( SQLWhereSeperator.OR, key, value );
	}
	
	public SQLQuery parent()
	{
		return parent;
	}
	
	public SQLWhereGroup put( SQLWhereSeperator sep, Map<String, Object> map )
	{
		for ( Entry<String, Object> e : map.entrySet() )
			if ( e.getValue() instanceof Collection )
				for ( Object o : ( ( Collection ) e.getValue() ) )
					put( sep, e.getKey(), o );
			else
				elements.add( new SQLKeyValue( sep, e ) );
		return this;
	}
	
	public SQLWhereGroup put( SQLWhereSeperator sep, String key, Object value )
	{
		elements.add( new SQLKeyValue( SQLWhereSeperator.OR, key, value ) );
		return this;
	}
	
	public SQLWhereGroup putGroup( SQLWhereSeperator sep )
	{
		SQLWhereGroup where = new SQLWhereGroup( sep, this, parent );
		elements.add( where );
		return where;
	}
	
	public SQLWhereGroup putGroup( SQLWhereSeperator sep, Map<String, Object> map )
	{
		SQLWhereGroup where = putGroup( sep );
		for ( Entry<String, Object> e : map.entrySet() )
			if ( e.getValue() instanceof Collection )
				for ( Object o : ( ( Collection ) e.getValue() ) )
					where.put( sep, e.getKey(), o );
			else
				where.put( sep, e.getKey(), e.getValue() );
		return this;
	}
	
	public SQLWhereGroup putGroup( SQLWhereSeperator sep, String key, Collection<Object> values )
	{
		SQLWhereGroup where = putGroup( sep );
		for ( Object o : values )
			where.put( sep, key, o );
		return this;
	}
	
	@Override
	public SQLWhereSeperator seperator()
	{
		return pre;
	}
	
	public int size()
	{
		return elements.size();
	}
	
	@Override
	public String toQuery()
	{
		List<String> segments = Lists.newArrayList();
		
		for ( SQLWhereElement e : elements )
		{
			if ( segments.size() > 0 )
				segments.add( e.seperator().name() );
			if ( e instanceof SQLWhereGroup )
				segments.add( "(" + e.toQuery() + ")" );
			else
				segments.add( e.toQuery() );
		}
		
		if ( segments.size() == 0 )
			return "";
		
		return Joiner.on( " " ).join( segments );
	}
}
