package com.chiorichan.framework;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
	
	public Map<String, Object> selectOne( String table, Object where )
	{
		Map<String, Object> result = select( table, where );
		
		if ( result == null || result.size() < 1 )
			return null;
		
		Object o = result.values().toArray()[0];
		
		if ( o instanceof Map )
			return (Map<String, Object>) o;
		
		return null;
	}
	
	public Map<String, Object> select( String table, Object where )
	{
		return select( table, where, null );
	}
	
	public Map<String, Object> select( String table, Object where, ArrayValueImpl options0 )
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
			return null;
		}
		
		Map<String, Object> options;
		
		if ( options0 == null || !( options0 instanceof ArrayValueImpl ) )
			options = new HashMap<String, Object>();
		else
			options = ( (ArrayValueImpl) options0 ).toJavaMap( fw.getEnv(), HashMap.class );
		
		if ( !options.containsKey( "limit" ) )
			options.put( "limit", 0 );
		if ( !options.containsKey( "offSet" ) )
			options.put( "offSet", 0 );
		if ( !options.containsKey( "orderBy" ) )
			options.put( "orderBy", "" );
		if ( !options.containsKey( "groupBy" ) )
			options.put( "groupBy", "" );
		if ( !options.containsKey( "fields" ) )
			options.put( "fields", "*" );
		
		String limit = ( (int) options.get( "limit" ) > 0 ) ? " LIMIT " + ( (int) options.get( "offSet" ) ) + ", " + ( (int) options.get( "limit" ) ) : "";
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
		
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		
		JSONObject json;
		try
		{
			json = convert( rs );
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
		
		return new Gson().fromJson( json.toString(), Map.class );
	}
	
	public static JSONObject convert( ResultSet rs ) throws SQLException, JSONException
	{
		JSONObject json = new JSONObject();
		ResultSetMetaData rsmd = rs.getMetaData();
		int x = 0;
		
		do
		{
			int numColumns = rsmd.getColumnCount();
			JSONObject obj = new JSONObject();
			
			for ( int i = 1; i < numColumns + 1; i++ )
			{
				String column_name = rsmd.getColumnName( i );
				
				if ( rsmd.getColumnType( i ) == java.sql.Types.ARRAY )
				{
					obj.put( column_name, rs.getArray( column_name ) );
				}
				else if ( rsmd.getColumnType( i ) == java.sql.Types.BIGINT )
				{
					obj.put( column_name, rs.getInt( column_name ) );
				}
				else if ( rsmd.getColumnType( i ) == java.sql.Types.BOOLEAN )
				{
					obj.put( column_name, rs.getBoolean( column_name ) );
				}
				else if ( rsmd.getColumnType( i ) == java.sql.Types.BLOB )
				{
					obj.put( column_name, rs.getBlob( column_name ) );
				}
				else if ( rsmd.getColumnType( i ) == java.sql.Types.DOUBLE )
				{
					obj.put( column_name, rs.getDouble( column_name ) );
				}
				else if ( rsmd.getColumnType( i ) == java.sql.Types.FLOAT )
				{
					obj.put( column_name, rs.getFloat( column_name ) );
				}
				else if ( rsmd.getColumnType( i ) == java.sql.Types.INTEGER )
				{
					obj.put( column_name, rs.getInt( column_name ) );
				}
				else if ( rsmd.getColumnType( i ) == java.sql.Types.NVARCHAR )
				{
					obj.put( column_name, rs.getNString( column_name ) );
				}
				else if ( rsmd.getColumnType( i ) == java.sql.Types.VARCHAR )
				{
					obj.put( column_name, rs.getString( column_name ) );
				}
				else if ( rsmd.getColumnType( i ) == java.sql.Types.TINYINT )
				{
					obj.put( column_name, rs.getInt( column_name ) );
				}
				else if ( rsmd.getColumnType( i ) == java.sql.Types.SMALLINT )
				{
					obj.put( column_name, rs.getInt( column_name ) );
				}
				else if ( rsmd.getColumnType( i ) == java.sql.Types.DATE )
				{
					obj.put( column_name, rs.getDate( column_name ) );
				}
				else if ( rsmd.getColumnType( i ) == java.sql.Types.TIMESTAMP )
				{
					obj.put( column_name, rs.getTimestamp( column_name ) );
				}
				else
				{
					obj.put( column_name, rs.getObject( column_name ) );
				}
			}
			
			json.put( "" + x, obj );
			x++;
		}
		while ( rs.next() );
		
		return json;
	}
}
