/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.datastore.sql;

import java.sql.SQLException;

import com.chiorichan.lang.StartupException;


/**
 * 
 */
public class MySQLDatastore extends SQLDatastore
{
	String user, pass, connection;
	
	public MySQLDatastore( String db, String user, String pass, String host, String port ) throws SQLException
	{
		if ( host == null )
			host = "localhost";
		
		if ( port == null )
			port = "3306";
		
		try
		{
			Class.forName( "com.mysql.jdbc.Driver" );
		}
		catch ( ClassNotFoundException e )
		{
			throw new StartupException( "We could not locate the 'com.mysql.jdbc.Driver' library, be sure to have this library in your build path." );
		}
		
		this.user = user;
		this.pass = pass;
		connection = "jdbc:mysql://" + host + ":" + port + "/" + db;
		
		attemptConnection( connection, user, pass );
		
		getLogger().info( "We succesully connected to the sql database using '" + connection + "'." );
		
		// Loader.getLogger().warning( "There was a problem connecting to the sql database using 'jdbc:mysql://" + host + ":" + port + "/" + db + "'." );
	}
}
