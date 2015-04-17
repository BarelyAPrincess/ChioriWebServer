/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.http;

import io.netty.handler.codec.http.HttpResponseStatus;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.filefilter.WildcardFileFilter;

import com.chiorichan.Loader;
import com.chiorichan.factory.FileInterpreter;
import com.chiorichan.lang.HttpError;
import com.chiorichan.lang.SiteException;
import com.chiorichan.site.Site;
import com.google.common.collect.Maps;

public class WebInterpreter extends FileInterpreter
{
	protected Map<String, String> rewriteParams = Maps.newTreeMap();
	protected boolean isDirectoryRequest = false;
	protected boolean fwRequest = false;
	protected HttpResponseStatus status = HttpResponseStatus.OK;
	protected String html = null;
	
	public WebInterpreter( HttpRequestWrapper request ) throws IOException, HttpError, SiteException
	{
		super();
		
		File dest = null;
		Routes routes = request.getSite().getRoutes();
		boolean wasSuccessful = false;
		
		String uri = request.getURI();
		String domain = request.getParentDomain();
		String subdomain = request.getSubDomain();
		
		fwRequest = uri.startsWith( "/fw" );
		if ( fwRequest )
		{
			Site fwSite = Loader.getSiteManager().getFrameworkSite();
			routes = fwSite.getRoutes();
			request.setSite( fwSite );
			// request.setUri( uri.substring( 3 ) );
		}
		
		if ( uri.startsWith( "/fw/~" ) )
		{
			int indexOf = uri.indexOf( "/", 5 );
			
			if ( indexOf < 0 )
				indexOf = uri.length();
			
			String siteId = uri.substring( 5, indexOf );
			
			Site fwSite = Loader.getSiteManager().getSiteById( siteId );
			
			if ( fwSite != null )
			{
				fwRequest = false;
				routes = fwSite.getRoutes();
				request.setSite( fwSite );
				uri = ( indexOf == uri.length() ) ? "/" : uri.substring( indexOf );
				request.setUri( uri );
				Loader.getLogger().info( "Detected a virtual site request. New request on site '" + siteId + "' with URI '" + uri + "'. &4Caution: There could be site bugs when doing these kind of requests." );
			}
		}
		
		Route route = routes.searchRoutes( uri, domain, subdomain );
		
		if ( route != null )
		{
			rewriteParams.putAll( route.getRewrites() );
			interpParams.putAll( route.getParams() );
			dest = route.getFile();
			html = route.getHTML();
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
					isDirectoryRequest = true;
				else
					throw new HttpError( 403, "Directory Listing is Disallowed on this Server!" );
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
			status = HttpResponseStatus.NOT_FOUND;
	}
	
	public HttpResponseStatus getStatus()
	{
		return status;
	}
	
	public Map<String, String> getRewriteParams()
	{
		return rewriteParams;
	}
	
	public boolean isDirectoryRequest()
	{
		return isDirectoryRequest;
	}
	
	public boolean isFrameworkRequest()
	{
		return fwRequest;
	}
	
	public String getHTML()
	{
		return html;
	}
	
	public boolean hasHTML()
	{
		return html != null;
	}
	
	@Override
	public String toString()
	{
		String overrides = "";
		
		for ( Entry<String, String> o : interpParams.entrySet() )
		{
			String l = o.getValue();
			if ( l != null )
			{
				l = l.replace( "\n", "" );
				l = l.replace( "\r", "" );
			}
			
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
		
		return "WebInterpreter[content=" + data.writerIndex() + " bytes,contentType=" + getContentType() + ",overrides=[" + overrides + "],rewrites=[" + rewrites + "]]";
	}
}
