package com.chiorichan.framework;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import net.glxn.qrgen.QRCode;
import net.glxn.qrgen.image.ImageType;

public class FrameworkImageUtils
{
	protected Framework fw;
	
	public FrameworkImageUtils(Framework fw0)
	{
		fw = fw0;
	}
	
	public void QRPNG( String code ) throws IOException
	{
		ByteArrayOutputStream out = QRCode.from( code ).withSize( 200, 200 ).setMargin( 1 ).to( ImageType.PNG ).stream();
		
		out.flush();
		out.close();
		
		fw.getEnv().flush();
		fw.getEnv().getOriginalOut().flush();
		
		byte[] b = out.toByteArray();
		
		fw.getEnv().getOriginalOut().write( b );
	}
}
