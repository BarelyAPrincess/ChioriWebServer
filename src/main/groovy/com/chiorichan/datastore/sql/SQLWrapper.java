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
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.commons.lang3.Validate;

import com.chiorichan.datastore.Datastore;
import com.chiorichan.datastore.sql.bases.SQLDatastore;
import com.mysql.jdbc.CommunicationsException;
import com.mysql.jdbc.exceptions.MySQLNonTransientConnectionException;

/**
 * Wraps the SQL Connection
 */
public class SQLWrapper
{
	private SQLDatastore ds;
	private Connection sql;
	private String savedConnection, savedUser = null, savedPass = null;
	
	public SQLWrapper( SQLDatastore ds, Connection sql )
	{
		savedConnection = null;
		this.ds = ds;
		this.sql = sql;
	}
	
	public SQLWrapper( SQLDatastore ds, String connection ) throws SQLException
	{
		this.ds = ds;
		connect( connection );
	}
	
	public SQLWrapper( SQLDatastore ds, String connection, String user, String pass ) throws SQLException
	{
		this.ds = ds;
		connect( connection, user, pass );
	}
	
	void connect() throws SQLException
	{
		if ( savedUser != null && savedPass != null )
			connect( savedConnection, savedUser, savedPass );
		else
			connect( savedConnection );
	}
	
	void connect( String connection ) throws SQLException
	{
		if ( sql != null && !sql.isClosed() )
			sql.close();
		
		Validate.notNull( connection );
		
		savedConnection = connection;
		sql = DriverManager.getConnection( connection );
		sql.setAutoCommit( true );
	}
	
	void connect( String connection, String user, String pass ) throws SQLException
	{
		if ( sql != null && !sql.isClosed() )
			sql.close();
		
		Validate.notNull( connection );
		Validate.notNull( user );
		
		savedConnection = connection;
		savedUser = user;
		savedPass = pass;
		sql = DriverManager.getConnection( connection, user, pass );
		sql.setAutoCommit( true );
	}
	
	public SQLDatastore datastore()
	{
		return ds;
	}
	
	public Connection direct()
	{
		return sql;
	}
	
	public DatabaseMetaData getMetaData() throws SQLException
	{
		return sql.getMetaData();
	}
	
	public boolean isClosed() throws SQLException
	{
		return sql.isClosed();
	}
	
	public boolean isConnected()
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
	
	PreparedStatement prepareStatement( String query ) throws SQLException
	{
		return prepareStatement( query, false );
	}
	
	PreparedStatement prepareStatement( String query, boolean retry ) throws SQLException
	{
		try
		{
			return sql.prepareStatement( query );
		}
		catch ( CommunicationsException | MySQLNonTransientConnectionException e )
		{
			if ( !retry && reconnect() )
				return prepareStatement( query, true );
			else
				throw e;
		}
	}
	
	PreparedStatement prepareStatement( String query, boolean retry, int resultSetType ) throws SQLException
	{
		try
		{
			return sql.prepareStatement( query, resultSetType );
		}
		catch ( CommunicationsException | MySQLNonTransientConnectionException e )
		{
			if ( !retry && reconnect() )
				return prepareStatement( query, true, resultSetType );
			else
				throw e;
		}
	}
	
	PreparedStatement prepareStatement( String query, boolean retry, int resultSetType, int resultSetConcurrency ) throws SQLException
	{
		try
		{
			return sql.prepareStatement( query, resultSetType, resultSetConcurrency );
		}
		catch ( CommunicationsException | MySQLNonTransientConnectionException e )
		{
			if ( !retry && reconnect() )
				return prepareStatement( query, true, resultSetType, resultSetConcurrency );
			else
				throw e;
		}
	}
	
	PreparedStatement prepareStatement( String query, boolean retry, int resultSetType, int resultSetConcurrency, int resultSetHoldability ) throws SQLException
	{
		try
		{
			return sql.prepareStatement( query, resultSetType, resultSetConcurrency, resultSetHoldability );
		}
		catch ( CommunicationsException | MySQLNonTransientConnectionException e )
		{
			if ( !retry && reconnect() )
				return prepareStatement( query, true, resultSetType, resultSetConcurrency, resultSetHoldability );
			else
				throw e;
		}
	}
	
	PreparedStatement prepareStatement( String query, int resultSetType ) throws SQLException
	{
		return prepareStatement( query, false, resultSetType );
	}
	
	PreparedStatement prepareStatement( String query, int resultSetType, int resultSetConcurrency ) throws SQLException
	{
		return prepareStatement( query, false, resultSetType, resultSetConcurrency );
	}
	
	PreparedStatement prepareStatement( String query, int resultSetType, int resultSetConcurrency, int resultSetHoldability ) throws SQLException
	{
		return prepareStatement( query, false, resultSetType, resultSetConcurrency, resultSetHoldability );
	}
	
	boolean reconnect()
	{
		try
		{
			Validate.notNull( savedConnection );
			
			connect();
			Datastore.getLogger().info( "We succesully connected to the sql database. Connection: " + savedConnection );
			return true;
		}
		catch ( Exception e )
		{
			Datastore.getLogger().severe( "There was an error reconnecting. Connection: " + savedConnection, e );
		}
		return false;
	}
}
