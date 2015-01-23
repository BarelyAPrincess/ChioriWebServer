package com.chiorichan.permission.structure;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.gradle.jarjar.com.google.common.collect.Sets;

import com.chiorichan.util.ObjectUtil;

public class PermissionValueEnum extends PermissionValue<String>
{
	private Set<String> enumList = Sets.newHashSet();
	private int maxLen = -1;
	
	public PermissionValueEnum( String val, int len, String... enums )
	{
		super( val );
		maxLen = len;
		enumList = new HashSet<String>( Arrays.asList( enums ) );
	}
	
	public Set<String> getEnumList()
	{
		return enumList;
	}
	
	public int getMaxLen()
	{
		return maxLen;
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
