/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.factory;

import io.netty.buffer.ByteBuf;

import java.nio.charset.Charset;
import java.util.List;

import com.chiorichan.site.Site;
import com.chiorichan.util.ObjectUtil;
import com.google.common.collect.Lists;

/**
 * Holds the result after evaling block of code or a file
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class EvalFactoryResult
{
	List<Exception> caughtExceptions = Lists.newArrayList();
	boolean success = false;
	String reason = null;
	ByteBuf buf = null;
	Object obj = null;
	EvalMetaData meta;
	Site site;
	
	public EvalFactoryResult( EvalMetaData meta, Site site )
	{
		this.meta = meta;
		this.site = site;
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
	
	public EvalFactoryResult setResult( ByteBuf buf, boolean success )
	{
		this.buf = buf;
		this.success = success;
		return this;
	}
	
	public EvalMetaData getMeta()
	{
		return meta;
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
		return ( ( buf == null ) ? "" : buf.toString( Charset.defaultCharset() ) ) + ( ( includeObj ) ? ObjectUtil.castToString( obj ) : "" );
	}
	
	public Object getObject()
	{
		return obj;
	}
	
	public ByteBuf getResult()
	{
		return buf;
	}
	
	@Override
	public String toString()
	{
		return "EvalFactoryResult{success=" + success + ",reason=" + reason + ",size=" + buf.writerIndex() + ",obj=" + obj + ",meta=" + meta + ",site=" + site + "}";
	}
	
	public void addException( Exception exception )
	{
		caughtExceptions.add( exception );
	}
	
	public Exception[] getExceptions()
	{
		return caughtExceptions.toArray( new Exception[0] );
	}
	
	public boolean hasExceptions()
	{
		return !caughtExceptions.isEmpty();
	}
}
