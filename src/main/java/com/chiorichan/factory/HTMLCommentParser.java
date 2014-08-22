package com.chiorichan.factory;

public abstract class HTMLCommentParser extends BasicParser
{
	public HTMLCommentParser(String argumentName)
	{
		super( "<!-- *" + argumentName + "\\((.*)\\) *-->", "(<!-- *" + argumentName + "\\(.*\\) *-->)" );
	}
}
