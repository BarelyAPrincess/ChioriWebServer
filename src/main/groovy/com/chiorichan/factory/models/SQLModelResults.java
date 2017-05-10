/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Joel Greene <joel.greene@penoaks.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.factory.models;

import com.chiorichan.datastore.sql.SQLBase;
import com.chiorichan.factory.Jsonable;
import com.chiorichan.factory.api.Builtin;
import com.chiorichan.utils.UtilObjects;
import groovy.json.JsonBuilder;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SQLModelResults implements List<SQLModel>, Jsonable
{
	private SQLModelBuilder parent;
	private List<SQLModel> results;

	public SQLModelResults( SQLModelBuilder parent, SQLBase sql ) throws SQLException
	{
		this.parent = parent;

		Set<Map<String, Object>> rows = sql.set();
		this.results = rows.stream().map( m -> new SQLModel( parent, m ) ).collect( Collectors.toList() );
	}

	public SQLModelResults( SQLModelBuilder parent, List<SQLModel> results )
	{
		this.parent = parent;
		this.results = results;
	}

	@Override
	public int size()
	{
		return results.size();
	}

	@Override
	public boolean isEmpty()
	{
		return results.isEmpty();
	}

	@Override
	public boolean contains( Object o )
	{
		return results.contains( o );
	}

	@Override
	public Iterator<SQLModel> iterator()
	{
		return results.iterator();
	}

	@Override
	public Object[] toArray()
	{
		return results.toArray();
	}

	@Override
	public <T> T[] toArray( T[] a )
	{
		return results.toArray( a );
	}

	@Override
	public boolean add( SQLModel sqlModel )
	{
		return results.add( sqlModel );
	}

	@Override
	public boolean remove( Object o )
	{
		return results.remove( o );
	}

	@Override
	public boolean containsAll( Collection<?> c )
	{
		return results.containsAll( c );
	}

	@Override
	public boolean addAll( Collection<? extends SQLModel> c )
	{
		return results.addAll( c );
	}

	@Override
	public boolean addAll( int index, Collection<? extends SQLModel> c )
	{
		return results.addAll( index, c );
	}

	@Override
	public boolean removeAll( Collection<?> c )
	{
		return results.removeAll( c );
	}

	@Override
	public boolean retainAll( Collection<?> c )
	{
		return results.retainAll( c );
	}

	@Override
	public void clear()
	{
		results.clear();
	}

	@Override
	public SQLModel get( int index )
	{
		return results.get( index );
	}

	@Override
	public SQLModel set( int index, SQLModel element )
	{
		return results.set( index, element );
	}

	@Override
	public void add( int index, SQLModel element )
	{
		results.add( index, element );
	}

	@Override
	public SQLModel remove( int index )
	{
		return results.remove( index );
	}

	@Override
	public int indexOf( Object o )
	{
		return results.indexOf( o );
	}

	@Override
	public int lastIndexOf( Object o )
	{
		return results.lastIndexOf( o );
	}

	@Override
	public ListIterator<SQLModel> listIterator()
	{
		return results.listIterator();
	}

	@Override
	public ListIterator<SQLModel> listIterator( int index )
	{
		return results.listIterator( index );
	}

	@Override
	public List<SQLModel> subList( int fromIndex, int toIndex )
	{
		return results.subList( fromIndex, toIndex );
	}

	public String createHTMLTable() throws SQLException
	{
		return createHTMLTable( null, null );
	}

	public String createHTMLTable( String tableId ) throws SQLException
	{
		return createHTMLTable( tableId, null );
	}

	public String createHTMLTable( String tableId, String altTableClass ) throws SQLException
	{
		List<Object> tbl = new ArrayList<>();
		List<String> columnNames = parent.getPrintedTableColumns();
		List<String> columnTitles = new ArrayList<>();

		if ( columnNames == null )
			columnNames = parent.getColumns();

		for ( String column : columnNames )
			columnTitles.add( parent.getColumnFriendlyName( column ) );

		for ( SQLModel model : results )
		{
			Map<Integer, String> row = new HashMap<>();

			int i = 0;
			for ( String column : columnNames )
			{
				row.put( i, UtilObjects.castToString( model.propertyMissing( column ) ) );
				i++;
			}

			tbl.add( row );
		}

		if ( tbl.size() == 0 )
			tbl.add( "We have nothing to display." );

		return Builtin.createTable( tbl, columnTitles, tableId, altTableClass );
	}

	public SQLModelBuilder back()
	{
		return parent;
	}

	@Override
	public String toJson()
	{
		return new JsonBuilder( results ).toString();
	}
}
