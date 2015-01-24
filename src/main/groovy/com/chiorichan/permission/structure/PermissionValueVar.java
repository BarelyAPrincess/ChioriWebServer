package com.chiorichan.permission.structure;

import com.chiorichan.util.ObjectUtil;

public class PermissionValueVar extends PermissionValue<String>
{
	private int maxLen = -1;
	
	public PermissionValueVar( String val, int len )
	{
		super( val );
		maxLen = len;
	}
	
	public int getMaxLen()
	{
		return maxLen;
	}
	
	@Override
	public String toString()
	{
		return "[type=" + getType() + ",value=" + getValue() + ",maxlen=" + maxLen + "]";
	}
	
	@Override
	public PermissionValue createChild( Object val )
	{
		try
		{
			PermissionValue<String> newVal = (PermissionValue<String>) clone();
			newVal.setValue( ObjectUtil.castToString( val ) );
			return newVal;
		}
		catch( CloneNotSupportedException e )
		{
			throw new RuntimeException( e );
		}
	}
}
