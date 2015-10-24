/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.datastore.sql;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Set;

import com.chiorichan.datastore.sql.query.SQLQueryDelete;
import com.chiorichan.datastore.sql.query.SQLQueryInsert;
import com.chiorichan.datastore.sql.query.SQLQuerySelect;
import com.chiorichan.datastore.sql.query.SQLQueryUpdate;
import com.google.common.collect.Sets;
import com.mysql.jdbc.exceptions.jdbc4.CommunicationsException;

/**
 * Interfaces with MySQL Table
 */
public class SQLTable extends SQLBase<SQLTable>
{
	private final String table;
	private final DatabaseMetaData meta;
	
	public SQLTable( SQLWrapper sql, String table ) throws SQLException
	{
		super( sql, false );
		this.table = table;
		meta = sql.getMetaData();
	}
	
	public void addColumn( Class<?> colType, String colName, int max ) throws SQLException
	{
		// TODO New Empty Method
	}
	
	public SQLTable addColumn( String colType, String colName, int max ) throws SQLException
	{
		/*
		 * if ( exists() )
		 * {
		 * 
		 * }
		 * else
		 * queryWithException( "CREATE TABLE `test` ( `reqlevel` varchar(255) NOT NULL DEFAULT '-1' );" );
		 */
		
		return this;
	}
	
	public SQLTable addColumnInt( String colName, int i ) throws SQLException
	{
		return this;
	}
	
	public SQLTable addColumnInt( String colName, int i, int def ) throws SQLException
	{
		return this;
	}
	
	public SQLTable addColumnText( String colName, int i ) throws SQLException
	{
		return this;
	}
	
	public SQLTable addColumnVar( String colName, int i ) throws SQLException
	{
		
		
		return this;
	}
	
	public SQLTable addColumnVar( String colName, int i, String def ) throws SQLException
	{
		return this;
	}
	
	public Collection<String> columnNames() throws SQLException
	{
		Set<String> rtn = Sets.newLinkedHashSet();
		
		query( "SELECT * FROM `" + table + "` LIMIT 1;", false );
		
		ResultSetMetaData rsmd = resultSet().getMetaData();
		
		for ( int i = 1; i < rsmd.getColumnCount() + 1; i++ )
			rtn.add( rsmd.getColumnName( i ) );
		
		return rtn;
	}
	
	public SqlTableColumns columns() throws SQLException
	{
		SqlTableColumns rtn = new SqlTableColumns();
		
		ResultSet columns = sql.getMetaData().getColumns( null, null, table, null );
		
		while ( columns.next() )
		{
			String name = columns.getString( "COLUMN_NAME" );
			int type = columns.getInt( "DATA_TYPE" );
			int size = columns.getInt( "COLUMN_SIZE" );
			String def = columns.getString( "COLUMN_DEF" );
			boolean nullable = "YES".equals( columns.getString( "IS_NULLABLE" ) );
			
			rtn.add( name, size, type, def, nullable );
		}
		
		return rtn;
	}
	
	public SQLQueryDelete delete()
	{
		return new SQLQueryDelete( sql, table );
	}
	
	public SQLTable drop() throws SQLException
	{
		// TODO Drop Table
		return this;
	}
	
	@Override
	public SQLTable execute() throws SQLException
	{
		return this;
	}
	
	public boolean exists()
	{
		return exists( false );
	}
	
	private boolean exists( boolean retry )
	{
		try
		{
			ResultSet rs = meta.getTables( null, null, null, null );
			// return DbFunc.rowCount( rs ) > 0;
			while ( rs.next() )
				if ( rs.getString( 3 ).equalsIgnoreCase( table ) )
				{
					setPass();
					return true;
				}
		}
		catch ( CommunicationsException e )
		{
			if ( !retry )
				return exists( true );
			setFail( e );
		}
		catch ( SQLException e )
		{
			setFail( e );
		}
		return false;
	}
	
	public SQLQueryInsert insert()
	{
		return new SQLQueryInsert( sql, table );
	}
	
	@Override
	public int rowCount() throws SQLException
	{
		return -1;
	}
	
	public SQLQuerySelect select()
	{
		return new SQLQuerySelect( sql, table );
	}
	
	public SQLQuerySelect select( Collection<String> fields )
	{
		return select().fields( fields );
	}
	
	public SQLQueryUpdate update()
	{
		return new SQLQueryUpdate( sql, table );
	}
}
