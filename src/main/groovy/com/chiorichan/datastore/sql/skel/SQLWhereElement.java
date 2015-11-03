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
 * 
 */
public abstract class SQLWhereElement
{
	private SQLWhereElementSep seperator = SQLWhereElementSep.NONE;
	
	public final SQLWhereElementSep seperator()
	{
		return seperator;
	}
	
	public final void seperator( SQLWhereElementSep seperator )
	{
		this.seperator = seperator;
	}
	
	public abstract String toSqlQuery();
	
	public Object value()
	{
		return null;
	}
}
