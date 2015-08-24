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
import java.sql.DriverManager;
import java.sql.SQLException;

import com.chiorichan.ConsoleLogger;
import com.chiorichan.datastore.Datastore;
import com.chiorichan.datastore.DatastoreManager;
import com.chiorichan.datastore.sql.SQLQuery.SQLQueryType;

/**
 * 
 */
public class SQLDatastore extends Datastore
{
	protected Connection sql;
	
	public static ConsoleLogger getLogger()
	{
		return DatastoreManager.getLogger();
	}
	
	protected void attemptConnection( String connection ) throws SQLException
	{
		sql = DriverManager.getConnection( connection );
		sql.setAutoCommit( true );
	}
	
	protected void attemptConnection( String connection, String user, String pass ) throws SQLException
	{
		sql = DriverManager.getConnection( connection, user, pass );
		sql.setAutoCommit( true );
	}
	
	public SQLQuery delete( String table )
	{
		return new SQLQuery( sql, SQLQueryType.DELETE, table );
	}
	
	public SQLQuery insert( String table )
	{
		return new SQLQuery( sql, SQLQueryType.INSERT, table );
	}
	
	public Boolean isConnected()
	{
		if ( sql == null )
			return false;
		
		try
		{
			return !sql.isClosed();
		}
		catch ( SQLException e )
		{
			return false;
		}
	}
	
	public SQLQuery select( String table )
	{
		return new SQLQuery( sql, SQLQueryType.SELECT, table );
	}
	
	public SQLQuery update( String table )
	{
		return new SQLQuery( sql, SQLQueryType.UPDATE, table );
	}
}
