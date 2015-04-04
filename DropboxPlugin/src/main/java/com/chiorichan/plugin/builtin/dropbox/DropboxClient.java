/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.plugin.builtin.dropbox;

import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxRequestConfig;

/**
 * Used to link between Dropbox and end requester
 * Also stores Dropbox Client instance
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class DropboxClient
{
	String accessToken;
	DbxClient client;
	
	public DropboxClient( DbxRequestConfig dbxAppConfig, String accessToken )
	{
		client = new DbxClient( dbxAppConfig, accessToken );
		this.accessToken = accessToken;
	}
	
	public DbxClient getDbxClient()
	{
		return client;
	}
}
