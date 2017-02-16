/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * <p>
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 * <p>
 * All Rights Reserved.
 */
package com.chiorichan.http;

import com.chiorichan.AppConfig;
import com.chiorichan.factory.FileInterpreter;
import com.chiorichan.factory.ScriptingContext;
import com.chiorichan.lang.HttpError;
import com.chiorichan.net.NetworkManager;
import com.chiorichan.site.DomainMapping;
import com.chiorichan.site.SiteManager;
import com.chiorichan.zutils.ZIO;
import com.chiorichan.zutils.ZObjects;
import com.google.common.collect.Maps;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class WebInterpreter extends FileInterpreter
{
	protected Map<String, String> rewriteParams = Maps.newTreeMap();
	protected boolean isDirectoryRequest = false;
	protected boolean fwRequest = false;
	protected HttpResponseStatus status = HttpResponseStatus.OK;
	protected String html = null;

	public WebInterpreter( HttpRequestWrapper request ) throws IOException, HttpError
	{
		super();

		File dest = null;
		Routes routes = request.getLocation().getRoutes();
		boolean wasSuccessful = false;

		String uri = request.getUri();

		fwRequest = uri.startsWith( "/wisp" );
		if ( fwRequest )
		{
			DomainMapping defaultMapping = SiteManager.instance().getDefaultSite().getDefaultMapping();
			request.setDomainMapping( defaultMapping );
			request.setUri( uri.substring( 5 ) );
			routes = defaultMapping.getSite().getRoutes();
		}
		else
		{
			DomainMapping mapping = request.getDomainMapping();
			if ( mapping.hasConfig( "redirect" ) && !ZObjects.isEmpty( mapping.getConfig( "redirect" ) ) )
			{
				String url = mapping.getConfig( "redirect" );
				status = HttpResponseStatus.valueOf( ZObjects.castToInt( mapping.getConfig( "redirectCode" ), 301 ) );
				request.getResponse().sendRedirect( url.toLowerCase().startsWith( "http" ) ? url : request.getFullDomain() + url, status.code() );
				return;
			}
		}

		Route route = routes.searchRoutes( uri, request.getHostDomain() );

		if ( route != null )
		{
			rewriteParams.putAll( route.getRewrites() );
			annotations.putAll( route.getParams() );
			dest = new File( request.getDomainMapping().directory(), route.getFile() );

			if ( route.isRedirect() )
			{
				status = HttpResponseStatus.valueOf( route.httpCode() );
				request.getResponse().sendRedirect( route.getRedirect().toLowerCase().startsWith( "http" ) ? route.getRedirect() : request.getFullDomain() + route.getRedirect(), status.code() );
				return;
			}

			html = route.getHTML();
			wasSuccessful = true;
		}

		/* Try to find the file on the local file system */
		if ( !wasSuccessful )
		{
			dest = new File( request.getDomainMapping().directory(), uri );

			if ( dest.isDirectory() )
			{
				FileFilter fileFilter = new WildcardFileFilter( "index.*" );
				Map<String, File> maps = ZIO.mapExtensions( dest.listFiles( fileFilter ) );

				List<String> preferredExtensions = ScriptingContext.getPreferredExtensions();

				File selectedFile = null;

				if ( maps.size() > 0 )
				{
					for ( String ext : preferredExtensions )
						if ( maps.containsKey( ext.toLowerCase() ) )
						{
							selectedFile = maps.get( ext.toLowerCase() );
							break;
						}
					if ( selectedFile == null )
						selectedFile = new ArrayList<File>( maps.values() ).get( 0 );
				}

				if ( selectedFile != null )
				{
					uri = uri + "/" + selectedFile.getName();
					dest = new File( request.getDomainMapping().directory(), uri );
				}
				else if ( AppConfig.get().getBoolean( "server.allowDirectoryListing" ) )
					isDirectoryRequest = true;
				else
					throw new HttpError( 403, "Directory Listing is Disallowed" );
			}

			if ( !dest.exists() )
				if ( dest.getParentFile().exists() && dest.getParentFile().isDirectory() )
				{
					FileFilter fileFilter = new WildcardFileFilter( dest.getName() + ".*" );
					File[] files = dest.getParentFile().listFiles( fileFilter );

					if ( files != null && files.length > 0 )
						for ( File f : files )
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

					// Attempt to determine is it's possible that the uri is a name without extension but it also contains server-side options
					// For Example: uri(http://images.example.com/logo_x150.jpg) = file([root]/images/logo.jpg) and resize to 150 width.
					fileFilter = new WildcardFileFilter( "*" ); // dest.getName() + "_*"
					files = dest.getParentFile().listFiles( fileFilter );

					if ( files != null && files.length > 0 )
						for ( File f : files )
							if ( f.exists() && !f.isDirectory() )
							{
								String destFileName = dest.getName();
								String fileNameFull = f.getName();
								String fileName = fileNameFull.contains( "." ) ? fileNameFull.substring( 0, fileNameFull.lastIndexOf( "." ) ) : fileNameFull;
								String ext = fileNameFull.contains( "." ) ? fileNameFull.substring( fileNameFull.lastIndexOf( "." ) + 1 ) : "";
								String ext2 = destFileName.contains( "." ) ? destFileName.substring( destFileName.lastIndexOf( "." ) + 1 ) : "";

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

			wasSuccessful = dest.exists();
		}

		if ( wasSuccessful )
		{
			if ( dest != null && dest.exists() )
				interpretParamsFromFile( dest );

			if ( dest != null && !dest.exists() )
			{
				NetworkManager.getLogger().warning( "We tried to load the file `" + dest.getAbsolutePath() + "` but could not find it, will throw a 404 error now." );
				// TODO Give detailed error about the missing file
				status = HttpResponseStatus.NOT_FOUND;
			}
		}
		else
			status = HttpResponseStatus.NOT_FOUND;
	}

	public String getHTML()
	{
		return html;
	}

	public Map<String, String> getRewriteParams()
	{
		return rewriteParams;
	}

	public HttpResponseStatus getStatus()
	{
		return status;
	}

	public boolean hasHTML()
	{
		return html != null;
	}

	public boolean isDirectoryRequest()
	{
		return isDirectoryRequest;
	}

	public boolean isFrameworkRequest()
	{
		return fwRequest;
	}

	@Override
	public String toString()
	{
		String overrides = "";

		for ( Entry<String, String> o : annotations.entrySet() )
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
			rewrites += "," + o.getKey() + "=" + o.getValue();

		if ( rewrites.length() > 1 )
			rewrites = rewrites.substring( 1 );

		// String cachedFileStr = ( cachedFile == null ) ? "N/A" : cachedFile.getAbsolutePath();

		return "WebInterpreter[content=" + data.writerIndex() + " bytes,contentType=" + getContentType() + ",overrides=[" + overrides + "],rewrites=[" + rewrites + "]]";
	}
}
