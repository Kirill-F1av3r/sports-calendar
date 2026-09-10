import type {
  AuthResponse,
  CalendarFormData,
  CalendarMetadataResponse,
  CalendarResponse,
  ConnectResponse,
  CreatedIdResponse,
  EventFormData,
  EventResponse,
  ExportJobResponse,
  IntegrationStatusResponse,
  PageResponse
} from "./types";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

type QueryValue = string | number | null | undefined;

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number
  ) {
    super(message);
  }
}

async function request<T>(
  path: string,
  options: RequestInit = {},
  accessToken?: string | null
): Promise<T> {
  const headers = new Headers(options.headers);

  if (options.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  if (accessToken) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
    credentials: "include"
  });

  if (!response.ok) {
    throw new ApiError(await errorMessage(response), response.status);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

async function errorMessage(response: Response): Promise<string> {
  const fallback = `HTTP ${response.status}`;
  const text = await response.text();
  if (!text) {
    return fallback;
  }

  try {
    const parsed = JSON.parse(text) as { message?: string; details?: string[] };
    if (parsed.details?.length) {
      return `${parsed.message ?? fallback}: ${parsed.details.join(", ")}`;
    }
    return parsed.message ?? text;
  } catch {
    return text;
  }
}

function query(params: Record<string, QueryValue>): string {
  const searchParams = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== null && value !== undefined && value !== "") {
      searchParams.set(key, String(value));
    }
  });
  const value = searchParams.toString();
  return value ? `?${value}` : "";
}

function calendarBody(data: CalendarFormData) {
  return {
    name: data.name.trim(),
    sportType: data.sportType.trim(),
    year: data.year ? Number(data.year) : null
  };
}

function eventBody(data: EventFormData) {
  return {
    title: data.title.trim(),
    startDate: data.startDate,
    endDate: data.endDate || null,
    competitionLevel: data.competitionLevel || null,
    location: data.location.trim(),
    externalUrl: data.externalUrl.trim(),
    disciplines: data.disciplines
      .split(",")
      .map((value) => value.trim())
      .filter(Boolean),
    priority: data.priority || null
  };
}

export const api = {
  register(email: string, password: string, fullName: string) {
    return request<CreatedIdResponse>("/auth/register", {
      method: "POST",
      body: JSON.stringify({ email, password, fullName })
    });
  },

  login(email: string, password: string) {
    return request<AuthResponse>("/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password })
    });
  },

  refresh() {
    return request<AuthResponse>("/auth/refresh", { method: "POST" });
  },

  logout() {
    return request<void>("/auth/logout", { method: "POST" });
  },

  metadata() {
    return request<CalendarMetadataResponse>("/calendars/metadata");
  },

  listCalendars(
    accessToken: string,
    params: { year?: string; sportType?: string; search?: string; sort?: string }
  ) {
    return request<CalendarResponse[]>(
      `/calendars${query(params)}`,
      undefined,
      accessToken
    );
  },

  getCalendar(accessToken: string, calendarId: string) {
    return request<CalendarResponse>(`/calendars/${calendarId}`, undefined, accessToken);
  },

  createCalendar(accessToken: string, data: CalendarFormData) {
    return request<CalendarResponse>(
      "/calendars",
      {
        method: "POST",
        body: JSON.stringify(calendarBody(data))
      },
      accessToken
    );
  },

  updateCalendar(accessToken: string, calendarId: string, data: CalendarFormData) {
    return request<CalendarResponse>(
      `/calendars/${calendarId}`,
      {
        method: "PATCH",
        body: JSON.stringify(calendarBody(data))
      },
      accessToken
    );
  },

  deleteCalendar(accessToken: string, calendarId: string) {
    return request<void>(`/calendars/${calendarId}`, { method: "DELETE" }, accessToken);
  },

  copyCalendar(
    accessToken: string,
    calendarId: string,
    data: CalendarFormData,
    filters: {
      from?: string;
      to?: string;
      competitionLevel?: string;
      priority?: string;
      search?: string;
    }
  ) {
    return request<CalendarResponse>(
      `/calendars/${calendarId}/copies${query(filters)}`,
      {
        method: "POST",
        body: JSON.stringify(calendarBody(data))
      },
      accessToken
    );
  },

  listEvents(
    accessToken: string,
    calendarId: string,
    params: {
      from?: string;
      to?: string;
      competitionLevel?: string;
      priority?: string;
      search?: string;
      page?: number;
      size?: number;
      sort?: string;
    }
  ) {
    return request<PageResponse<EventResponse>>(
      `/calendars/${calendarId}/events${query(params)}`,
      undefined,
      accessToken
    );
  },

  createEvent(accessToken: string, calendarId: string, data: EventFormData) {
    return request<EventResponse>(
      `/calendars/${calendarId}/events`,
      {
        method: "POST",
        body: JSON.stringify(eventBody(data))
      },
      accessToken
    );
  },

  updateEvent(accessToken: string, calendarId: string, eventId: string, data: EventFormData) {
    return request<EventResponse>(
      `/calendars/${calendarId}/events/${eventId}`,
      {
        method: "PATCH",
        body: JSON.stringify(eventBody(data))
      },
      accessToken
    );
  },

  deleteEvent(accessToken: string, calendarId: string, eventId: string) {
    return request<void>(
      `/calendars/${calendarId}/events/${eventId}`,
      { method: "DELETE" },
      accessToken
    );
  },

  integrationStatus(accessToken: string) {
    return request<IntegrationStatusResponse>(
      "/integrations/google/status",
      undefined,
      accessToken
    );
  },

  connectGoogle(accessToken: string) {
    return request<ConnectResponse>("/integrations/google/connect", undefined, accessToken);
  },

  disconnectGoogle(accessToken: string) {
    return request<void>("/integrations/google", { method: "DELETE" }, accessToken);
  },

  createExport(accessToken: string, calendarId: string) {
    return request<ExportJobResponse>(
      "/exports",
      {
        method: "POST",
        body: JSON.stringify({ calendarId, provider: "GOOGLE_SHEETS" })
      },
      accessToken
    );
  },

  getExport(accessToken: string, jobId: string) {
    return request<ExportJobResponse>(`/exports/${jobId}`, undefined, accessToken);
  }
};
