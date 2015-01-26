package com.chiorichan.permission.structure;

import com.chiorichan.util.ObjectUtil;

public class PermissionValueInt extends PermissionValue<Integer>
{
	public PermissionValueInt( String name, Integer val )
	{
		super( name, val );
	}
	
	@Override
	public PermissionValue createChild( Object val )
	{
		try
		{
			PermissionValue<Integer> newVal = (PermissionValue<Integer>) clone();
			newVal.setValue( ObjectUtil.castToLong( val ).intValue() );
			return newVal;
		}
		catch( CloneNotSupportedException e )
		{
			throw new RuntimeException( e );
		}
	}
}
