/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.database;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.lang3.Validate;

import com.chiorichan.Loader;
import com.chiorichan.util.ObjectUtil;

public abstract class SqlTable
{
	protected ResultSet rs;
	
	public Object toObject( Object clz ) throws SQLException
	{
		return toObject( clz, rs );
	}
	
	protected void setValues( ResultSet rs ) throws SQLException
	{
		toObject( this, rs );
	}
	
	protected Object toObject( Object clz, ResultSet rs ) throws SQLException
	{
		Validate.notNull( clz );
		Validate.notNull( rs );
		
		if ( rs.getRow() == 0 )
			rs.first();
		
		for ( Field f : clz.getClass().getDeclaredFields() )
		{
			SqlColumn sc = f.getAnnotation( SqlColumn.class );
			
			try
			{
				if ( sc != null && rs.getObject( sc.name() ) != null )
				{
					Object obj = rs.getObject( sc.name() );
					if ( f.getType().equals( String.class ) )
					{
						f.set( clz, ObjectUtil.castToString( obj ) );
					}
					else if ( obj instanceof String && ( f.getType().equals( Long.class ) || f.getType().getSimpleName().equalsIgnoreCase( "long" ) ) )
					{
						f.set( clz, Long.parseLong( (String) obj ) );
					}
					else if ( obj instanceof String && ( f.getType().equals( Integer.class ) || f.getType().getSimpleName().equalsIgnoreCase( "int" ) ) )
					{
						f.set( clz, Integer.parseInt( (String) obj ) );
					}
					else
					{
						f.set( clz, obj );
					}
				}
			}
			catch ( IllegalArgumentException e )
			{
				Loader.getLogger().severe( "We can't cast the value '" + rs.getObject( sc.name() ) + "' from column `" + sc.name() + "` with type `" + rs.getObject( sc.name() ).getClass().getSimpleName() + "` to FIELD `" + f.getName() + "` with type `" + f.getType() + "`." );
			}
			catch ( IllegalAccessException e )
			{
				Loader.getLogger().severe( "We don't have access to FIELD `" + f.getName() + "`, Be sure the field has a PUBLIC modifier." );
			}
		}
		
		return clz;
	}
	
	public boolean next() throws SQLException
	{
		boolean result = rs.next();
		setValues( rs );
		return result;
	}
	
	public boolean previous() throws SQLException
	{
		boolean result = rs.previous();
		setValues( rs );
		return result;
	}
	
	public SqlTable select( DatabaseEngine sql, String query ) throws SQLException
	{
		rs = sql.query( query );
		setValues( rs );
		
		return this;
	}
	
	public int getRowCount()
	{
		int rowCnt = 0;
		try
		{
			int curRow = rs.getRow();
			rs.last();
			rowCnt = rs.getRow();
			rs.relative( curRow );
		}
		catch ( Exception e )
		{}
		
		return rowCnt;
	}
	
	public ResultSet getResultSet()
	{
		return rs;
	}
}
