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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.chiorichan.util.DbFunc;

/**
 *
 */
public class SQLRawQuery extends SQLBase<SQLRawQuery>
{
	private String query;
	private List<Object> objs = new ArrayList<>();

	public SQLRawQuery( SQLWrapper sql, String query )
	{
		super( sql, false );
		this.query = query;
	}

	public SQLRawQuery( SQLWrapper sql, String query, Collection<Object> objs )
	{
		this( sql, query );
		this.objs.addAll( objs );
	}

	@Override
	protected SQLRawQuery execute0() throws SQLException
	{
		query( toSqlQuery(), false, sqlValues() );
		return this;
	}

	@SuppressWarnings( "deprecation" )
	@Override
	public int rowCount()
	{
		try
		{
			int cnt = statement().getUpdateCount();
			return cnt == -1 ? DbFunc.rowCount( resultSet() ) : cnt;
		}
		catch ( NullPointerException | SQLException e )
		{
			return -1;
		}
	}

	@Override
	public Object[] sqlValues()
	{
		return objs.toArray( new Object[0] );
	}

	@Override
	public String toSqlQuery()
	{
		return query;
	}
}
