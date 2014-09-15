package com.chiorichan.http;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.chiorichan.Loader;
import com.chiorichan.database.DatabaseEngine;
import com.chiorichan.framework.Site;
import com.chiorichan.util.StringUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class Routes
{
	/**
	 * Prevents file and sql lag from reloading the routes for every dozen requests made within a small span of time.
	 */
	private long lastRequest = 0;
	private Set<Route> routes = Sets.newHashSet();
	private Site site;
	
	public enum RouteType
	{
		NOTSET(), SQL(), FILE();
		
		public String toString()
		{
			switch ( this )
			{
				case FILE:
					return "File";
				case SQL:
					return "Sql";
				default:
					return "Not Set";
			}
		}
	}
	
	public class Route
	{
		protected RouteType type = RouteType.NOTSET;
		protected Map<String, String> params = Maps.newLinkedHashMap();
		protected Map<String, String> rewrites = Maps.newHashMap();
		
		protected Route(ResultSet rs) throws SQLException
		{
			type = RouteType.SQL;
			params = DatabaseEngine.toStringsMap( rs );
		}
		
		public String toString()
		{
			return "Type: " + type + ", Params: " + params;
		}
		
		/**
		 * 
		 * @param args, line input in the format of "pattern '/dir/[cat=]/[id=]', to '/dir/view_item.gsp'"
		 * @throws IOException thrown is input string is not valid
		 */
		public Route(String args) throws IOException
		{
			if ( args == null || args.isEmpty() )
				throw new IOException( "args can't be null or empty" );
			
			type = RouteType.FILE;
			
			for ( String o : args.split( "," ) )
			{
				String key = null;
				String val = null;
				
				o = o.trim();
				
				if ( o.contains( ":" ) )
				{
					key = o.substring( 0, o.indexOf( ":" ) );
					val = o.substring( o.indexOf( ":" ) + 1 );
				}
				else if ( ( !o.contains( "\"" ) && !o.contains( "'" ) ) || ( o.contains( "\"" ) && o.indexOf( " " ) < o.indexOf( "\"" ) ) || ( o.contains( "'" ) && o.indexOf( " " ) < o.indexOf( "'" ) ) )
				{
					key = o.substring( 0, o.indexOf( " " ) );
					val = o.substring( o.indexOf( " " ) + 1 );
				}
				
				if ( key != null && val != null )
				{
					key = StringUtils.trimToEmpty( key.toLowerCase() );
					val = StringUtils.trimToEmpty( val );
					
					val = StringUtils.removeStart( val, "\"" );
					val = StringUtils.removeStart( val, "'" );
					
					val = StringUtils.removeEnd( val, "\"" );
					val = StringUtils.removeEnd( val, "'" );
					
					params.put( key, val );
				}
			}
			
			params.put( "domain", site.getDomain() );
		}
		
		public RouteType getRouteType()
		{
			return type;
		}
		
		public Map<String, String> getRewrites()
		{
			return rewrites;
		}
		
		public Map<String, String> getParams()
		{
			return params;
		}
		
		public File getFile()
		{
			if ( params.get( "file" ) != null && !params.get( "file" ).isEmpty() )
				return new File( site.getSourceDirectory(), params.get( "file" ) );
			else
				return null;
		}
		
		public String match( String domain, String subdomain, String uri )
		{
			String prop = params.get( "pattern" );
			
			if ( prop == null )
				prop = params.get( "page" );
			
			if ( prop == null )
			{
				Loader.getLogger().warning( "The `pattern` attibute was null for route '" + this + "'. Unusable!" );
				return null;
			}
			
			prop = StringUtils.trimToEmpty( prop );
			uri = StringUtils.trimToEmpty( uri );
			
			if ( prop.startsWith( "/" ) )
			{
				prop = prop.substring( 1 );
				params.put( "pattern", prop );
			}
			
			if ( !StringUtils.trimToEmpty( params.get( "subdomain" ) ).isEmpty() && !subdomain.equals( params.get( "subdomain" ) ) )
			{
				Loader.getLogger().fine( "The subdomain does not match for " + prop + " on route " + this );
				return null;
			}
			
			String[] propsRaw = prop.split( "[.//]" );
			String[] urisRaw = uri.split( "[.//]" );
			
			ArrayList<String> props = Lists.newArrayList();
			ArrayList<String> uris = Lists.newArrayList();
			
			for ( String s : propsRaw )
				if ( s != null && !s.isEmpty() )
					props.add( s );
			
			for ( String s : urisRaw )
				if ( s != null && !s.isEmpty() )
					uris.add( s );
			
			String weight = StringUtils.repeat( "?", Math.max( props.size(), uris.size() ) );
			
			boolean match = true;
			for ( int i = 0; i < Math.max( props.size(), uris.size() ); i++ )
			{
				try
				{
					Loader.getLogger().fine( prop + " --> " + props.get( i ) + " == " + uris.get( i ) );
					
					if ( props.get( i ).matches( "\\[([a-zA-Z0-9]+)=\\]" ) )
					{
						weight = StringUtil.replaceAt( weight, i, "Z" );
						
						String key = props.get( i ).replaceAll( "[\\[\\]=]", "" );
						String value = uris.get( i );
						
						rewrites.put( key, value );
						
						// PREG MATCH
						Loader.getLogger().fine( "Found a PREG match for " + prop + " on route " + this );
					}
					else if ( props.get( i ).equals( uris.get( i ) ) )
					{
						weight = StringUtil.replaceAt( weight, i, "A" );
						
						Loader.getLogger().fine( "Found a match for " + prop + " on route " + this );
						// MATCH
					}
					else
					{
						match = false;
						Loader.getLogger().fine( "Found no match for " + prop + " on route " + this );
						break;
						// NO MATCH
					}
				}
				catch ( ArrayIndexOutOfBoundsException e )
				{
					match = false;
					break;
				}
			}
			
			return ( match ) ? weight : null;
		}
	}
	
	public Routes(Site _site)
	{
		site = _site;
	}
	
	public Route searchRoutes( String uri, String domain, String subdomain ) throws IOException
	{
		File routesFile = new File( Loader.getWebRoot() + Loader.PATH_SEPERATOR + site.getRoot() + Loader.PATH_SEPERATOR + "routes" );
		
		if ( routes.size() < 1 || lastRequest - System.currentTimeMillis() > 1000 )
		{
			try
			{
				if ( routesFile.exists() )
				{
					routes.clear();
					String contents = FileUtils.readFileToString( routesFile );
					for ( String l : contents.split( "\n" ) )
					{
						try
						{
							routes.add( new Route( l ) );
						}
						catch ( IOException e1 )
						{	
							
						}
					}
				}
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}
			
			try
			{
				DatabaseEngine sql = Loader.getPersistenceManager().getDatabase();
				ResultSet rs = sql.query( "SELECT * FROM `pages` WHERE (site = '" + subdomain + "' OR site = '') AND domain = '" + domain + "' UNION SELECT * FROM `pages` WHERE (site = '" + subdomain + "' OR site = '') AND domain = '';" );
				if ( sql.getRowCount( rs ) > 0 )
				{
					do
					{
						routes.add( new Route( rs ) );
					}
					while ( rs.next() );
				}
			}
			catch ( SQLException e )
			{
				throw new IOException( e );
			}
		}
		lastRequest = System.currentTimeMillis();
		
		if ( routes.size() > 0 )
		{
			Map<String, Route> matches = Maps.newTreeMap();
			int keyInter = 0;
			
			for ( Route route : routes )
			{
				String weight = route.match( domain, subdomain, uri );
				if ( weight != null )
				{
					matches.put( weight + keyInter, route );
					keyInter++;
				}
			}
			
			if ( matches.size() > 0 )
			{
				return (Route) matches.values().toArray()[0];
			}
			else
				Loader.getLogger().fine( "Failed to find a page redirect for Rewrite... '" + subdomain + "." + domain + "' '" + uri + "'" );
		}
		else
			Loader.getLogger().fine( "Failed to find a page redirect for Rewrite... '" + subdomain + "." + domain + "' '" + uri + "'" );
		
		return null;
	}
}
