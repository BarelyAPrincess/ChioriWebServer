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

import com.chiorichan.lang.ErrorReporting;
import com.chiorichan.lang.EvalException;
import com.chiorichan.util.ObjectFunc;
import com.google.common.collect.Lists;

/**
 * Contains the end result of {@link EvalFactory#eval(EvalExecutionContext)}
 */
public class EvalFactoryResult
{
	private final List<EvalException> caughtExceptions = Lists.newArrayList();
	private boolean success = false;
	private String reason = null;
	private ByteBuf content;
	private Object obj = null;
	private final EvalExecutionContext context;
	
	EvalFactoryResult( EvalExecutionContext context, ByteBuf content )
	{
		this.context = context;
		this.content = content;
	}
	
	public EvalFactoryResult setReason( String reason )
	{
		this.reason = reason;
		return this;
	}
	
	public String getReason()
	{
		if ( reason == null || reason.isEmpty() )
			reason = "There was no available result reason at this time.";
		
		return reason;
	}
	
	public EvalFactoryResult success( boolean success )
	{
		this.success = success;
		return this;
	}
	
	public EvalExecutionContext context()
	{
		return context;
	}
	
	public boolean isSuccessful()
	{
		return success;
	}
	
	public String getString()
	{
		return getString( false );
	}
	
	public String getString( boolean includeObj )
	{
		return ( ( content == null ) ? "" : content.toString( Charset.defaultCharset() ) ) + ( ( includeObj ) ? ObjectFunc.castToString( obj ) : "" );
	}
	
	public Object getObject()
	{
		return obj;
	}
	
	public ByteBuf content()
	{
		return content;
	}
	
	public void object( Object obj )
	{
		this.obj = obj;
	}
	
	@Override
	public String toString()
	{
		return String.format( "EvalFactoryResult{success=%s,reason=%s,size=%s,obj=%s,context=%s}", success, reason, content.writerIndex(), obj, context );
	}
	
	public void addException( EvalException exception )
	{
		if ( exception != null )
			caughtExceptions.add( exception );
	}
	
	public void addException( ErrorReporting level, Throwable throwable, ShellFactory factory )
	{
		if ( throwable != null )
			caughtExceptions.add( new EvalException( level, throwable, factory ) );
	}
	
	public EvalException[] getIgnorableExceptions()
	{
		List<EvalException> exs = Lists.newArrayList();
		for ( EvalException e : caughtExceptions )
			if ( e.isIgnorable() )
				exs.add( e );
		return exs.toArray( new EvalException[0] );
	}
	
	public EvalException[] getExceptions()
	{
		return caughtExceptions.toArray( new EvalException[0] );
	}
	
	public boolean hasIgnorableExceptions()
	{
		for ( EvalException e : caughtExceptions )
			if ( e.isIgnorable() )
				return true;
		return false;
	}
	
	public boolean hasExceptions()
	{
		return !caughtExceptions.isEmpty();
	}
}
