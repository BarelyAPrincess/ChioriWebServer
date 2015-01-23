package com.chiorichan.permission.structure;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.gradle.jarjar.com.google.common.collect.Sets;

public abstract class PermissionValue<Type>
{
	private Type value;
	
	private PermissionValue( Type type )
	{
		value = type;
	}
	
	public Type getValue()
	{
		return value;
	}
	
	public PermissionType getType()
	{
		switch ( this.getClass().getSimpleName() )
		{
			case "PermissionBoolean":
				return PermissionType.BOOLEAN;
			case "PermissionEnum":
				return PermissionType.ENUM;
			case "PermissionVar":
				return PermissionType.VAR;
			case "PermissionInt":
				return PermissionType.INT;
			default:
				return PermissionType.UNKNOWN;
		}
	}
	
	public enum PermissionType
	{
		UNKNOWN, BOOLEAN, ENUM, VAR, INT;
	}
	
	public class PermissionBoolean extends PermissionValue<Boolean>
	{
		public PermissionBoolean( Boolean val )
		{
			super( val );
		}
	}
	
	public class PermissionEnum extends PermissionValue<String>
	{
		private Set<String> enumList = Sets.newHashSet();
		private int maxLen = -1;
		
		public PermissionEnum( String val, int len, String... enums )
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
	}
	
	public class PermissionVar extends PermissionValue<String>
	{
		private int maxLen = -1;
		
		public PermissionVar( String val, int len )
		{
			super( val );
			maxLen = len;
		}
		
		public int getMaxLen()
		{
			return maxLen;
		}
	}
	
	public class PermissionInt extends PermissionValue<Integer>
	{
		public PermissionInt( Integer val )
		{
			super( val );
		}
	}
}
