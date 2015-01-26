package com.chiorichan.http;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

import com.chiorichan.ContentTypes;
import com.chiorichan.exception.HttpErrorException;
import com.chiorichan.framework.WebUtils;
import com.chiorichan.util.Versioning;
import com.google.common.collect.Lists;

public class DirectoryInterpreter
{
	public void processDirectoryListing( HttpHandler http, WebInterpreter fi ) throws HttpErrorException, IOException
	{
		File dir = fi.getFile();
		
		if ( !dir.exists() || !dir.isDirectory() )
			throw new HttpErrorException( 500 );
		
		HttpResponseWrapper response = http.getResponse();
		
		response.setContentType( "text/html" );
		response.setEncoding( "utf-8" );
		
		File[] files = dir.listFiles();
		List<Object> tbl = Lists.newArrayList();
		StringBuilder sb = new StringBuilder();
		SimpleDateFormat sdf = new SimpleDateFormat( "dd-MMM-yyyy HH:mm:ss" );
		
		sb.append( "<h1>Index of " + http.getRequest().getURI() + "</h1>" );
		
		for ( File f : files )
		{
			List<String> l = Lists.newArrayList();
			String type = ContentTypes.getContentType( f );
			
			l.add( "<img src=\"/fw/icons/" + type + "\" />" );
			l.add( f.getName() );
			l.add( sdf.format( f.lastModified() ) );
			
			InputStream stream = null;
			try
			{
				URL url = f.toURI().toURL();
				stream = url.openStream();
				l.add( String.valueOf( stream.available() ) + "kb" );
			}
			finally
			{
				if ( stream != null )
					stream.close();
			}
			
			l.add( type );
			
			tbl.add( l );
		}
		
		sb.append( WebUtils.createTable( tbl, Arrays.asList( new String[] {"", "Name", "Last Modified", "Size", "Type"} ) ) );
		sb.append( "<hr>" );
		sb.append( "<small>Running <a href=\"https://github.com/ChioriGreene/ChioriWebServer\">" + Versioning.getProduct() + "</a> Version " + Versioning.getVersion() + "<br />" + Versioning.getCopyright() + "</small>" );
		
		response.print( sb.toString() );
		response.sendResponse();
		
		// throw new HttpErrorException( 403, "Sorry, Directory Listing has not been implemented on this Server!" );
	}
}
