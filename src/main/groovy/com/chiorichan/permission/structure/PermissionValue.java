package com.chiorichan.permission.structure;

public abstract class PermissionValue<Type>
{
	private Type value;
	private final String name;
	
	protected PermissionValue( String permName, Type type )
	{
		value = type;
		name = permName;
	}
	
	public Type getValue()
	{
		return value;
	}
	
	public String getName()
	{
		return name;
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
