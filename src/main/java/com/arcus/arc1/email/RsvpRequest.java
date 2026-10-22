package com.arcus.arc1.email;

import java.time.LocalDate;
import java.time.LocalTime;

public record RsvpRequest(

        String name,
        String phone,

        String attendance,

        Integer guests,

        LocalDate arrivalDate,

        LocalTime arrivalTime,

        LocalDate departureDate,

        LocalTime departureTime,

        String travelBack
) {}