package com.chiorichan.factory;

public interface BindingProvider
{
	public CodeEvalFactory getCodeFactory();
	public CodeEvalFactory forceNewCodeFactory();
}
