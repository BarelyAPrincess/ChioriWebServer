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

import java.nio.charset.Charset;
import java.util.List;

import com.chiorichan.lang.ReportingLevel;
import com.chiorichan.lang.EvalException;
import com.chiorichan.util.ObjectFunc;
import com.google.common.collect.Lists;

/**
 * Contains the end result of {@link ScriptingFactory#eval(ScriptingContext)}
 */
public class ScriptingResult
{
	private final List<EvalException> caughtExceptions = Lists.newArrayList();
	private boolean success = false;
	private String reason = null;
	private ByteBuf content;
	private Object obj = null;
	private final ScriptingContext context;
	
	ScriptingResult( ScriptingContext context, ByteBuf content )
	{
		this.context = context;
		this.content = content;
	}
	
	public ScriptingResult addException( ReportingLevel level, Throwable throwable )
	{
		if ( throwable != null )
			caughtExceptions.add( new EvalException( level, throwable ).populateScriptTrace( context.factory().stack() ) );
		return this;
	}
	
	public ScriptingResult addException( EvalException exception )
	{
		if ( exception != null )
		{
			// If this EvalException never had it's script trace populated, we handle it here
			if ( !exception.hasScriptTrace() )
				if ( context.factory() != null )
					exception.populateScriptTrace( context.factory().stack() );
				else if ( context.request() != null )
					exception.populateScriptTrace( context.request().getEvalFactory().stack() );
			caughtExceptions.add( exception );
		}
		return this;
	}
	
	public ByteBuf content()
	{
		return content;
	}
	
	public ScriptingContext context()
	{
		return context;
	}
	
	public EvalException[] getExceptions()
	{
		return caughtExceptions.toArray( new EvalException[0] );
	}
	
	public EvalException[] getIgnorableExceptions()
	{
		List<EvalException> exs = Lists.newArrayList();
		for ( EvalException e : caughtExceptions )
			if ( e.isIgnorable() )
				exs.add( e );
		return exs.toArray( new EvalException[0] );
	}
	
	public EvalException[] getNotIgnorableExceptions()
	{
		List<EvalException> exs = Lists.newArrayList();
		for ( EvalException e : caughtExceptions )
			if ( !e.isIgnorable() )
				exs.add( e );
		return exs.toArray( new EvalException[0] );
	}
	
	public Object getObject()
	{
		return obj;
	}
	
	public String getReason()
	{
		if ( reason == null || reason.isEmpty() )
			reason = "There was no available result reason at this time.";
		
		return reason;
	}
	
	public String getString()
	{
		return getString( false );
	}
	
	public String getString( boolean includeObj )
	{
		return ( ( content == null ) ? "" : content.toString( Charset.defaultCharset() ) ) + ( ( includeObj ) ? ObjectFunc.castToString( obj ) : "" );
	}
	
	public boolean hasExceptions()
	{
		return !caughtExceptions.isEmpty();
	}
	
	public boolean hasIgnorableExceptions()
	{
		for ( EvalException e : caughtExceptions )
			if ( e.isIgnorable() )
				return true;
		return false;
	}
	
	public boolean hasNotIgnorableExceptions()
	{
		for ( EvalException e : caughtExceptions )
			if ( !e.isIgnorable() )
				return true;
		return false;
	}
	
	public boolean isSuccessful()
	{
		return success;
	}
	
	public void object( Object obj )
	{
		this.obj = obj;
	}
	
	public ScriptingResult setReason( String reason )
	{
		this.reason = reason;
		return this;
	}
	
	public ScriptingResult success( boolean success )
	{
		this.success = success;
		return this;
	}
	
	@Override
	public String toString()
	{
		return String.format( "EvalFactoryResult{success=%s,reason=%s,size=%s,obj=%s,context=%s}", success, reason, content.writerIndex(), obj, context );
	}
}
