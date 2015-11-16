/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.datastore.sql.skel;

import org.apache.commons.lang3.Validate;

/**
 * 
 */
public final class SQLWhereKeyValue<T extends SQLSkelWhere<?, ?>> extends SQLWhereElement
{
	enum Operands
	{
		EQUAL( "=" ), NOTEQUAL( "!=" ), LIKE( "LIKE" ), NOTLIKE( "NOT LIKE" ), GREATER( ">" ), LESSER( "<" ), REGEXP( "REGEXP" );
		
		private String operator;
		
		Operands( String operator )
		{
			this.operator = operator;
		}
		
		String stringValue()
		{
			return operator;
		}
	}
	
	private final String key;
	private Operands operator = Operands.EQUAL;
	private Object value = "";
	private final T parent;
	
	public SQLWhereKeyValue( T parent, String key )
	{
		Validate.notNull( parent );
		this.key = key;
		this.parent = parent;
	}
	
	public T between( Number n )
	{
		moreThan( n );
		parent.and().where( key ).lessThan( n );
		return parent;
	}
	
	@Override
	public boolean equals( Object obj )
	{
		if ( ! ( obj instanceof SQLWhereKeyValue ) )
			throw new IllegalArgumentException( "Received a call to the equals() method for the SQLWhereKeyValue class! We could be wrong but since the object was not an instance of this class, we decided to alert you that if you were attempting to match a key and value, the correct method would be matches()." );
		return super.equals( obj );
	}
	
	protected String key()
	{
		return key;
	}
	
	public T lessThan( Number n )
	{
		operator = Operands.LESSER;
		value = n;
		parent.where( this );
		return parent;
	}
	
	public T like( String value )
	{
		operator = Operands.LIKE;
		this.value = value;
		parent.where( this );
		return parent;
	}
	
	/**
	 * Similar to {@link #like(String)}, except will wrap the value with wild card characters if none exist.
	 */
	public T likeWild( String value )
	{
		if ( !value.contains( "%" ) )
			value = "%" + value + "%";
		
		return like( value );
	}
	
	public T matches( Object value )
	{
		operator = Operands.EQUAL;
		this.value = value;
		parent.where( this );
		return parent;
	}
	
	public T moreThan( Number n )
	{
		operator = Operands.GREATER;
		value = n;
		parent.where( this );
		return parent;
	}
	
	public T not( Object value )
	{
		operator = Operands.NOTEQUAL;
		this.value = value;
		parent.where( this );
		return parent;
	}
	
	public T notLike( String value )
	{
		operator = Operands.NOTLIKE;
		this.value = value;
		parent.where( this );
		return parent;
	}
	
	protected Operands operand()
	{
		return operator;
	}
	
	public T regex( String value )
	{
		operator = Operands.REGEXP;
		this.value = value;
		parent.where( this );
		return parent;
	}
	
	@Override
	public String toSqlQuery()
	{
		return String.format( "`%s` %s %%s", key, operator.stringValue() );
	}
	
	@Override
	public String toString()
	{
		return toSqlQuery();
	}
	
	@Override
	public Object value()
	{
		return value;
	}
}
