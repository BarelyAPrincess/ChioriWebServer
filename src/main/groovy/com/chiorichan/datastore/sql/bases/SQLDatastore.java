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
	
	public SQLWrapper direct()
	{
		Validate.notNull( sql );
		return sql;
	}
	
	public DatabaseEngineLegacy getLegacy()
	{
		if ( sql == null )
			throw new IllegalStateException( "The SQL instance is not initalized!" );
		
		return new DatabaseEngineLegacy( sql.direct() );
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
	
	public SQLRawQuery query( String query )
	{
		if ( sql == null )
			throw new IllegalStateException( "The SQL instance is not initalized!" );
		
		return new SQLRawQuery( sql, query );
	}
	
	public SQLQuerySelect select( String table ) throws SQLException
	{
		if ( sql == null )
			throw new IllegalStateException( "The SQL instance is not initalized!" );
		
		return new SQLTable( sql, table ).select();
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
}
