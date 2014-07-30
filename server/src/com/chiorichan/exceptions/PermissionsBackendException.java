package com.chiorichan.exceptions;

/**
 * This exception is thrown when a permissions backend has issues loading
 */
public class PermissionsBackendException extends Exception {
	public PermissionsBackendException() {
	}

	public PermissionsBackendException(String message) {
		super(message);
	}

	public PermissionsBackendException(String message, Throwable cause) {
		super(message, cause);
	}

	public PermissionsBackendException(Throwable cause) {
		super(cause);
	}

	public PermissionsBackendException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
