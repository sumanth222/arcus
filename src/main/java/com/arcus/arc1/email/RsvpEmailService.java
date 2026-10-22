package com.arcus.arc1.email;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class RsvpEmailService {

    private final RestClient restClient;
    private final String apiKey;
    private final String sender;
    private final String recipient;

    public RsvpEmailService(
            RestClient.Builder restClientBuilder,
            @Value("${rsvp.email.api-key}") String apiKey,
            @Value("${rsvp.email.from}") String sender,
            @Value("${rsvp.email.to}") String recipient) {

        this.restClient = restClientBuilder
                .baseUrl("https://api.resend.com")
                .build();

        this.apiKey = apiKey;
        this.sender = sender;
        this.recipient = recipient;
    }

    public void sendGuestList(List<Map<String, Object>> guests) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Resend API key is not configured");
        }

        String html = buildHtmlEmail(guests);
        String plainText = buildPlainTextEmail(guests);

        Map<String, Object> payload = Map.of(
                "from", sender,
                "to", List.of(recipient),
                "subject", "Wedding RSVP Guest List",
                "html", html,
                "text", plainText
        );

        try {
            restClient.post()
                    .uri("/emails")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

        } catch (RestClientResponseException ex) {
            // Resend returned an HTTP error. Keep the response body
            // in server logs to help diagnose configuration problems.
            System.err.println(
                    "Resend API returned HTTP "
                            + ex.getStatusCode()
                            + ": " + ex.getResponseBodyAsString());

            throw new IllegalStateException(
                    "Resend rejected the guest-list email request (HTTP "
                            + ex.getStatusCode() + ")",
                    ex);

        } catch (Exception ex) {
            System.err.println(
                    "Failed to send guest-list email: "
                            + ex.getMessage());

            throw new IllegalStateException(
                    "Could not send guest-list email via Resend",
                    ex);
        }
    }

    private String buildHtmlEmail(List<Map<String, Object>> guests) {
        StringBuilder rows = new StringBuilder();

        for (Map<String, Object> guest : guests) {
            rows.append("<tr>");

            appendCell(rows, guest, "name");
            appendCell(rows, guest, "phone");
            appendCell(rows, guest, "attendance");
            appendCell(rows, guest, "guests");
            appendCell(rows, guest, "arrival_date");
            appendCell(rows, guest, "arrival_time");
            appendCell(rows, guest, "departure_date");
            appendCell(rows, guest, "departure_time");
            appendCell(rows, guest, "travel_back");

            rows.append("</tr>");
        }

        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family:Arial,sans-serif;color:#333;">
                  <h2>Wedding RSVP Guest List</h2>
                  <p>Total RSVP submissions: %d</p>
                  <table cellpadding="8" cellspacing="0" border="1"
                         style="border-collapse:collapse;border-color:#ddd;">
                    <thead style="background:#f5eee8;">
                      <tr>
                        <th>Name</th>
                        <th>Phone</th>
                        <th>Attendance</th>
                        <th>Guests</th>
                        <th>Arrival date</th>
                        <th>Arrival time</th>
                        <th>Departure date</th>
                        <th>Departure time</th>
                        <th>Travel back</th>
                      </tr>
                    </thead>
                    <tbody>%s</tbody>
                  </table>
                </body>
                </html>
                """.formatted(guests.size(), rows);
    }

    private void appendCell(
            StringBuilder rows,
            Map<String, Object> guest,
            String key) {

        rows.append("<td>")
                .append(escapeHtml(value(guest.get(key))))
                .append("</td>");
    }

    private String buildPlainTextEmail(
            List<Map<String, Object>> guests) {

        String details = guests.stream()
                .map(guest -> """
                        Name: %s
                        Phone: %s
                        Attendance: %s
                        Guests: %s
                        Arrival: %s %s
                        Departure: %s %s
                        Travel back: %s
                        """.formatted(
                        value(guest.get("name")),
                        value(guest.get("phone")),
                        value(guest.get("attendance")),
                        value(guest.get("guests")),
                        value(guest.get("arrival_date")),
                        value(guest.get("arrival_time")),
                        value(guest.get("departure_date")),
                        value(guest.get("departure_time")),
                        value(guest.get("travel_back"))
                ))
                .collect(Collectors.joining("\n--------------------\n"));

        return "Wedding RSVP Guest List\n"
                + "Total submissions: " + guests.size()
                + "\n\n" + details;
    }

    private String value(Object value) {
        return value == null ? "-" : value.toString();
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}