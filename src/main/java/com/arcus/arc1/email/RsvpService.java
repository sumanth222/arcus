
package com.arcus.arc1.rsvp;

import com.arcus.arc1.email.RsvpEmailService;
import com.arcus.arc1.email.RsvpRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.sql.Time;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class RsvpService {

    private final JdbcTemplate jdbc;
    private final RsvpEmailService emailService;

    public RsvpService(
            JdbcTemplate jdbc,
            RsvpEmailService emailService) {
        this.jdbc = jdbc;
        this.emailService = emailService;
    }

    public Map<String, Object> submit(RsvpRequest request) {
        String attendance = request.attendance()
                .trim()
                .toUpperCase(Locale.ROOT);

        String travelBack = request.travelBack() == null
                || request.travelBack().isBlank()
                ? null
                : switch (request.travelBack().trim()
                .toLowerCase(Locale.ROOT)) {
            case "flight" -> "Flight";
            case "train" -> "Train";
            case "bus" -> "Bus";
            default -> throw new IllegalArgumentException(
                    "Return travel must be Flight, Train, or Bus");
        };

        // Save the RSVP first.
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

        // Fetch the full guest list, including the latest RSVP.
        List<Map<String, Object>> guests = jdbc.queryForList("""
                SELECT id, name, phone, attendance, guests,
                       arrival_date, arrival_time,
                       departure_date, departure_time,
                       travel_back, created_at
                FROM public.wedding_rsvps
                ORDER BY created_at DESC
                """);

        boolean emailSent;

        try {
            emailService.sendGuestList(guests);
            emailSent = true;
        } catch (Exception ex) {
            emailSent = false;
            System.err.println(
                    "RSVP saved, but Resend email failed: "
                            + ex.getMessage());
            ex.printStackTrace();
        }

        return Map.of(
                "success", true,
                "message", "RSVP saved successfully",
                "emailSent", emailSent
        );
    }
}