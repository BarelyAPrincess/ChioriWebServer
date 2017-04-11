/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.factory.models;

import com.chiorichan.datastore.sql.query.SQLQuerySelect;
import com.chiorichan.utils.UtilMaps;
import com.chiorichan.utils.UtilObjects;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SQLModelPaginated
{
	private SQLModelBuilder parent;
	private List<SQLModel> resultsOrig;
	private Map<Integer, List<SQLModel>> results;

	private int perPage;
	private int currentPage;

	public SQLModelPaginated( SQLModelBuilder parent, SQLQuerySelect sqlRaw, int perPage, int currentPage ) throws SQLException
	{
		UtilObjects.notNull( sqlRaw );
		UtilObjects.isTrue( perPage > 0 );
		UtilObjects.isTrue( currentPage >= 0 );

		this.parent = parent;
		this.perPage = perPage;
		this.currentPage = currentPage;

		Set<Map<String, Object>> rows = sqlRaw.set();
		List<SQLModel> resultsOrig = rows.stream().map( m -> new SQLModel( parent, m ) ).collect( Collectors.toList() );
		results = UtilMaps.paginate( resultsOrig, perPage );

		if ( currentPage > results.size() - 1 )
			this.currentPage = results.size() - 1;
	}

	public SQLModelBuilder back()
	{
		return parent;
	}

	public int perPage()
	{
		return perPage;
	}

	public int currentPage()
	{
		return currentPage;
	}

	/**
	 * Finds a paginated page that follows the specified item and sets the current page
	 *
	 * @return was the row found
	 */
	public boolean after( String column, Object value )
	{
		for ( int i = 0; i < resultsOrig.size(); i++ )
		{
			if ( resultsOrig.get( i ).matches( column, value ) )
			{
				int page = i / perPage;
				int index = i % perPage;

				UtilObjects.isTrue( results.get( page ).get( index ) == resultsOrig.get( i ) );

				if ( page + 1 > results.size() )
					return false; // Specified item was already on the last page
				currentPage = page + 1;
				return true;
			}
		}
		return false;
	}

	public SQLModelResults get()
	{
		return new SQLModelResults( parent, results.get( currentPage ) );
	}

	public int pages()
	{
		return results.size();
	}

	public SQLModelPaginated repaginate( int perPage )
	{
		this.perPage = perPage;
		results = UtilMaps.paginate( resultsOrig, perPage );
		return this;
	}

	public int remainder()
	{
		return results.size() == 0 ? 0 : UtilMaps.last( results ).size();
	}
}
