package com.chiorichan.permission.structure;

import com.chiorichan.util.ObjectUtil;

public class PermissionValueBoolean extends PermissionValue<Boolean>
{
	public PermissionValueBoolean( String name, Boolean val )
	{
		super( name, val );
	}
	
	@Override
	public PermissionValue createChild( Object val )
	{
		try
		{
			PermissionValue<Boolean> newVal = (PermissionValue<Boolean>) clone();
			newVal.setValue( ObjectUtil.castToBool( val ) );
			return newVal;
		}
		catch( CloneNotSupportedException e )
		{
			throw new RuntimeException( e );
		}
	}
}
