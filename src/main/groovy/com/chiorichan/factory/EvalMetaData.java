/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.factory;

import groovy.lang.Script;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import com.chiorichan.site.Site;
import com.chiorichan.util.ObjectUtil;
import com.google.common.collect.Maps;

public class EvalMetaData
{
	public Map<String, Object> params = Maps.newLinkedHashMap();
	public String contentType;
	public String fileName;
	public String scriptName;
	public String source;
	public String shell;
	public Script script;
	public Site site;
	
	public EvalMetaData()
	{
		
	}
	
	public EvalMetaData( FileInterpreter fi )
	{
		contentType = fi.getContentType();
		shell = fi.getParams().get( "shell" );
		fileName = ( fi.getFile() != null ) ? fi.getFile().getAbsolutePath() : fi.getParams().get( "file" );
	}
	
	public EvalMetaData( String file ) throws IOException
	{
		this( new FileInterpreter( new File( file ) ) );
	}
	
	public Map<String, Object> getParams()
	{
		return params;
	}
	
	public Map<String, String> getParamStrings()
	{
		if ( params == null )
			return null;
		
		Map<String, String> newParams = Maps.newLinkedHashMap();
		
		for ( Entry<String, Object> e : params.entrySet() )
			try
			{
				newParams.put( e.getKey(), ObjectUtil.castToStringWithException( e.getValue() ) );
			}
			catch ( ClassCastException cce )
			{
				
			}
		
		return newParams;
	}
	
	@Override
	public String toString()
	{
		return "EvalMetaData{fileName=" + fileName + ",scriptName=" + scriptName + ",script=" + script + ",shell=" + shell + ",sourceSize=" + source.length() + ",site=" + site + ",contentType=" + contentType + ",params=[" + params + "]}";
	}
}
