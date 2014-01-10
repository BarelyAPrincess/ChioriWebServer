package com.chiorichan.util;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;

public class Common
{
	/**
	 * @return Epoch based on the current Timezone
	 */
	public static int getEpoch()
	{
		return (int) ( System.currentTimeMillis() / 1000 );
	}
	
	public static byte[] createChecksum( String filename ) throws Exception
	{
		InputStream fis = new FileInputStream( filename );
		
		byte[] buffer = new byte[1024];
		MessageDigest complete = MessageDigest.getInstance( "MD5" );
		int numRead;
		
		do
		{
			numRead = fis.read( buffer );
			if ( numRead > 0 )
			{
				complete.update( buffer, 0, numRead );
			}
		}
		while ( numRead != -1 );
		
		fis.close();
		return complete.digest();
	}
	
	public static String getMD5Checksum( String filename ) throws Exception
	{
		byte[] b = createChecksum( filename );
		String result = "";
		
		for ( int i = 0; i < b.length; i++ )
		{
			result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16 ).substring( 1 );
		}
		return result;
	}
	
	public static String md5( String data )
	{
		try
		{
			byte[] bytesOfMessage = data.getBytes( "UTF-8" );
			MessageDigest complete = MessageDigest.getInstance( "MD5" );
			
			byte[] b = complete.digest( bytesOfMessage );
			String result = "";
			
			for ( int i = 0; i < b.length; i++ )
			{
				result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16 ).substring( 1 );
			}
			
			return result;
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static boolean isValidMD5( String s )
	{
		return s.matches( "[a-fA-F0-9]{32}" );
	}
}
