/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.permission;

import java.util.Set;

import com.chiorichan.permission.lang.PermissionValueException;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

/**
 * References the model value held by a permission
 * Unset or default permissions will have no value and be permission type {@link PermissionType#DEFAULT}
 * 
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
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
		if ( value == null )
			return null;
		
		try
		{
			Object obj = type.cast( value );
			if ( obj == null )
				throw new ClassCastException();
			return new PermissionValue( this, obj );
		}
		catch ( ClassCastException e )
		{
			throw new PermissionValueException( "Can't cast %s to type %s", value.getClass().getName(), type );
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
	
	public PermissionModelValue setDescription( String description )
	{
		return setDescription( description, true );
	}
	
	/**
	 * Sets the description of this permission.
	 * <p>
	 * This will not be saved to disk, and is a temporary operation until the server reloads permissions.
	 * 
	 * @param description
	 *            The new description to set
	 * @param commit
	 *            shall we make the call to the backend to save these changes?
	 */
	public PermissionModelValue setDescription( String description, boolean commit )
	{
		this.description = description == null ? "" : description;
		if ( commit )
			perm.commit();
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
		return setValue( value, true );
	}
	
	public PermissionModelValue setValue( Object value, boolean commit )
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
		
		if ( commit )
			perm.commit();
		
		return this;
	}
	
	public PermissionModelValue setValueDefault( Object valueDefault )
	{
		return setValueDefault( valueDefault, true );
	}
	
	public PermissionModelValue setValueDefault( Object valueDefault, boolean commit )
	{
		if ( valueDefault == null )
			valueDefault = type.getBlankValue();
		
		try
		{
			Object obj = type.cast( valueDefault );
			if ( obj == null )
				throw new ClassCastException();
			this.valueDefault = new PermissionValue( this, obj );
		}
		catch ( ClassCastException e )
		{
			throw new PermissionValueException( "Can't cast %s to type %s", valueDefault.getClass().getName(), type );
		}
		
		if ( commit )
			perm.commit();
		
		return this;
	}
	
	@Override
	public String toString()
	{
		return String.format( "PermissionModelValue{name=%s,type=%s,value=%s,default=%s}", name, type, value, valueDefault );
	}
}
