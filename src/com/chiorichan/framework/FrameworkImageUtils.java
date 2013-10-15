package com.chiorichan.framework;

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
	
	public String QRPNG( String code ) throws IOException
	{
		return new String(QRCode.from( code ).withSize( 200, 200 ).setMargin( 1 ).to( ImageType.PNG ).stream().toByteArray(), "ISO-8859-1");
	}
}