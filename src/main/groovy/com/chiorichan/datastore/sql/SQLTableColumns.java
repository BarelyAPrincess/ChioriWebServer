/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.datastore.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.chiorichan.util.StringFunc;
import com.google.common.collect.Lists;

public class SQLTableColumns implements Iterable<String>
{
	public class SQLColumn
	{
		private final String name;
		private final int size;
		private final int type;
		private final String def;
		private final boolean isNullable;
		
		SQLColumn( String name, int size, int type, String def, boolean isNullable )
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
	
	private SQLWrapper sql;
	private String table;
	
	private final List<SQLColumn> columns = Lists.newArrayList();
	
	public SQLTableColumns( SQLWrapper sql, String table ) throws SQLException
	{
		this.sql = sql;
		this.table = table;
		refresh();
	}
	
	public List<String> columnNames()
	{
		List<String> rtn = Lists.newArrayList();
		for ( SQLColumn m : columns )
			rtn.add( m.name );
		return rtn;
	}
	
	public List<String> columnNamesRequired()
	{
		List<String> rtn = Lists.newArrayList();
		for ( SQLColumn m : columns )
			if ( StringFunc.isNull( m.def ) && !m.isNullable )
				rtn.add( m.name );
		return rtn;
	}
	
	public List<SQLColumn> columns()
	{
		return Collections.unmodifiableList( columns );
	}
	
	public List<SQLColumn> columnsRequired()
	{
		List<SQLColumn> rtn = Lists.newArrayList();
		for ( SQLColumn m : columns )
			if ( StringFunc.isNull( m.def ) && !m.isNullable )
				rtn.add( m );
		return rtn;
	}
	
	public boolean contains( String colName )
	{
		for ( SQLColumn m : columns )
			if ( m.name().equals( colName ) )
				return true;
		return false;
	}
	
	public int count()
	{
		return columns.size();
	}
	
	public SQLColumn get( String name )
	{
		for ( SQLColumn c : columns )
			if ( c.name.equals( name ) )
				return c;
		return null;
	}
	
	@Override
	public Iterator<String> iterator()
	{
		List<String> rtn = Lists.newArrayList();
		for ( SQLColumn m : columns )
			rtn.add( m.name );
		return rtn.iterator();
	}
	
	public void refresh() throws SQLException
	{
		ResultSet sqlColumns = sql.getMetaData().getColumns( null, null, table, null );
		columns.clear();
		
		while ( sqlColumns.next() )
		{
			String name = sqlColumns.getString( "COLUMN_NAME" );
			int type = sqlColumns.getInt( "DATA_TYPE" );
			int size = sqlColumns.getInt( "COLUMN_SIZE" );
			String def = sqlColumns.getString( "COLUMN_DEF" );
			boolean isNullable = "YES".equals( sqlColumns.getString( "IS_NULLABLE" ) );
			
			columns.add( new SQLColumn( name, size, type, def, isNullable ) );
		}
	}
}
