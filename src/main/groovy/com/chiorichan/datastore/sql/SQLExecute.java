/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.datastore.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

import com.chiorichan.util.DbFunc;

/**
 *
 */
@SuppressWarnings( "rawtypes" )
public class SQLExecute<P extends SQLBase> extends SQLResultSet implements SQLResultSkel
{
	private P parent;

	public SQLExecute( ResultSet result, P parent )
	{
		super( result );
		this.parent = parent;
	}

	@Override
	public Map<String, Map<String, Object>> map() throws SQLException
	{
		return DbFunc.resultToMap( result );
	}

	public P parent()
	{
		return parent;
	}

	@Override
	public Map<String, Object> row() throws SQLException
	{
		return DbFunc.rowToMap( result );
	}

	@Override
	public int rowCount() throws SQLException
	{
		return parent.rowCount();
	}

	@Override
	public Set<Map<String, Object>> set() throws SQLException
	{
		return DbFunc.resultToSet( result );
	}

	@Override
	public Map<String, Map<String, String>> stringMap() throws SQLException
	{
		return DbFunc.resultToStringMap( result );
	}

	@Override
	public Map<String, String> stringRow() throws SQLException
	{
		return DbFunc.rowToStringMap( result );
	}

	@Override
	public Set<Map<String, String>> stringSet() throws SQLException
	{
		return DbFunc.resultToStringSet( result );
	}

	@Override
	public String toSqlQuery() throws SQLException
	{
		if ( result.getStatement() instanceof PreparedStatement )
			return DbFunc.toString( ( PreparedStatement ) result.getStatement() );
		return null;
	}
}
