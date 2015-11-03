/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.plugin.dropbox;

import com.chiorichan.http.HttpRequestWrapper;
import com.dropbox.core.DbxSessionStore;

/**
 * Implements Chiori-chan's Web Server Session for the Dropbox SDK
 */
public class DropboxChioriSessionStore implements DbxSessionStore
{
	private HttpRequestWrapper request;
	private String sessionKey = "dropbox-auth-csrf-token";
	
	public DropboxChioriSessionStore( HttpRequestWrapper request )
	{
		this.request = request;
	}
	
	public DropboxChioriSessionStore( HttpRequestWrapper request, String sessionKey )
	{
		this.request = request;
		this.sessionKey = sessionKey;
	}
	
	@Override
	public void clear()
	{
		request.setVariable( sessionKey, null );
	}
	
	@Override
	public String get()
	{
		return request.getVariable( sessionKey );
	}
	
	@Override
	public void set( String value )
	{
		request.setVariable( sessionKey, value );
	}
}
