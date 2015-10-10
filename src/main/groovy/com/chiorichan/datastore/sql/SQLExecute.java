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
	
	public P parent()
	{
		return parent;
	}
	
	@Override
	public Map<String, Map<String, Object>> resultToMap() throws SQLException
	{
		return DbFunc.resultToMap( result );
	}
	
	@Override
	public Set<Map<String, Object>> resultToSet() throws SQLException
	{
		return DbFunc.resultToSet( result );
	}
	
	@Override
	public Map<String, Map<String, String>> resultToStringMap() throws SQLException
	{
		return DbFunc.resultToStringMap( result );
	}
	
	@Override
	public Set<Map<String, String>> resultToStringSet() throws SQLException
	{
		return DbFunc.resultToStringSet( result );
	}
	
	@Override
	public int rowCount() throws SQLException
	{
		return parent.rowCount();
	}
	
	@Override
	public Map<String, Object> rowToMap() throws SQLException
	{
		return DbFunc.rowToMap( result );
	}
	
	@Override
	public Map<String, String> rowToStringMap() throws SQLException
	{
		return DbFunc.rowToStringMap( result );
	}
}
