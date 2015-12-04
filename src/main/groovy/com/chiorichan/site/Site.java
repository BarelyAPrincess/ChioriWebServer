/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.site;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

import com.chiorichan.configuration.file.YamlConfiguration;
import com.chiorichan.datastore.sql.bases.SQLDatastore;
import com.chiorichan.http.HttpCookie;
import com.chiorichan.http.Routes;

/**
 * Provides the API needed for each site storage method
 */
public interface Site
{
	String getSiteId();
	
	String getTitle();
	
	File rootDirectory();
	
	String getDomain();
	
	void unload();
	
	String getName();
	
	String getSessionKey();
	
	HttpCookie createSessionCookie( String sessionId );
	
	File subDomainDirectory( String subDomain );
	
	boolean subDomainExists( String subDomain );
	
	File tempDirectory();
	
	Routes getRoutes();
	
	File publicDirectory();
	
	YamlConfiguration getConfig();
	
	SQLDatastore getDatastore();
	
	File resourceDirectory();
	
	File resourcePackage( String pack ) throws FileNotFoundException;
	
	File resourceFile( String file ) throws FileNotFoundException;
	
	@Deprecated
	List<String> getMetatags();
	
	@Deprecated
	Map<String, String> getAliases();
}
