package com.chiorichan.permission.structure;

public abstract class PermissionValue<Type>
{
	private Type value;
	
	protected PermissionValue( Type type )
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
				return PermissionType.BOOL;
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
	
	public abstract PermissionValue createChild( Object val );
	
	protected void setValue( Type val )
	{
		value = val;
	}
	
	public enum PermissionType
	{
		UNKNOWN, BOOL, ENUM, VAR, INT;
	}
}
