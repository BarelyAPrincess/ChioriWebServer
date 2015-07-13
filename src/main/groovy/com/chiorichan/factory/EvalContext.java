/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.factory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import com.chiorichan.ContentTypes;
import com.chiorichan.http.HttpRequestWrapper;
import com.chiorichan.lang.ErrorReporting;
import com.chiorichan.lang.EvalException;
import com.chiorichan.lang.EvalMultipleException;
import com.chiorichan.site.Site;
import com.chiorichan.site.SiteManager;
import com.chiorichan.util.StringFunc;

/**
 * Provides the context to a requested eval of the EvalFactory
 */
public class EvalContext
{
	private Charset charset = Charset.defaultCharset();
	private ByteBuf content = Unpooled.buffer();
	private String contentType;
	private EvalFactory factory;
	private String filename;
	private HttpRequestWrapper request = null;
	private EvalResult result = null;
	private String name;
	private String shell = "embedded";
	private Site site;
	private String source = null;
	private boolean required = false;
	
	private EvalContext()
	{
		
	}
	
	public static EvalContext fromAuto( final Site site, final String res )
	{
		// Might need a better attempt at auto determining file types
		EvalContext context = fromFile( site, res );
		if ( context.result().hasExceptions() )
		{
			if ( res.contains( "." ) )
				return EvalContext.fromPackage( site, res );
			
			context = new EvalContext();
			context.result().addException( new EvalException( ErrorReporting.E_ERROR, String.format( "We chould not auto determine the resource type for '%s'", res ) ) );
			return context;
		}
		return context;
	}
	
	static EvalContext fromFile( final File file )
	{
		try
		{
			return fromFile( new FileInterpreter( file ) );
		}
		catch ( IOException e )
		{
			EvalContext context = new EvalContext();
			EvalException.exceptionHandler( e, context );
			return context;
		}
	}
	
	public static EvalContext fromFile( final FileInterpreter fi )
	{
		EvalContext context = fromSource( fi.consumeBytes(), fi.getFilePath() );
		context.contentType = fi.getContentType();
		context.shell = fi.getParams().get( "shell" );
		return context;
	}
	
	public static EvalContext fromFile( final Site site, final String file )
	{
		// We block absolute file paths for both unix-like and windows
		if ( file.startsWith( File.separator ) || file.matches( "[A-Za-z]:\\.*" ) )
			throw new SecurityException( "To protect system resource, this page has been blocked from accessing an absolute file path." );
		if ( file.startsWith( ".." + File.separator ) )
			throw new SecurityException( "To protect system resource, this page has been blocked from accessing a protected file path." );
		return fromFile( new File( site.getAbsoluteRoot(), file ) );
	}
	
	public static EvalContext fromPackage( final Site site, final String pack )
	{
		EvalContext context = null;
		
		try
		{
			File packFile = site.getResourceWithException( pack );
			FileInterpreter fi = new FileInterpreter( packFile );
			
			context = EvalContext.fromFile( fi );
		}
		catch ( IOException e )
		{
			context = EvalContext.fromSource( "", pack );
			context.result().addException( new EvalException( ErrorReporting.E_IGNORABLE, String.format( "Could not locate the package '%s' within site '%s'", pack, site.getSiteId() ) ) );
		}
		
		context.site( site );
		
		return context;
	}
	
	public static EvalContext fromSource( byte[] source )
	{
		return fromSource( source, "<no file>" );
	}
	
	public static EvalContext fromSource( final byte[] source, final File file )
	{
		return fromSource( source, file.getAbsolutePath() );
	}
	
	public static EvalContext fromSource( final byte[] source, final String filename )
	{
		EvalContext context = new EvalContext();
		context.filename = filename;
		context.write( source );
		context.baseSource( new String( source, context.charset ) );
		return context;
	}
	
	public static EvalContext fromSource( String source )
	{
		return fromSource( source, "" );
	}
	
	public static EvalContext fromSource( final String source, final File file )
	{
		return fromSource( source, file.getAbsolutePath() );
	}
	
	public static EvalContext fromSource( final String source, final String filename )
	{
		EvalContext context = fromSource( new byte[0], filename );
		context.write( source.getBytes( context.charset ) );
		return context;
	}
	
	public String baseSource()
	{
		return source;
	}
	
	public EvalContext baseSource( String source )
	{
		this.source = source;
		return this;
	}
	
	public ByteBuf buffer()
	{
		return content;
	}
	
	public String bufferHash()
	{
		return StringFunc.md5( readBytes() );
	}
	
	Charset charset()
	{
		return charset;
	}
	
	void charset( Charset charset )
	{
		this.charset = charset;
	}
	
	public String contentType()
	{
		return contentType;
	}
	
	public EvalContext contentType( final String contentType )
	{
		this.contentType = contentType;
		return this;
	}
	
	public Object eval() throws EvalException, EvalMultipleException
	{
		if ( request == null && factory == null )
			throw new IllegalArgumentException( "We can't eval() this EvalContext until you provide either the request or the factory." );
		if ( request != null && factory == null )
			factory = request.getEvalFactory();
		
		result = factory.eval( this );
		
		String str = result.getString( false );
		
		if ( required && result.hasNotIgnorableExceptions() )
			ErrorReporting.throwExceptions( result.getExceptions() );
		if ( result.hasIgnorableExceptions() )
			str = ErrorReporting.printExceptions( result.getIgnorableExceptions() ) + "\n" + str;
		
		factory.print( str );
		return result.getObject();
	}
	
	public EvalFactory factory()
	{
		return factory;
	}
	
	EvalContext factory( final EvalFactory factory )
	{
		this.factory = factory;
		
		if ( contentType() == null && filename() != null )
			contentType( ContentTypes.getContentType( filename() ) );
		
		return this;
	}
	
	public String filename()
	{
		return filename;
	}
	
	public String name()
	{
		return name;
	}
	
	public EvalContext name( String name )
	{
		this.name = name;
		return this;
	}
	
	public String read() throws EvalException, EvalMultipleException
	{
		return read( false, true );
	}
	
	public String read( boolean printErrors ) throws EvalException, EvalMultipleException
	{
		return read( false, printErrors );
	}
	
	public String read( boolean includeObj, boolean printErrors ) throws EvalException, EvalMultipleException
	{
		EvalResult result = null;
		if ( request != null )
			result = request.getEvalFactory().eval( this );
		else if ( factory != null )
			result = factory.eval( this );
		else
			throw new IllegalArgumentException( "We can't read() this EvalContext until you provide either the request or the factory." );
		
		String str = result.getString( includeObj );
		
		if ( required && result.hasNotIgnorableExceptions() )
			ErrorReporting.throwExceptions( result.getExceptions() );
		if ( printErrors && result.hasIgnorableExceptions() )
			str = ErrorReporting.printExceptions( result.getIgnorableExceptions() ) + "\n" + str;
		
		return str;
	}
	
	public byte[] readBytes()
	{
		int inx = content.readerIndex();
		byte[] bytes = new byte[content.readableBytes()];
		content.readBytes( bytes );
		content.readerIndex( inx );
		return bytes;
	}
	
	public String readString()
	{
		return content.toString( charset );
	}
	
	public String readString( Charset charset )
	{
		return content.toString( charset );
	}
	
	public HttpRequestWrapper request()
	{
		return request;
	}
	
	public EvalContext request( HttpRequestWrapper request )
	{
		this.request = request;
		return this;
	}
	
	public EvalContext require()
	{
		required = true;
		return this;
	}
	
	public EvalContext require( boolean required )
	{
		this.required = required;
		return this;
	}
	
	/**
	 * Attempts to erase the entire ByteBuf content
	 */
	public void reset()
	{
		int size = content.writerIndex();
		content.clear();
		content.writeBytes( new byte[size] );
		content.clear();
	}
	
	public void resetAndWrite( byte... bytes )
	{
		reset();
		if ( bytes.length < 1 )
			return;
		write( bytes );
	}
	
	public void resetAndWrite( ByteBuf source )
	{
		reset();
		if ( source == null )
			return;
		write( source );
	}
	
	public void resetAndWrite( String str )
	{
		reset();
		if ( str == null )
			return;
		write( str.getBytes( charset ) );
	}
	
	public EvalResult result()
	{
		if ( result == null )
			result = new EvalResult( this, content );
		return result;
	}
	
	public String shell()
	{
		return shell;
	}
	
	public EvalContext shell( String shell )
	{
		this.shell = shell;
		return this;
	}
	
	public Site site()
	{
		return site == null ? SiteManager.INSTANCE.getDefaultSite() : site;
	}
	
	public EvalContext site( Site site )
	{
		this.site = site;
		return this;
	}
	
	@Override
	public String toString()
	{
		return String.format( "EvalExecutionContext {name=%s,filename=%s,shell=%s,sourceSize=%s,contentType=%s}", name, filename, shell, content.readableBytes(), contentType );
	}
	
	public void write( byte... bytes )
	{
		content.writeBytes( bytes );
	}
	
	public void write( ByteBuf source )
	{
		content.writeBytes( source );
	}
}
