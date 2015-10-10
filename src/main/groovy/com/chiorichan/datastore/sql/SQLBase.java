/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.datastore.sql;

import java.io.NotSerializableException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import com.chiorichan.Loader;
import com.chiorichan.util.DbFunc;
import com.mysql.jdbc.exceptions.jdbc4.CommunicationsException;
import com.mysql.jdbc.exceptions.jdbc4.MySQLNonTransientConnectionException;

/**
 * Provides the SQL Base Class for all SQL Classes
 */
@SuppressWarnings( {"unchecked", "rawtypes"} )
public abstract class SQLBase<T extends SQLBase> implements SQLResultSkel
{
	protected SQLWrapper sql;
	
	private PreparedStatement stmt = null;
	private boolean autoExecute;
	private boolean debug = false;
	private SQLException lastException = null;
	
	private boolean isFirstCall = true;
	
	protected SQLBase( SQLWrapper sql, boolean autoExecute )
	{
		this.autoExecute = autoExecute;
		
		Validate.notNull( sql );
		this.sql = sql;
	}
	
	/**
	 * @return The current autoExecute value
	 */
	public boolean autoExecute()
	{
		return autoExecute;
	}
	
	/**
	 * Sets if this SQL class will auto execute any changes made to it's query
	 * 
	 * @param autoExecute
	 *            The new autoExecute value
	 */
	public T autoExecute( boolean autoExecute )
	{
		this.autoExecute = autoExecute;
		return ( T ) this;
	}
	
	public T close() throws SQLException
	{
		if ( stmt != null && !stmt.isClosed() )
			stmt.close();
		return ( T ) this;
	}
	
	public abstract T execute() throws SQLException;
	
	public boolean isConnected()
	{
		return sql.isConnected();
	}
	
	public boolean isDebug()
	{
		return debug;
	}
	
	public SQLException lastException()
	{
		return lastException;
	}
	
	private PreparedStatement query( String sqlQuery, boolean isUpdate, boolean save, boolean retry, Object... args ) throws SQLException
	{
		try
		{
			if ( sql == null || !sql.isConnected() )
				throw new SQLException( "The SQL connection is closed or was never opened." );
			
			// stmt = con.prepareStatement( query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE );
			PreparedStatement stmt = sql.prepareStatement( sqlQuery );
			
			int x = 0;
			for ( Object s : args )
				try
				{
					x++;
					stmt.setObject( x, s );
				}
				catch ( SQLException e )
				{
					if ( e.getCause() instanceof NotSerializableException )
						Loader.getLogger().severe( "The object " + s.getClass() + " (" + s.toString() + ") is not serializable!" );
					// Ignore
					// else
					if ( !e.getMessage().startsWith( "Parameter index out of range" ) )
						throw e;
				}
			
			if ( isUpdate )
				stmt.executeUpdate();
			else
				stmt.execute();
			
			if ( save )
			{
				this.stmt = stmt;
				isFirstCall = true;
			}
			
			setPass();
			return stmt;
		}
		catch ( CommunicationsException | MySQLNonTransientConnectionException e )
		{
			if ( !retry && sql.reconnect() )
				return query( sqlQuery, isUpdate, save, true, args );
			else
			{
				setFail( e );
				throw e;
			}
		}
		catch ( SQLException e )
		{
			setFail( e );
			throw e;
		}
	}
	
	protected PreparedStatement query( String sqlQuery, boolean isUpdate, boolean save, Object... args ) throws SQLException
	{
		return query( sqlQuery, isUpdate, save, false, args );
	}
	
	protected PreparedStatement query( String sqlQuery, boolean isUpdate, Object... args ) throws SQLException
	{
		return query( sqlQuery, isUpdate, true, false, args );
	}
	
	public SQLExecute<T> result() throws SQLException
	{
		ResultSet rs = resultSet();
		if ( rs == null )
			return null;
		return new SQLExecute<T>( rs, ( T ) this );
	}
	
	ResultSet resultSet() throws SQLException
	{
		ResultSet rs = statement().getResultSet();
		
		if ( rs == null )
			return null;
		
		if ( isFirstCall )
			// Not being before first, on your first call means no results
			if ( !rs.isBeforeFirst() )
				return null;
			// Next returning null also means no results
			else if ( !rs.next() )
				return null;
		
		isFirstCall = false;
		return rs;
	}
	
	@Override
	public final Map<String, Map<String, Object>> resultToMap() throws SQLException
	{
		return DbFunc.resultToMap( resultSet() );
	}
	
	@Override
	public final Set<Map<String, Object>> resultToSet() throws SQLException
	{
		return DbFunc.resultToSet( resultSet() );
	}
	
	@Override
	public final Map<String, Map<String, String>> resultToStringMap() throws SQLException
	{
		return DbFunc.resultToStringMap( resultSet() );
	}
	
	@Override
	public Set<Map<String, String>> resultToStringSet() throws SQLException
	{
		return DbFunc.resultToStringSet( resultSet() );
	}
	
	@Override
	public final Map<String, Object> rowToMap() throws SQLException
	{
		return DbFunc.rowToMap( resultSet() );
	}
	
	@Override
	public Map<String, String> rowToStringMap() throws SQLException
	{
		return DbFunc.rowToStringMap( resultSet() );
	}
	
	public void setDebug()
	{
		debug = !debug;
	}
	
	public void setDebug( boolean debug )
	{
		this.debug = debug;
	}
	
	protected void setFail( SQLException lastException )
	{
		this.lastException = lastException();
	}
	
	protected void setPass()
	{
		lastException = null;
	}
	
	public Object[] sqlValues()
	{
		return new Object[0];
	}
	
	protected PreparedStatement statement() throws SQLException
	{
		if ( stmt == null || stmt.isClosed() )
		{
			lastException = null;
			try
			{
				execute();
			}
			catch ( SQLException e )
			{
				lastException = e;
				stmt = null;
			}
			
			if ( lastException != null )
				throw lastException;
			
			if ( stmt == null && lastException == null )
				throw new IllegalStateException( "There was an unknown problem encountered while trying to auto execute the SQL query. Both Statement and lastException was null." );
		}
		
		return stmt;
	}
	
	public String toSqlQuery()
	{
		return null;
	}
	
	@Override
	public String toString()
	{
		if ( stmt == null )
			return null;
		return stmt.toString().substring( stmt.toString().indexOf( ": " ) + 2 ).trim();
	}
	
	protected void updateExecution()
	{
		if ( autoExecute )
			try
			{
				stmt = null;
				lastException = null;
				execute();
			}
			catch ( SQLException e )
			{
				lastException = e;
				stmt = null;
			}
		else if ( stmt != null )
		{
			try
			{
				stmt.close();
			}
			catch ( SQLException e )
			{
				e.printStackTrace();
			}
			stmt = null;
		}
	}
}
