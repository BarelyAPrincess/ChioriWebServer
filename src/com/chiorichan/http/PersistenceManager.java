package com.chiorichan.http;

import java.net.ConnectException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.chiorichan.Loader;
import com.chiorichan.database.SqlConnector;
import com.chiorichan.file.YamlConfiguration;
import com.chiorichan.framework.SiteManager;

/**
 * Persistence manager handles when a session needs to be purged from the memory.
 * 
 * @author Chiori Greene
 * @copyright Greenetree LLC 2014
 */
public class PersistenceManager
{
	protected SqlConnector sql = new SqlConnector();
	protected SiteManager _sites = new SiteManager( sql );
	
	static protected List<PersistentSession> sessionList = new ArrayList<PersistentSession>();
	private static Timer timer1 = new Timer();
	
	public PersistenceManager()
	{
		YamlConfiguration config = Loader.getConfig();
		
		switch ( config.getString( "framework-database.type", "mysql" ) )
		{
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
				catch ( ConnectException e )
				{
					//e.printStackTrace();
					Loader.getConsole().severe( "We had a problem connecting to database '" + host + "'. Reason: " + e.getMessage() );
					System.exit( 1 );
				}
				catch ( SQLException e )
				{
					//e.printStackTrace();
					
					if ( e.getCause() instanceof ConnectException )
						Loader.getConsole().severe( "We had a problem connecting to database '" + host + "'. Reason: " + e.getMessage() );
					else
						Loader.getConsole().severe( e.getMessage() );
					
					System.exit( 1 );
				}
				catch ( ClassNotFoundException e )
				{
					Loader.getConsole().severe( "We could not locate the 'com.mysql.jdbc.Driver' library regardless that its suppose to be included. If your running from source code be sure to have this library in your build path." );
					System.exit( 1 );
				}
				
				break;
			default:
				Loader.getConsole().severe( "The Framework Database can not be anything other then mySql at the moment. Please change 'framework-database.type' to 'mysql' in 'chiori.yml'" );
				System.exit( 1 );
		}
		
		_sites.loadSites();
	}
	
	protected PersistentSession find( HttpRequest request )
	{
		PersistentSession sess = null;
		
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
		
		return sess;
	}
	
	public static void mainThreadHeartbeat( long tick )
	{
		for ( PersistentSession var1 : sessionList )
				{
					if ( var1.getTimeout() > 0 && var1.getTimeout() < ( System.currentTimeMillis() / 1000 ) )
					{	
						sessionList.remove( var1 ); // This should allow this session to get picked up by the Java Garbage Collector once it's released by other classes.
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
}
