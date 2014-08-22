package net.glxn.qrgen.exception;

public class QRGenerationException extends RuntimeException
{
	private static final long serialVersionUID = 5462950734528367920L;

	public QRGenerationException(String message, Throwable underlyingException)
	{
		super( message, underlyingException );
	}
}
