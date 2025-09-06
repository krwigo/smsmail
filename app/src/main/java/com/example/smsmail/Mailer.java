package com.example.smsmail;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

public class Mailer {

    public static boolean sendMail(
        Context context,
        String fromNum,
        String body,
        long timestamp
    ) {
        Log.d("SMSMAIL", "Mailer.sendMail");

        SharedPreferences prefs = context.getSharedPreferences(
            "config",
            Context.MODE_PRIVATE
        );
        String host = prefs.getString("host", "");
        String port = prefs.getString("port", "587");
        String user = prefs.getString("user", "");
        String pass = prefs.getString("pass", "");
        String from = prefs.getString("from", user);
        String to = prefs.getString("to", user);
        String subjectPrefix = prefs.getString("subject", "SMS");

        if (
            host.isEmpty() || user.isEmpty() || pass.isEmpty() || to.isEmpty()
        ) {
            Log.d("SMSMAIL", "Mailer.sendMail !config");
            return false;
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(
            props,
            new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass);
                }
            }
        );

        try {
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(from));
            msg.setRecipients(
                Message.RecipientType.TO,
                InternetAddress.parse(to)
            );
            msg.setSubject(subjectPrefix + " from " + fromNum);
            msg.setText(body + "\n--\ntimestamp: " + timestamp);

            Transport.send(msg);
            Log.d("SMSMAIL", "Mailer.sendMail true");
            return true;
        } catch (MessagingException e) {
            Log.d("SMSMAIL", "Mailer.sendMail false");
            e.printStackTrace();
            return false;
        }
    }
}
