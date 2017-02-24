/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.factory.models;

import com.chiorichan.datastore.sql.SQLBase;
import com.chiorichan.datastore.sql.skel.SQLSkelLimit;
import com.chiorichan.datastore.sql.skel.SQLSkelOrderBy;
import com.chiorichan.datastore.sql.skel.SQLSkelWhere;
import com.chiorichan.factory.groovy.ScriptingBaseGroovy;
import com.chiorichan.logger.Log;
import groovy.lang.Binding;
import groovy.lang.Closure;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides a SQL model builder
 */
public class SQLQueryBuilder extends ScriptingBaseGroovy
{
	private SQLBase sql;
	private SQLQueryBuilder builder = null;
	private List<String> dateColumns;

	public SQLQueryBuilder()
	{

	}

	protected SQLQueryBuilder( SQLQueryBuilder builder, SQLBase lastState )
	{
		builder = builder;
		sql = lastState;
	}

	@Override
	public void setBinding( Binding binding )
	{
		super.setBinding( binding );

		/**
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

	public SQLQueryBuilder where( String column, String value ) throws SQLException
	{
		SQLQueryBuilder builder = duplicate();

		SQLBase sql = builder.sql;

		if ( !( sql instanceof SQLSkelWhere ) )
			throw new IllegalStateException( "Current SQL state does not implement the SQL where functionality." );

		( ( SQLSkelWhere ) sql ).whereMatches( column, value );

		return builder;
	}

	public SQLQueryBuilder orderBy( List<String> columns, String dir ) throws SQLException
	{
		SQLQueryBuilder builder = duplicate();

		SQLBase sql = builder.sql;

		if ( !( sql instanceof SQLSkelOrderBy ) )
			throw new IllegalStateException( "Current SQL state does not implement the SQL orderBy functionality." );

		( ( SQLSkelOrderBy ) sql ).orderBy( columns );

		if ( dir.trim().equalsIgnoreCase( "asc" ) )
			( ( SQLSkelOrderBy ) sql ).orderAsc();
		else if ( dir.trim().equalsIgnoreCase( "desc" ) )
			( ( SQLSkelOrderBy ) sql ).orderDesc();

		return builder;
	}

	public SQLQueryBuilder skip( int i ) throws SQLException
	{
		return offset( i );
	}

	public SQLQueryBuilder offset( int i ) throws SQLException
	{
		SQLQueryBuilder builder = duplicate();

		SQLBase sql = builder.sql;

		if ( !( sql instanceof SQLSkelLimit ) )
			throw new IllegalStateException( "Current SQL state does not implement the SQL limit functionality." );

		( ( SQLSkelLimit ) sql ).offset( i );

		return builder;
	}

	public SQLQueryBuilder take( int i ) throws SQLException
	{
		return limit( i );
	}

	public SQLQueryBuilder limit( int i ) throws SQLException
	{
		SQLQueryBuilder builder = duplicate();

		SQLBase sql = builder.sql;

		if ( !( sql instanceof SQLSkelLimit ) )
			throw new IllegalStateException( "Current SQL state does not implement the SQL limit functionality." );

		( ( SQLSkelLimit ) sql ).limit( i );

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

	protected SQLQueryBuilder duplicate() throws SQLException
	{
		SQLBase lastState = SQLQueryBuilder.this.sql;
		if ( lastState instanceof Cloneable )
			lastState = lastState.clone();
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
	 * We just have it here so we don't have to declare abstract.
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
