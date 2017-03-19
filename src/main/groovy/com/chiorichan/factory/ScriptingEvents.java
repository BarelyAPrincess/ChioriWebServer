package com.chiorichan.factory;

/**
 * Provides an interface for which the Scripting Engine to notify Scripts of events, such as exception or before execution.
 */
public interface ScriptingEvents
{
	void onBeforeExecute( ScriptingContext context );

	void onAfterExecute( ScriptingContext context );

	void onException( ScriptingContext context, Throwable throwable );
}
