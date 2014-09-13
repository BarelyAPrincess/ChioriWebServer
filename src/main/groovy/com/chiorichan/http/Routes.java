package com.chiorichan.http;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.chiorichan.Loader;
import com.chiorichan.database.DatabaseEngine;
import com.chiorichan.framework.Site;
import com.chiorichan.util.StringUtil;
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
			String prop = params.get( "page" );
			
			if ( prop == null || prop.isEmpty() )
				prop = params.get( "pattern" );
			
			if ( prop == null || prop.isEmpty() )
			{
				Loader.getLogger().warning( "The `pattern` attibute was empty for route '" + this + "'. Unusable!" );
				return null;
			}
			
			if ( prop.startsWith( "/" ) )
			{
				prop = prop.substring( 1 );
				params.put( "page", prop );
			}
			
			String[] props = prop.split( "[.//]" );
			String[] uris = uri.split( "[.//]" );
			
			String weight = StringUtils.repeat( "?", Math.max( props.length, uris.length ) );
			
			boolean match = true;
			for ( int i = 0; i < Math.max( props.length, uris.length ); i++ )
			{
				try
				{
					Loader.getLogger().fine( prop + " --> " + props[i] + " == " + uris[i] );
					
					if ( props[i].matches( "\\[([a-zA-Z0-9]+)=\\]" ) )
					{
						weight = StringUtil.replaceAt( weight, i, "Z" );
						
						String key = props[i].replaceAll( "[\\[\\]=]", "" );
						String value = uris[i];
						
						rewrites.put( key, value );
						
						// PREG MATCH
						Loader.getLogger().fine( "Found a PREG match to " + params.get( "page" ) );
					}
					else if ( props[i].equals( uris[i] ) )
					{
						weight = StringUtil.replaceAt( weight, i, "A" );
						
						Loader.getLogger().fine( "Found a match to " + params.get( "page" ) );
						// MATCH
					}
					else
					{
						match = false;
						Loader.getLogger().fine( "Found no match to " + params.get( "page" ) );
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
