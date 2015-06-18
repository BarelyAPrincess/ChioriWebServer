/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.factory;

import groovy.lang.GroovyShell;
import groovy.lang.Script;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;

import com.chiorichan.ContentTypes;
import com.chiorichan.http.HttpRequestWrapper;
import com.chiorichan.site.Site;
import com.chiorichan.site.SiteManager;

/**
 * Provides the context to a requested eval of the EvalFactory
 * 
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
public class EvalExecutionContext
{
	private Charset charset = Charset.defaultCharset();
	
	private ByteBuf content = Unpooled.buffer();
	
	private String contentType;
	
	private EvalFactory factory;
	
	private String filename;
	
	private HttpRequestWrapper request = null;
	
	private EvalFactoryResult result = null;
	
	private Script script;
	
	private String scriptName;
	
	private String shell = "embedded";
	
	private Site site;
	
	private String source = null;
	
	private EvalExecutionContext()
	{
		
	}
	
	public static EvalExecutionContext fromFile( final File file ) throws IOException
	{
		return fromSource( FileUtils.readFileToByteArray( file ), file.getAbsolutePath() );
		// EvalException.exceptionHandler( e, shellFactory, result, ErrorReporting.E_WARNING, String.format( "Exception caught while trying to read file '%s' from disk", fi.getAbsolutePath() ) );
	}
	
	public static EvalExecutionContext fromFile( final FileInterpreter fi )
	{
		EvalExecutionContext context = fromSource( fi.consumeBytes(), fi.getFilePath() );
		context.contentType = fi.getContentType();
		context.shell = fi.getParams().get( "shell" );
		return context;
	}
	
	public static EvalExecutionContext fromSource( byte[] source )
	{
		return fromSource( source, "<no file>" );
	}
	
	public static EvalExecutionContext fromSource( final byte[] source, final String filename )
	{
		EvalExecutionContext context = new EvalExecutionContext();
		context.filename = filename;
		context.write( source );
		context.baseSource( new String( source, context.charset ) );
		return context;
	}
	
	public static EvalExecutionContext fromSource( String source )
	{
		return fromSource( source, "" );
	}
	
	public static EvalExecutionContext fromSource( final String source, final String filename )
	{
		EvalExecutionContext context = fromSource( new byte[0], filename );
		context.write( source.getBytes( context.charset ) );
		return context;
	}
	
	public String baseSource()
	{
		return source;
	}
	
	public EvalExecutionContext baseSource( String source )
	{
		this.source = source;
		return this;
	}
	
	public ByteBuf buffer()
	{
		return content;
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
	
	public EvalExecutionContext contentType( final String contentType )
	{
		this.contentType = contentType;
		return this;
	}
	
	public EvalFactory factory()
	{
		return factory;
	}
	
	EvalExecutionContext factory( final EvalFactory factory )
	{
		this.factory = factory;
		return this;
	}
	
	public String filename()
	{
		return filename;
	}
	
	public boolean prepare( GroovyShell shell )
	{
		if ( contentType() == null && filename() != null )
			contentType( ContentTypes.getContentType( filename() ) );
		
		shell.setVariable( "__FILE__", filename );
		
		return true;
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
	
	public EvalExecutionContext request( HttpRequestWrapper request )
	{
		this.request = request;
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
	
	public EvalFactoryResult result()
	{
		if ( result == null )
			result = new EvalFactoryResult( this, content );
		return result;
	}
	
	public Script script()
	{
		return script;
	}
	
	public void script( String scriptName, Script script )
	{
		this.scriptName = scriptName;
		this.script = script;
	}
	
	public String scriptName()
	{
		return scriptName;
	}
	
	public String shell()
	{
		return shell;
	}
	
	public EvalExecutionContext shell( final String shell )
	{
		this.shell = shell;
		return this;
	}
	
	public Site site()
	{
		return site == null ? SiteManager.INSTANCE.getDefaultSite() : site;
	}
	
	public EvalExecutionContext site( Site site )
	{
		this.site = site;
		return this;
	}
	
	@Override
	public String toString()
	{
		return String.format( "EvalExecutionContext {filename=%s,scriptName=%s,script=%s,shell=%s,sourceSize=%s,contentType=%s}", filename, scriptName, script, shell, content.readableBytes(), contentType );
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
