/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.datastore.sql.skel;

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

/**
 * 
 */
public class SQLWhereGroup<B, P> extends SQLWhereElement implements SQLSkelWhere<SQLWhereGroup<B, P>, P>
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
	public SQLWhereKeyValue<SQLWhereGroup<B, P>> where( String key )
	{
		SQLWhereKeyValue<SQLWhereGroup<B, P>> keyValue = new SQLWhereKeyValue<SQLWhereGroup<B, P>>( this, key, this );
		keyValue.seperator( currentSeperator );
		elements.add( keyValue );
		and();
		return keyValue;
	}
}
