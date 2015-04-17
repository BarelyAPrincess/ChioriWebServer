/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.database;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONException;

import com.chiorichan.ConsoleLogger;
import com.chiorichan.Loader;
import com.chiorichan.lang.StartupException;
import com.chiorichan.util.ObjectUtil;
import com.chiorichan.util.StringUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mysql.jdbc.exceptions.jdbc4.CommunicationsException;
import com.mysql.jdbc.exceptions.jdbc4.MySQLNonTransientConnectionException;

/**
 * Gives easy access to the SQL Database within Groovy scripts.
 */
public class DatabaseEngine
{
	public enum DBType
	{
		SQLITE, MYSQL, UNKNOWN;
	}
	
	private Connection con;
	private String savedDb, savedUser, savedPass, savedHost, savedPort;
	private DBType type = DBType.UNKNOWN;
	
	public DatabaseEngine()
	{
		
	}
	
	public DatabaseEngine( String db, String user, String pass ) throws SQLException, ClassNotFoundException, ConnectException
	{
		init( db, user, pass, null, null );
	}
	
	public DatabaseEngine( String db, String user, String pass, String host ) throws SQLException, ClassNotFoundException, ConnectException
	{
		init( db, user, pass, host, null );
	}
	
	public DatabaseEngine( String db, String user, String pass, String host, String port ) throws SQLException, ClassNotFoundException, ConnectException
	{
		init( db, user, pass, host, port );
	}
	
	public DatabaseEngine( String filename ) throws SQLException, ClassNotFoundException
	{
		init( filename );
	}
	
	/**
	 * Initializes a sqLite connection.
	 * 
	 * @param filename
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	public void init( String filename ) throws SQLException
	{
		try
		{
			Class.forName( "org.sqlite.JDBC" );
		}
		catch ( ClassNotFoundException e )
		{
			throw new StartupException( "We could not locate the 'org.sqlite.JDBC' library, be sure to have this library in your build path." );
		}
		
		File sqliteDb = new File( filename );
		
		if ( !sqliteDb.exists() )
		{
			getLogger().warning( "The SQLite file '" + sqliteDb.getAbsolutePath() + "' did not exist, we will attempt to create a blank one now." );
			try
			{
				sqliteDb.createNewFile();
			}
			catch ( IOException e )
			{
				throw new SQLException( "We had a problem creating the SQLite file, the exact exception message was: " + e.getMessage(), e );
			}
		}
		
		con = DriverManager.getConnection( "jdbc:sqlite:" + sqliteDb.getAbsolutePath() );
		
		getLogger().info( "We succesully connected to the sqLite database using 'jdbc:sqlite:" + sqliteDb.getAbsolutePath() + "'" );
		type = DBType.SQLITE;
	}
	
	/**
	 * Initializes a mySQL connection.
	 * 
	 * @param db
	 * @param user
	 * @param pass
	 * @param host
	 * @param port
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 * @throws ConnectException
	 */
	public void init( String db, String user, String pass, String host, String port ) throws SQLException
	{
		if ( host == null )
			host = "localhost";
		
		if ( port == null )
			port = "3306";
		
		try
		{
			Class.forName( "com.mysql.jdbc.Driver" );
		}
		catch ( ClassNotFoundException e )
		{
			throw new StartupException( "We could not locate the 'com.mysql.jdbc.Driver' library, be sure to have this library in your build path." );
		}
		
		savedDb = db;
		savedUser = user;
		savedPass = pass;
		savedHost = host;
		savedPort = port;
		
		try
		{
			con = DriverManager.getConnection( "jdbc:mysql://" + host + ":" + port + "/" + db, user, pass );
		}
		catch ( SQLException e )
		{
			throw e;
		}
		
		if ( con != null && !con.isClosed() )
			Loader.getLogger().info( "We succesully connected to the sql database using 'jdbc:mysql://" + host + ":" + port + "/" + db + "'." );
		else
			Loader.getLogger().warning( "There was a problem connecting to the sql database using 'jdbc:mysql://" + host + ":" + port + "/" + db + "'." );
		
		type = DBType.MYSQL;
	}
	
	public LinkedHashMap<String, Object> selectOne( String table, List<String> keys, List<? extends Object> values ) throws SQLException
	{
		if ( con == null )
			throw new SQLException( "The SQL connection is closed or was never opened." );
		
		if ( isNull( keys ) || isNull( values ) )
		{
			Loader.getLogger().warning( "[DB ERROR] Either keys array or values array equals null!\n" );
			return null;
		}
		
		if ( keys.size() != values.size() )
		{
			System.err.print( "[DB ERROR] Keys array and values array must match in length!\n" );
			return null;
		}
		
		LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
		
		String where = "";
		
		if ( keys.size() > 0 && values.size() > 0 )
		{
			int x = 0;
			String prefix = "";
			for ( String s : keys )
			{
				where += prefix + "`" + s + "` = '" + values.get( x ) + "'";
				x++;
				prefix = " AND ";
			}
		}
		
		ResultSet rs = query( "SELECT * FROM `" + table + "` WHERE " + where + " LIMIT 1;" );
		
		if ( rs != null && getRowCount( rs ) > 0 )
		{
			try
			{
				ResultSetMetaData rsmd = rs.getMetaData();
				int columnCount = rsmd.getColumnCount();
				
				do
				{
					for ( int i = 1; i < columnCount + 1; i++ )
					{
						result.put( rsmd.getColumnName( i ), rs.getObject( i ) );
					}
				}
				while ( rs.next() );
				
				return result;
			}
			catch ( Exception e )
			{
				e.printStackTrace();
			}
		}
		
		return null;
	}
	
	public Boolean isNull( Object o )
	{
		if ( o == null )
			return true;
		
		return false;
	}
	
	public int getRowCount( ResultSet rs )
	{
		try
		{
			// int curRow = rs.getRow();
			rs.last();
			int lastRow = rs.getRow();
			rs.first(); // TODO: Set the row???
			return lastRow;
		}
		catch ( Exception e )
		{
			return 0;
		}
	}
	
	public Boolean isConnected()
	{
		if ( con == null )
			return false;
		
		try
		{
			return !con.isClosed();
		}
		catch ( SQLException e )
		{
			return false;
		}
	}
	
	public Boolean reconnect()
	{
		try
		{
			if ( savedHost == null || savedPort == null || savedDb == null )
			{
				Loader.getLogger().severe( "There was an error reconnection to the DB, unknown cause other then connection string are NULL." );
				return false;
			}
			
			con = DriverManager.getConnection( "jdbc:mysql://" + savedHost + ":" + savedPort + "/" + savedDb, savedUser, savedPass );
			Loader.getLogger().info( "We succesully connected to the sql database." );
		}
		catch ( Exception e )
		{
			Loader.getLogger().severe( "There was an error reconnection to the DB, " + "jdbc:mysql://" + savedHost + ":" + savedPort + "/" + savedDb + ", " + savedUser + " " + savedPass, e );
		}
		
		return true;
	}
	
	public int queryUpdate( String query, Object... args ) throws SQLException
	{
		PreparedStatement stmt = null;
		
		if ( con == null )
			throw new SQLException( "The SQL connection is closed or was never opened." );
		
		try
		{
			stmt = con.prepareStatement( query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE );
			
			int x = 0;
			
			for ( Object s : args )
				try
				{
					x++;
					stmt.setString( x, ObjectUtil.castToString( s ) );
				}
				catch ( SQLException e )
				{
					if ( !e.getMessage().startsWith( "Parameter index out of range" ) )
						throw e;
				}
			
			stmt.execute();
			
			Loader.getLogger().fine( "Update Query: \"" + stmt.toString() + "\" which affected " + stmt.getUpdateCount() + " row(s)." );
		}
		catch ( MySQLNonTransientConnectionException e )
		{
			if ( reconnect() )
				return queryUpdate( query );
		}
		catch ( CommunicationsException e )
		{
			if ( reconnect() )
				return queryUpdate( query );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}
		
		return stmt.getUpdateCount();
	}
	
	public int queryUpdate( String query ) throws SQLException
	{
		int cnt = 0;
		
		if ( con == null )
			throw new SQLException( "The SQL connection is closed or was never opened." );
		
		try
		{
			PreparedStatement statement = con.prepareStatement( query );
			statement.execute();
			cnt = statement.getUpdateCount();
		}
		catch ( MySQLNonTransientConnectionException e )
		{
			if ( reconnect() )
				return queryUpdate( query );
		}
		catch ( CommunicationsException e )
		{
			if ( reconnect() )
				return queryUpdate( query );
		}
		catch ( SQLException e )
		{
			throw e;
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}
		
		Loader.getLogger().fine( "Update Query: \"" + query + "\" which affected " + cnt + " row(s)." );
		return cnt;
	}
	
	public ResultSet query( String query ) throws SQLException
	{
		return query( query, false );
	}
	
	public ResultSet query( String query, boolean retried ) throws SQLException
	{
		Statement stmt = null;
		ResultSet result = null;
		
		if ( con == null )
			throw new SQLException( "The SQL connection is closed or was never opened." );
		
		try
		{
			try
			{
				stmt = con.createStatement( ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE );
			}
			catch ( CommunicationsException e )
			{
				if ( reconnect() )
					stmt = con.createStatement( ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE );
			}
			finally
			{
				if ( stmt == null )
					stmt = con.createStatement();
			}
			
			result = stmt.executeQuery( query );
			
			Loader.getLogger().fine( "SQL Query `" + query + "` returned " + getRowCount( result ) + " rows!" );
		}
		catch ( CommunicationsException | MySQLNonTransientConnectionException e )
		{
			if ( !retried && reconnect() )
				return query( query, true );
			else
			{
				throw e;
			}
		}
		catch ( Throwable t )
		{
			t.printStackTrace();
			throw t;
		}
		
		return result;
	}
	
	public ResultSet query( String query, Object... args ) throws SQLException
	{
		return query( query, false, args );
	}
	
	public ResultSet query( String query, boolean retried, Object... args ) throws SQLException
	{
		PreparedStatement stmt = null;
		ResultSet result = null;
		
		if ( con == null )
			throw new SQLException( "The SQL connection is closed or was never opened." );
		
		try
		{
			stmt = con.prepareStatement( query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE );
			
			int x = 0;
			
			for ( Object s : args )
				try
				{
					x++;
					Loader.getLogger().debug( x + " -> " + ObjectUtil.castToString( s ) );
					stmt.setString( x, ObjectUtil.castToString( s ) );
				}
				catch ( SQLException e )
				{
					if ( !e.getMessage().startsWith( "Parameter index out of range" ) )
						throw e;
				}
			
			result = stmt.executeQuery();
			
			Loader.getLogger().fine( "SQL Query `" + stmt.toString() + "` returned " + getRowCount( result ) + " rows!" );
		}
		catch ( CommunicationsException | MySQLNonTransientConnectionException e )
		{
			if ( !retried && reconnect() )
				return query( query, true, args );
			else
			{
				throw e;
			}
		}
		catch ( Throwable t )
		{
			t.printStackTrace();
			throw t;
		}
		
		return result;
	}
	
	public boolean update( String table, List<? extends Object> keys, List<? extends Object> values )
	{
		return update( table, keys, values, null, null );
	}
	
	public boolean update( String table, List<? extends Object> keys, List<? extends Object> list, List<? extends Object> keysW, List<? extends Object> valuesW )
	{
		if ( isNull( keys ) || isNull( list ) )
		{
			System.err.print( "[DB ERROR] Either keys array or values array equals null!\n" );
			return false;
		}
		
		if ( keys.size() != list.size() )
		{
			System.err.print( "[DB ERROR] Keys array and values array must match in length!\n" );
			return false;
		}
		
		if ( keysW.size() != valuesW.size() )
		{
			System.err.print( "[DB ERROR] Where keys array and where values array must match in length!\n" );
			return false;
		}
		
		if ( keys.size() < 1 )
		{
			System.err.print( "[DB ERROR] The keys to be updated can not be empty!\n" );
			return false;
		}
		
		String where = "";
		String update = "";
		
		int x = 0;
		String prefix = "";
		
		for ( Object s : keys )
		{
			update += prefix + "`" + s + "` = '" + list.get( x ) + "'";
			x++;
			prefix = ", ";
		}
		
		if ( !isNull( keysW ) && !isNull( valuesW ) && keysW.size() > 0 && valuesW.size() > 0 )
		{
			x = 0;
			prefix = "";
			
			for ( Object s : keysW )
			{
				where += prefix + "`" + s + "` = '" + valuesW.get( x ) + "'";
				x++;
				prefix = " AND ";
			}
			
			if ( where.length() > 0 )
				where = " WHERE " + where;
		}
		
		int cnt = 0;
		try
		{
			cnt = queryUpdate( "UPDATE " + table + " SET " + update + where + ";" );
		}
		catch ( SQLException e )
		{
			e.printStackTrace();
			return false;
		}
		
		return ( cnt > 0 );
	}
	
	public LinkedHashMap<String, Object> selectOne( String table, String key, String val ) throws SQLException
	{
		return selectOne( table, Arrays.asList( key ), Arrays.asList( val ) );
	}
	
	public static LinkedHashMap<String, Object> convert( ResultSet rs ) throws SQLException, JSONException
	{
		LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
		int x = 0;
		
		rs.first();
		
		do
		{
			result.put( "" + x, convertRow( rs ) );
			x++;
		}
		while ( rs.next() );
		
		return result;
	}
	
	public static Map<String, String> toStringsMap( ResultSet rs ) throws SQLException
	{
		Map<String, Object> source = convertRow( rs );
		Map<String, String> result = Maps.newLinkedHashMap();
		
		for ( Entry<String, Object> e : source.entrySet() )
		{
			String val = ObjectUtil.castToString( e.getValue() );
			if ( val != null )
				result.put( e.getKey(), val );
		}
		
		return result;
	}
	
	public static Map<String, Object> convertRow( ResultSet rs ) throws SQLException
	{
		Map<String, Object> result = Maps.newLinkedHashMap();
		ResultSetMetaData rsmd = rs.getMetaData();
		
		int numColumns = rsmd.getColumnCount();
		
		for ( int i = 1; i < numColumns + 1; i++ )
		{
			String columnName = rsmd.getColumnName( i );
			
			// Loader.getLogger().info( "Column: " + columnName + " <-> " + rsmd.getColumnTypeName( i ) );
			
			if ( rsmd.getColumnType( i ) == java.sql.Types.ARRAY )
			{
				result.put( columnName, rs.getArray( columnName ) );
			}
			else if ( rsmd.getColumnType( i ) == java.sql.Types.BIGINT )
			{
				result.put( columnName, rs.getInt( columnName ) );
			}
			else if ( rsmd.getColumnType( i ) == java.sql.Types.TINYINT )
			{
				result.put( columnName, rs.getInt( columnName ) );
			}
			else if ( rsmd.getColumnType( i ) == java.sql.Types.BIT ) // Sometimes tinyints are read as bits
			{
				result.put( columnName, rs.getInt( columnName ) );
			}
			else if ( rsmd.getColumnType( i ) == java.sql.Types.BOOLEAN )
			{
				result.put( columnName, rs.getBoolean( columnName ) );
			}
			else if ( rsmd.getColumnTypeName( i ).contains( "BLOB" ) || rsmd.getColumnType( i ) == java.sql.Types.BINARY )
			{
				// BLOG = Max Length 65,535. Recommended that you use a LONGBLOG.
				byte[] bytes = rs.getBytes( columnName );
				result.put( columnName, bytes );
				/*
				 * try
				 * {
				 * result.put( columnName, new String( bytes, "ISO-8859-1" ) );
				 * }
				 * catch ( UnsupportedEncodingException e )
				 * {
				 * e.printStackTrace();
				 * }
				 */
			}
			else if ( rsmd.getColumnType( i ) == java.sql.Types.DOUBLE )
			{
				result.put( columnName, rs.getDouble( columnName ) );
			}
			else if ( rsmd.getColumnType( i ) == java.sql.Types.FLOAT )
			{
				result.put( columnName, rs.getFloat( columnName ) );
			}
			else if ( rsmd.getColumnTypeName( i ).equals( "INT" ) )
			{
				result.put( columnName, rs.getInt( columnName ) );
			}
			else if ( rsmd.getColumnType( i ) == java.sql.Types.NVARCHAR )
			{
				result.put( columnName, rs.getNString( columnName ) );
			}
			else if ( rsmd.getColumnTypeName( i ).equals( "VARCHAR" ) )
			{
				result.put( columnName, rs.getString( columnName ) );
			}
			else if ( rsmd.getColumnType( i ) == java.sql.Types.SMALLINT )
			{
				result.put( columnName, rs.getInt( columnName ) );
			}
			else if ( rsmd.getColumnType( i ) == java.sql.Types.DATE )
			{
				result.put( columnName, rs.getDate( columnName ) );
			}
			else if ( rsmd.getColumnType( i ) == java.sql.Types.TIMESTAMP )
			{
				result.put( columnName, rs.getTimestamp( columnName ) );
			}
			else
			{
				result.put( columnName, rs.getObject( columnName ) );
			}
		}
		
		return result;
	}
	
	public ResultSetMetaData getTableMetaData( String table ) throws SQLException
	{
		ResultSet rs = query( "SELECT * FROM " + table );
		return rs.getMetaData();
	}
	
	public List<String> getTableFieldNames( String table ) throws SQLException
	{
		List<String> rtn = Lists.newArrayList();
		
		ResultSet rs = query( "SELECT * FROM " + table );
		
		ResultSetMetaData rsmd = rs.getMetaData();
		
		int numColumns = rsmd.getColumnCount();
		
		for ( int i = 1; i < numColumns + 1; i++ )
		{
			rtn.add( rsmd.getColumnName( i ) );
		}
		
		return rtn;
	}
	
	public List<String> getTableFieldTypes( String table ) throws SQLException
	{
		List<String> rtn = Lists.newArrayList();
		
		ResultSet rs = query( "SELECT * FROM " + table );
		
		ResultSetMetaData rsmd = rs.getMetaData();
		
		int numColumns = rsmd.getColumnCount();
		
		for ( int i = 1; i < numColumns + 1; i++ )
		{
			rtn.add( rsmd.getColumnTypeName( i ) );
		}
		
		return rtn;
	}
	
	@SuppressWarnings( "unchecked" )
	public LinkedHashMap<String, Object> selectOne( String table, Object where ) throws SQLException
	{
		LinkedHashMap<String, Object> result = select( table, where );
		
		if ( result == null || result.size() < 1 )
			return null;
		
		return ( LinkedHashMap<String, Object> ) result.get( "0" );
	}
	
	public LinkedHashMap<String, Object> select( String table ) throws SQLException
	{
		return select( table, null, null );
	}
	
	public LinkedHashMap<String, Object> select( String table, Object where ) throws SQLException
	{
		return select( table, where, null );
	}
	
	@SuppressWarnings( "unchecked" )
	public LinkedHashMap<String, Object> select( String table, Object where, Map<String, Object> options0 ) throws SQLException
	{
		String subWhere = "";
		
		String whr = "";
		
		if ( where instanceof String )
		{
			whr = ( ( String ) where );
		}
		else if ( where instanceof Map )
		{
			Map<String, Object> whereMap = ( Map<String, Object> ) where;
			
			String tmp = "", opr = "";// , opr2 = "";
			
			for ( Entry<String, Object> entry : whereMap.entrySet() )
			{
				if ( entry.getValue() instanceof Map )
				{
					opr = "AND";
					if ( entry.getKey().indexOf( "|" ) >= 0 )
						opr = "OR";
					if ( entry.getKey().indexOf( "&" ) >= 0 )
						opr = "AND";
					
					String tmp2 = "";
					Map<String, Object> val = ( Map<String, Object> ) entry.getValue();
					
					for ( Entry<String, Object> entry2 : val.entrySet() )
					{
						// opr2 = "AND";
						if ( entry.getKey().indexOf( "|" ) >= 0 )
							opr = "OR";
						if ( entry.getKey().indexOf( "&" ) >= 0 )
							opr = "AND";
						
						String key = entry2.getKey().replace( "|", "" ).replace( "&", "" );
						tmp2 = "`" + key + "` = '" + entry2.getValue() + "'";
						tmp += ( tmp.isEmpty() ) ? tmp2 : subWhere + " " + opr + " " + tmp2;
					}
					
					whr = ( whr.isEmpty() ) ? "(" + tmp + ")" : whr + " " + opr + " (" + tmp + ")";
				}
				else
				{
					opr = "AND";
					if ( entry.getKey().indexOf( "|" ) >= 0 )
						opr = "OR";
					if ( entry.getKey().indexOf( "&" ) >= 0 )
						opr = "AND";
					
					String key = entry.getKey().replace( "|", "" ).replace( "&", "" );
					
					tmp = "`" + key + "` = '" + entry.getValue() + "'";
					whr = ( whr.isEmpty() ) ? tmp : whr + " " + opr + " " + tmp;
				}
			}
		}
		else
		{
			whr = "";
		}
		
		Map<String, String> options = new LinkedHashMap<String, String>();
		
		if ( options0 != null )
			for ( Entry<String, Object> o : options0.entrySet() )
			{
				options.put( o.getKey().toLowerCase(), ObjectUtil.castToString( o.getValue() ) );
			}
		
		if ( !options.containsKey( "limit" ) || ! ( options.get( "limit" ) instanceof String ) )
			options.put( "limit", "0" );
		if ( !options.containsKey( "offset" ) || ! ( options.get( "offset" ) instanceof String ) )
			options.put( "offset", "0" );
		if ( !options.containsKey( "orderby" ) )
			options.put( "orderby", "" );
		if ( !options.containsKey( "groupby" ) )
			options.put( "groupby", "" );
		if ( !options.containsKey( "fields" ) )
			options.put( "fields", "*" );
		if ( !options.containsKey( "debug" ) )
			options.put( "debug", "false" );
		
		String limit = ( Integer.parseInt( options.get( "limit" ) ) > 0 ) ? " LIMIT " + Integer.parseInt( options.get( "offset" ) ) + ", " + Integer.parseInt( options.get( "limit" ) ) : "";
		String orderby = ( ( String ) options.get( "orderby" ) ) == "" ? "" : " ORDER BY " + ( ( String ) options.get( "orderby" ) );
		String groupby = ( ( String ) options.get( "groupby" ) ) == "" ? "" : " GROUP BY " + ( ( String ) options.get( "groupby" ) );
		
		where = ( whr.isEmpty() ) ? "" : " WHERE " + whr;
		
		String query = "SELECT " + ( ( String ) options.get( "fields" ) ) + " FROM `" + table + "`" + where + groupby + orderby + limit + ";";
		
		// TODO: Act on result!
		sqlInjectionDetection( query );
		
		ResultSet rs = query( query );
		
		if ( rs == null )
		{
			if ( StringUtil.isTrue( options.get( "debug" ) ) )
				Loader.getLogger().info( "Making SELECT query \"" + query + "\" which returned an error." );
			else
				Loader.getLogger().fine( "Making SELECT query \"" + query + "\" which returned an error." );
			return null;
		}
		
		if ( getRowCount( rs ) < 1 )
		{
			if ( StringUtil.isTrue( options.get( "debug" ) ) )
				Loader.getLogger().info( "Making SELECT query \"" + query + "\" which returned no results." );
			else
				Loader.getLogger().fine( "Making SELECT query \"" + query + "\" which returned no results." );
			return new LinkedHashMap<String, Object>();
		}
		
		LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
		try
		{
			result = convert( rs );
		}
		catch ( JSONException e )
		{
			e.printStackTrace();
		}
		
		if ( StringUtil.isTrue( options.get( "debug" ) ) )
			Loader.getLogger().info( "Making SELECT query \"" + query + "\" which returned " + getRowCount( rs ) + " row(s)." );
		else
			Loader.getLogger().fine( "Making SELECT query \"" + query + "\" which returned " + getRowCount( rs ) + " row(s)." );
		
		return result;
	}
	
	/**
	 * Checks Query String for Attempted SQL Injection by Checking for Certain Commands After the First 6 Characters.
	 * Warning: This Check Will Return True (or Positive) if You Check A Query That Inserts an Image.
	 */
	public boolean sqlInjectionDetection( String query )
	{
		query = query.toUpperCase();
		boolean safe = false;
		
		String[] unSafeWords = new String[] {"SELECT", "UPDATE", "DELETE", "INSERT", "UNION", "--"};
		
		String splice = query.substring( 0, 6 );
		
		for ( String word : unSafeWords )
		{
			if ( splice.equals( word ) )
			{
				safe = true;
			}
		}
		
		if ( !safe )
			return false;
		// GetFramework()->getServer()->Panic(400, "SQL Injection Detected! Notify administrators ASAP. Debug \"" .
		// $QueryString . "\".");
		
		splice = query.substring( 6 );
		for ( String word : unSafeWords )
		{
			if ( splice.contains( word ) )
			{
				safe = false;
			}
		}
		
		return safe;
		// GetFramework()->getServer()->Panic(400, "SQL Injection Detected! Notify administrators ASAP. Debug \"" .
		// $QueryString . "\".");
	}
	
	public boolean update( String table, Map<String, Object> data )
	{
		return update( table, data, "", 1, false );
	}
	
	public boolean update( String table, Map<String, Object> data, Object where )
	{
		return update( table, data, where, 1, false );
	}
	
	public boolean update( String table, Map<String, Object> data, Object where, int lmt )
	{
		return update( table, data, where, lmt, false );
	}
	
	@SuppressWarnings( "unchecked" )
	public boolean update( String table, Map<String, Object> data, Object where, int lmt, boolean disableInjectionCheck )
	{
		String subWhere = "";
		
		String whr = "";
		
		if ( where instanceof String )
		{
			whr = ( ( String ) where );
		}
		else if ( where instanceof Map )
		{
			Map<String, Object> whereMap = ( Map<String, Object> ) where;
			
			String tmp = "", opr = "";// , opr2 = "";
			
			for ( Entry<String, Object> entry : whereMap.entrySet() )
			{
				if ( entry.getValue() instanceof Map )
				{
					opr = "AND";
					if ( entry.getKey().indexOf( "|" ) >= 0 )
						opr = "OR";
					if ( entry.getKey().indexOf( "&" ) >= 0 )
						opr = "AND";
					
					String tmp2 = "";
					Map<String, Object> val = ( Map<String, Object> ) entry.getValue();
					
					for ( Entry<String, Object> entry2 : val.entrySet() )
					{
						// opr2 = "AND";
						if ( entry.getKey().indexOf( "|" ) >= 0 )
							opr = "OR";
						if ( entry.getKey().indexOf( "&" ) >= 0 )
							opr = "AND";
						
						String key = entry2.getKey().replace( "|", "" ).replace( "&", "" );
						tmp2 = "`" + key + "` = '" + entry2.getValue() + "'";
						tmp += ( tmp.isEmpty() ) ? tmp2 : subWhere + " " + opr + " " + tmp2;
					}
					
					whr = ( whr.isEmpty() ) ? "(" + tmp + ")" : whr + " " + opr + " (" + tmp + ")";
				}
				else
				{
					opr = "AND";
					if ( entry.getKey().indexOf( "|" ) >= 0 )
						opr = "OR";
					if ( entry.getKey().indexOf( "&" ) >= 0 )
						opr = "AND";
					
					String key = entry.getKey().replace( "|", "" ).replace( "&", "" );
					
					tmp = "`" + key + "` = '" + entry.getValue() + "'";
					whr = ( whr.isEmpty() ) ? tmp : whr + " " + opr + " " + tmp;
				}
			}
		}
		else
		{
			whr = "";
		}
		
		String limit = ( lmt > 0 ) ? " LIMIT " + lmt : "";
		where = ( whr.isEmpty() ) ? "" : " WHERE " + whr;
		
		String set = "";
		
		for ( Entry<String, Object> e : data.entrySet() )
			set += ", `" + e.getKey() + "` = '" + e.getValue() + "'";
		
		if ( set.length() > 2 )
			set = set.substring( 2 );
		
		String query = "UPDATE " + table + " SET " + set + where + limit + ";";
		
		if ( !disableInjectionCheck )
			sqlInjectionDetection( query );
		
		int result = 0;
		try
		{
			result = queryUpdate( query );
		}
		catch ( SQLException e )
		{
			e.printStackTrace();
		}
		
		if ( result > 0 )
		{
			Loader.getLogger().fine( "Making UPDATE query \"" + query + "\" which affected " + result + " rows." );
			return true;
		}
		else
		{
			Loader.getLogger().fine( "Making UPDATE query \"" + query + "\" which had no affect on the database." );
			return false;
		}
	}
	
	public boolean delete( String table, Map<String, Object> where )
	{
		return delete( table, where, 1 );
	}
	
	@SuppressWarnings( "unchecked" )
	public boolean delete( String table, Map<String, Object> where, int limit )
	{
		String whr = "";
		
		String tmp = "", opr = "";// , opr2 = "";
		
		for ( Entry<String, Object> entry : where.entrySet() )
		{
			if ( entry.getValue() instanceof Map )
			{
				opr = "AND";
				if ( entry.getKey().indexOf( "|" ) >= 0 )
					opr = "OR";
				if ( entry.getKey().indexOf( "&" ) >= 0 )
					opr = "AND";
				
				String tmp2 = "";
				Map<String, Object> val = ( Map<String, Object> ) entry.getValue();
				
				for ( Entry<String, Object> entry2 : val.entrySet() )
				{
					// opr2 = "AND";
					if ( entry.getKey().indexOf( "|" ) >= 0 )
						opr = "OR";
					if ( entry.getKey().indexOf( "&" ) >= 0 )
						opr = "AND";
					
					String key = entry2.getKey().replace( "|", "" ).replace( "&", "" );
					tmp2 = "`" + key + "` = '" + entry2.getValue() + "'";
					tmp += ( tmp.isEmpty() ) ? tmp2 : opr + " " + tmp2;
				}
				
				whr = ( whr.isEmpty() ) ? "(" + tmp + ")" : whr + " " + opr + " (" + tmp + ")";
			}
			else
			{
				opr = "AND";
				if ( entry.getKey().indexOf( "|" ) >= 0 )
					opr = "OR";
				if ( entry.getKey().indexOf( "&" ) >= 0 )
					opr = "AND";
				
				String key = entry.getKey().replace( "|", "" ).replace( "&", "" );
				
				tmp = "`" + key + "` = '" + entry.getValue() + "'";
				whr = ( whr.isEmpty() ) ? tmp : whr + " " + opr + " " + tmp;
			}
		}
		
		return delete( table, whr, limit );
	}
	
	public boolean delete( String table, String where )
	{
		return delete( table, where, 1 );
	}
	
	public boolean delete( String table, String where, int limit )
	{
		String lmt = "";
		if ( limit > 0 )
			lmt = " LIMIT " + limit;
		
		int i = 0;
		try
		{
			i = queryUpdate( "DELETE FROM `" + table + "` WHERE " + where + lmt + ";" );
		}
		catch ( SQLException e )
		{
			e.printStackTrace();
		}
		
		Loader.getLogger().fine( "Deleting from table " + table + " where " + where + " " + i );
		
		return true;
	}
	
	public boolean insert( String table, Map<String, Object> data )
	{
		return insert( table, data, false );
	}
	
	public boolean insert( String table, Map<String, Object> where, boolean disableInjectionCheck )
	{
		String keys = "";
		String values = "";
		
		for ( Entry<String, Object> e : where.entrySet() )
		{
			String key = escape( e.getKey() );
			
			String value;
			try
			{
				value = escape( ( String ) e.getValue() );
			}
			catch ( Exception ee )
			{
				value = ObjectUtil.castToString( e.getValue() );
			}
			
			if ( keys.isEmpty() )
			{
				keys = "`" + key + "`";
			}
			else
			{
				keys += ", `" + key + "`";
			}
			
			if ( values.isEmpty() )
			{
				values = "'" + value + "'";
			}
			else
			{
				values += ", '" + value + "'";
			}
		}
		
		String query = "INSERT INTO " + table + " (" + keys + ")VALUES(" + values + ");";
		
		if ( !disableInjectionCheck && query.length() < 255 )
			sqlInjectionDetection( query );
		
		int result = 0;
		try
		{
			result = queryUpdate( query );
		}
		catch ( SQLException e )
		{
			e.printStackTrace();
		}
		
		if ( result > 0 )
		{
			Loader.getLogger().fine( "Making INSERT query \"" + query + "\" which affected " + result + " rows." );
			return true;
		}
		else
		{
			Loader.getLogger().fine( "Making INSERT query \"" + query + "\" which had no affect on the database" );
			return false;
		}
	}
	
	public static String escape( String str )
	{
		if ( str == null )
		{
			return null;
		}
		
		if ( str.replaceAll( "[a-zA-Z0-9_!@#$%^&*()-=+~.;:,\\Q[\\E\\Q]\\E<>{}\\/? ]", "" ).length() < 1 )
		{
			return str;
		}
		
		String cleanString = str;
		cleanString = cleanString.replaceAll( "\\\\", "\\\\\\\\" );
		cleanString = cleanString.replaceAll( "\\n", "\\\\n" );
		cleanString = cleanString.replaceAll( "\\r", "\\\\r" );
		cleanString = cleanString.replaceAll( "\\t", "\\\\t" );
		cleanString = cleanString.replaceAll( "\\00", "\\\\0" );
		cleanString = cleanString.replaceAll( "'", "\\\\'" );
		cleanString = cleanString.replaceAll( "\\\"", "\\\\\"" );
		
		return cleanString;
	}
	
	public boolean tableExist( String table )
	{
		try
		{
			DatabaseMetaData md = con.getMetaData();
			ResultSet rs = md.getTables( null, null, "%", null );
			while ( rs.next() )
			{
				if ( rs.getString( 3 ).equalsIgnoreCase( table ) )
					return true;
			}
		}
		catch ( CommunicationsException e )
		{
			// Retry
			return tableExist( table );
		}
		catch ( SQLException e )
		{
			e.printStackTrace();
		}
		return false;
	}
	
	public static ConsoleLogger getLogger()
	{
		return Loader.getLogger( "DBEngine" );
	}
	
	public DBType getType()
	{
		return type;
	}
}
