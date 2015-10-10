/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.factory.api;

import com.chiorichan.Loader;
import com.chiorichan.database.DatabaseEngineLegacy;

/**
 * 
 */
public class DB
{
	/**
	 * Returns an instance of the server database
	 * 
	 * @return
	 *         The server database engine
	 * @throws IllegalStateException
	 *             thrown if the requested database is unconfigured
	 */
	public static DatabaseEngineLegacy getServerDatabase()
	{
		DatabaseEngineLegacy engine = Loader.getDatabase().getLegacy();
		
		if ( engine == null )
			throw new IllegalStateException( "The server database is unconfigured. It will need to be setup in order for you to use the getServerDatabase() method." );
		
		return engine;
	}
}
