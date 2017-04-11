/*
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.factory.models

import com.chiorichan.datastore.sql.query.SQLQuerySelect
import com.chiorichan.datastore.sql.skel.*
import com.chiorichan.factory.ScriptingContext
import com.chiorichan.factory.ScriptingEvents
import com.chiorichan.factory.groovy.ScriptingBaseHttp
import com.chiorichan.logger.Log
import com.chiorichan.utils.UtilObjects
import com.chiorichan.utils.UtilStrings

import java.sql.SQLException

/**
 * Provides a SQL model builder
 * */
abstract class SQLModelBuilder extends ScriptingBaseHttp implements SQLSkelOrderBy<SQLModelBuilder>, SQLSkelLimit<SQLModelBuilder>, SQLSkelGroupBy<SQLModelBuilder>, SQLSkelWhere<SQLModelBuilder, SQLModelBuilder>, Cloneable, ScriptingEvents
{
	protected List<String> dateColumns
	protected SQLQuerySelect sqlBase

	@Override
	void onBeforeExecute( ScriptingContext context )
	{
		if ( sqlBase == null )
			sqlBase = getSql().select( getTable() )
	}

	@Override
	void onAfterExecute( ScriptingContext context )
	{
		// Do Nothing
	}

	@Override
	void onException( ScriptingContext context, Throwable throwable )
	{
		// Do Nothing
	}

	String getTable()
	{
		String simpleName = getClass().getSimpleName()
		if ( !simpleName.endsWith( "s" ) )
			simpleName += "s"
		simpleName = simpleName.substring( 0, 1 ).toLowerCase() + simpleName.substring( 1 )
		Log.get().info( String.format( "The getTable() method in %s was not overridden, so the SQL table name was calculated to be %s", getClass().getSimpleName(), simpleName ) )
		return simpleName
	}

	List<String> getPrintedTableColumns()
	{
		return null
	}

	String getColumnFriendlyName( String rawColumnName )
	{
		return rawColumnName
	}

	String getPrimaryKey() throws SQLException
	{
		return getSql().table( getTable() ).primaryKey()
	}

	void setDateColumns( List<String> dateColumns )
	{
		this.dateColumns = dateColumns
	}

	SQLModelResults all() throws SQLException
	{
		return new SQLModelResults( this, getSql().select( getTable() ) )
	}

	SQLModelPaginated paginate()
	{
		return paginate( 15 )
	}

	SQLModelPaginated paginate( int perPage )
	{
		return paginate( perPage, 0 )
	}

	SQLModelPaginated paginate( int perPage, int currentPage )
	{
		return new SQLModelPaginated( this, sqlBase, perPage, currentPage )
	}

	@Override
	SQLModelBuilder or()
	{
		sqlBase.or()
		return this
	}

	@Override
	SQLModelBuilder and()
	{
		sqlBase.and()
		return this
	}

	@Override
	SQLWhereKeyValue<SQLModelBuilder> where( String key )
	{
		return new SQLWhereKeyValue<>( this, key )
	}

	@Override
	SQLWhereElementSep separator()
	{
		return sqlBase.separator()
	}

	@Override
	SQLModelBuilder where( SQLWhereElement element )
	{
		SQLModelBuilder builder = clone()
		( ( SQLSkelWhere ) builder.sqlBase ).where( element )
		return builder
	}

	@Override
	SQLWhereGroup group()
	{
		SQLWhereGroup<SQLModelBuilder, SQLModelBuilder> group = new SQLWhereGroup<>( this, this )
		SQLModelBuilder builder = where( group )
		group.seperator( builder.separator() )
		or()
		return group
	}

	@Override
	SQLModelBuilder whereMatches( String key, Object value )
	{
		SQLModelBuilder builder = clone()
		( ( SQLSkelWhere ) builder.sqlBase ).whereMatches( key, value )
		return builder
	}

	@Override
	SQLModelBuilder whereMatches( Collection<String> valueKeys, Collection<Object> valueValues )
	{
		SQLModelBuilder builder = clone()
		( ( SQLSkelWhere ) builder.sqlBase ).whereMatches( valueKeys, valueValues )
		return builder
	}

	@Override
	SQLModelBuilder whereMatches( Map<String, Object> values )
	{
		SQLModelBuilder builder = clone()
		( ( SQLSkelWhere ) builder.sqlBase ).where( values )
		return builder
	}

	@Override
	SQLModelBuilder where( Map<String, Object> map )
	{
		SQLModelBuilder builder = clone()
		( ( SQLSkelWhere ) builder.sqlBase ).where( map )
		return builder
	}

	@Override
	SQLModelBuilder orderBy( Collection<String> columns, String dir )
	{
		SQLModelBuilder builder = clone()
		( ( SQLSkelOrderBy ) builder.sqlBase ).orderBy( columns, dir )
		return builder
	}

	@Override
	SQLModelBuilder orderBy( Collection<String> columns )
	{
		SQLModelBuilder builder = clone()
		( ( SQLSkelOrderBy ) builder.sqlBase ).orderBy( columns )
		return builder
	}

	@Override
	SQLModelBuilder orderBy( String column, String dir )
	{
		SQLModelBuilder builder = clone()
		( ( SQLSkelOrderBy ) builder.sqlBase ).orderBy( column, dir )
		return builder
	}

	@Override
	SQLModelBuilder orderBy( String column )
	{
		SQLModelBuilder builder = clone()
		( ( SQLSkelOrderBy ) builder.sqlBase ).orderBy( column )
		return builder
	}

	@Override
	SQLModelBuilder orderAsc()
	{
		SQLModelBuilder builder = clone()
		( ( SQLSkelOrderBy ) builder.sqlBase ).orderAsc()
		return builder
	}

	@Override
	SQLModelBuilder orderDesc()
	{
		SQLModelBuilder builder = clone()
		( ( SQLSkelOrderBy ) builder.sqlBase ).orderDesc()
		return builder
	}

	@Override
	SQLModelBuilder rand()
	{
		SQLModelBuilder builder = clone()
		( ( SQLSkelOrderBy ) builder.sqlBase ).rand()
		return builder
	}

	@Override
	SQLModelBuilder rand( boolean rand )
	{
		SQLModelBuilder builder = clone()
		( ( SQLSkelOrderBy ) builder.sqlBase ).rand( rand )
		return builder
	}

	SQLModelBuilder skip( int i )
	{
		return offset( i )
	}

	@Override
	SQLModelBuilder offset( int i )
	{
		SQLModelBuilder builder = clone()
		( ( SQLSkelLimit ) builder.sqlBase ).offset( i )
		return builder
	}

	SQLModelBuilder take( int i )
	{
		return limit( i )
	}

	@Override
	int offset()
	{
		return ( ( SQLSkelLimit ) clone().sqlBase ).offset()
	}

	@Override
	int limit()
	{
		return ( ( SQLSkelLimit ) clone().sqlBase ).limit()
	}

	@Override
	SQLModelBuilder limit( int limit, int offset )
	{
		SQLModelBuilder builder = clone()
		( ( SQLSkelLimit ) builder.sqlBase ).limit( limit, offset )
		return builder
	}

	@Override
	SQLModelBuilder limit( int limit )
	{
		SQLModelBuilder builder = clone()
		( ( SQLSkelLimit ) builder.sqlBase ).limit( limit )
		return builder
	}

	@Override
	SQLModelBuilder groupBy( Collection<String> columns )
	{
		SQLModelBuilder builder = clone()
		( ( SQLSkelGroupBy ) builder.sqlBase ).groupBy( columns )
		return builder
	}

	@Override
	SQLModelBuilder groupBy( String... columns )
	{
		SQLModelBuilder builder = clone()
		( ( SQLSkelGroupBy ) builder.sqlBase ).groupBy( columns )
		return builder
	}

	@Override
	SQLModelBuilder groupBy( String column )
	{
		SQLModelBuilder builder = clone()
		( ( SQLSkelGroupBy ) builder.sqlBase ).groupBy( column )
		return builder
	}

	SQLModelResults get() throws SQLException
	{
		return new SQLModelResults( this, sqlBase )
	}

	SQLModel first() throws SQLException
	{
		return new SQLModel( this, sqlBase.first() )
	}

	SQLModel last() throws SQLException
	{
		return new SQLModel( this, sqlBase.first() )
	}

	SQLModelResults find( String value ) throws SQLException
	{
		SQLQuerySelect sql = sqlBase.clone()

		( sql as SQLSkelWhere ).whereMatches( getPrimaryKey(), value )

		return new SQLModelResults( this, sql )
	}

	SQLModel create()
	{
		return null
	}

	SQLModelBuilder when( boolean i, Closure closure )
	{
		if ( i )
		{
			Object rtn = closure.call( this )
			if ( rtn != null && !( rtn instanceof SQLModelBuilder ) )
				throw new IllegalStateException( String.format( "The provided closure to when(), MUST return null or an instance of SQLQueryBuilder but %s was returned.", rtn == null ? "null" : rtn.getClass().getSimpleName() ) )
			return ( SQLModelBuilder ) rtn
		}
		return this
	}

	List<String> getColumns() throws SQLException
	{
		return getSql().table( getTable() ).columnNames()
	}

	/**
	 * Gets the SQL Base object used to back this builder.
	 *
	 * @return The backing SQL Base
	 */
	SQLQuerySelect getBase()
	{
		UtilObjects.notNull( sqlBase )
		return sqlBase
	}

	protected void setBase( SQLQuerySelect sqlBase )
	{
		UtilObjects.notNull( sqlBase )
		this.sqlBase = sqlBase
	}

	SQLModelBuilder root()
	{
		SQLModelBuilder root = this

		while ( root instanceof SQLModelBuilderChild )
			root = ( ( SQLModelBuilderChild ) root ).back()

		return root
	}

	def methodMissing( String key, args )
	{
		String methodName = "scope" + UtilStrings.capitalizeWordsFully( key )
		MetaMethod method = getMetaClass().getMetaMethod( methodName, new Object[0] )
		if ( method == null )
			throw new MissingMethodException( methodName, getClass(), new Object[0] )
		return method.invoke( this, new Object[0] )
	}

	@Override
	SQLModelBuilder clone()
	{
		return new SQLModelBuilderChild( this, sqlBase )
	}
}
