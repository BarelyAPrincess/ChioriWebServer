/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.permission;

import java.util.Set;

import com.chiorichan.permission.lang.PermissionValueException;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

/**
 * References the model value held by a permission
 * Unset or default permissions will have no value and be permission type {@link PermissionType#DEFAULT}
 */
@SuppressWarnings( "unchecked" )
public class PermissionModelValue
{
	private String description = "";
	private Set<String> enums = Sets.newHashSet();
	private int maxLen = -1;
	private final String name;
	private final Permission perm;
	private final PermissionType type;
	private PermissionValue value;
	private Object valueDefault;
	
	public PermissionModelValue( String name, PermissionType type, Permission perm )
	{
		this.name = name;
		this.type = type;
		this.perm = perm;
	}
	
	public PermissionValue createValue( Object value )
	{
		if ( value instanceof PermissionValue )
			value = ( ( PermissionValue ) value ).getValue();
		
		try
		{
			Object obj = type.cast( value );
			if ( obj == null && value == null )
				throw new PermissionValueException( "The assigned value must not be null." );
			if ( obj == null )
				throw new ClassCastException();
			return new PermissionValue( this, obj );
		}
		catch ( ClassCastException e )
		{
			throw new PermissionValueException( String.format( "Can't cast %s to type %s for permission %s.", value.getClass(), type, perm.getNamespace() ) );
		}
	}
	
	/**
	 * Gets a brief description of this permission, if set
	 * 
	 * @return Brief description of this permission
	 */
	public String getDescription()
	{
		return description;
	}
	
	public Set<String> getEnums()
	{
		return enums;
	}
	
	public String getEnumsString()
	{
		return Joiner.on( "|" ).join( enums );
	}
	
	public int getMaxLen()
	{
		return maxLen;
	}
	
	public PermissionValue getModelValue()
	{
		return value;
	}
	
	public String getName()
	{
		return name;
	}
	
	public Permission getPermission()
	{
		return perm;
	}
	
	public PermissionType getType()
	{
		return type;
	}
	
	public <T> T getValue()
	{
		if ( value == null )
			return getValueDefault();
		
		return ( T ) value.getValue();
	}
	
	public <T> T getValueDefault()
	{
		assert ( ! ( valueDefault instanceof PermissionValue ) );
		
		if ( value == null && this != PermissionDefault.DEFAULT.getNode().getModel() )
			return ( T ) PermissionDefault.DEFAULT.getNode().getModel().getValue();
		else if ( value == null )
			return ( T ) getType().getBlankValue();
		
		return ( T ) valueDefault;
	}
	
	public boolean hasDescription()
	{
		return description != null && !description.isEmpty();
	}
	
	/**
	 * Sets the description of this permission.
	 * <p>
	 * This will not be saved to disk, and is a temporary operation until the server reloads permissions.
	 * 
	 * @param description
	 *            The new description to set
	 */
	public PermissionModelValue setDescription( String description )
	{
		this.description = description == null ? "" : description;
		return this;
	}
	
	public void setEnums( Set<String> enums )
	{
		if ( type != PermissionType.ENUM )
			throw new PermissionValueException( "This model value does not support enumerates, %s", this );
		
		this.enums = enums;
	}
	
	public void setMaxLen( int maxLen )
	{
		if ( !type.hasMax() )
			throw new PermissionValueException( String.format( "This model value does not support the maxLen ability, %s", this ) );
		
		this.maxLen = maxLen;
	}
	
	public PermissionModelValue setValue( Object value )
	{
		if ( value == null )
			value = getValueDefault();
		
		try
		{
			Object obj = type.cast( value );
			if ( obj == null )
				throw new ClassCastException();
			this.value = new PermissionValue( this, obj );
		}
		catch ( ClassCastException e )
		{
			throw new PermissionValueException( "Can't cast %s to type %s", value.getClass().getName(), type );
		}
		
		return this;
	}
	
	public PermissionModelValue setValueDefault( Object valueDefault )
	{
		if ( valueDefault == null )
			valueDefault = type.getBlankValue();
		
		try
		{
			Object obj = type.cast( valueDefault );
			if ( obj == null )
				throw new ClassCastException();
			this.valueDefault = obj;
		}
		catch ( ClassCastException e )
		{
			throw new PermissionValueException( "Can't cast %s to type %s", valueDefault.getClass().getName(), type );
		}
		
		return this;
	}
	
	@Override
	public String toString()
	{
		return String.format( "PermissionModelValue{name=%s,type=%s,value=%s,default=%s}", name, type, value == null ? null : value.getValue(), valueDefault );
	}
}
