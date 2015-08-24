/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.datastore.sql;

import java.util.Map.Entry;

/**
 * 
 */
public class SQLKeyValue implements SQLWhereElement
{
	static enum KeyValueSeperator
	{
		EQUAL( "=" ), NOT( "NOT" ), LIKE( "LIKE" ), LESSTHAN( "<" ), GREATERTHAN( ">" );
		
		private String character;
		
		KeyValueSeperator( String character )
		{
			this.character = character;
		}
	}
	
	String key;
	Object value;
	SQLWhereSeperator pre = SQLWhereSeperator.AND;
	KeyValueSeperator sep = KeyValueSeperator.EQUAL;
	
	public SQLKeyValue( SQLWhereSeperator pre, Entry<String, Object> e )
	{
		this( pre, e.getKey(), e.getValue() );
	}
	
	public SQLKeyValue( SQLWhereSeperator pre, String key, Object value )
	{
		this.pre = pre;
		this.key = key;
		this.value = value;
	}
	
	@Override
	public SQLWhereSeperator seperator()
	{
		return pre;
	}
	
	public SQLKeyValue seperator( String seperator )
	{
		sep = KeyValueSeperator.valueOf( seperator );
		return this;
	}
	
	@Override
	public String toQuery()
	{
		return String.format( "`%s` %s '%s'", key, sep.character, value ).trim();
	}
}
