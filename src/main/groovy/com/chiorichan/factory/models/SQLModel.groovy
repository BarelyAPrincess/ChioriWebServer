/*
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.factory.models

/**
 * Provides DB model
 **/
public class SQLModel
{
	private SQLQueryBuilder builder;
	private columnData = [:];
	private columns = [];

	public SQLModel( SQLQueryBuilder builder, Map<String, Object> row )
	{
		this.builder = builder;

		columnData.putAll( row );
		columns.addAll( columnData.keySet() );
	}

	public void save()
	{

	}

	public void delete()
	{

	}

	def propertyMissing( String key, value )
	{
		if ( columns.contains( key ) )
		{
			columnData[key] = value
		}
		else
		{
			throw new IllegalArgumentException( "This table does not contain the column named " + key )
		};
	}

	def propertyMissing( String key )
	{
		columnData[key]
	}
}
