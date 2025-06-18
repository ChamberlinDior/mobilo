package com.mobility.auth.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.*;

import java.util.Properties;

@Configuration
public class MailConfig {

    private final Logger log = LoggerFactory.getLogger(MailConfig.class);

    // ==== PROD : JavaMailSender pointant vers votre smarthost ====
    @Bean
    @Profile("!local")
    public JavaMailSender javaMailSenderProd(
            @Value("${spring.mail.host}") String host,
            @Value("${spring.mail.port}") int port,
            @Value("${spring.mail.username}") String username,
            @Value("${spring.mail.password}") String password,
            @Value("${spring.mail.properties.mail.smtp.auth}") boolean auth,
            @Value("${spring.mail.properties.mail.smtp.starttls.enable}") boolean starttls
    ) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", String.valueOf(auth));
        props.put("mail.smtp.starttls.enable", String.valueOf(starttls));
        props.put("mail.debug", "false");

        return mailSender;
    }

    // ==== LOCAL : stub l'envoi dans les logs (sans appel réseau) ====
    @Bean
    @Profile("local")
    public JavaMailSender javaMailSenderLocal() {
        return new JavaMailSenderImpl() {
            @Override
            public void send(SimpleMailMessage message) {
                log.debug("✉️  [MAIL SIMULÉ] To={}  Subject={}  Body={}​",
                        String.join(",", message.getTo()),
                        message.getSubject(),
                        message.getText());
            }
            @Override
            public void send(SimpleMailMessage... messages) {
                for (SimpleMailMessage m : messages) send(m);
            }
            // Si vous utilisez MimeMessage, override aussi send(MimeMessage…)
        };
    }
}
