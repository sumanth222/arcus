package com.arcus.arc1.rsvp;

import com.arcus.arc1.email.RsvpRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.sql.Time;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class RsvpService {

    private final JdbcTemplate jdbc;
    private final JavaMailSender mailSender;
    private final String emailTo;
    private final String emailFrom;

    public RsvpService(
            JdbcTemplate jdbc,
            JavaMailSender mailSender,
            @Value("${rsvp.email.to}") String emailTo,
            @Value("${spring.mail.username}") String emailFrom
    ) {
        this.jdbc = jdbc;
        this.mailSender = mailSender;
        this.emailTo = emailTo;
        this.emailFrom = emailFrom;
    }

    public Map<String, Object> submit(RsvpRequest request) {
        String attendance = request.attendance()
                .trim()
                .toUpperCase(Locale.ROOT);

        String travelBack = request.travelBack() == null
                || request.travelBack().isBlank()
                ? null
                : request.travelBack().trim();

        if (travelBack != null) {
            travelBack = switch (travelBack.toLowerCase(Locale.ROOT)) {
                case "flight" -> "Flight";
                case "train" -> "Train";
                case "bus" -> "Bus";
                default -> throw new IllegalArgumentException(
                        "Return travel must be Flight, Train, or Bus"
                );
            };
        }

        // Save RSVP to Supabase PostgreSQL.
        jdbc.update("""
                INSERT INTO public.wedding_rsvps (
                    name, phone, attendance, guests,
                    arrival_date, arrival_time,
                    departure_date, departure_time, travel_back
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                request.name().trim(),
                request.phone().trim(),
                attendance,
                request.guests(),
                request.arrivalDate() == null
                        ? null : Date.valueOf(request.arrivalDate()),
                request.arrivalTime() == null
                        ? null : Time.valueOf(request.arrivalTime()),
                request.departureDate() == null
                        ? null : Date.valueOf(request.departureDate()),
                request.departureTime() == null
                        ? null : Time.valueOf(request.departureTime()),
                travelBack
        );

        // Fetch the complete guest list.
        List<Map<String, Object>> guests = jdbc.queryForList("""
                SELECT id, name, phone, attendance, guests,
                       arrival_date, arrival_time,
                       departure_date, departure_time,
                       travel_back, created_at
                FROM public.wedding_rsvps
                ORDER BY created_at DESC
                """);

        boolean emailSent = true;

        try {
            sendGuestListEmail(guests);
        } catch (Exception ex) {
            emailSent = false;
            System.err.println(
                    "RSVP saved, but guest-list email failed: "
                            + ex.getMessage()
            );
        }

        return Map.of(
                "success", true,
                "message", "RSVP saved successfully",
                "emailSent", emailSent
        );
    }

    private void sendGuestListEmail(
            List<Map<String, Object>> guests
    ) {
        String rows = guests.stream()
                .map(this::toHtmlRow)
                .collect(Collectors.joining());

        String html = """
                <h2>Wedding RSVP Guest List</h2>
                <p>Total submissions: %d</p>
                <table border="1" cellpadding="8" cellspacing="0"
                       style="border-collapse:collapse;font-family:Arial,sans-serif">
                  <thead>
                    <tr>
                      <th>Name</th><th>Phone</th><th>Attendance</th>
                      <th>Guests</th><th>Arrival</th><th>Departure</th>
                      <th>Return travel</th>
                    </tr>
                  </thead>
                  <tbody>%s</tbody>
                </table>
                """.formatted(guests.size(), rows);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(emailFrom);
        message.setTo(emailTo);
        message.setSubject(
                "Wedding RSVP Guest List (" + guests.size() + " submissions)"
        );
        message.setText(
                guests.stream()
                        .map(this::toPlainText)
                        .collect(Collectors.joining("\n\n"))
        );

        // Send an HTML email using JavaMailSender.
        // Use MimeMessageHelper so the HTML table renders correctly.
        try {
            var mimeMessage = mailSender.createMimeMessage();
            var helper = new org.springframework.mail.javamail.MimeMessageHelper(
                    mimeMessage, true, "UTF-8"
            );
            helper.setFrom(emailFrom);
            helper.setTo(emailTo);
            helper.setSubject(
                    "Wedding RSVP Guest List (" + guests.size() + " submissions)"
            );
            helper.setText(
                    guests.stream()
                            .map(this::toPlainText)
                            .collect(Collectors.joining("\n\n")),
                    html
            );
            mailSender.send(mimeMessage);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not send guest-list email", ex);
        }
    }

    private String toPlainText(Map<String, Object> guest) {
        return """
                Name: %s
                Phone: %s
                Attendance: %s
                Guests: %s
                Arrival: %s
                Departure: %s
                Return travel: %s
                """.formatted(
                guest.get("name"),
                guest.get("phone"),
                guest.get("attendance"),
                guest.get("guests"),
                guest.get("arrival_date") + " " + guest.get("arrival_time"),
                guest.get("departure_date") + " " + guest.get("departure_time"),
                guest.get("travel_back")
        );
    }

    private String toHtmlRow(Map<String, Object> guest) {
        return """
                <tr>
                  <td>%s</td><td>%s</td><td>%s</td>
                  <td>%s</td><td>%s</td><td>%s</td><td>%s</td>
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

    private String escapeDateTime(Object date, Object time) {
        String dateText = date == null ? "" : date.toString();
        String timeText = time == null ? "" : time.toString();
        return escape(dateText + (timeText.isBlank() ? "" : " " + timeText));
    }

    private String escape(Object value) {
        if (value == null) {
            return "";
        }

        return Objects.toString(value, "")
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}