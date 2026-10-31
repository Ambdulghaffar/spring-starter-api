package com.ambdulghaffar.springstarterapi.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {
    private final JavaMailSender mailSender;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.mail-from}")
    private String from;

    @Async
    public void sendVerificationLink(String to, String token) {
        send(to,
                "Confirmez votre adresse email",
                """
                Bonjour,

                Merci de votre inscription. Pour activer votre compte, cliquez sur ce lien (valable 24 heures) :
                %s/verify-email?token=%s

                Si vous n'êtes pas à l'origine de cette inscription, ignorez cet email.
                """.formatted(frontendUrl, token),
                "Verification");
    }

    @Async
    public void sendResetLink(String to, String token) {
        send(to,
                "Réinitialisation de votre mot de passe",
                """
                Bonjour,

                Pour choisir un nouveau mot de passe, cliquez sur ce lien (valable 30 minutes) :
                %s/reset-password?token=%s

                Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.
                """.formatted(frontendUrl, token),
                "Reset");
    }

    private void send(String to, String subject, String text, String label) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("{} email accepted by SMTP server", label);
        } catch (MailException e) {
            log.error("Failed to send {} email", label.toLowerCase(), e);
        }
    }
}