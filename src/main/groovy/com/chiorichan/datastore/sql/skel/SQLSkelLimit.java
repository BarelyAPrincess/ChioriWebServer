/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.datastore.sql.skel;


/**
 * Provides the Skeleton Interface for SQL Queries implementing the Limit Methods
 */
public interface SQLSkelLimit<T>
{
	int limit();
	
	T limit( int limit, int offset );
	
	T limit( int limit );
	
	int offset();
	
	T offset( int offset );
}
