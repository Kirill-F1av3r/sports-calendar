export type AuthResponse = {
  accessToken: string;
  expiresIn: number;
};

export type CreatedIdResponse = {
  id: string;
};

export type CalendarResponse = {
  id: string;
  ownerId: string;
  name: string;
  sportType: string | null;
  year: number | null;
  createdAt: string;
  updatedAt: string;
};

export type EventResponse = {
  id: string;
  calendarId: string;
  title: string;
  startDate: string;
  endDate: string;
  competitionLevel: string | null;
  competitionLevelTitle: string | null;
  location: string | null;
  externalUrl: string | null;
  disciplines: string[];
  priority: string | null;
  priorityTitle: string | null;
  createdAt: string;
  updatedAt: string;
};

export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
};

export type EnumOption = {
  code: string;
  title: string;
};

export type CalendarMetadataResponse = {
  competitionLevels: EnumOption[];
  priorities: EnumOption[];
};

export type IntegrationStatusResponse = {
  provider: string;
  connected: boolean;
  email: string | null;
  scopes: string | null;
};

export type ConnectResponse = {
  redirectUrl: string;
};

export type ExportJobResponse = {
  jobId: string;
  provider: string;
  status: "PENDING" | "PROCESSING" | "SUCCESS" | "FAILED";
  spreadsheetId: string | null;
  spreadsheetUrl: string | null;
  errorMessage: string | null;
};

export type CalendarFormData = {
  name: string;
  sportType: string;
  year: string;
};

export type EventFormData = {
  title: string;
  startDate: string;
  endDate: string;
  competitionLevel: string;
  location: string;
  externalUrl: string;
  disciplines: string;
  priority: string;
};
