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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.chiorichan.datastore.DatastoreManager;
import com.mysql.jdbc.exceptions.jdbc4.CommunicationsException;
import com.mysql.jdbc.exceptions.jdbc4.MySQLNonTransientConnectionException;

/**
 * 
 */
public class SQLResult
{
	private Connection sql;
	private SQLQuery query;
	PreparedStatement stmt = null;
	
	public SQLResult( Connection sql, SQLQuery query ) throws SQLException
	{
		this.sql = sql;
		this.query = query;
		execute();
	}
	
	public SQLResult close() throws SQLException
	{
		if ( stmt != null && !stmt.isClosed() )
			stmt.close();
		return this;
	}
	
	public SQLResult execute() throws SQLException
	{
		String sqlQuery = query.toQuery();
		
		if ( sql == null || sql.isClosed() )
			throw new SQLException( "The SQL connection is closed or was never opened." );
		
		try
		{
			// stmt = con.prepareStatement( query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE );
			stmt = sql.prepareStatement( sqlQuery );
			
			/*
			 * int x = 0;
			 * for ( Object s : args )
			 * try
			 * {
			 * x++;
			 * stmt.setObject( x, s );
			 * }
			 * catch ( SQLException e )
			 * {
			 * if ( !e.getMessage().startsWith( "Parameter index out of range" ) )
			 * throw e;
			 * }
			 */
			
			stmt.executeQuery();
			
			if ( query.isDebug() )
				DatastoreManager.getLogger().info( "SQL Query `" + sqlQuery + "` affected " + rowCount() + " rows!" );
		}
		catch ( CommunicationsException | MySQLNonTransientConnectionException e )
		{
			// if ( !retried && reconnect() )
			// return query( query, true, args );
			// else
			throw e;
		}
		catch ( Throwable t )
		{
			t.printStackTrace();
			throw t;
		}
		
		return this;
	}
	
	public ResultSet getResultSet() throws SQLException
	{
		return stmt.getResultSet();
	}
	
	public int rowCount()
	{
		try
		{
			ResultSet result = stmt.getResultSet();
			
			int row = result.getRow();
			result.last();
			int lastRow = result.getRow();
			result.absolute( row );
			return lastRow;
		}
		catch ( Exception e )
		{
			return 0;
		}
	}
}
