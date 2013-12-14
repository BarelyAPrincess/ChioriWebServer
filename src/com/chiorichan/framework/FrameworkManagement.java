package com.chiorichan.framework;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.chiorichan.Loader;
import com.chiorichan.database.SqlConnector;
import com.chiorichan.event.server.RequestEvent;
import com.chiorichan.event.server.ServerVars;
import com.chiorichan.file.YamlConfiguration;

public class FrameworkManagement
{
	protected SqlConnector sql = new SqlConnector();
	protected SiteManager _sites = new SiteManager( sql );
	protected Map<String, FrameworkGuard> fwStor = new HashMap<String, FrameworkGuard>();
	
	public FrameworkManagement()
	{
		// TODO: Schedule a framework cleanup task every five minutes.
		
		initalize();
	}
	
	public SiteManager getSiteManager()
	{
		return _sites;
	}
	
	public void initalize()
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
				catch ( SQLException e )
				{
					e.printStackTrace();
					Loader.getLogger().severe( e.getMessage() );
					System.exit( 1 );
				}
				catch ( ClassNotFoundException e )
				{
					Loader.getLogger().severe( "We could not locate the 'com.mysql.jdbc.Driver' library regardless that its suppose to be included. If your running from source code be sure to have this library in your build path." );
					System.exit( 1 );
				}
				catch ( ConnectException e )
				{
					e.printStackTrace();
					Loader.getLogger().severe( "We had a problem connecting to the database host '" + host + "'" );
					System.exit( 1 );
				}
				
				break;
			default:
				Loader.getLogger().severe( "The Framework Database can not be anything other then mySql at the moment. Please change 'framework-database.type' to 'mysql' in 'chiori.yml'" );
				System.exit( 1 );
		}
		
		_sites.loadSites();
	}
	
	public void handleRequest( HttpServletRequest request, HttpServletResponse response ) throws IOException
	{
		String uri = request.getRequestURI();
		String domain = request.getServerName();
		String site = "";
		
		request.setCharacterEncoding( "ISO-8859-1" );
		response.setCharacterEncoding( "ISO-8859-1" );
		
		if ( uri.startsWith( "/" ) )
			uri = uri.substring( 1 );
		
		if ( domain.equalsIgnoreCase( "localhost" ) || domain.equalsIgnoreCase( "127.0.0.1" ) | domain.equalsIgnoreCase( request.getLocalAddr() ) )
			domain = "";
		
		if ( domain.split( "\\." ).length > 2 )
		{
			String[] r = domain.split( "\\.", 2 );
			site = r[0];
			domain = r[1];
		}
		
		Site currentSite = _sites.getSiteByDomain( domain );
		
		// TODO: Generate a blank site and save it based on the information known
		if ( currentSite == null )
			currentSite = new Site( "default", Loader.getConfig().getString( "framework.sites.defaultTitle", "Unnamed Chiori Framework Site" ), domain );
		
		response.addHeader( "Access-Control-Allow-Origin", currentSite.getYaml().getString( "web.allowed-origin", "*" ) );
		
		Session _sess = new Session( currentSite, request, response );
		Framework fw;
		
		if ( fwStor.containsKey( _sess.getId() ) )
		{
			FrameworkGuard fg = fwStor.get( _sess.getId() );
			fg.rearmTimeout();
			fw = fg._fw;
			fw._stale = true;
		}
		else
		{
			fw = new Framework( request, response );
			fwStor.put( _sess.getId(), new FrameworkGuard( fw ) );
		}
		
		fw.currentSite = currentSite;
		
		RequestEvent event = new RequestEvent( fw );
		Loader.getPluginManager().callEvent( event );
		
		if ( event.isCancelled() )
		{
			Loader.getLogger().warning( "Navigation was cancelled by a plugin for ip '" + request.getRemoteAddr() + "' '" + request.getHeader( "Host" ) + request.getRequestURI() + "'" );
			
			int status = event.getStatus();
			String reason = event.getReason();
			
			if ( status < 400 && status > 599 )
			{
				status = 502;
				reason = "Navigation Cancelled by Internal Event";
			}
			
			response.sendError( status, reason );
			return;
		}
		
		if ( !fw.rewriteVirtual( domain, site, uri ) )
		{
			File siteRoot = new File( Loader.webroot, currentSite.getWebRoot( site ) );
			
			if ( !siteRoot.exists() )
				siteRoot.mkdirs();
			
			if ( uri.isEmpty() )
				uri = "/";
			
			File dest = new File( siteRoot, uri );
			
			if ( uri.endsWith( ".groovy" ) || uri.endsWith( ".chi" ) || ( dest.isDirectory() && new File( dest, "index.groovy" ).exists() ) || ( dest.isDirectory() && new File( dest, "index.chi" ).exists() ) )
			{
				fw.loadPageInternal( "", "", "", uri, "", "-1" );
			}
			else
			{
				if ( dest.isDirectory() )
				{
					if ( new File( dest, "index.html" ).exists() )
						uri = uri + "/index.html";
					else if ( new File( dest, "index.htm" ).exists() )
						uri = uri + "/index.htm";
					else
						response.sendError( 403, "Directory Listing is Denied on this Server!" );
				}
				else
				{
					dest = new File( siteRoot, uri );
					
					String target = dest.getAbsolutePath();
					Loader.getLogger().fine( "Requesting file (" + currentSite.siteId + ") '" + target + "'" );
					
					FileInputStream is;
					try
					{
						is = new FileInputStream( target );
					}
					catch ( FileNotFoundException e )
					{
						response.sendError( 404 );
						return;
					}
					
					try
					{
						ServletOutputStream buffer = response.getOutputStream();
						
						int nRead;
						byte[] data = new byte[16384];
						
						while ( ( nRead = is.read( data, 0, data.length ) ) != -1 )
						{
							buffer.write( data, 0, nRead );
						}
						
						buffer.flush();
						
						is.close();
					}
					catch ( IOException e )
					{
						e.printStackTrace();
						response.sendError( 500, e.getMessage() );
						return;
					}
				}
			}
		}
	}
}
