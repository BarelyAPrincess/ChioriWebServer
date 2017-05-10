/*
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Joel Greene <joel.greene@penoaks.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.factory.models

import groovy.transform.CompileStatic

/**
 * Provides DB model
 **/
@CompileStatic
class SQLModel
{
	private SQLModelBuilder builder
	private Map columnData = [:]
	private List columns = []

	SQLModel( SQLModelBuilder builder, Map row )
	{
		this.builder = builder

		columnData.putAll( row )
		columns.addAll( columnData.keySet() )
	}

	void save()
	{

	}

	void delete()
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

	def methodMissing( String key, args )
	{
		SQLModelBuilder rootBuilder = builder.root()
		MetaMethod method = rootBuilder.getMetaClass().getMetaMethod( key, [this].toArray() )
		if ( method == null )
			throw new MissingMethodException( key, rootBuilder.getClass(), [this].toArray() )
		return method.invoke( rootBuilder, [this].toArray() )
	}

	boolean matches( String column, Object value )
	{
		if ( !columnData.containsKey( column ) )
			return false
		return columnData.get( column ) == value
	}

	SQLModelBuilder belongsTo( String belongingModelPackage, String columnLeft, String columnRight )
	{
		SQLModelBuilder belongingModel = builder.model( belongingModelPackage )
		return belongingModel.whereMatches( columnRight, columnData[columnLeft] )
	}
}
