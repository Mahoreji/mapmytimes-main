package in.mapmytour.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    @Value("${spring.mail.host}")
    private String gmailHost;

    @Value("${spring.mail.port}")
    private int gmailPort;

    @Value("${spring.mail.username}")
    private String gmailUsername;

    @Value("${spring.mail.password}")
    private String gmailPassword;

    @Value("${zoho.mail.host}")
    private String zohoHost;

    @Value("${zoho.mail.port}")
    private int zohoPort;

    @Value("${zoho.mail.username}")
    private String zohoUsername;

    @Value("${zoho.mail.password}")
    private String zohoPassword;

    @Bean
    @Primary
    public JavaMailSender gmailMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(gmailHost);
        mailSender.setPort(gmailPort);
        mailSender.setUsername(gmailUsername);
        mailSender.setPassword(gmailPassword);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.debug", "true");

        return mailSender;
    }

    @Bean(name = "zohoMailSender")
    public JavaMailSender zohoMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(zohoHost);
        mailSender.setPort(zohoPort);
        mailSender.setUsername(zohoUsername);
        mailSender.setPassword(zohoPassword);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.debug", "true");

        return mailSender;
    }
}
