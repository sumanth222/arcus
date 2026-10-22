        package com.arcus.arc1.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class RsvpEmailService {

    private final JavaMailSender mailSender;
    private final String recipient;
    private final String sender;

    public RsvpEmailService(
            JavaMailSender mailSender,
            @Value("${rsvp.email.to}") String recipient,
            @Value("${spring.mail.username}") String sender
    ) {
        this.mailSender = mailSender;
        this.recipient = recipient;
        this.sender = sender;
    }

    public void sendGuestList(List<Map<String, Object>> guests) {
        String rows = guests.stream()
                .map(this::toHtmlRow)
                .collect(Collectors.joining());

        String html = """
                <!DOCTYPE html>
                <html>
                <body style="font-family:Arial,sans-serif;color:#45352e">
                    <h2>Wedding RSVP Guest List</h2>
                    <p>Total submissions: %d</p>
                    <table border="1" cellpadding="8" cellspacing="0"
                           style="border-collapse:collapse;width:100%%">
                        <thead>
                            <tr>
                                <th>Name</th>
                                <th>Phone</th>
                                <th>Attendance</th>
                                <th>Guests</th>
                                <th>Arrival</th>
                                <th>Departure</th>
                                <th>Return travel</th>
                            </tr>
                        </thead>
                        <tbody>%s</tbody>
                    </table>
                </body>
                </html>
                """.formatted(guests.size(), rows);

        String plainText = guests.stream()
                .map(this::toPlainText)
                .collect(Collectors.joining("\n\n"));

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(sender);
            helper.setTo(recipient);
            helper.setSubject(
                    "Wedding RSVP Guest List (" + guests.size() + " submissions)"
            );
            helper.setText(plainText, html);

            mailSender.send(mimeMessage);

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Failed to send wedding RSVP guest list email", ex
            );
        }
    }

    private String toHtmlRow(Map<String, Object> guest) {
        return """
                <tr>
                    <td>%s</td>
                    <td>%s</td>
                    <td>%s</td>
                    <td>%s</td>
                    <td>%s</td>
                    <td>%s</td>
                    <td>%s</td>
                </tr>
                """.formatted(
                escape(guest.get("name")),
                escape(guest.get("phone")),
                escape(guest.get("attendance")),
                escape(guest.get("guests")),
                escapeDateTime(
                        guest.get("arrival_date"),
                        guest.get("arrival_time")
                ),
                escapeDateTime(
                        guest.get("departure_date"),
                        guest.get("departure_time")
                ),
                escape(guest.get("travel_back"))
        );
    }

    private String toPlainText(Map<String, Object> guest) {
        return """
                Name: %s
                Phone: %s
                Attendance: %s
                Number of guests: %s
                Arrival: %s
                Departure: %s
                Return travel: %s
                """.formatted(
                safe(guest.get("name")),
                safe(guest.get("phone")),
                safe(guest.get("attendance")),
                safe(guest.get("guests")),
                safeDateTime(
                        guest.get("arrival_date"),
                        guest.get("arrival_time")
                ),
                safeDateTime(
                        guest.get("departure_date"),
                        guest.get("departure_time")
                ),
                safe(guest.get("travel_back"))
        );
    }

    private String escapeDateTime(Object date, Object time) {
        return escape(safeDateTime(date, time));
    }

    private String safeDateTime(Object date, Object time) {
        String dateText = safe(date);
        String timeText = safe(time);

        if (dateText.isBlank()) {
            return timeText;
        }

        return timeText.isBlank()
                ? dateText
                : dateText + " " + timeText;
    }

    private String safe(Object value) {
        return Objects.toString(value, "");
    }

    private String escape(Object value) {
        return safe(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}