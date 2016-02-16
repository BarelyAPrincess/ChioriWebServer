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
import java.util.Map;
import java.util.Set;

/**
 * Provides a SQL Result/Execute skeleton for {@link SQLBase} and {@link SQLExcute}
 */
public interface SQLResultSkel
{
	Map<String, Map<String, Object>> map() throws SQLException;

	Set<Map<String, Object>> set() throws SQLException;

	Map<String, Map<String, String>> stringMap() throws SQLException;

	Map<String, Object> row() throws SQLException;

	Map<String, String> stringRow() throws SQLException;

	Set<Map<String, String>> stringSet() throws SQLException;

	int rowCount() throws SQLException;

	String toSqlQuery() throws SQLException;
}
