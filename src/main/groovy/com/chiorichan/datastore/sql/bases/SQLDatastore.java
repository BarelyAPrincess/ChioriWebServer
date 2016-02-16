/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.datastore.sql.bases;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import com.chiorichan.database.DatabaseEngineLegacy;
import com.chiorichan.datastore.Datastore;
import com.chiorichan.datastore.sql.SQLRawQuery;
import com.chiorichan.datastore.sql.SQLTable;
import com.chiorichan.datastore.sql.SQLWrapper;
import com.chiorichan.datastore.sql.query.SQLQueryDelete;
import com.chiorichan.datastore.sql.query.SQLQueryInsert;
import com.chiorichan.datastore.sql.query.SQLQuerySelect;
import com.chiorichan.datastore.sql.query.SQLQueryUpdate;

/**
 * Base for SQL Datastore
 */
public class SQLDatastore extends Datastore
{
	protected SQLWrapper sql = null;

	public SQLQueryDelete delete( String table ) throws SQLException
	{
		if ( sql == null )
			throw new IllegalStateException( "The SQL instance is not initalized!" );

		return new SQLTable( sql, table ).delete();
	}

	public SQLQueryDelete delete( String table, Map<String, Object> where ) throws SQLException
	{
		return delete( table, where, -1 );
	}

	public SQLQueryDelete delete( String table, Map<String, Object> where, int lmt ) throws SQLException
	{
		return delete( table ).where( where ).limit( lmt );
	}

	public SQLWrapper direct()
	{
		Validate.notNull( sql );
		return sql;
	}

	public DatabaseEngineLegacy getLegacy()
	{
		if ( sql == null )
			throw new IllegalStateException( "The SQL instance is not initalized!" );

		return new DatabaseEngineLegacy( sql );
	}

	public boolean initalized()
	{
		return sql != null;
	}

	public SQLQueryInsert insert( String table ) throws SQLException
	{
		if ( sql == null )
			throw new IllegalStateException( "The SQL instance is not initalized!" );

		return new SQLTable( sql, table ).insert();
	}

	public SQLQueryInsert insert( String table, Map<String, Object> data ) throws SQLException
	{
		return insert( table ).values( data );
	}

	public SQLRawQuery query( String query )
	{
		if ( sql == null )
			throw new IllegalStateException( "The SQL instance is not initalized!" );

		return new SQLRawQuery( sql, query );
	}

	public SQLRawQuery query( String query, Collection<Object> objs )
	{
		if ( sql == null )
			throw new IllegalStateException( "The SQL instance is not initalized!" );

		return new SQLRawQuery( sql, query, objs );
	}

	/**
	 * Legacy Method
	 */
	public SQLRawQuery query( String table, String suffix ) throws SQLException
	{
		return query( String.format( "SELECT * FROM `%s` WHERE %s", table, suffix ) );
	}

	public SQLQuerySelect select( String table ) throws SQLException
	{
		if ( sql == null )
			throw new IllegalStateException( "The SQL instance is not initalized!" );

		return new SQLTable( sql, table ).select();
	}

	/**
	 * Legacy Method
	 */
	public SQLQuerySelect select( String table, Map<String, Object> map ) throws SQLException
	{
		return select( table ).where( map );
	}

	/**
	 * Legacy Method
	 */
	public SQLQuerySelect select( String table, String key, Object value ) throws SQLException
	{
		return select( table, new HashMap<String, Object>()
		{
			{
				put( key, value );
			}
		} );
	}

	/**
	 * Legacy Method
	 */
	public Map<String, Object> selectOne( String table, Map<String, Object> where ) throws SQLException
	{
		return select( table, where ).first();
	}

	/**
	 * Legacy Method
	 */
	public Map<String, Object> selectOne( String table, String key, Object value ) throws SQLException
	{
		return select( table, new HashMap<String, Object>()
		{
			{
				put( key, value );
			}
		} ).first();
	}

	public SQLTable table( String table ) throws SQLException
	{
		if ( sql == null )
			throw new IllegalStateException( "The SQL instance is not initalized!" );

		return new SQLTable( sql, table );
	}

	public SQLQueryUpdate update( String table ) throws SQLException
	{
		if ( sql == null )
			throw new IllegalStateException( "The SQL instance is not initalized!" );

		return new SQLTable( sql, table ).update();
	}

	/**
	 * Legacy Method
	 */
	public SQLQueryUpdate update( String table, Collection<String> dataKeys, Collection<Object> dataValues ) throws SQLException
	{
		return update( table, dataKeys, dataValues, null, null, -1 );
	}

	/**
	 * Legacy Method
	 */
	public SQLQueryUpdate update( String table, Collection<String> dataKeys, Collection<Object> dataValues, Collection<String> whereKeys, Collection<Object> whereValues ) throws SQLException
	{
		return update( table, dataKeys, dataValues, whereKeys, whereValues, -1 );
	}

	/**
	 * Legacy Method
	 */
	public SQLQueryUpdate update( String table, Collection<String> dataKeys, Collection<Object> dataValues, Collection<String> whereKeys, Collection<Object> whereValues, int lmt ) throws SQLException
	{
		SQLQueryUpdate query = update( table ).limit( lmt );
		if ( dataKeys != null && dataValues != null && Math.min( dataKeys.size(), dataValues.size() ) > 0 )
			query.values( dataKeys.toArray( new String[0] ), dataValues.toArray( new Object[0] ) );
		if ( whereKeys != null && whereValues != null && Math.min( whereKeys.size(), whereValues.size() ) > 0 )
			query.whereMatches( whereKeys, whereValues );
		return query;
	}

	/**
	 * Legacy Method
	 */
	public SQLQueryUpdate update( String table, Collection<String> dataKeys, Collection<Object> dataValues, int lmt ) throws SQLException
	{
		return update( table, dataKeys, dataValues, null, null, lmt );
	}

	/**
	 * Legacy Method
	 */
	public SQLQueryUpdate update( String table, Map<String, Object> data ) throws SQLException
	{
		return update( table, data, null, -1 );
	}

	/**
	 * Legacy Method
	 */
	public SQLQueryUpdate update( String table, Map<String, Object> data, int lmt ) throws SQLException
	{
		return update( table, data, null, lmt );
	}

	/**
	 * Legacy Method
	 */
	public SQLQueryUpdate update( String table, Map<String, Object> data, Map<String, Object> where ) throws SQLException
	{
		return update( table, data, where, -1 );
	}

	/**
	 * Legacy Method
	 */
	public SQLQueryUpdate update( String table, Map<String, Object> data, Map<String, Object> where, int lmt ) throws SQLException
	{
		SQLQueryUpdate query = update( table ).limit( lmt );
		if ( data != null && data.size() > 0 )
			query.values( data );
		if ( where != null && where.size() > 0 )
			query.where( where );
		return query;
	}
}
