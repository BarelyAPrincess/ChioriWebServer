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
import java.util.Iterator;
import java.util.List;

import com.chiorichan.util.StringFunc;
import com.google.common.collect.Lists;

public class SqlTableColumns implements Iterable<String>
{
	public class SqlColumn
	{
		private final String name;
		private final int size;
		private final int type;
		private final String def;
		private final boolean isNullable;
		
		SqlColumn( String name, int size, int type, String def, boolean isNullable )
		{
			this.name = name;
			this.size = size;
			this.type = type;
			this.def = def;
			this.isNullable = isNullable;
		}
		
		public String def()
		{
			return def;
		}
		
		public boolean isNullable()
		{
			return isNullable;
		}
		
		public String name()
		{
			return name;
		}
		
		public int size()
		{
			return size;
		}
		
		public int type()
		{
			return type;
		}
	}
	
	private final List<SqlColumn> columns = Lists.newArrayList();
	
	void add( String name, int size, int type, String def, boolean isNullable ) throws SQLException
	{
		columns.add( new SqlColumn( name, size, type, def, isNullable ) );
	}
	
	public List<String> columnNames()
	{
		List<String> rtn = Lists.newArrayList();
		for ( SqlColumn m : columns )
			rtn.add( m.name );
		return rtn;
	}
	
	public List<String> columnNamesRequired()
	{
		List<String> rtn = Lists.newArrayList();
		for ( SqlColumn m : columns )
			if ( StringFunc.isNull( m.def ) && !m.isNullable )
				rtn.add( m.name );
		return rtn;
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
