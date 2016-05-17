/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2016 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.factory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import com.chiorichan.AppConfig;
import com.chiorichan.ContentTypes;
import com.chiorichan.http.HttpRequestWrapper;
import com.chiorichan.lang.ExceptionContext;
import com.chiorichan.lang.ExceptionReport;
import com.chiorichan.lang.MultipleException;
import com.chiorichan.lang.ReportingLevel;
import com.chiorichan.lang.ScriptingException;
import com.chiorichan.site.Site;
import com.chiorichan.site.SiteManager;
import com.chiorichan.util.SecureFunc;

/**
 * Provides the context to a requested eval of the EvalFactory
 */
public class ScriptingContext implements ExceptionContext
{
	public static ScriptingContext fromAuto( final Site site, final String res )
	{
		// Might need a better attempt at auto determining file types
		// File types meaning located in public webroot verses resource

		ScriptingContext context = null;
		context = fromFile( site, res );
		if ( context == null || context.result().hasExceptions() )
		{
			if ( res.contains( "." ) )
				return ScriptingContext.fromPackage( site, res );

			context = new ScriptingContext();
			context.result().addException( new ScriptingException( ReportingLevel.E_ERROR, String.format( "We chould not auto determine the resource type for '%s'", res ) ) );
			return context;
		}
		return context;
	}

	static ScriptingContext fromFile( final File file )
	{
		try
		{
			return fromFile( new FileInterpreter( file ) );
		}
		catch ( IOException e )
		{
			ScriptingContext context = new ScriptingContext();
			context.result.handleException( e, context );
			return context;
		}
	}

	public static ScriptingContext fromFile( final FileInterpreter fi )
	{
		ScriptingContext context = fromSource( fi.consumeBytes(), fi.getFilePath() );
		context.contentType = fi.getContentType();
		context.shell = fi.getAnnotations().get( "shell" );
		return context;
	}

	public static ScriptingContext fromFile( final Site site, final String file )
	{
		// We block absolute file paths for both unix-like and windows
		if ( file.startsWith( File.separator ) || file.matches( "[A-Za-z]:\\.*" ) )
			throw new SecurityException( "To protect system resources, this page has been blocked from accessing an absolute file path." );
		if ( file.startsWith( ".." + File.separator ) )
			throw new SecurityException( "To protect system resources, this page has been blocked from accessing a protected file path." );
		try
		{
			return fromFile( site.resourceFile( file ) );
		}
		catch ( IOException e )
		{
			ScriptingContext context = ScriptingContext.fromSource( "", file );
			context.result().addException( new ScriptingException( ReportingLevel.E_IGNORABLE, String.format( "Could not locate the file '%s' within site '%s'", file, site.getId() ), e ) );
			context.site( site );
			return context;
		}
	}

	public static ScriptingContext fromPackage( final Site site, final String pack )
	{
		ScriptingContext context = null;

		try
		{
			File packFile = site.resourcePackage( pack );
			FileInterpreter fi = new FileInterpreter( packFile );

			context = ScriptingContext.fromFile( fi );
		}
		catch ( IOException e )
		{
			context = ScriptingContext.fromSource( "", pack );
			context.result().addException( new ScriptingException( ReportingLevel.E_IGNORABLE, String.format( "Could not locate the package '%s' within site '%s'", pack, site.getId() ), e ) );
		}

		context.site( site );

		return context;
	}

	public static ScriptingContext fromSource( byte[] source )
	{
		return fromSource( source, "<no file>" );
	}

	public static ScriptingContext fromSource( final byte[] source, final File file )
	{
		return fromSource( source, file.getAbsolutePath() );
	}

	public static ScriptingContext fromSource( final byte[] source, final String filename )
	{
		ScriptingContext context = new ScriptingContext();
		context.filename = filename;
		context.write( source );
		context.baseSource( new String( source, context.charset ) );
		return context;
	}

	public static ScriptingContext fromSource( String source )
	{
		return fromSource( source, "" );
	}

	public static ScriptingContext fromSource( final String source, final File file )
	{
		return fromSource( source, file.getAbsolutePath() );
	}

	public static ScriptingContext fromSource( final String source, final String filename )
	{
		ScriptingContext context = fromSource( new byte[0], filename );
		context.write( source.getBytes( context.charset ) );
		return context;
	}

	public static List<String> getPreferredExtensions()
	{
		return AppConfig.get().getStringList( "advanced.scripting.preferredExtensions", Arrays.asList( "html", "htm", "groovy", "gsp", "jsp", "chi" ) );
	}

	private Charset charset = Charset.defaultCharset();

	private ByteBuf content = Unpooled.buffer();

	private String contentType;

	private ScriptingFactory factory;

	private String filename;

	private HttpRequestWrapper request = null;

	private ScriptingResult result = null;

	private String name;

	private String shell = "embedded";

	private Site site;

	private String source = null;

	private boolean required = false;

	private ScriptingContext()
	{

	}

	public String baseSource()
	{
		return source;
	}

	public ScriptingContext baseSource( String source )
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
		return SecureFunc.md5( readBytes() );
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

	public ScriptingContext contentType( final String contentType )
	{
		this.contentType = contentType;
		return this;
	}

	public Object eval() throws ScriptingException, MultipleException
	{
		if ( request == null && factory == null )
			throw new IllegalArgumentException( "We can't eval() this EvalContext until you provide either the request or the factory." );
		if ( request != null && factory == null )
			factory = request.getEvalFactory();

		result = factory.eval( this );

		String str = result.getString( false );

		if ( required && result.hasNonIgnorableExceptions() )
			try
			{
				ExceptionReport.throwExceptions( result.getExceptions() );
			}
			catch ( Throwable e )
			{
				if ( e instanceof ScriptingException )
					throw ( ScriptingException ) e;
				if ( e instanceof MultipleException )
					throw ( MultipleException ) e;
				else
					throw new IllegalStateException( "Well this was unexpected, we should only be throwing ScriptingExceptions here!", e );
			}

		if ( result.hasIgnorableExceptions() )
			str = ExceptionReport.printExceptions( result.getIgnorableExceptions() ) + "\n" + str;

		factory.print( str );
		return result.getObject();
	}

	public ScriptingFactory factory()
	{
		return factory;
	}

	ScriptingContext factory( final ScriptingFactory factory )
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

	public ScriptingContext name( String name )
	{
		this.name = name;
		return this;
	}

	public String read() throws ScriptingException, MultipleException
	{
		return read( false, true );
	}

	public String read( boolean printErrors ) throws ScriptingException, MultipleException
	{
		return read( false, printErrors );
	}

	public String read( boolean includeObj, boolean printErrors ) throws ScriptingException, MultipleException
	{
		ScriptingResult result = null;
		if ( request != null )
			result = request.getEvalFactory().eval( this );
		else if ( factory != null )
			result = factory.eval( this );
		else
			throw new IllegalArgumentException( "We can't read() this EvalContext until you provide either the request or the factory." );

		String str = result.getString( includeObj );

		if ( required && result.hasNonIgnorableExceptions() )
			try
			{
				ExceptionReport.throwExceptions( result.getExceptions() );
			}
			catch ( Throwable e )
			{
				if ( e instanceof ScriptingException )
					throw ( ScriptingException ) e;
				if ( e instanceof MultipleException )
					throw ( MultipleException ) e;
				else
					throw new IllegalStateException( "Well this was unexpected, we should only be throwing ScriptingExceptions here!", e );
			}
		if ( printErrors && result.hasIgnorableExceptions() )
			str = ExceptionReport.printExceptions( result.getIgnorableExceptions() ) + "\n" + str;

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

	public ScriptingContext request( HttpRequestWrapper request )
	{
		this.request = request;
		return this;
	}

	/**
	 * Ups the priority of this context from failing to REQUIRED
	 *
	 * @return this object
	 */
	public ScriptingContext require()
	{
		required = true;
		return this;
	}

	/**
	 * Toggles the priority of this context to/from REQUIRED
	 *
	 * @return this object
	 */
	public ScriptingContext require( boolean required )
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

	public ScriptingResult result()
	{
		if ( result == null )
			result = new ScriptingResult( this, content );
		return result;
	}

	public String shell()
	{
		return shell;
	}

	public ScriptingContext shell( String shell )
	{
		this.shell = shell;
		return this;
	}

	public Site site()
	{
		return site == null ? SiteManager.instance().getDefaultSite() : site;
	}

	public ScriptingContext site( Site site )
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
