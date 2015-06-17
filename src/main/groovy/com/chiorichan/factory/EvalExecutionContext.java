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

import java.io.ByteArrayOutputStream;
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
	private EvalFactoryResult result = null;
	private HttpRequestWrapper request = null;
	private ByteBuf content = Unpooled.buffer();
	private Charset charset = Charset.defaultCharset();
	
	private String contentType;
	private String filename;
	private String scriptName;
	private Script script;
	private String source = null;
	
	private String shell = "embedded";
	private Site site;
	
	private EvalExecutionContext()
	{
		
	}
	
	public static EvalExecutionContext fromFile( final FileInterpreter fi )
	{
		EvalExecutionContext request = fromSource( fi.consumeBytes(), fi.getFilePath() );
		
		request.contentType = fi.getContentType();
		request.shell = fi.getParams().get( "shell" );
		
		return request;
	}
	
	public static EvalExecutionContext fromFile( final File file ) throws IOException
	{
		return fromSource( FileUtils.readFileToByteArray( file ), file.getAbsolutePath() );
		// EvalException.exceptionHandler( e, shellFactory, result, ErrorReporting.E_WARNING, String.format( "Exception caught while trying to read file '%s' from disk", fi.getAbsolutePath() ) );
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
	
	public ByteBuf buffer()
	{
		return content;
	}
	
	public String readString()
	{
		return content.toString( charset );
	}
	
	public String readString( Charset charset )
	{
		return content.toString( charset );
	}
	
	public byte[] readBytes()
	{
		int inx = content.readerIndex();
		byte[] bytes = new byte[content.readableBytes()];
		content.readBytes( bytes );
		content.readerIndex( inx );
		return bytes;
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
	
	public void write( byte... bytes )
	{
		content.writeBytes( bytes );
	}
	
	public void resetAndWrite( byte... bytes )
	{
		if ( bytes.length < 1 )
			return;
		reset();
		write( bytes );
	}
	
	public void resetAndWrite( String str )
	{
		if ( str == null )
			return;
		resetAndWrite( str.getBytes( charset ) );
	}
	
	public String filename()
	{
		return filename;
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
	
	public Site site()
	{
		return site == null ? SiteManager.INSTANCE.getDefaultSite() : site;
	}
	
	public EvalExecutionContext site( Site site )
	{
		this.site = site;
		return this;
	}
	
	public EvalExecutionContext shell( final String shell )
	{
		this.shell = shell;
		return this;
	}
	
	public String shell()
	{
		return shell;
	}
	
	public boolean prepare( GroovyShell shell )
	{
		if ( contentType() == null && filename() != null )
			contentType( ContentTypes.getContentType( filename() ) );
		else
			contentType( shell() );
		
		shell.setVariable( "__FILE__", filename );
		
		return true;
	}
	
	@Override
	public String toString()
	{
		return String.format( "EvalExecutionContext {filename=%s,scriptName=%s,script=%s,shell=%s,sourceSize=%s,contentType=%s}", filename, scriptName, script, shell, content.readableBytes(), contentType );
	}
	
	public EvalExecutionContext request( HttpRequestWrapper request )
	{
		this.request = request;
		return this;
	}
	
	public HttpRequestWrapper request()
	{
		return request;
	}
	
	public void script( String scriptName, Script script )
	{
		this.scriptName = scriptName;
		this.script = script;
	}
	
	public Script script()
	{
		return script;
	}
	
	public String scriptName()
	{
		return scriptName;
	}
	
	public EvalFactoryResult result()
	{
		if ( result == null )
			result = new EvalFactoryResult( this, content );
		return result;
	}
	
	public EvalExecutionContext baseSource( String source )
	{
		this.source = source;
		return this;
	}
	
	public String baseSource()
	{
		return source;
	}
	
	/**
	 * Used internally to preserve previously evaluated output without redirecting the output stream
	 */
	private byte[] saved;
	
	/**
	 * Used internally to preserve previously evaluated output without redirecting the output stream
	 */
	void internalEvalBegin( ByteArrayOutputStream bs )
	{
		saved = bs.toByteArray();
		bs.reset();
	}
	
	/**
	 * Used internally to preserve previously evaluated output without redirecting the output stream
	 */
	void internalEvalEnd( ByteArrayOutputStream bs ) throws IOException
	{
		resetAndWrite( bs.toByteArray() );
		bs.reset();
		bs.write( saved );
		saved = null;
	}
	
	void charset( Charset charset )
	{
		this.charset = charset;
	}
	
	Charset charset()
	{
		return charset;
	}
}
