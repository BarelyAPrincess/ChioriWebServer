/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
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
import java.sql.Types;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.MissingFormatArgumentException;
import java.util.UnknownFormatConversionException;

import org.json.JSONException;

import com.chiorichan.ConsoleColor;
import com.chiorichan.ConsoleLogger;
import com.chiorichan.Loader;
import com.chiorichan.lang.StartupException;
import com.chiorichan.util.ObjectFunc;
import com.chiorichan.util.StringFunc;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mysql.jdbc.exceptions.jdbc4.CommunicationsException;
import com.mysql.jdbc.exceptions.jdbc4.MySQLNonTransientConnectionException;

/**
 * Gives easy access to the SQL Database within Groovy scripts.
 */
public class DatabaseEngine
{
	private enum DatabaseType
	{
		SQLITE, H2, MYSQL, UNKNOWN;
	}
	
	@SuppressWarnings( "serial" )
	private static class FoundException extends Exception
	{
		int matchingType = 0;
		
		FoundException( int matchingType )
		{
			this.matchingType = matchingType;
		}
		
		int getType()
		{
			return matchingType;
		}
	}
	
	/**
	 * Provides an easy way to catch a matching type without tons of if...then statements
	 */
	private static class TypeCatcher
	{
		Class<?> origType;
		
		public TypeCatcher( Class<?> origType )
		{
			this.origType = origType;
		}
		
		public void check( Class<?> clz, int matchingType ) throws FoundException
		{
			if ( origType == clz )
				throw new FoundException( matchingType );
		}
	}
	
	private boolean debug = false;
	
	private Connection con;
	private String savedDb, savedUser, savedPass, savedHost, savedPort;
	private DatabaseType type = DatabaseType.UNKNOWN;
	
	public DatabaseEngine()
	{
		
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
	
	public static Map<String, Object> convertRow( ResultSet rs ) throws SQLException
	{
		Map<String, Object> result = Maps.newLinkedHashMap();
		ResultSetMetaData rsmd = rs.getMetaData();
		
		int numColumns = rsmd.getColumnCount();
		
		for ( int i = 1; i < numColumns + 1; i++ )
		{
			String columnName = rsmd.getColumnName( i );
			
			// Loader.getLogger().info( "Column: " + columnName + " <-> " + rsmd.getColumnTypeName( i ) );
			
			if ( rsmd.getColumnType( i ) == Types.ARRAY )
				result.put( columnName, rs.getArray( columnName ).getArray() );
			else if ( rsmd.getColumnType( i ) == Types.BIGINT )
				result.put( columnName, rs.getInt( columnName ) );
			else if ( rsmd.getColumnType( i ) == Types.TINYINT )
				result.put( columnName, rs.getInt( columnName ) );
			else if ( rsmd.getColumnType( i ) == Types.BIT )
				result.put( columnName, rs.getInt( columnName ) );
			else if ( rsmd.getColumnType( i ) == java.sql.Types.BOOLEAN )
				result.put( columnName, rs.getBoolean( columnName ) );
			else if ( rsmd.getColumnTypeName( i ).contains( "BLOB" ) || rsmd.getColumnType( i ) == Types.BINARY )
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
			else if ( rsmd.getColumnType( i ) == Types.DOUBLE )
				result.put( columnName, rs.getDouble( columnName ) );
			else if ( rsmd.getColumnType( i ) == Types.FLOAT )
				result.put( columnName, rs.getFloat( columnName ) );
			else if ( rsmd.getColumnTypeName( i ).equals( "INT" ) )
				result.put( columnName, rs.getInt( columnName ) );
			else if ( rsmd.getColumnType( i ) == Types.NVARCHAR )
				result.put( columnName, rs.getNString( columnName ) );
			else if ( rsmd.getColumnTypeName( i ).equals( "VARCHAR" ) )
				result.put( columnName, rs.getString( columnName ) );
			else if ( rsmd.getColumnType( i ) == Types.SMALLINT )
				result.put( columnName, rs.getInt( columnName ) );
			else if ( rsmd.getColumnType( i ) == Types.DATE )
				result.put( columnName, rs.getDate( columnName ) );
			else if ( rsmd.getColumnType( i ) == Types.TIMESTAMP )
				result.put( columnName, rs.getTimestamp( columnName ) );
			else
				result.put( columnName, rs.getObject( columnName ) );
		}
		
		return result;
	}
	
	public static String escape( String str )
	{
		if ( str == null )
			return null;
		
		if ( str.replaceAll( "[a-zA-Z0-9_!@#$%^&*()-=+~.;:,\\Q[\\E\\Q]\\E<>{}\\/? ]", "" ).length() < 1 )
			return str;
		
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
	
	public static int getColumnType( Class<?> clz )
	{
		TypeCatcher tc = new TypeCatcher( clz );
		
		try
		{
			tc.check( Integer.class, Types.INTEGER );
			tc.check( int.class, Types.INTEGER );
			tc.check( Boolean.class, Types.TINYINT );
			tc.check( boolean.class, Types.TINYINT );
			tc.check( float.class, Types.FLOAT );
			tc.check( Float.class, Types.FLOAT );
			tc.check( double.class, Types.DOUBLE );
			tc.check( Double.class, Types.DOUBLE );
			tc.check( String.class, Types.VARCHAR );
		}
		catch ( FoundException e )
		{
			return e.getType();
		}
		
		return Types.NULL;
	}
	
	public static ConsoleLogger getLogger()
	{
		return Loader.getLogger( "DBEngine" );
	}
	
	public static Map<String, String> toStringsMap( ResultSet rs ) throws SQLException
	{
		Map<String, Object> source = convertRow( rs );
		Map<String, String> result = Maps.newLinkedHashMap();
		
		for ( Entry<String, Object> e : source.entrySet() )
		{
			String val = ObjectFunc.castToString( e.getValue() );
			if ( val != null )
				result.put( e.getKey(), val );
		}
		
		return result;
	}
	
	public void addColumn( String table, String columnName, Class<?> clz ) throws SQLException
	{
		addColumn( table, columnName, clz, -1L );
	}
	
	public void addColumn( String table, String columnName, Class<?> clz, long maxLenReq ) throws SQLException
	{
		String type = "VARCHAR(255)";
		
		if ( maxLenReq < 1 )
			maxLenReq = 255;
		
		if ( clz == String.class )
			if ( maxLenReq < 256 )
				type = "VARCHAR(255)";
			else
				type = "TEXT";
		
		if ( clz == Integer.class || clz == int.class || clz == Long.class || clz == long.class )
		{
			if ( maxLenReq < 256 )
				type = "TINYINT(" + maxLenReq + ")";
			if ( maxLenReq > 255 && maxLenReq < 65536 )
				type = "SMALLINT(" + maxLenReq + ")";
			if ( maxLenReq > 65535 && maxLenReq < 16777216 )
				type = "MEDIUMINT(" + maxLenReq + ")";
			if ( maxLenReq > 16777215 && maxLenReq < 4294967296L )
				type = "INT(" + maxLenReq + ")";
			if ( maxLenReq > 4294967295L )
				type = "BIGINT(" + maxLenReq + ")";
		}
		
		if ( clz == Boolean.class || clz == boolean.class )
			type = "TINYINT(1)";
		if ( clz == Float.class || clz == float.class )
			type = "FLOAT(" + maxLenReq + ",2)";
		if ( clz == Double.class || clz == double.class )
			type = "DOUBLE(" + maxLenReq + ",2)";
		
		// Loader.getLogger().debug( "Query: " + "ALTER TABLE `" + table + "` ADD `" + columnName + "` " + type + ";" );
		queryUpdate( "ALTER TABLE `" + table + "` ADD `" + columnName + "` " + type + ";" );
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
		
		log( "Deleting from table " + table + " where " + where + " " + i );
		
		return true;
	}
	
	public boolean deleteAll( String table, Map<String, Object> where )
	{
		return delete( table, where, -1 );
	}
	
	public boolean deleteAll( String table, String where )
	{
		return delete( table, where, -1 );
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
	
	public List<String> getTableColumnNames( String table ) throws SQLException
	{
		List<String> rtn = Lists.newArrayList();
		
		ResultSet rs = query( "SELECT * FROM `" + table + "` LIMIT 1" );
		
		ResultSetMetaData rsmd = rs.getMetaData();
		
		int numColumns = rsmd.getColumnCount();
		
		for ( int i = 1; i < numColumns + 1; i++ )
			rtn.add( rsmd.getColumnName( i ) );
		
		return rtn;
	}
	
	public SqlTableColumns getTableColumns( String table ) throws SQLException
	{
		SqlTableColumns rtn = new SqlTableColumns();
		
		ResultSet rs = query( "SELECT * FROM `" + table + "` LIMIT 1;" );
		
		ResultSetMetaData rsmd = rs.getMetaData();
		
		for ( int i = 1; i < rsmd.getColumnCount() + 1; i++ )
			rtn.add( rsmd, i );
		
		return rtn;
	}
	
	public List<String> getTableColumnTypes( String table ) throws SQLException
	{
		List<String> rtn = Lists.newArrayList();
		
		ResultSet rs = query( "SELECT * FROM " + table );
		
		ResultSetMetaData rsmd = rs.getMetaData();
		
		int numColumns = rsmd.getColumnCount();
		
		for ( int i = 1; i < numColumns + 1; i++ )
			rtn.add( rsmd.getColumnTypeName( i ) );
		
		return rtn;
	}
	
	public ResultSetMetaData getTableMetaData( String table ) throws SQLException
	{
		ResultSet rs = query( "SELECT * FROM " + table );
		return rs.getMetaData();
	}
	
	public DatabaseType getType()
	{
		return type;
	}
	
	/**
	 * Initializes a sqLite connection.
	 * 
	 * @param filename
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	public void initH2( String filename ) throws SQLException
	{
		try
		{
			Class.forName( "org.h2.Driver" );
		}
		catch ( ClassNotFoundException e )
		{
			throw new StartupException( "We could not locate the 'org.h2.Driver' library, be sure to have this library in your build path." );
		}
		
		File sqliteDb = new File( filename );
		
		con = DriverManager.getConnection( "jdbc:h2:" + sqliteDb.getAbsolutePath() );
		con.setAutoCommit( false );
		
		getLogger().info( "We succesully connected to the H2 database using 'jdbc:h2:" + sqliteDb.getAbsolutePath() + "'" );
		type = DatabaseType.H2;
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
	public void initMySql( String db, String user, String pass, String host, String port ) throws SQLException
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
			con.setAutoCommit( false );
		}
		catch ( SQLException e )
		{
			throw e;
		}
		
		if ( con != null && !con.isClosed() )
			Loader.getLogger().info( "We succesully connected to the sql database using 'jdbc:mysql://" + host + ":" + port + "/" + db + "'." );
		else
			Loader.getLogger().warning( "There was a problem connecting to the sql database using 'jdbc:mysql://" + host + ":" + port + "/" + db + "'." );
		
		type = DatabaseType.MYSQL;
	}
	
	/**
	 * Initializes a sqLite connection.
	 * 
	 * @param filename
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	public void initSQLite( String filename ) throws SQLException
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
		con.setAutoCommit( false );
		
		getLogger().info( "We succesully connected to the sqLite database using 'jdbc:sqlite:" + sqliteDb.getAbsolutePath() + "'" );
		type = DatabaseType.SQLITE;
	}
	
	public boolean insert( String table, Map<String, Object> data ) throws SQLException
	{
		return insert( table, data, false );
	}
	
	public boolean insert( String table, Map<String, Object> where, boolean disableInjectionCheck ) throws SQLException
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
				value = ObjectFunc.castToString( e.getValue() );
			}
			
			if ( keys.isEmpty() )
				keys = "`" + key + "`";
			else
				keys += ", `" + key + "`";
			
			if ( values.isEmpty() )
				values = "'" + value + "'";
			else
				values += ", '" + value + "'";
		}
		
		String query = "INSERT INTO " + table + " (" + keys + ")VALUES(" + values + ");";
		
		if ( !disableInjectionCheck && query.length() < 255 )
			sqlInjectionDetection( query );
		
		int result = queryUpdate( query );
		
		if ( result > 0 )
		{
			log( "Making INSERT query \"" + query + "\" which affected " + result + " rows." );
			return true;
		}
		else
		{
			log( "Making INSERT query \"" + query + "\" which had no effect on the database" );
			return false;
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
	
	public Boolean isNull( Object o )
	{
		if ( o == null )
			return true;
		
		return false;
	}
	
	private void log( boolean force, String msg, Object... objs )
	{
		try
		{
			msg = String.format( msg, objs );
		}
		catch ( UnknownFormatConversionException | MissingFormatArgumentException e )
		{
			getLogger().warning( "Following log entry throw an exception: " + e.getMessage() );
		}
		
		if ( debug || force )
			getLogger().info( ConsoleColor.GRAY + msg );
		else
			getLogger().fine( msg );
	}
	
	private void log( String msg, Object... objs )
	{
		log( false, msg, objs );
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
				if ( type == DatabaseType.SQLITE )
					stmt = con.createStatement( ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY );
				else
					stmt = con.createStatement( ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE );
			}
			catch ( CommunicationsException e )
			{
				if ( reconnect() )
					if ( type == DatabaseType.SQLITE )
						stmt = con.createStatement( ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY );
					else
						stmt = con.createStatement( ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE );
			}
			finally
			{
				if ( stmt == null )
					stmt = con.createStatement();
			}
			
			result = stmt.executeQuery( query );
			
			log( "SQL Query `" + query + "` returned " + getRowCount( result ) + " rows!" );
		}
		catch ( CommunicationsException | MySQLNonTransientConnectionException e )
		{
			if ( !retried && reconnect() )
				return query( query, true );
			else
				throw e;
		}
		catch ( Throwable t )
		{
			getLogger().warning( "SQL Exception: " + t.getMessage() );
			throw t;
		}
		
		return result;
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
					stmt.setObject( x, s );
				}
				catch ( SQLException e )
				{
					if ( !e.getMessage().startsWith( "Parameter index out of range" ) )
						throw e;
				}
			
			result = stmt.executeQuery();
			
			log( "SQL Query `" + stmt.toString() + "` returned " + getRowCount( result ) + " rows!" );
		}
		catch ( CommunicationsException | MySQLNonTransientConnectionException e )
		{
			if ( !retried && reconnect() )
				return query( query, true, args );
			else
				throw e;
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
		
		log( "Update Query: \"" + StringFunc.limitLength( query, 255 ) + "\" which affected " + cnt + " row(s)." );
		return cnt;
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
					stmt.setObject( x, s );
				}
				catch ( SQLException e )
				{
					if ( !e.getMessage().startsWith( "Parameter index out of range" ) )
						throw e;
				}
			
			stmt.execute();
			
			log( "Update Query: \"" + stmt.toString() + "\" which affected " + stmt.getUpdateCount() + " row(s)." );
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
			whr = ( ( String ) where );
		else if ( where instanceof Map )
		{
			Map<String, Object> whereMap = ( Map<String, Object> ) where;
			
			String tmp = "", opr = "";// , opr2 = "";
			
			for ( Entry<String, Object> entry : whereMap.entrySet() )
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
		else
			whr = "";
		
		Map<String, String> options = new LinkedHashMap<String, String>();
		
		if ( options0 != null )
			for ( Entry<String, Object> o : options0.entrySet() )
				options.put( o.getKey().toLowerCase(), ObjectFunc.castToString( o.getValue() ) );
		
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
		String orderby = ( options.get( "orderby" ) ) == "" ? "" : " ORDER BY " + ( options.get( "orderby" ) );
		String groupby = ( options.get( "groupby" ) ) == "" ? "" : " GROUP BY " + ( options.get( "groupby" ) );
		
		where = ( whr.isEmpty() ) ? "" : " WHERE " + whr;
		
		String query = "SELECT " + ( options.get( "fields" ) ) + " FROM `" + table + "`" + where + groupby + orderby + limit + ";";
		
		// TODO: Act on result!
		sqlInjectionDetection( query );
		
		ResultSet rs = query( query );
		
		if ( rs == null )
		{
			log( StringFunc.isTrue( options.get( "debug" ) ), "Making SELECT query \"" + query + "\" which returned an error." );
			return null;
		}
		
		if ( getRowCount( rs ) < 1 )
		{
			log( StringFunc.isTrue( options.get( "debug" ) ), "Making SELECT query \"" + query + "\" which returned no results." );
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
		
		if ( StringFunc.isTrue( options.get( "debug" ) ) )
			Loader.getLogger().info( "Making SELECT query \"" + query + "\" which returned " + getRowCount( rs ) + " row(s)." );
		else
			log( "Making SELECT query \"" + query + "\" which returned " + getRowCount( rs ) + " row(s)." );
		
		return result;
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
			try
			{
				ResultSetMetaData rsmd = rs.getMetaData();
				int columnCount = rsmd.getColumnCount();
				
				do
					for ( int i = 1; i < columnCount + 1; i++ )
						result.put( rsmd.getColumnName( i ), rs.getObject( i ) );
				while ( rs.next() );
				
				return result;
			}
			catch ( Exception e )
			{
				e.printStackTrace();
			}
		
		return null;
	}
	
	@SuppressWarnings( "unchecked" )
	public LinkedHashMap<String, Object> selectOne( String table, Object where ) throws SQLException
	{
		LinkedHashMap<String, Object> result = select( table, where );
		
		if ( result == null || result.size() < 1 )
			return null;
		
		return ( LinkedHashMap<String, Object> ) result.get( "0" );
	}
	
	public LinkedHashMap<String, Object> selectOne( String table, String key, String val ) throws SQLException
	{
		return selectOne( table, Arrays.asList( key ), Arrays.asList( val ) );
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
			if ( splice.equals( word ) )
				safe = true;
		
		if ( !safe )
		{
			Loader.getLogger().warning( "SQL Injection was detected for query '" + query + "', if this is a false positive then it might need reporting to the developers." );
			return false;
		}
		
		splice = query.substring( 6 );
		for ( String word : unSafeWords )
			if ( splice.contains( word ) )
				safe = false;
		
		return safe;
	}
	
	public boolean tableExist( String table )
	{
		try
		{
			DatabaseMetaData md = con.getMetaData();
			ResultSet rs = md.getTables( null, null, "%", null );
			while ( rs.next() )
				if ( rs.getString( 3 ).equalsIgnoreCase( table ) )
					return true;
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
	
	public boolean update( String table, Map<String, Object> data ) throws SQLException
	{
		return update( table, data, "", 1, false );
	}
	
	public boolean update( String table, Map<String, Object> data, Object where ) throws SQLException
	{
		return update( table, data, where, 1, false );
	}
	
	public boolean update( String table, Map<String, Object> data, Object where, int lmt ) throws SQLException
	{
		return update( table, data, where, lmt, false );
	}
	
	@SuppressWarnings( "unchecked" )
	public boolean update( String table, Map<String, Object> data, Object where, int lmt, boolean disableInjectionCheck ) throws SQLException
	{
		String subWhere = "";
		String whr = "";
		
		if ( where instanceof String )
			whr = ( ( String ) where );
		else if ( where instanceof Map )
		{
			Map<String, Object> whereMap = ( Map<String, Object> ) where;
			
			String tmp = "", opr = "";
			
			for ( Entry<String, Object> entry : whereMap.entrySet() )
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
						tmp += ( tmp.isEmpty() ) ? tmp2 : String.format( "%s %s %s", subWhere, opr, tmp2 );
					}
					
					whr = ( whr.isEmpty() ) ? String.format( "(%s)", tmp ) : String.format( "%s %s (%s)", whr, opr, tmp );
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
					whr = ( whr.isEmpty() ) ? tmp : String.format( "%s %s %s", whr, opr, tmp );
				}
		}
		else
			whr = "";
		
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
		
		int result = queryUpdate( query );
		
		if ( result > 0 )
		{
			log( "Making UPDATE query \"" + query + "\" which affected " + result + " rows." );
			return true;
		}
		else
		{
			log( "Making UPDATE query \"" + query + "\" which had no affect on the database." );
			return false;
		}
	}
}
