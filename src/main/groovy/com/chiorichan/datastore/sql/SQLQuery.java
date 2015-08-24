/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.datastore.sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.chiorichan.util.DatastoreFunc;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

/**
 * 
 */
public class SQLQuery
{
	static enum SQLQueryType
	{
		SELECT, UPDATE, DELETE, INSERT;
	}
	
	private Connection sql;
	private SQLQueryType type;
	private String table;
	private List<String> fields = Lists.newArrayList();
	private SQLWhereGroup where = null;
	private boolean debug = false;
	private int limit = -1;
	private int offset = -1;
	private List<String> orderBy = Lists.newArrayList();
	private List<String> groupBy = Lists.newArrayList();
	
	public SQLQuery( Connection sql, SQLQueryType type, String table )
	{
		this.sql = sql;
		this.type = type;
		this.table = table;
	}
	
	public SQLQuery debug()
	{
		debug = !debug;
		return this;
	}
	
	public SQLQuery debug( boolean debug )
	{
		this.debug = debug;
		return this;
	}
	
	public SQLResult execute() throws SQLException
	{
		return new SQLResult( sql, this );
	}
	
	public String fields()
	{
		if ( fields.size() == 0 )
			return "*";
		
		return Joiner.on( ", " ).join( DatastoreFunc.wrap( fields, '`' ) );
	}
	
	public SQLQuery fields( String... fields )
	{
		this.fields.addAll( Arrays.asList( fields ) );
		return this;
	}
	
	public String groupBy()
	{
		if ( groupBy.size() == 0 )
			throw new IllegalStateException( "groupBy is empty!" );
		
		return Joiner.on( ", " ).join( DatastoreFunc.wrap( groupBy, '`' ) );
	}
	
	public SQLQuery groupBy( Collection<String> columns )
	{
		groupBy.addAll( columns );
		return this;
	}
	
	public SQLQuery groupBy( String column )
	{
		groupBy.add( column );
		return this;
	}
	
	public boolean isDebug()
	{
		return debug;
	}
	
	public int limit()
	{
		return limit;
	}
	
	public SQLQuery limit( int limit )
	{
		this.limit = limit;
		return this;
	}
	
	public int offset()
	{
		return offset;
	}
	
	public SQLQuery offset( int offset )
	{
		this.offset = offset;
		return this;
	}
	
	public String orderBy()
	{
		if ( orderBy.size() == 0 )
			throw new IllegalStateException( "orderBy is empty!" );
		
		return Joiner.on( ", " ).join( DatastoreFunc.wrap( orderBy, '`' ) );
	}
	
	public SQLQuery orderBy( Collection<String> columns )
	{
		orderBy.addAll( columns );
		return this;
	}
	
	public SQLQuery orderBy( String column )
	{
		orderBy.add( column );
		return this;
	}
	
	public SQLQuery removeFields( String... fields )
	{
		this.fields.removeAll( Arrays.asList( fields ) );
		return this;
	}
	
	public String toQuery()
	{
		List<String> segments = Lists.newArrayList();
		
		segments.add( "SELECT" );
		segments.add( fields() );
		segments.add( "FROM" );
		segments.add( DatastoreFunc.wrap( table, '`' ) );
		
		if ( where != null && where.size() > 0 )
		{
			segments.add( "WHERE" );
			segments.add( where.toQuery() );
		}
		
		if ( groupBy.size() > 0 )
		{
			segments.add( "GROUP BY" );
			segments.add( groupBy() );
		}
		
		if ( orderBy.size() > 0 )
		{
			segments.add( "ORDER BY" );
			segments.add( orderBy() );
		}
		
		if ( limit > 0 )
			segments.add( "LIMIT " + limit );
		
		if ( offset > 0 )
			segments.add( "OFFSET " + limit );
		
		return Joiner.on( " " ).join( segments ) + ";";
	}
	
	public SQLQueryType type()
	{
		return type;
	}
	
	public SQLWhereGroup where()
	{
		if ( where == null )
			where = new SQLWhereGroup( this );
		return where;
	}
	
	public SQLQuery where( Map<String, Object> map )
	{
		where().put( SQLWhereSeperator.AND, map );
		return this;
	}
	
	public SQLQuery where( SQLWhereSeperator sep, Map<String, Object> map )
	{
		where().put( sep, map );
		return this;
	}
	
	public SQLQuery where( SQLWhereSeperator sep, String key, String value )
	{
		where().put( sep, key, value );
		return this;
	}
	
	public SQLQuery where( String key, String value )
	{
		where().put( SQLWhereSeperator.AND, key, value );
		return this;
	}
}
