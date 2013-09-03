package com.chiorichan.framework;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.json.JSONException;
import org.json.JSONObject;

import vnet.java.util.MySQLUtils;

import com.caucho.quercus.env.ArrayValueImpl;
import com.chiorichan.Loader;
import com.chiorichan.database.SqlConnector;
import com.google.gson.Gson;

public class FrameworkDatabaseEngine
{
	protected Framework fw;
	
	public FrameworkDatabaseEngine(Framework fw0)
	{
		fw = fw0;
	}
	
	public Map<String, Object> selectOne( String table, Object where ) throws SQLException
	{
		Map<String, Object> result = select( table, where );
		
		if ( result == null || result.size() < 1 )
			return null;
		
		Object o = result.values().toArray()[0];
		
		if ( o instanceof Map )
			return (Map<String, Object>) o;
		
		return null;
	}
	
	public Map<String, Object> select( String table ) throws SQLException
	{
		return select( table, null, null );
	}
	
	public Map<String, Object> select( String table, Object where ) throws SQLException
	{
		return select( table, where, null );
	}
	
	public Map<String, Object> select( String table, Object where, ArrayValueImpl options0 ) throws SQLException
	{
		String subWhere = "";
		SqlConnector sql = fw.getCurrentSite().sql;
		
		if ( sql == null )
			return null;
		
		String whr = "";
		
		if ( where instanceof String )
		{
			whr = ( (String) where );
		}
		else if ( where instanceof ArrayValueImpl )
		{
			Map<String, Object> whereMap = ( (ArrayValueImpl) where ).toJavaMap( fw.getEnv(), HashMap.class );
			
			String tmp = "", opr = "", opr2 = "";
			
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
					Map<String, Object> val = (Map<String, Object>) entry.getValue();
					
					for ( Entry<String, Object> entry2 : val.entrySet() )
					{
						opr2 = "AND";
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
		
		Map<String, String> options;
		
		if ( options0 == null || !( options0 instanceof ArrayValueImpl ) )
			options = new HashMap<String, String>();
		else
			options = ( (ArrayValueImpl) options0 ).toJavaMap( fw.getEnv(), HashMap.class );
		
		if ( !options.containsKey( "limit" ) || !( options.get( "limit" ) instanceof String ) )
			options.put( "limit", "0" );
		if ( !options.containsKey( "offSet" ) || !( options.get( "offSet" ) instanceof String ) )
			options.put( "offSet", "0" );
		if ( !options.containsKey( "orderBy" ) )
			options.put( "orderBy", "" );
		if ( !options.containsKey( "groupBy" ) )
			options.put( "groupBy", "" );
		if ( !options.containsKey( "fields" ) )
			options.put( "fields", "*" );
		
		String limit = ( Integer.parseInt( options.get( "limit" ) ) > 0 ) ? " LIMIT " + Integer.parseInt( options.get( "offSet" ) ) + ", " + Integer.parseInt( options.get( "limit" ) ) : "";
		String orderby = ( (String) options.get( "orderBy" ) ) == "" ? "" : " ORDER BY " + ( (String) options.get( "orderBy" ) );
		String groupby = ( (String) options.get( "groupBy" ) ) == "" ? "" : " GROUP BY " + ( (String) options.get( "groupBy" ) );
		
		where = ( whr.isEmpty() ) ? "" : " WHERE " + whr;
		
		String query = "SELECT " + ( (String) options.get( "fields" ) ) + " FROM `" + table + "`" + where + groupby + orderby + limit + ";";
		
		// TODO: SQL Injection Detection Port to Java
		// $this->SQLInjectionDetection($query);
		
		ResultSet rs = sql.query( query );
		
		if ( rs == null )
		{
			Loader.getLogger().warning( "Making SELECT query \"" + query + "\" which returned an error." );
			return null;
		}
		
		if ( sql.getRowCount( rs ) < 1 )
		{
			Loader.getLogger().warning( "Making SELECT query \"" + query + "\" which returned no results." );
			return new HashMap<String, Object>();
		}
		
		JSONObject json;
		try
		{
			json = SqlConnector.convert( rs );
		}
		catch ( SQLException | JSONException e )
		{
			e.printStackTrace();
			return null;
		}
		
		/*
		 * do { Map<String, Object> row = new HashMap<String, Object>();
		 * 
		 * rs.
		 * 
		 * result.add( row ); } while ( rs.next() );
		 * 
		 * $result = array(); foreach($out as $row) { $result[] = $row; }
		 */
		
		Loader.getLogger().fine( "Making SELECT query \"" + query + "\" which returned " + sql.getRowCount( rs ) + " row(s)." );
		
		return new Gson().fromJson( json.toString(), TreeMap.class );
	}
	
	@Deprecated
	public String array2Where( List<String> where )
	{
		return array2Where( where, "AND", null );
	}
	
	@Deprecated
	public String array2Where( List<String> where, String limiter )
	{
		return array2Where( where, limiter, null );
	}
	
	@Deprecated
	public String array2Where( List<String> where, String limiter, String prepend )
	{
		if ( prepend == null )
			prepend = "";
		
		if ( limiter == null || limiter.isEmpty() )
			limiter = "AND";
		
		StringBuilder sb = new StringBuilder();
		
		for ( String s : where )
		{
			sb.append( " " + limiter + " " + s );
		}
		
		return prepend + sb.toString().substring( limiter.length() + 2 );
	}
	
	/**
	 * Checks Query String for Attempted SQL Injection by Checking for Certain Commands After the First 6 Characters.
	 * Warning: This Check Will Return True (or Positive) if You Check A Query That Inserts an Image.
	 */
	public boolean SQLInjectionDetection( String query )
	{
		query = query.toUpperCase();
		boolean safe = false;
		
		String[] unSafeWords = new String[] { "SELECT", "UPDATE", "DELETE", "INSERT", "UNION", "--" };
		
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
	
	public boolean update( String table, ArrayValueImpl data )
	{
		return update( table, data, "", 1, false );
	}
	
	public boolean update( String table, ArrayValueImpl data, Object where )
	{
		return update( table, data, where, 1, false );
	}
	
	public boolean update( String table, ArrayValueImpl data, Object where, int lmt )
	{
		return update( table, data, where, lmt, false );
	}
	
	public boolean update( String table, ArrayValueImpl data, Object where, int lmt, boolean disableInjectionCheck )
	{
		String subWhere = "";
		SqlConnector sql = fw.getCurrentSite().sql;
		
		if ( sql == null )
			return false;
		
		String whr = "";
		
		if ( where instanceof String )
		{
			whr = ( (String) where );
		}
		else if ( where instanceof ArrayValueImpl )
		{
			Map<String, Object> whereMap = ( (ArrayValueImpl) where ).toJavaMap( fw.getEnv(), HashMap.class );
			
			String tmp = "", opr = "", opr2 = "";
			
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
					Map<String, Object> val = (Map<String, Object>) entry.getValue();
					
					for ( Entry<String, Object> entry2 : val.entrySet() )
					{
						opr2 = "AND";
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
		
		Map<String, Object> dada = data.toJavaMap( fw.getEnv(), Map.class );
		
		for ( Entry e : dada.entrySet() )
			set += ", `" + e.getKey() + "` = '" + e.getValue() + "'";
		
		if ( set.length() > 2 )
			set = set.substring( 2 );
		
		String query = "UPDATE " + table + " SET " + set + where + limit + ";";
		
		if ( !disableInjectionCheck )
			SQLInjectionDetection( query );
		
		int result = sql.queryUpdate( query );
		
		if ( result > 0 )
		{
			Loader.getLogger().info( "Making UPDATE query \"" + query + "\" which affected " + result + " rows." );
			return true;
		}
		else
		{
			Loader.getLogger().info( "Making UPDATE query \"" + query + "\" which had no affect on the database." );
			return false;
		}
	}
	
	public boolean delete( String table, ArrayValueImpl where )
	{
		return delete( table, where, 1 );
	}
	
	public boolean delete( String table, ArrayValueImpl where, int limit )
	{
		String whr = "";
		Map<String, Object> whereMap = where.toJavaMap( fw.getEnv(), HashMap.class );
		
		String tmp = "", opr = "", opr2 = "";
		
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
				Map<String, Object> val = (Map<String, Object>) entry.getValue();
				
				for ( Entry<String, Object> entry2 : val.entrySet() )
				{
					opr2 = "AND";
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
		SqlConnector sql = fw.getCurrentSite().sql;
		
		String lmt = "";
		if ( limit > 0 )
			lmt = " LIMIT 1";
		
		int i = sql.queryUpdate( "DELETE FROM `" + table + "` WHERE " + where + lmt + ";" );
		
		Loader.getLogger().info( "Deleting from table " + table + " where " + where + " " + i );
		
		return true;
	}
	
	public boolean insert( String table, ArrayValueImpl data )
	{
		return insert( table, data, false );
	}
	
	public boolean insert( String table, ArrayValueImpl data, boolean disableInjectionCheck )
	{
		SqlConnector sql = fw.getCurrentSite().sql;
		
		Map<String, String> whereMap = data.toJavaMap( fw.getEnv(), HashMap.class );
		
		String keys = "";
		String values = "";
		
		for ( Entry<String, String> e : whereMap.entrySet() )
		{
			String key = MySQLUtils.escape( e.getKey() );
			String value = MySQLUtils.escape( e.getValue() );
			
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
			SQLInjectionDetection( query );
		
		int result = sql.queryUpdate( query );
		
		if ( result > 0 )
		{
			Loader.getLogger().info( "Making INSERT query \"" + query + "\" which affected " + result + " rows." );
			return true;
		}
		else
		{
			Loader.getLogger().info( "Making INSERT query \"" + query + "\" which had no affect on the database" );
			return false;
		}
	}
	
	public String escape( String str )
	{
		return str;
	}
}
