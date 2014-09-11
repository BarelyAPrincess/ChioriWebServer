/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.http;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.StringUtils;

import com.chiorichan.ContentTypes;
import com.chiorichan.Loader;
import com.chiorichan.database.DatabaseEngine;
import com.chiorichan.exceptions.HttpErrorException;
import com.chiorichan.framework.FileInterpreter;
import com.chiorichan.framework.SiteException;
import com.chiorichan.util.StringUtil;
import com.google.common.collect.Maps;

public class WebInterpreter extends FileInterpreter
{
	Map<String, String> rewriteParams = Maps.newTreeMap();
	
	@Override
	public String toString()
	{
		String overrides = "";
		
		for ( Entry<String, String> o : interpParams.entrySet() )
		{
			String l = o.getValue();
			if ( l != null )
				l = l.replace( "\n", "" );
			
			overrides += "," + o.getKey() + "=" + l;
		}
		
		if ( overrides.length() > 1 )
			overrides = overrides.substring( 1 );
		
		String rewrites = "";
		
		for ( Entry<String, String> o : rewriteParams.entrySet() )
		{
			rewrites += "," + o.getKey() + "=" + o.getValue();
		}
		
		if ( rewrites.length() > 1 )
			rewrites = rewrites.substring( 1 );
		
		// String cachedFileStr = ( cachedFile == null ) ? "N/A" : cachedFile.getAbsolutePath();
		
		return "WebInterpreter{content=" + bs.size() + " bytes,contentType=" + getContentType() + ",overrides={" + overrides + "},rewrites={" + rewrites + "}}";
	}
	
	@SuppressWarnings( "unchecked" )
	public WebInterpreter(HttpRequest request) throws IOException, HttpErrorException, SiteException
	{
		super();
		
		DatabaseEngine sql = Loader.getPersistenceManager().getDatabase();
		
		File dest = null;
		
		String uri = request.getURI();
		String domain = request.getParentDomain();
		String subdomain = request.getSubDomain();
		
		boolean wasSuccessful = false;
		
		// Try to find the virtual file from the database
		try
		{
			// Original Select Query
			// TODO: Fix the select issue with blank subdomains. It's not suppose to be 1111 but it is to prevent the redirect loop.
			// ResultSet rs = sql.query( "SELECT * FROM `pages` WHERE (site = '" + subdomain + "' OR site = 'FIXME') AND domain = '" + domain + "' UNION SELECT * FROM `pages` WHERE (site = '" + subdomain +
			// "' OR site = 'FIXME') AND domain = '';" );
			
			// ResultSet rs = sql.query( "SELECT * FROM `pages` WHERE site = '" + subdomain + "' AND domain = '" + domain + "' UNION SELECT * FROM `pages` WHERE site = '' AND domain = '" + domain +
			// "' UNION SELECT * FROM `pages` WHERE site = '" + subdomain + "' AND domain = '' UNION SELECT * FROM `pages` WHERE site = '' AND domain = '';" );
			
			ResultSet rs = sql.query( "SELECT * FROM `pages` WHERE (site = '" + subdomain + "' OR site = '') AND domain = '" + domain + "' UNION SELECT * FROM `pages` WHERE (site = '" + subdomain + "' OR site = '') AND domain = '';" );
			
			if ( sql.getRowCount( rs ) > 0 )
			{
				Map<String, Map<String, String>> rewrite = Maps.newTreeMap();
				int keyInter = 0;
				
				do
				{
					Map<String, String> data = Maps.newTreeMap();
					
					String prop = rs.getString( "page" );
					
					if ( prop.startsWith( "/" ) )
						prop = prop.substring( 1 );
					
					data.put( "page", prop );
					
					String[] props = prop.split( "[.//]" );
					String[] uris = uri.split( "[.//]" );
					
					String weight = StringUtils.repeat( "?", Math.max( props.length, uris.length ) );
					
					boolean whole_match = true;
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
								
								rewriteParams.put( key, value );
								
								// PREP MATCH
								Loader.getLogger().fine( "Found a PREG match to " + rs.getString( "page" ) );
							}
							else if ( props[i].equals( uris[i] ) )
							{
								weight = StringUtil.replaceAt( weight, i, "A" );
								
								Loader.getLogger().fine( "Found a match to " + rs.getString( "page" ) );
								// MATCH
							}
							else
							{
								whole_match = false;
								Loader.getLogger().fine( "Found no match to " + rs.getString( "page" ) );
								break;
								// NO MATCH
							}
						}
						catch ( ArrayIndexOutOfBoundsException e )
						{
							whole_match = false;
							break;
						}
					}
					
					if ( whole_match )
					{
						ResultSetMetaData rsmd = rs.getMetaData();
						
						int numColumns = rsmd.getColumnCount();
						
						for ( int i = 1; i < numColumns + 1; i++ )
						{
							String column_name = rsmd.getColumnName( i );
							
							if ( ( rs.getString( column_name ) != null && rs.getString( column_name ) != "" && rs.getString( column_name ) != "null" ) || !data.containsKey( column_name ) )
								data.put( column_name, rs.getString( column_name ) );
						}
						
						if ( data.get( "file" ) != null && !data.get( "file" ).isEmpty() )
							dest = new File( request.getSite().getSourceDirectory(), data.get( "file" ) );
						
						rewrite.put( weight + keyInter, data );
						keyInter++;
					}
				}
				while ( rs.next() );
				
				if ( rewrite.size() > 0 )
				{
					interpParams.putAll( (Map<String, String>) rewrite.values().toArray()[0] );
					wasSuccessful = true;
					
					// if ( request.getRewriteVars().size() > 0 )
					// Loader.getLogger().info( "Found rewrite params " + request.getRewriteVars() );
				}
				else
					Loader.getLogger().fine( "Failed to find a page redirect for Rewrite... '" + subdomain + "." + domain + "' '" + uri + "'" );
			}
			else
				Loader.getLogger().fine( "Failed to find a page redirect for Rewrite... '" + subdomain + "." + domain + "' '" + uri + "'" );
		}
		catch ( SQLException e )
		{
			throw new IOException( e );
		}
		
		// Try to find the file on the local filesystem
		if ( !wasSuccessful )
		{
			dest = new File( request.getSite().getAbsoluteRoot( subdomain ), uri );
			
			if ( dest.isDirectory() )
			{
				FileFilter fileFilter = new WildcardFileFilter( "index.*" );
				File[] files = dest.listFiles( fileFilter );
				
				File selectedFile = null;
				
				if ( files != null && files.length > 0 )
					for ( File f : files )
					{
						if ( f.exists() )
						{
							String filename = f.getName().toLowerCase();
							if ( filename.endsWith( ".chi" ) || filename.endsWith( ".groovy" ) || filename.endsWith( ".html" ) || filename.endsWith( ".htm" ) )
							{
								selectedFile = f;
								break;
							}
							else
								selectedFile = f;
						}
					}
				
				if ( selectedFile != null )
				{
					uri = uri + "/" + selectedFile.getName();
					dest = new File( request.getSite().getAbsoluteRoot( subdomain ), uri );
				}
				else if ( Loader.getConfig().getBoolean( "server.allowDirectoryListing" ) )
					// TODO: Implement Directory Listings
					throw new HttpErrorException( 403, "Sorry, Directory Listing has not been implemented on this Server!" );
				else
					throw new HttpErrorException( 403, "Directory Listing is Disallowed on this Server!" );
			}
			
			if ( !dest.exists() )
				// Attempt to determine if it's possible that the uri is a name without an extension.
				// For Example: uri(http://example.com/pages/aboutus) = file([root]/pages/aboutus.html)
				if ( dest.getParentFile().exists() && dest.getParentFile().isDirectory() )
				{
					FileFilter fileFilter = new WildcardFileFilter( dest.getName() + ".*" );
					File[] files = dest.getParentFile().listFiles( fileFilter );
					
					if ( files != null && files.length > 0 )
						for ( File f : files )
						{
							if ( f.exists() )
							{
								String filename = f.getName().toLowerCase();
								if ( filename.endsWith( ".chi" ) || filename.endsWith( ".gsp" ) || filename.endsWith( ".jsp" ) || filename.endsWith( ".groovy" ) || filename.endsWith( ".html" ) || filename.endsWith( ".htm" ) )
								{
									dest = f;
									break;
								}
								else
									dest = f;
							}
						}
					
					// Attempt to determine is it's possible that the uri is a name without extension but it also contains server-side options
					// For Example: uri(http://images.example.com/logo_x150.jpg) = file([root]/images/logo.jpg) and resize to 150 width.
					fileFilter = new WildcardFileFilter( "*" ); // dest.getName() + "_*"
					files = dest.getParentFile().listFiles( fileFilter );
					
					if ( files != null && files.length > 0 )
						for ( File f : files )
						{
							if ( f.exists() && !f.isDirectory() )
							{
								String destFileName = dest.getName();
								String fileNameFull = f.getName();
								String fileName = ( fileNameFull.contains( "." ) ) ? fileNameFull.substring( 0, fileNameFull.lastIndexOf( "." ) ) : fileNameFull;
								String ext = ( fileNameFull.contains( "." ) ) ? fileNameFull.substring( fileNameFull.lastIndexOf( "." ) + 1 ) : "";
								String ext2 = ( destFileName.contains( "." ) ) ? destFileName.substring( destFileName.lastIndexOf( "." ) + 1 ) : "";
								
								if ( destFileName.startsWith( fileName ) && ( ext2.isEmpty() || destFileName.endsWith( ext ) ) )
								{
									dest = f;
									
									String paramString = destFileName.substring( fileName.length() );
									
									if ( !ext2.isEmpty() )
										paramString = paramString.substring( 0, paramString.length() - ext2.length() - 1 );
									
									if ( !paramString.isEmpty() )
										rewriteParams.put( "serverSideOptions", paramString );
								}
							}
						}
				}
			
			wasSuccessful = dest.exists();
		}
		
		if ( wasSuccessful )
		{
			if ( dest != null && dest.exists() )
				interpretParamsFromFile( dest );
			else if ( !interpParams.containsKey( "shell" ) || interpParams.get( "shell" ) == null )
				interpParams.put( "shell", "html" );
		}
		else
			throw new HttpErrorException( 404 );
	}
	
	public Map<String, String> getRewriteParams()
	{
		return rewriteParams;
	}
}
