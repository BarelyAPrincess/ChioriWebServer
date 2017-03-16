/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * <p>
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 * <p>
 * All Rights Reserved.
 */
package com.chiorichan.factory.models;

import com.chiorichan.datastore.sql.SQLBase;
import com.chiorichan.datastore.sql.skel.SQLSkelGroupBy;
import com.chiorichan.datastore.sql.skel.SQLSkelLimit;
import com.chiorichan.datastore.sql.skel.SQLSkelOrderBy;
import com.chiorichan.datastore.sql.skel.SQLSkelWhere;
import com.chiorichan.datastore.sql.skel.SQLWhereElement;
import com.chiorichan.datastore.sql.skel.SQLWhereElementSep;
import com.chiorichan.datastore.sql.skel.SQLWhereGroup;
import com.chiorichan.datastore.sql.skel.SQLWhereKeyValue;
import com.chiorichan.factory.groovy.ScriptingBaseGroovy;
import com.chiorichan.logger.Log;
import groovy.lang.Binding;
import groovy.lang.Closure;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides a SQL model builder
 */
public class SQLQueryBuilder extends ScriptingBaseGroovy implements SQLSkelOrderBy<SQLQueryBuilder>, SQLSkelLimit<SQLQueryBuilder>, SQLSkelGroupBy<SQLQueryBuilder>, SQLSkelWhere<SQLQueryBuilder, SQLQueryBuilder>
{
	private SQLBase sql;
	private SQLQueryBuilder builder = null;
	private List<String> dateColumns;

	public SQLQueryBuilder()
	{

	}

	protected SQLQueryBuilder( SQLQueryBuilder builder, SQLBase lastState )
	{
		this.builder = builder;
		this.sql = lastState;
	}

	@Override
	public void setBinding( Binding binding )
	{
		super.setBinding( binding );

		/*
		 * When you need access to the binding variable but it's not made available in the constructor, the next best option is to listen here at the setBinding() method.
		 * This really should have a second look by someone who knows more about Groovy. What are our alternatives?
		 */
		try
		{
			sql = getSql().select( getTable() );
		}
		catch ( SQLException e )
		{
			e.printStackTrace();
		}
	}

	public String getTable()
	{
		String simpleName = getClass().getSimpleName();
		if ( !simpleName.endsWith( "s" ) )
			simpleName += "s";
		simpleName = simpleName.substring( 0, 1 ).toLowerCase() + simpleName.substring( 1 );
		Log.get().info( "The getTable() method in %s was not overridden, so the SQL table name was calculated to be %s", getClass().getSimpleName(), simpleName );
		return simpleName;
	}

	public List<String> getPrintedTableColumns()
	{
		return null;
	}

	public String getColumnFriendlyName( String rawColumnName )
	{
		return rawColumnName;
	}

	public String getPrimaryKey() throws SQLException
	{
		return getSql().table( getTable() ).primaryKey();
	}

	public void setDateColumns( List<String> dateColumns )
	{
		this.dateColumns = dateColumns;
	}

	public SQLModelResults all() throws SQLException
	{
		return new SQLModelResults( this, getSql().select( getTable() ) );
	}

	@Override
	public SQLQueryBuilder or()
	{
		SQLQueryBuilder builder = duplicate( SQLSkelWhere.class );
		( ( SQLSkelWhere ) builder.sql ).or();
		return builder;
	}

	@Override
	public SQLQueryBuilder and()
	{
		SQLQueryBuilder builder = duplicate( SQLSkelWhere.class );
		( ( SQLSkelWhere ) builder.sql ).and();
		return builder;
	}

	@Override
	public SQLWhereKeyValue<SQLQueryBuilder> where( String key )
	{
		return new SQLWhereKeyValue<>( this, key );
	}

	@Override
	public SQLWhereElementSep separator()
	{
		return duplicate( SQLSkelWhere.class ).separator();
	}

	@Override
	public SQLQueryBuilder where( SQLWhereElement element )
	{
		SQLQueryBuilder builder = duplicate( SQLSkelWhere.class );
		( ( SQLSkelWhere ) builder.sql ).where( element );
		return builder;
	}

	@Override
	public SQLWhereGroup group()
	{
		SQLWhereGroup<SQLQueryBuilder, SQLQueryBuilder> group = new SQLWhereGroup<>( this, this );
		SQLQueryBuilder builder = where( group );
		group.seperator( builder.separator() );
		or();
		return group;
	}

	@Override
	public SQLQueryBuilder whereMatches( String key, Object value )
	{
		SQLQueryBuilder builder = duplicate( SQLSkelWhere.class );
		( ( SQLSkelWhere ) builder.sql ).whereMatches( key, value );
		return builder;
	}

	@Override
	public SQLQueryBuilder whereMatches( Collection<String> valueKeys, Collection<Object> valueValues )
	{
		SQLQueryBuilder builder = duplicate( SQLSkelWhere.class );
		( ( SQLSkelWhere ) builder.sql ).whereMatches( valueKeys, valueValues );
		return builder;
	}

	@Override
	public SQLQueryBuilder whereMatches( Map<String, Object> values )
	{
		SQLQueryBuilder builder = duplicate( SQLSkelWhere.class );
		( ( SQLSkelWhere ) builder.sql ).where( values );
		return builder;
	}

	@Override
	public SQLQueryBuilder where( Map<String, Object> map )
	{
		SQLQueryBuilder builder = duplicate( SQLSkelWhere.class );
		( ( SQLSkelWhere ) builder.sql ).where( map );
		return builder;
	}

	@Override
	public SQLQueryBuilder orderBy( Collection columns )
	{
		SQLQueryBuilder builder = duplicate( SQLSkelOrderBy.class );
		( ( SQLSkelOrderBy ) builder.sql ).orderBy( columns );
		return builder;
	}

	@Override
	public SQLQueryBuilder orderBy( String... columns )
	{
		SQLQueryBuilder builder = duplicate( SQLSkelOrderBy.class );
		( ( SQLSkelOrderBy ) builder.sql ).orderBy( columns );
		return builder;
	}

	@Override
	public SQLQueryBuilder orderBy( String column )
	{
		SQLQueryBuilder builder = duplicate( SQLSkelOrderBy.class );
		( ( SQLSkelOrderBy ) builder.sql ).orderBy( column );
		return builder;
	}

	@Override
	public SQLQueryBuilder orderAsc()
	{
		SQLQueryBuilder builder = duplicate( SQLSkelOrderBy.class );
		( ( SQLSkelOrderBy ) builder.sql ).orderAsc();
		return builder;
	}

	@Override
	public SQLQueryBuilder orderDesc()
	{
		SQLQueryBuilder builder = duplicate( SQLSkelOrderBy.class );
		( ( SQLSkelOrderBy ) builder.sql ).orderDesc();
		return builder;
	}

	@Override
	public SQLQueryBuilder rand()
	{
		SQLQueryBuilder builder = duplicate( SQLSkelOrderBy.class );
		( ( SQLSkelOrderBy ) builder.sql ).rand();
		return builder;
	}

	@Override
	public SQLQueryBuilder rand( boolean rand )
	{
		SQLQueryBuilder builder = duplicate( SQLSkelOrderBy.class );
		( ( SQLSkelOrderBy ) builder.sql ).rand( rand );
		return builder;
	}

	@Override
	public SQLQueryBuilder orderBy( Collection<String> columns, String dir )
	{
		SQLQueryBuilder builder = duplicate( SQLSkelOrderBy.class );
		( ( SQLSkelOrderBy ) builder.sql ).orderBy( columns, dir );
		return builder;
	}

	public SQLQueryBuilder skip( int i )
	{
		return offset( i );
	}

	@Override
	public SQLQueryBuilder offset( int i )
	{
		SQLQueryBuilder builder = duplicate( SQLSkelLimit.class );
		( ( SQLSkelLimit ) builder.sql ).offset( i );
		return builder;
	}

	public SQLQueryBuilder take( int i )
	{
		return limit( i );
	}

	@Override
	public int offset()
	{
		return ( ( SQLSkelLimit ) duplicate( SQLSkelLimit.class ).sql ).offset();
	}

	@Override
	public int limit()
	{
		return ( ( SQLSkelLimit ) duplicate( SQLSkelLimit.class ).sql ).limit();
	}

	@Override
	public SQLQueryBuilder limit( int limit, int offset )
	{
		SQLQueryBuilder builder = duplicate( SQLSkelLimit.class );
		( ( SQLSkelLimit ) builder.sql ).limit( limit, offset );
		return builder;
	}

	@Override
	public SQLQueryBuilder limit( int limit )
	{
		SQLQueryBuilder builder = duplicate( SQLSkelLimit.class );
		( ( SQLSkelLimit ) builder.sql ).limit( limit );
		return builder;
	}

	@Override
	public SQLQueryBuilder groupBy( Collection<String> columns )
	{
		SQLQueryBuilder builder = duplicate( SQLSkelGroupBy.class );
		( ( SQLSkelGroupBy ) builder.sql ).groupBy( columns );
		return builder;
	}

	@Override
	public SQLQueryBuilder groupBy( String... columns )
	{
		SQLQueryBuilder builder = duplicate( SQLSkelGroupBy.class );
		( ( SQLSkelGroupBy ) builder.sql ).groupBy( columns );
		return builder;
	}

	@Override
	public SQLQueryBuilder groupBy( String column )
	{
		SQLQueryBuilder builder = duplicate( SQLSkelGroupBy.class );
		( ( SQLSkelGroupBy ) builder.sql ).groupBy( column );
		return builder;
	}

	public List<SQLModel> get() throws SQLException
	{
		return new ArrayList<SQLModel>()
		{{
			for ( Map<String, Object> row : ( Set<Map<String, Object>> ) sql.set() )
				add( new SQLModel( SQLQueryBuilder.this, row ) );
		}};
	}

	public SQLModel first() throws SQLException
	{
		return new SQLModel( this, sql.first() );
	}

	public SQLModel last() throws SQLException
	{
		return new SQLModel( this, sql.first() );
	}

	public List<SQLModel> find( String value ) throws SQLException
	{
		SQLBase db = sql.clone();

		if ( !( db instanceof SQLSkelWhere ) )
			throw new IllegalStateException( "Current SQL state does not implement the SQL where functionality." );

		( ( SQLSkelWhere ) db ).whereMatches( getPrimaryKey(), value );

		return new ArrayList<SQLModel>()
		{{
			for ( Map<String, Object> row : ( Set<Map<String, Object>> ) db.set() )
				add( new SQLModel( SQLQueryBuilder.this, row ) );
		}};
	}

	public SQLModel create()
	{
		return null;
	}

	public SQLQueryBuilder back()
	{
		return builder;
	}

	public SQLQueryBuilder when( boolean i, Closure closure )
	{
		if ( i )
		{
			Object rtn = closure.call( this );
			if ( !( rtn instanceof SQLQueryBuilder ) )
				throw new IllegalStateException( String.format( "The provided closure to when(), MUST return an instance of SQLQueryBuilder but %s was returned.", rtn == null ? "null" : rtn.getClass().getSimpleName() ) );
			return ( SQLQueryBuilder ) rtn;
		}
		return this;
	}

	protected SQLQueryBuilder duplicate( Class<?> clz )
	{
		SQLQueryBuilder builder = duplicate();
		if ( !( clz.isInstance( builder.sql ) ) )
			throw new IllegalStateException( "Current SQL state does not implement " + clz.getSimpleName() + " class." );
		return builder;
	}

	protected SQLQueryBuilder duplicate()
	{
		SQLBase lastState = SQLQueryBuilder.this.sql.clone();
		return new SQLQueryBuilder( SQLQueryBuilder.this, lastState )
		{
			@Override
			public Object run()
			{
				return null;
			}

			@Override
			public String getTable()
			{
				return SQLQueryBuilder.this.getTable();
			}

			// TODO Implement additional overridable methods that pull from the parent
		};
	}

	/**
	 * This is normally overridden.
	 * We just have it here so we don't have to declare this class as abstract.
	 */
	@Override
	public Object run()
	{
		return null;
	}

	public List<String> getColumns() throws SQLException
	{
		return getSql().table( getTable() ).columnNames();
	}
}
