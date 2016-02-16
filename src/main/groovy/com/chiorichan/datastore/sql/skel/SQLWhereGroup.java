/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.datastore.sql.skel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.chiorichan.datastore.DatastoreManager;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

/**
 *
 */
public class SQLWhereGroup<B extends SQLSkelWhere<?, ?>, P> extends SQLWhereElement implements SQLSkelWhere<SQLWhereGroup<B, P>, P>
{
	private List<SQLWhereElement> elements = Lists.newLinkedList();
	private SQLWhereElementSep currentSeperator = SQLWhereElementSep.NONE;

	/*
	 * key = val
	 * key NOT val
	 * key LIKE val
	 * key < val
	 * key > val
	 *
	 * WHERE
	 * KEY -> DIVIDER -> VALUE
	 * AND
	 * OR
	 */

	private B back = null;
	private P parent = null;

	public SQLWhereGroup( B back, P parent )
	{
		this.back = back;
		this.parent = parent;
	}

	@Override
	public SQLWhereGroup<B, P> and()
	{
		if ( elements.size() < 1 )
			currentSeperator = SQLWhereElementSep.NONE;
		else
			currentSeperator = SQLWhereElementSep.AND;
		return this;
	}

	public B back()
	{
		back.where( this );
		return back;
	}

	@Override
	public SQLWhereGroup<SQLWhereGroup<B, P>, P> group()
	{
		SQLWhereGroup<SQLWhereGroup<B, P>, P> group = new SQLWhereGroup<SQLWhereGroup<B, P>, P>( this, parent );
		group.seperator( currentSeperator );
		elements.add( group );
		or();
		return group;
	}

	@Override
	public SQLWhereGroup<B, P> or()
	{
		if ( elements.size() < 1 )
			currentSeperator = SQLWhereElementSep.NONE;
		else
			currentSeperator = SQLWhereElementSep.OR;
		return this;
	}

	public P parent()
	{
		back.where( this );
		return parent;
	}

	public int size()
	{
		return elements.size();
	}

	@Override
	public String toSqlQuery()
	{
		List<String> segments = Lists.newLinkedList();

		for ( SQLWhereElement e : elements )
		{
			if ( e.seperator() != SQLWhereElementSep.NONE && e != elements.get( 0 ) )
				segments.add( e.seperator().toString() );
			segments.add( e.toSqlQuery() );
		}

		if ( segments.size() == 0 )
			return "";

		return "(" + Joiner.on( " " ).join( segments ) + ")";
	}

	@Override
	public SQLWhereGroup<B, P> where( Map<String, Object> map )
	{
		for ( Entry<String, Object> e : map.entrySet() )
		{
			String key = e.getKey();
			Object val = e.getValue();

			if ( key.startsWith( "|" ) )
			{
				key = key.substring( 1 );
				or();
			}
			else if ( key.startsWith( "&" ) )
			{
				key = key.substring( 1 );
				and();
			}

			if ( val instanceof Map )
				try
				{
					SQLWhereGroup<?, ?> group = group();

					@SuppressWarnings( "unchecked" )
					Map<String, Object> submap = ( Map<String, Object> ) val;
					for ( Entry<String, Object> e2 : submap.entrySet() )
					{
						String key2 = e2.getKey();
						Object val2 = e2.getValue();

						if ( key2.startsWith( "|" ) )
						{
							key2 = key2.substring( 1 );
							group.or();
						}
						else if ( key2.startsWith( "&" ) )
						{
							key2 = key2.substring( 1 );
							group.and();
						}

						where( key2 ).matches( val2 );
					}
				}
				catch ( ClassCastException ee )
				{
					DatastoreManager.getLogger().severe( ee );
				}
			else
				where( key ).matches( val );
		}

		return this;
	}

	@Override
	public SQLWhereGroup<B, P> where( SQLWhereElement element )
	{
		element.seperator( currentSeperator );
		elements.add( element );
		and();

		return this;
	}

	@Override
	public SQLWhereKeyValue<SQLWhereGroup<B, P>> where( String key )
	{
		return new SQLWhereKeyValue<SQLWhereGroup<B, P>>( this, key );
	}

	@Override
	public SQLWhereGroup<B, P> whereMatches( Collection<String> valueKeys, Collection<Object> valueValues )
	{
		SQLWhereGroup<SQLWhereGroup<B, P>, P> group = new SQLWhereGroup<SQLWhereGroup<B, P>, P>( this, parent );

		List<String> listKeys = new ArrayList<>( valueKeys );
		List<Object> listValues = new ArrayList<>( valueValues );

		for ( int i = 0; i < Math.min( listKeys.size(), listValues.size() ); i++ )
		{
			SQLWhereKeyValue<SQLWhereGroup<SQLWhereGroup<B, P>, P>> groupElement = group.where( listKeys.get( i ) );
			groupElement.seperator( SQLWhereElementSep.AND );
			groupElement.matches( listValues.get( i ) );
		}

		group.parent();
		or();
		return this;
	}

	@Override
	public SQLWhereGroup<B, P> whereMatches( Map<String, Object> values )
	{
		SQLWhereGroup<SQLWhereGroup<B, P>, P> group = new SQLWhereGroup<SQLWhereGroup<B, P>, P>( this, parent );

		for ( Entry<String, Object> val : values.entrySet() )
		{
			SQLWhereKeyValue<SQLWhereGroup<SQLWhereGroup<B, P>, P>> groupElement = group.where( val.getKey() );
			groupElement.seperator( SQLWhereElementSep.AND );
			groupElement.matches( val.getValue() );
		}

		group.parent();
		or();
		return this;
	}

	@Override
	public SQLWhereGroup<B, P> whereMatches( String key, Object value )
	{
		return new SQLWhereKeyValue<SQLWhereGroup<B, P>>( this, key ).matches( value );
	}
}
