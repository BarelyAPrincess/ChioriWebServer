/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.factory;

import com.chiorichan.framework.Site;

/**
 * Holds the result after evaling block of code or a file
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class EvalFactoryResult
{
	boolean success = false;
	String reason = null;
	String code = null;
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
	
	public EvalFactoryResult setResult( String code )
	{
		this.code = code;
		return this;
	}
	
	public EvalFactoryResult setResult( boolean success )
	{
		this.success = success;
		return this;
	}
	
	public EvalFactoryResult setResult( String code, boolean success )
	{
		this.code = code;
		this.success = success;
		return this;
	}
	
	public EvalMetaData getMeta()
	{
		return meta;
	}
	
	public String getResult()
	{
		return code;
	}
	
	public boolean isSuccessful()
	{
		return success;
	}
	
	@Override
	public String toString()
	{
		return code;
	}
}
