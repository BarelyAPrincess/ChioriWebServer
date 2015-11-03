/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.datastore.sql.skel;

import java.util.Collection;

/**
 * 
 */
public interface SQLSkelGroupBy<T>
{
	T groupBy( Collection<String> columns );
	
	T groupBy( String... columns );
	
	T groupBy( String column );
}
