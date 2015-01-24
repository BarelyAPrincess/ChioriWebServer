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
	
	@Override
	public String toString()
	{
		return "[type=" + getType() + ",value=" + getValue() + "]";
	}
	
	public PermissionType getType()
	{
		switch ( this.getClass().getSimpleName() )
		{
			case "PermissionValueBoolean":
				return PermissionType.BOOL;
			case "PermissionValueEnum":
				return PermissionType.ENUM;
			case "PermissionValueVar":
				return PermissionType.VAR;
			case "PermissionValueInt":
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
