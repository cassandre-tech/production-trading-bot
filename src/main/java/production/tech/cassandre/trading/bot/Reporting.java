package production.tech.cassandre.trading.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * Class doing the reporting (by mail).
 */
@Service
public class Reporting {

    /** Environment. */
    @Autowired
    private Environment environment;

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    /** Mail sender. */
    private final JavaMailSender mailSender;

    /**
     * Constructor.
     *
     * @param newMailSender mail sender
     */
    public Reporting(final JavaMailSender newMailSender) {
        this.mailSender = newMailSender;
    }

    /**
     * Send report.
     *
     * @param messageSubject message subject
     * @param messageBody    message body
     */
    public void sendReport(final String messageSubject, final String messageBody) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("no-reply@cassandre.tech");
        message.setTo("contact@cassandre.tech");
        message.setSubject(messageSubject);
        message.setText(messageBody);

        final String[] activeProfiles = environment.getActiveProfiles();
        if (Arrays.stream(activeProfiles).noneMatch("test"::equalsIgnoreCase)) {
            logger.info("Email sent");
            mailSender.send(message);
        }
    }

}
