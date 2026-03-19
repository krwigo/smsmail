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

    public enum DeliveryResult {
        SUCCESS,
        RETRYABLE_FAILURE,
        CONFIG_ERROR,
    }

    public static DeliveryResult sendSmsMail(
        Context context,
        String fromNum,
        String body,
        long timestamp
    ) {
        String subjectPrefix = getSubjectPrefix(context);
        String subject = subjectPrefix + " from " + fromNum;
        String messageBody = body + "\n--\ntimestamp: " + timestamp;
        return sendEmail(context, subject, messageBody);
    }

    public static DeliveryResult sendBatteryLowMail(
        Context context,
        int batteryPercent
    ) {
        String subjectPrefix = getSubjectPrefix(context);
        String subject = subjectPrefix + " battery low";
        String messageBody = "Battery is low.\n\nLevel: " + batteryPercent + "%";
        return sendEmail(context, subject, messageBody);
    }

    public static DeliveryResult sendMissedCallMail(
        Context context,
        String number,
        String cachedName,
        long timestamp
    ) {
        String subjectPrefix = getSubjectPrefix(context);
        String caller = cachedName != null && !cachedName.isEmpty()
            ? cachedName + " (" + number + ")"
            : number;
        String subject = subjectPrefix + " missed call";
        String messageBody =
            "Missed call from: " +
            caller +
            "\nTimestamp: " +
            timestamp;
        return sendEmail(context, subject, messageBody);
    }

    private static DeliveryResult sendEmail(
        Context context,
        String subject,
        String body
    ) {
        Log.d("SMSMAIL", "Mailer.sendEmail");
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

        if (
            host.isEmpty() || user.isEmpty() || pass.isEmpty() || to.isEmpty()
        ) {
            Log.d("SMSMAIL", "Mailer.sendEmail !config");
            return DeliveryResult.CONFIG_ERROR;
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
            msg.setSubject(subject);
            msg.setText(body);

            Transport.send(msg);
            Log.d("SMSMAIL", "Mailer.sendEmail true");
            return DeliveryResult.SUCCESS;
        } catch (MessagingException e) {
            Log.d("SMSMAIL", "Mailer.sendEmail false");
            e.printStackTrace();
            return DeliveryResult.RETRYABLE_FAILURE;
        }
    }

    private static String getSubjectPrefix(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
            "config",
            Context.MODE_PRIVATE
        );
        return prefs.getString("subject", "SMS");
    }
}
