/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.http;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.filefilter.WildcardFileFilter;

import com.chiorichan.Loader;
import com.chiorichan.exceptions.HttpErrorException;
import com.chiorichan.framework.FileInterpreter;
import com.chiorichan.framework.SiteException;
import com.chiorichan.http.Routes.Route;
import com.google.common.collect.Maps;

public class WebInterpreter extends FileInterpreter
{
	protected Map<String, String> rewriteParams = Maps.newTreeMap();
	
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
	
	public WebInterpreter(HttpRequest request, Routes routes) throws IOException, HttpErrorException, SiteException
	{
		super();
		
		File dest = null;
		boolean wasSuccessful = false;
		
		String uri = request.getURI();
		String domain = request.getParentDomain();
		String subdomain = request.getSubDomain();
		
		Route route = routes.searchRoutes( uri, domain, subdomain );
		
		if ( route != null )
		{
			rewriteParams.putAll( route.getRewrites() );
			interpParams.putAll( route.getParams() );
			dest = route.getFile();
			wasSuccessful = true;
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
