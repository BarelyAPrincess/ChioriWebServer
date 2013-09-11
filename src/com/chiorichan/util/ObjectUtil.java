package com.chiorichan.util;

import java.util.Map;
import java.util.Map.Entry;

import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.LargeStringBuilderValue;
import com.caucho.quercus.env.StringValue;
import com.chiorichan.Loader;

public class ObjectUtil
{
	public static ArrayValue castToArrayValue( Map<String, Object> array )
	{
		ArrayValueImpl newArray = new ArrayValueImpl();
		
		for ( Entry<String, Object> e : array.entrySet() )
			ArrayValueCast( newArray, e.getKey(), e.getValue() );
		
		return newArray;
	}
	
	public static String castToString( Object value )
	{
		switch ( value.getClass().getName() )
		{
			case "java.lang.Long":
				return Long.toString( (long) value );
			case "java.lang.String":
				return (String) value;
			case "java.lang.Integer":
				return Integer.toString( (int) value );
			case "java.lang.Double":
				return Double.toString( (double) value );
			case "java.lang.Boolean":
				return ( (boolean) value ) ? "true" : "false";
			default:
				Loader.getLogger().warning( "Uncaught Convertion to String of Type: " + value.getClass().getName() );
				return null;
		}
	}
	
	public static void ArrayValueCast( ArrayValue array, String key, Object obj )
	{
		switch ( obj.getClass().getName() )
		{
			case "java.lang.Long":
				array.put( key, (long) obj );
				break;
			case "java.lang.String":
				array.put( key, (String) obj );
				break;
			case "java.lang.Integer":
				array.put( key, (int) obj );
				break;
			case "java.lang.Double":
				array.put( key, (double) obj );
				break;
			case "java.lang.Boolean":
				array.put( key, (boolean) obj );
				break;
			case "java.lang.Char":
				array.put( key, (char) obj );
				break;
			default:
				if ( obj instanceof Map )
				{
					StringValue sv = new LargeStringBuilderValue();
					sv.append( key );
					array.put( sv, castToArrayValue( (Map) obj ) );
				}
				else
				{
					array.put( key, obj.toString() );
					Loader.getLogger().warning( "Uncaught Convertion to Type: " + obj.getClass().getName() );
				}
		}
	}
}
