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

import com.chiorichan.datastore.Datastore;
import com.chiorichan.util.DbFunc;
import com.google.common.base.Joiner;
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

	private ResultSet resultSetCache = null;

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
	 *             The new autoExecute value
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

	public T debug()
	{
		debug = !debug;
		return ( T ) this;
	}

	public T debug( boolean debug )
	{
		this.debug = debug;
		return ( T ) this;
	}

	public final T execute() throws SQLException
	{
		resultSetCache = null;
		return execute0();
	}

	protected abstract T execute0() throws SQLException;

	public final Map<String, Object> first() throws SQLException
	{
		Map<String, Map<String, Object>> map = map();
		if ( map.size() == 0 )
			return null;
		return map.get( map.keySet().toArray( new String[0] )[0] );
	}

	public boolean isConnected()
	{
		return sql.isConnected();
	}

	public boolean isDebug()
	{
		return debug;
	}

	public final Map<String, Object> last() throws SQLException
	{
		Map<String, Map<String, Object>> map = map();
		if ( map.size() == 0 )
			return null;
		return map.get( map.keySet().toArray( new String[0] )[map.keySet().size() - 1] );
	}

	public SQLException lastException()
	{
		return lastException;
	}

	@Override
	public final Map<String, Map<String, Object>> map() throws SQLException
	{
		return DbFunc.resultToMap( resultSet() );
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
				if ( s != null )
					try
					{
						x++;
						stmt.setObject( x, s );
					}
					catch ( SQLException e )
					{
						if ( e.getCause() instanceof NotSerializableException )
							Datastore.getLogger().severe( "The object " + s.getClass() + " (" + s.toString() + ") is not serializable!" );

						if ( !e.getMessage().startsWith( "Parameter index out of range" ) )
							throw e;
					}
					catch ( ArrayIndexOutOfBoundsException e )
					{
						Datastore.getLogger().warning( String.format( "SQL Query '%s' is missing enough replace points (?) to satify the argument '%s', index '%s'", sqlQuery, s, x ) );
					}

			try
			{
				if ( isUpdate )
					stmt.executeUpdate();
				else
					stmt.execute();
			}
			catch ( SQLException e )
			{
				Datastore.getLogger().severe( "SQL query failed \"" + sqlQuery + "\" with arguments '" + Joiner.on( ", " ).join( args ) + "' with explanation '" + e.getMessage() + "'" );
				if ( isDebug() )
					e.printStackTrace();
				throw e;
			}

			if ( save )
				setStatement( stmt );

			if ( debug && save )
				Datastore.getLogger().debug( "SQL query \"" + sqlQuery + "\" -> \"" + DbFunc.toString( stmt ) + "\" " + ( isUpdate ? "affected" : "returned" ) + " " + rowCount() + " results" );
			else if ( debug )
				Datastore.getLogger().debug( "SQL query \"" + sqlQuery + "\" -> \"" + DbFunc.toString( stmt ) + "\"" );

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

	/**
	 * Gets the ResultSet for the last execution
	 *
	 * @return
	 *         The resulting {@link ResultSet} from the last execution, will return null is query was update or there were no results
	 * @throws SQLException
	 *              if a database access error occurs or this method is called on a closed
	 *              Statement
	 */
	ResultSet resultSet() throws SQLException
	{
		if ( resultSetCache == null )
		{
			resultSetCache = statement().getResultSet();

			if ( resultSetCache == null )
				return null;

			if ( isFirstCall )
				// Not being before first, on your first call means no results
				// Next returning null also means no results
				if ( !resultSetCache.isBeforeFirst() || !resultSetCache.next() )
					return null;

			isFirstCall = false;
		}

		return resultSetCache;
	}

	@Override
	public final Map<String, Object> row() throws SQLException
	{
		return DbFunc.rowToMap( resultSet() );
	}

	@Override
	public final Set<Map<String, Object>> set() throws SQLException
	{
		return DbFunc.resultToSet( resultSet() );
	}

	protected void setFail( SQLException lastException )
	{
		this.lastException = lastException();
	}

	protected void setPass()
	{
		lastException = null;
	}

	protected void setStatement( PreparedStatement stat )
	{
		this.stmt = stat;
		isFirstCall = true;
	}

	public abstract Object[] sqlValues();

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

	@Override
	public final Map<String, Map<String, String>> stringMap() throws SQLException
	{
		return DbFunc.resultToStringMap( resultSet() );
	}

	@Override
	public Map<String, String> stringRow() throws SQLException
	{
		return DbFunc.rowToStringMap( resultSet() );
	}

	@Override
	public Set<Map<String, String>> stringSet() throws SQLException
	{
		return DbFunc.resultToStringSet( resultSet() );
	}

	@Override
	public String toString()
	{
		return DbFunc.toString( stmt );
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
