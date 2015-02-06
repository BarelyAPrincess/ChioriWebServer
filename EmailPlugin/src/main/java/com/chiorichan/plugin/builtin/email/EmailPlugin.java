package com.chiorichan.plugin.builtin.email;

import java.util.Date;
import java.util.Properties;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.chiorichan.plugin.loader.Plugin;

public class EmailPlugin extends Plugin
{
	public void onEnable()
	{
		saveDefaultConfig();
		getConfig().options().copyDefaults( true );
	}
	
	public void mail( String sendTo, String subject, String message ) throws MessagingException
	{
		String host = getConfig().getString( "mail.host" );
		int port = getConfig().getInt( "mail.smtpport", 25 );
		String user = getConfig().getString( "mail.login" );
		String pass = getConfig().getString( "mail.password" );
		
		Properties props = new Properties();
		props.setProperty( "mail.smtp.host", host );
		props.setProperty( "mail.user", user );
		props.setProperty( "mail.password", pass );
		props.setProperty( "mail.smtp.auth", "true" );
		
		Session session = Session.getInstance( props, null );
		
		try
		{
			MimeMessage msg = new MimeMessage( session );
			msg.setFrom( new InternetAddress( getConfig().getString( "mail.login" ) ) );
			msg.setRecipients( Message.RecipientType.TO, sendTo );
			msg.setSubject( subject );
			msg.setSentDate( new Date() );
			
			BodyPart messageBodyPart = new MimeBodyPart();
			messageBodyPart.setText( message );
			Multipart body = new MimeMultipart();
			body.addBodyPart( messageBodyPart );
			msg.setContent( body, "text/html; charset=utf-8" );
			
			Transport transport = session.getTransport( "smtp" );
			transport.connect( host, port, user, pass );
			transport.sendMessage( msg, msg.getAllRecipients() );
			transport.close();
			
			getLogger().info( "&2Sent message subjected '" + subject + "' to '" + sendTo + "'" );
		}
		catch ( MessagingException mex )
		{
			System.out.println( "send failed, exception: " + mex );
			throw mex;
		}
	}
}

// http://www.tutorialspoint.com/java/java_sending_email.htm
