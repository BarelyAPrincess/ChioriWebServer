/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.lang;

import java.util.Map;

import com.chiorichan.http.ErrorDocument;
import com.google.common.collect.Maps;

/**
 * Holds configuration read from a Apache Configuration File
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class ApacheConfContainer
{
	Map<Integer, ErrorDocument> errorDocuments = Maps.newHashMap();
	
	public void putErrorDocument( ErrorDocument doc )
	{
		errorDocuments.put( doc.getHttpCode(), doc );
	}
	
	public ErrorDocument getErrorDocument( int httpCode )
	{
		return errorDocuments.get( httpCode );
	}
}
