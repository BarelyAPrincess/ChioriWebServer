package com.chiorichan.framework;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONException;

import vnet.java.util.MySQLUtils;

import com.chiorichan.Loader;
import com.chiorichan.database.SqlConnector;
import com.chiorichan.util.ObjectUtil;

public class FrameworkDatabaseEngine
{
	protected Framework fw;
	
	public FrameworkDatabaseEngine(Framework fw0)
	{
		fw = fw0;
	}
	
	public LinkedHashMap<String, Object> selectOne( String table, Object where ) throws SQLException
	{
		LinkedHashMap<String, Object> result = select( table, where );
		
		if ( result == null || result.size() < 1 )
			return null;
		
		return (LinkedHashMap<String, Object>) result.get( "0" );
	}
	
	public LinkedHashMap<String, Object> select( String table ) throws SQLException
	{
		return select( table, null, null );
	}
	
	public LinkedHashMap<String, Object> select( String table, Object where ) throws SQLException
	{
		return select( table, where, null );
	}
	
	public LinkedHashMap<String, Object> select( String table, Object where, Map<String, Object> options0 ) throws SQLException
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
		else if ( where instanceof Map )
		{
			Map<String, Object> whereMap = (Map<String, Object>) where;
			
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
		
		options = (Map<String, String>) ( ( options0 == null ) ? new LinkedHashMap<String, String>() : options0 );
		
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
		
		// TODO: Act on result!
		SQLInjectionDetection( query );
		
		ResultSet rs = sql.query( query );
		
		if ( rs == null )
		{
			Loader.getLogger().warning( "Making SELECT query \"" + query + "\" which returned an error." );
			return null;
		}
		
		if ( sql.getRowCount( rs ) < 1 )
		{
			Loader.getLogger().warning( "Making SELECT query \"" + query + "\" which returned no results." );
			return new LinkedHashMap<String, Object>();
		}
		
		LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
		try
		{
			result = SqlConnector.convert( rs );
		}
		catch ( JSONException e )
		{
			e.printStackTrace();
		}
		
		Loader.getLogger().info( "Making SELECT query \"" + query + "\" which returned " + sql.getRowCount( rs ) + " row(s)." );
		
		return result;
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
	
	public boolean update( String table, Map<String, Object> data, Object where, int lmt, boolean disableInjectionCheck )
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
		else if ( where instanceof Map )
		{
			Map<String, Object> whereMap = (Map<String, Object>) where;
			
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
		
		for ( Entry<String, Object> e : data.entrySet() )
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
	
	public boolean delete( String table, Map<String, Object> where )
	{
		return delete( table, where, 1 );
	}
	
	public boolean delete( String table, Map<String, Object> where, int limit )
	{
		String whr = "";
		
		String tmp = "", opr = "", opr2 = "";
		
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
	
	public boolean insert( String table, Map<String, Object> data )
	{
		return insert( table, data, false );
	}
	
	public boolean insert( String table, Map<String, Object> where, boolean disableInjectionCheck )
	{
		SqlConnector sql = fw.getCurrentSite().sql;
		
		String keys = "";
		String values = "";
		
		for ( Entry<String, Object> e : where.entrySet() )
		{
			String key = escape( e.getKey() );
			
			String value;
			try
			{
				value = escape( (String) e.getValue() );
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
		return MySQLUtils.escape( str );
	}
}
