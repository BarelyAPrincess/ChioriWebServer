package com.chiorichan.http;

import java.net.ConnectException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.chiorichan.Loader;
import com.chiorichan.database.SqlConnector;
import com.chiorichan.file.YamlConfiguration;
import com.chiorichan.framework.SiteManager;
import com.chiorichan.util.Common;
import com.google.common.collect.Maps;

/**
 * Persistence manager handles sessions kept in memory. It also manages when to unload the session to free memeory.
 * 
 * @author Chiori Greene
 * @copyright Greenetree LLC 2014
 */
public class PersistenceManager
{
	protected SqlConnector sql = new SqlConnector();
	protected SiteManager _sites = new SiteManager( sql );
	
	static protected List<PersistentSession> sessionList = new ArrayList<PersistentSession>();
	
	public PersistenceManager()
	{
		YamlConfiguration config = Loader.getConfig();
		
		try
		{
			Class.forName( "com.mysql.jdbc.Driver" );
		}
		catch ( ClassNotFoundException e )
		{
			Loader.getLogger().severe( "We could not locate the 'com.mysql.jdbc.Driver' library regardless that its suppose to be included. If your running from source code be sure to have this library in your build path." );
			Loader.stop();
		}
		
		switch ( config.getString( "framework-database.type", "mysql" ) )
		{
			case "sqlite":
				String filename = config.getString( "framework.database.dbfile", "framework.db" );
				
				try
				{
					sql.init( filename );
				}
				catch ( SQLException e )
				{
					if ( e.getCause() instanceof ConnectException )
						Loader.getLogger().severe( "We had a problem connecting to database '" + filename + "'. Reason: " + e.getCause().getMessage() );
					else
						Loader.getLogger().severe( e.getMessage() );
					
					Loader.stop();
				}
				
				break;
			case "mysql":
				String host = config.getString( "framework.database.host", "localhost" );
				String port = config.getString( "framework.database.port", "3306" );
				String database = config.getString( "framework.database.database", "chiorifw" );
				String username = config.getString( "framework.database.user", "fwuser" );
				String password = config.getString( "framework.database.pass", "fwpass" );
				
				try
				{
					sql.init( database, username, password, host, port );
				}
				catch ( SQLException e )
				{
					// e.printStackTrace();
					
					if ( e.getCause() instanceof ConnectException )
						Loader.getLogger().severe( "We had a problem connecting to database '" + host + "'. Reason: " + e.getCause().getMessage() );
					else
						Loader.getLogger().severe( e.getMessage() );
					
					Loader.stop();
				}
				
				break;
			default:
				Loader.getLogger().panic( "The Framework Database can not support anything other then mySql or sqLite at the moment. Please change 'framework-database.type' to 'mysql' or 'sqLite' in 'chiori.yml'" );
		}
	}
	
	public void loadSessions()
	{
		synchronized ( sessionList )
		{
			sessionList.clear();
			
			try
			{
				ResultSet rs = sql.query( "SELECT * FROM `sessions`;" );
				
				if ( sql.getRowCount( rs ) > 0 )
					do
					{
						sessionList.add( new PersistentSession( rs ) );
					}
					while ( rs.next() );
			}
			catch ( SQLException e )
			{
				Loader.getLogger().panic( e.getMessage() );
			}
		}
	}
	
	protected PersistentSession find( HttpRequest request )
	{
		PersistentSession sess = null;
		
		synchronized ( sessionList )
		{
			for ( PersistentSession s : sessionList )
			{
				if ( s.matchClient( request ) )
				{
					sess = s;
					sess.setRequest( request, true );
				}
			}
			
			if ( sess == null )
			{
				sess = new PersistentSession( request );
				sessionList.add( sess );
			}
		}
		
		return sess;
	}
	
	public static void mainThreadHeartbeat( long tick )
	{
		// TODO: Cleanup the session database tables of unused sessions.
		
		Map<String, Integer> sessionLimits = Maps.newHashMap();
		
		synchronized ( sessionList )
		{
			for ( PersistentSession var1 : sessionList )
			{
				int cnt = ( sessionLimits.containsKey( var1.getId() ) ) ? sessionLimits.get( var1.getId() ) : 0;
				sessionLimits.put( var1.getId(), cnt + 1 );
				
				if ( sessionLimits.get( var1.getId() ) > Loader.getConfig().getInt( "server.sessionLimit", 6 ) )
				{
					// Finish making this work.
				}
				
				// Loader.getLogger().debug( "" + var1 );
				
				if ( var1.getTimeout() > 0 && var1.getTimeout() < Common.getEpoch() )
				{
					Loader.getLogger().info( "&4Unloaded expired session: " + var1.getId() );
					
					sessionList.remove( var1 ); // This should allow this session to get picked up by the Java Garbage
											// Collector once it's released by other classes.
				}
			}
		}
	}
	
	public SiteManager getSiteManager()
	{
		return _sites;
	}
	
	public SqlConnector getSql()
	{
		return sql;
	}
	
	public List<PersistentSession> getSessions()
	{
		return sessionList;
	}
}
