package com.chiorichan.util;

public class Common
{
	/**
	 * @return Epoch based on the current Timezone 
	 */
	public static int getEpoch()
	{
		return (int) ( System.currentTimeMillis() / 1000 );
	}
}