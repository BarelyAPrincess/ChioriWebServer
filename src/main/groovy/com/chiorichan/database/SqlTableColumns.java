/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.database;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;

public class SqlTableColumns implements Iterable<String>
{
	public class SqlColumn
	{
		public final String name;
		public final int type;
		public final String label;
		public final String className;
		
		SqlColumn( String name, int type, String label, String className )
		{
			this.name = name;
			this.type = type;
			this.label = label;
			this.className = className;
		}
		
		public Object newType()
		{
			switch ( className )
			{
				case "java.lang.String":
					return "";
				case "java.lang.Integer":
					return 0;
				case "java.lang.Boolean":
					return false;
				default:
					// Loader.getLogger().debug( "Column Class: " + className );
					throw new IllegalArgumentException( "We could not instigate the proper column type " + className + " for column " + name + ", this might need to be inplemented." );
			}
		}
	}
	
	private final List<SqlColumn> columns = Lists.newArrayList();
	
	void add( ResultSetMetaData rsmd, int index ) throws SQLException
	{
		columns.add( new SqlColumn( rsmd.getColumnName( index ), rsmd.getColumnType( index ), rsmd.getColumnLabel( index ), rsmd.getColumnClassName( index ) ) );
	}
	
	public int count()
	{
		return columns.size();
	}
	
	public SqlColumn get( String name )
	{
		for ( SqlColumn c : columns )
			if ( c.name.equals( name ) )
				return c;
		return null;
	}
	
	@Override
	public Iterator<String> iterator()
	{
		List<String> rtn = Lists.newArrayList();
		for ( SqlColumn m : columns )
			rtn.add( m.name );
		return rtn.iterator();
	}
}
