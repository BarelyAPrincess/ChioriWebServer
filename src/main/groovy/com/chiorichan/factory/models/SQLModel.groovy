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
