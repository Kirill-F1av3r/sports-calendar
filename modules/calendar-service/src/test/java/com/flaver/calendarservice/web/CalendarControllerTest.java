package com.flaver.calendarservice.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.flaver.calendarservice.dto.CreateCalendarRequest;
import com.flaver.calendarservice.dto.CreateEventRequest;
import com.flaver.calendarservice.entity.Calendar;
import com.flaver.calendarservice.entity.Event;
import com.flaver.calendarservice.service.CalendarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class CalendarControllerTest {
    private CalendarService calendarService;
    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        calendarService = mock(CalendarService.class);
        CalendarController calendarController = new CalendarController(calendarService);
        mockMvc = MockMvcBuilders.standaloneSetup(calendarController).build();
    }

    @Test
    void createCalendar_returns201() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID savedId = UUID.randomUUID();

        Calendar saved = new Calendar();
        saved.setId(savedId);
        saved.setOwnerId(ownerId);
        saved.setName("X");

        when(calendarService.createCalendar(eq(ownerId), any(CreateCalendarRequest.class))).thenReturn(saved);

        String body = mapper.writeValueAsString(new CreateCalendarRequest("X", "Running"));

        mockMvc.perform(post("/calendars")
                        .header("X-User-Id", ownerId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(savedId.toString()));
    }

    @Test
    void list_returnsOk() throws Exception {
        UUID ownerId = UUID.randomUUID();
        Calendar calendar = new Calendar();
        calendar.setOwnerId(ownerId);
        calendar.setName("A");

        when(calendarService.findCalendars(eq(ownerId), any(), any(), any(), any())).thenReturn(List.of(calendar));

        mockMvc.perform(get("/calendars")
                        .header("X-User-Id", ownerId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].name").value("A"));
    }

    @Test
    void get_returnsCalendarOnly() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID calendarId = UUID.randomUUID();
        Calendar calendar = new Calendar();
        calendar.setId(calendarId);
        calendar.setOwnerId(ownerId);
        calendar.setName("C");

        when(calendarService.findOwnedOrThrow(calendarId, ownerId)).thenReturn(calendar);

        mockMvc.perform(get("/calendars/" + calendarId)
                        .header("X-User-Id", ownerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(calendarId.toString()))
                .andExpect(jsonPath("$.name").value("C"))
                .andExpect(jsonPath("$.events").doesNotExist());
    }

    @Test
    void addEvent_returns201() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID calendarId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        when(calendarService.addEvent(eq(calendarId), eq(ownerId), any(CreateEventRequest.class)))
                .thenAnswer(inv -> {
                    Event ev = new Event();
                    ev.setId(eventId);
                    ev.setCalendarId(calendarId);
                    ev.setTitle("Title");
                    ev.setStartDate(LocalDate.parse("2026-05-01"));
                    ev.setEndDate(LocalDate.parse("2026-05-02"));
                    return ev;
                });

        String body = mapper.writeValueAsString(new CreateEventRequest("Title", LocalDate.parse("2026-05-01"),
                LocalDate.parse("2026-05-02"), "Loc", null));

        mockMvc.perform(post("/calendars/" + calendarId + "/events")
                        .header("X-User-Id", ownerId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(eventId.toString()));
    }
}
