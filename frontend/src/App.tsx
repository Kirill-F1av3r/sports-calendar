import { useEffect, useMemo, useState } from "react";
import { api, ApiError } from "./api";
import type {
  CalendarFormData,
  CalendarMetadataResponse,
  CalendarResponse,
  EnumOption,
  EventFormData,
  EventResponse,
  ExportJobResponse,
  IntegrationStatusResponse,
  PageResponse
} from "./types";

const emptyCalendarForm: CalendarFormData = {
  name: "",
  sportType: "",
  year: new Date().getFullYear().toString()
};

const emptyEventForm: EventFormData = {
  title: "",
  startDate: "",
  endDate: "",
  competitionLevel: "OTHER",
  location: "",
  externalUrl: "",
  disciplines: "",
  priority: "OPTIONAL"
};

type View =
  | { name: "calendars" }
  | { name: "calendar"; calendarId: string };

export default function App() {
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [booting, setBooting] = useState(true);
  const [view, setView] = useState<View>({ name: "calendars" });
  const [globalError, setGlobalError] = useState<string | null>(null);

  useEffect(() => {
    api
      .refresh()
      .then((response) => setAccessToken(response.accessToken))
      .catch(() => setAccessToken(null))
      .finally(() => setBooting(false));
  }, []);

  async function logout() {
    try {
      await api.logout();
    } catch {
      // Если logout на backend уже невозможен, локальный выход всё равно нужен.
    }
    setAccessToken(null);
    setView({ name: "calendars" });
  }

  if (booting) {
    return <FullPageMessage title="Загрузка" text="Проверяем активную сессию..." />;
  }

  if (!accessToken) {
    return (
      <AuthPage
        onLogin={(token) => {
          setAccessToken(token);
          setGlobalError(null);
        }}
      />
    );
  }

  return (
    <div className="app">
      <header className="header">
        <button className="brand" onClick={() => setView({ name: "calendars" })}>
          Sports Calendar
        </button>
        <div className="header-actions">
          <button className="secondary" onClick={logout}>
            Выйти
          </button>
        </div>
      </header>

      <main className="main">
        {globalError && <Alert type="error" message={globalError} />}
        {view.name === "calendars" && (
          <CalendarsPage
            accessToken={accessToken}
            onOpen={(calendarId) => setView({ name: "calendar", calendarId })}
            onUnauthorized={() => setAccessToken(null)}
            onError={setGlobalError}
          />
        )}
        {view.name === "calendar" && (
          <CalendarPage
            accessToken={accessToken}
            calendarId={view.calendarId}
            onBack={() => setView({ name: "calendars" })}
            onOpen={(calendarId) => setView({ name: "calendar", calendarId })}
            onUnauthorized={() => setAccessToken(null)}
            onError={setGlobalError}
          />
        )}
      </main>
    </div>
  );
}

function AuthPage({ onLogin }: { onLogin: (accessToken: string) => void }) {
  const [mode, setMode] = useState<"login" | "register">("login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [fullName, setFullName] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    setLoading(true);
    setError(null);
    try {
      if (mode === "register") {
        await api.register(email, password, fullName);
      }
      const response = await api.login(email, password);
      onLogin(response.accessToken);
    } catch (err) {
      setError(readError(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-page">
      <form className="card auth-card" onSubmit={submit}>
        <h1>Sports Calendar</h1>

        <div className="tabs">
          <button
            type="button"
            className={mode === "login" ? "active" : ""}
            onClick={() => setMode("login")}
          >
            Вход
          </button>
          <button
            type="button"
            className={mode === "register" ? "active" : ""}
            onClick={() => setMode("register")}
          >
            Регистрация
          </button>
        </div>

        {error && <Alert type="error" message={error} />}

        <label>
          Email
          <input
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            type="email"
            placeholder="user@example.com"
            required
          />
        </label>
        <label>
          Пароль
          <input
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            type="password"
            required
          />
        </label>
        {mode === "register" && (
          <label>
            Имя
            <input value={fullName} onChange={(event) => setFullName(event.target.value)} required />
          </label>
        )}

        <button className="primary" disabled={loading}>
          {loading ? "Отправка..." : mode === "login" ? "Войти" : "Зарегистрироваться и войти"}
        </button>
      </form>
    </div>
  );
}

function CalendarsPage({
  accessToken,
  onOpen,
  onUnauthorized,
  onError
}: {
  accessToken: string;
  onOpen: (calendarId: string) => void;
  onUnauthorized: () => void;
  onError: (message: string | null) => void;
}) {
  const [calendars, setCalendars] = useState<CalendarResponse[]>([]);
  const [filters, setFilters] = useState({ search: "", year: "", sportType: "", sort: "updated,desc" });
  const [form, setForm] = useState<CalendarFormData>(emptyCalendarForm);
  const [editing, setEditing] = useState<CalendarResponse | null>(null);
  const [editForm, setEditForm] = useState<CalendarFormData>(emptyCalendarForm);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  async function loadCalendars() {
    setLoading(true);
    setMessage(null);
    try {
      const data = await api.listCalendars(accessToken, filters);
      setCalendars(data);
      onError(null);
    } catch (err) {
      handleError(err, onUnauthorized, onError);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadCalendars();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [accessToken]);

  async function createCalendar(event: React.FormEvent) {
    event.preventDefault();
    try {
      await api.createCalendar(accessToken, form);
      setForm(emptyCalendarForm);
      setMessage("Календарь создан.");
      await loadCalendars();
    } catch (err) {
      handleError(err, onUnauthorized, onError);
    }
  }

  async function updateCalendar(event: React.FormEvent) {
    event.preventDefault();
    if (!editing) {
      return;
    }
    try {
      await api.updateCalendar(accessToken, editing.id, editForm);
      setEditing(null);
      setMessage("Календарь обновлён.");
      await loadCalendars();
    } catch (err) {
      handleError(err, onUnauthorized, onError);
    }
  }

  async function deleteCalendar(calendar: CalendarResponse) {
    if (!window.confirm(`Удалить календарь "${calendar.name}" и все его события?`)) {
      return;
    }
    try {
      await api.deleteCalendar(accessToken, calendar.id);
      setMessage("Календарь удалён.");
      await loadCalendars();
    } catch (err) {
      handleError(err, onUnauthorized, onError);
    }
  }

  function startEdit(calendar: CalendarResponse) {
    setEditing(calendar);
    setEditForm({
      name: calendar.name,
      sportType: calendar.sportType ?? "",
      year: calendar.year?.toString() ?? ""
    });
  }

  return (
    <div className="page-grid">
      <section className="content">
        <div className="section-header">
          <div>
            <h2>Календари</h2>
            <p className="muted">Создавай, редактируй и открывай календари соревнований.</p>
          </div>
          <button className="secondary" onClick={loadCalendars} disabled={loading}>
            Обновить
          </button>
        </div>

        {message && <Alert type="success" message={message} />}

        <div className="card filters">
          <input
            placeholder="Поиск"
            value={filters.search}
            onChange={(event) => setFilters({ ...filters, search: event.target.value })}
          />
          <input
            placeholder="Год"
            value={filters.year}
            onChange={(event) => setFilters({ ...filters, year: event.target.value })}
            type="number"
          />
          <input
            placeholder="Вид спорта"
            value={filters.sportType}
            onChange={(event) => setFilters({ ...filters, sportType: event.target.value })}
          />
          <select value={filters.sort} onChange={(event) => setFilters({ ...filters, sort: event.target.value })}>
            <option value="updated,desc">Недавно обновлённые</option>
            <option value="created,desc">Недавно созданные</option>
            <option value="year,desc">Год ↓</option>
            <option value="year,asc">Год ↑</option>
            <option value="name,asc">Название А-Я</option>
          </select>
          <button className="primary" onClick={loadCalendars} disabled={loading}>
            Применить
          </button>
        </div>

        <div className="card table-card">
          {loading ? (
            <p className="muted">Загрузка...</p>
          ) : calendars.length === 0 ? (
            <p className="muted">Календарей пока нет.</p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Название</th>
                  <th>Вид спорта</th>
                  <th>Год</th>
                  <th>Обновлён</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {calendars.map((calendar) => (
                  <tr key={calendar.id}>
                    <td>{calendar.name}</td>
                    <td>{calendar.sportType || "—"}</td>
                    <td>{calendar.year || "—"}</td>
                    <td>{formatDateTime(calendar.updatedAt)}</td>
                    <td className="row-actions">
                      <button onClick={() => onOpen(calendar.id)}>Открыть</button>
                      <button onClick={() => startEdit(calendar)}>Редактировать</button>
                      <button className="danger" onClick={() => deleteCalendar(calendar)}>
                        Удалить
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </section>

      <aside className="sidebar">
        <CalendarForm title="Создать календарь" form={form} setForm={setForm} onSubmit={createCalendar} />

        {editing && (
          <CalendarForm
            title={`Редактировать: ${editing.name}`}
            form={editForm}
            setForm={setEditForm}
            onSubmit={updateCalendar}
            onCancel={() => setEditing(null)}
          />
        )}

        <GoogleIntegration accessToken={accessToken} onUnauthorized={onUnauthorized} onError={onError} />
      </aside>
    </div>
  );
}

function CalendarPage({
  accessToken,
  calendarId,
  onBack,
  onOpen,
  onUnauthorized,
  onError
}: {
  accessToken: string;
  calendarId: string;
  onBack: () => void;
  onOpen: (calendarId: string) => void;
  onUnauthorized: () => void;
  onError: (message: string | null) => void;
}) {
  const [calendar, setCalendar] = useState<CalendarResponse | null>(null);
  const [metadata, setMetadata] = useState<CalendarMetadataResponse | null>(null);
  const [events, setEvents] = useState<PageResponse<EventResponse> | null>(null);
  const [filters, setFilters] = useState({
    search: "",
    from: "",
    to: "",
    competitionLevel: "",
    priority: "",
    page: 0,
    size: 20,
    sort: "date,asc"
  });
  const [form, setForm] = useState<EventFormData>(emptyEventForm);
  const [editing, setEditing] = useState<EventResponse | null>(null);
  const [editForm, setEditForm] = useState<EventFormData>(emptyEventForm);
  const [copyForm, setCopyForm] = useState<CalendarFormData>(emptyCalendarForm);
  const [copiedCalendar, setCopiedCalendar] = useState<CalendarResponse | null>(null);
  const [exportJob, setExportJob] = useState<ExportJobResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  async function loadPage(nextFilters = filters) {
    setLoading(true);
    setMessage(null);
    try {
      const [calendarData, metadataData, eventsData] = await Promise.all([
        api.getCalendar(accessToken, calendarId),
        api.metadata(),
        api.listEvents(accessToken, calendarId, nextFilters)
      ]);
      setCalendar(calendarData);
      setMetadata(metadataData);
      setEvents(eventsData);
      onError(null);
    } catch (err) {
      handleError(err, onUnauthorized, onError);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadPage();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [accessToken, calendarId]);

  useEffect(() => {
    if (!calendar || copyForm.name) {
      return;
    }

    setCopyForm({
      name: `${calendar.name} — выборка`,
      sportType: calendar.sportType ?? "",
      year: calendar.year?.toString() ?? new Date().getFullYear().toString()
    });
  }, [calendar, copyForm.name]);

  useEffect(() => {
    if (!exportJob || exportJob.status === "SUCCESS" || exportJob.status === "FAILED") {
      return;
    }

    const timer = window.setInterval(() => {
      api
        .getExport(accessToken, exportJob.jobId)
        .then(setExportJob)
        .catch((err) => handleError(err, onUnauthorized, onError));
    }, 2500);

    return () => window.clearInterval(timer);
  }, [accessToken, exportJob, onError, onUnauthorized]);

  async function createEvent(event: React.FormEvent) {
    event.preventDefault();
    try {
      await api.createEvent(accessToken, calendarId, form);
      setForm(emptyEventForm);
      setMessage("Событие создано.");
      await loadPage();
    } catch (err) {
      handleError(err, onUnauthorized, onError);
    }
  }

  async function updateEvent(event: React.FormEvent) {
    event.preventDefault();
    if (!editing) {
      return;
    }
    try {
      await api.updateEvent(accessToken, calendarId, editing.id, editForm);
      setEditing(null);
      setMessage("Событие обновлено.");
      await loadPage();
    } catch (err) {
      handleError(err, onUnauthorized, onError);
    }
  }

  async function deleteEvent(event: EventResponse) {
    if (!window.confirm(`Удалить событие "${event.title}"?`)) {
      return;
    }
    try {
      await api.deleteEvent(accessToken, calendarId, event.id);
      setMessage("Событие удалено.");
      await loadPage();
    } catch (err) {
      handleError(err, onUnauthorized, onError);
    }
  }

  async function copyCalendarFromFilters(event: React.FormEvent) {
    event.preventDefault();
    try {
      const copied = await api.copyCalendar(accessToken, calendarId, copyForm, {
        from: filters.from,
        to: filters.to,
        competitionLevel: filters.competitionLevel,
        priority: filters.priority,
        search: filters.search
      });
      setCopiedCalendar(copied);
      setMessage(`Календарь "${copied.name}" создан.`);
      onError(null);
    } catch (err) {
      handleError(err, onUnauthorized, onError);
    }
  }

  async function startExport() {
    try {
      const job = await api.createExport(accessToken, calendarId);
      setExportJob(job);
      setMessage("Экспорт запущен.");
    } catch (err) {
      handleError(err, onUnauthorized, onError);
    }
  }

  function applyFilters() {
    const nextFilters = { ...filters, page: 0 };
    setFilters(nextFilters);
    void loadPage(nextFilters);
  }

  function changePage(page: number) {
    const nextFilters = { ...filters, page };
    setFilters(nextFilters);
    void loadPage(nextFilters);
  }

  function startEdit(event: EventResponse) {
    setEditing(event);
    setEditForm({
      title: event.title,
      startDate: event.startDate,
      endDate: event.endDate,
      competitionLevel: event.competitionLevel ?? "OTHER",
      location: event.location ?? "",
      externalUrl: event.externalUrl ?? "",
      disciplines: event.disciplines.join(", "),
      priority: event.priority ?? "OPTIONAL"
    });
  }

  const competitionLevels = metadata?.competitionLevels ?? [];
  const priorities = metadata?.priorities ?? [];

  return (
    <div className="page-grid">
      <section className="content">
        <button className="link-button" onClick={onBack}>
          ← К календарям
        </button>

        <div className="section-header">
          <div>
            <h2>{calendar?.name ?? "Календарь"}</h2>
            <p className="muted">
              {calendar?.sportType || "Вид спорта не указан"} · {calendar?.year || "год не указан"}
            </p>
          </div>
          <button className="secondary" onClick={() => loadPage()} disabled={loading}>
            Обновить
          </button>
        </div>

        {message && <Alert type="success" message={message} />}

        <div className="card filters event-filters">
          <input
            placeholder="Поиск"
            value={filters.search}
            onChange={(event) => setFilters({ ...filters, search: event.target.value })}
          />
          <input
            type="date"
            value={filters.from}
            onChange={(event) => setFilters({ ...filters, from: event.target.value })}
          />
          <input
            type="date"
            value={filters.to}
            onChange={(event) => setFilters({ ...filters, to: event.target.value })}
          />
          <select
            value={filters.competitionLevel}
            onChange={(event) => setFilters({ ...filters, competitionLevel: event.target.value })}
          >
            <option value="">Все уровни</option>
            {competitionLevels.map((option) => (
              <option key={option.code} value={option.code}>
                {option.title}
              </option>
            ))}
          </select>
          <select value={filters.priority} onChange={(event) => setFilters({ ...filters, priority: event.target.value })}>
            <option value="">Все приоритеты</option>
            {priorities.map((option) => (
              <option key={option.code} value={option.code}>
                {option.title}
              </option>
            ))}
          </select>
          <select value={filters.sort} onChange={(event) => setFilters({ ...filters, sort: event.target.value })}>
            <option value="date,asc">Дата ↑</option>
            <option value="date,desc">Дата ↓</option>
            <option value="title,asc">Название А-Я</option>
            <option value="priority,desc">Приоритет ↓</option>
            <option value="level,asc">Уровень ↑</option>
            <option value="updated,desc">Недавно обновлённые</option>
          </select>
          <button className="primary" onClick={applyFilters} disabled={loading}>
            Применить
          </button>
        </div>

        <div className="card table-card">
          {loading ? (
            <p className="muted">Загрузка...</p>
          ) : !events || events.content.length === 0 ? (
            <p className="muted">Событий пока нет.</p>
          ) : (
            <>
              <table>
                <thead>
                  <tr>
                    <th>Дата</th>
                    <th>Название</th>
                    <th>Уровень</th>
                    <th>Дисциплины</th>
                    <th>Место</th>
                    <th>Приоритет</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {events.content.map((event) => (
                    <tr key={event.id}>
                      <td>{formatDateRange(event.startDate, event.endDate)}</td>
                      <td>{event.title}</td>
                      <td>{event.competitionLevelTitle || event.competitionLevel || "—"}</td>
                      <td>{event.disciplines.length ? event.disciplines.join(", ") : "—"}</td>
                      <td>{event.location || "—"}</td>
                      <td>{event.priorityTitle || event.priority || "—"}</td>
                      <td className="row-actions">
                        {event.externalUrl && (
                          <a href={event.externalUrl} target="_blank" rel="noreferrer">
                            Ссылка
                          </a>
                        )}
                        <button onClick={() => startEdit(event)}>Редактировать</button>
                        <button className="danger" onClick={() => deleteEvent(event)}>
                          Удалить
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>

              <div className="pagination">
                <button disabled={events.page <= 0} onClick={() => changePage(events.page - 1)}>
                  Назад
                </button>
                <span>
                  Страница {events.page + 1} из {Math.max(events.totalPages, 1)} · всего {events.totalElements}
                </span>
                <button disabled={events.last} onClick={() => changePage(events.page + 1)}>
                  Вперёд
                </button>
              </div>
            </>
          )}
        </div>
      </section>

      <aside className="sidebar">
        <EventForm
          title="Создать событие"
          form={form}
          setForm={setForm}
          onSubmit={createEvent}
          competitionLevels={competitionLevels}
          priorities={priorities}
        />

        {editing && (
          <EventForm
            title={`Редактировать: ${editing.title}`}
            form={editForm}
            setForm={setEditForm}
            onSubmit={updateEvent}
            onCancel={() => setEditing(null)}
            competitionLevels={competitionLevels}
            priorities={priorities}
          />
        )}

        <CalendarForm
          title="Создать календарь из выборки"
          description="Будут скопированы события, подходящие под значения фильтров выше. Сортировка и текущая страница не учитываются."
          form={copyForm}
          setForm={setCopyForm}
          onSubmit={copyCalendarFromFilters}
        />

        {copiedCalendar && (
          <div className="card">
            <p className="muted">Создан календарь: {copiedCalendar.name}</p>
            <button className="secondary" onClick={() => onOpen(copiedCalendar.id)}>
              Открыть созданный календарь
            </button>
          </div>
        )}

        <div className="card">
          <h3>Экспорт в Google Sheets</h3>
          <p className="muted">Сначала подключи Google account на странице календарей.</p>
          <button className="primary" onClick={startExport}>
            Создать Google таблицу
          </button>
          {exportJob && <ExportStatus job={exportJob} />}
        </div>
      </aside>
    </div>
  );
}

function CalendarForm({
  title,
  description,
  form,
  setForm,
  onSubmit,
  onCancel
}: {
  title: string;
  description?: string;
  form: CalendarFormData;
  setForm: (form: CalendarFormData) => void;
  onSubmit: (event: React.FormEvent) => void;
  onCancel?: () => void;
}) {
  return (
    <form className="card form" onSubmit={onSubmit}>
      <h3>{title}</h3>
      {description && <p className="muted">{description}</p>}
      <label>
        Название
        <input value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} required />
      </label>
      <label>
        Вид спорта
        <input value={form.sportType} onChange={(event) => setForm({ ...form, sportType: event.target.value })} />
      </label>
      <label>
        Год
        <input
          value={form.year}
          onChange={(event) => setForm({ ...form, year: event.target.value })}
          type="number"
        />
      </label>
      <div className="form-actions">
        <button className="primary">Сохранить</button>
        {onCancel && (
          <button type="button" className="secondary" onClick={onCancel}>
            Отмена
          </button>
        )}
      </div>
    </form>
  );
}

function EventForm({
  title,
  form,
  setForm,
  onSubmit,
  onCancel,
  competitionLevels,
  priorities
}: {
  title: string;
  form: EventFormData;
  setForm: (form: EventFormData) => void;
  onSubmit: (event: React.FormEvent) => void;
  onCancel?: () => void;
  competitionLevels: EnumOption[];
  priorities: EnumOption[];
}) {
  return (
    <form className="card form" onSubmit={onSubmit}>
      <h3>{title}</h3>
      <label>
        Название
        <input value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} required />
      </label>
      <div className="two-columns">
        <label>
          Начало
          <input
            type="date"
            value={form.startDate}
            onChange={(event) => setForm({ ...form, startDate: event.target.value })}
            required
          />
        </label>
        <label>
          Окончание
          <input
            type="date"
            value={form.endDate}
            onChange={(event) => setForm({ ...form, endDate: event.target.value })}
          />
        </label>
      </div>
      <label>
        Уровень
        <select
          value={form.competitionLevel}
          onChange={(event) => setForm({ ...form, competitionLevel: event.target.value })}
        >
          {competitionLevels.map((option) => (
            <option key={option.code} value={option.code}>
              {option.title}
            </option>
          ))}
        </select>
      </label>
      <label>
        Место
        <input value={form.location} onChange={(event) => setForm({ ...form, location: event.target.value })} />
      </label>
      <label>
        Ссылка
        <input
          value={form.externalUrl}
          onChange={(event) => setForm({ ...form, externalUrl: event.target.value })}
          placeholder="https://example.com"
        />
      </label>
      <label>
        Дисциплины
        <input
          value={form.disciplines}
          onChange={(event) => setForm({ ...form, disciplines: event.target.value })}
          placeholder="Через запятую"
        />
      </label>
      <label>
        Приоритет
        <select value={form.priority} onChange={(event) => setForm({ ...form, priority: event.target.value })}>
          {priorities.map((option) => (
            <option key={option.code} value={option.code}>
              {option.title}
            </option>
          ))}
        </select>
      </label>
      <div className="form-actions">
        <button className="primary">Сохранить</button>
        {onCancel && (
          <button type="button" className="secondary" onClick={onCancel}>
            Отмена
          </button>
        )}
      </div>
    </form>
  );
}

function GoogleIntegration({
  accessToken,
  onUnauthorized,
  onError
}: {
  accessToken: string;
  onUnauthorized: () => void;
  onError: (message: string | null) => void;
}) {
  const [status, setStatus] = useState<IntegrationStatusResponse | null>(null);
  const [loading, setLoading] = useState(false);

  async function loadStatus() {
    setLoading(true);
    try {
      setStatus(await api.integrationStatus(accessToken));
      onError(null);
    } catch (err) {
      handleError(err, onUnauthorized, onError);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadStatus();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [accessToken]);

  async function connect() {
    try {
      const response = await api.connectGoogle(accessToken);
      window.open(response.redirectUrl, "_blank", "noopener,noreferrer");
    } catch (err) {
      handleError(err, onUnauthorized, onError);
    }
  }

  async function disconnect() {
    try {
      await api.disconnectGoogle(accessToken);
      await loadStatus();
    } catch (err) {
      handleError(err, onUnauthorized, onError);
    }
  }

  return (
    <div className="card">
      <h3>Google Sheets</h3>
      <p className="muted">
        Статус:{" "}
        <strong className={status?.connected ? "ok" : "warn"}>
          {status?.connected ? "подключено" : "не подключено"}
        </strong>
      </p>
      {status?.scopes && <p className="small">Scopes: {status.scopes}</p>}
      <div className="form-actions">
        <button className="primary" onClick={connect}>
          Подключить
        </button>
        <button className="secondary" onClick={loadStatus} disabled={loading}>
          Проверить
        </button>
        {status?.connected && (
          <button className="danger" onClick={disconnect}>
            Отключить
          </button>
        )}
      </div>
    </div>
  );
}

function ExportStatus({ job }: { job: ExportJobResponse }) {
  const message = useMemo(() => {
    if (job.status === "PENDING") return "Задача создана, ждёт обработки.";
    if (job.status === "PROCESSING") return "Worker создаёт Google таблицу.";
    if (job.status === "SUCCESS") return "Экспорт завершён.";
    return job.errorMessage ?? "Экспорт завершился ошибкой.";
  }, [job]);

  return (
    <div className="export-status">
      <p>
        <strong>{job.status}</strong>
      </p>
      <p className="muted">{message}</p>
      {job.spreadsheetUrl && (
        <a href={job.spreadsheetUrl} target="_blank" rel="noreferrer">
          Открыть Google таблицу
        </a>
      )}
    </div>
  );
}

function Alert({ type, message }: { type: "success" | "error"; message: string }) {
  return <div className={`alert ${type}`}>{message}</div>;
}

function FullPageMessage({ title, text }: { title: string; text: string }) {
  return (
    <div className="auth-page">
      <div className="card auth-card">
        <h1>{title}</h1>
        <p className="muted">{text}</p>
      </div>
    </div>
  );
}

function handleError(
  error: unknown,
  onUnauthorized: () => void,
  onError: (message: string | null) => void
) {
  if (error instanceof ApiError && error.status === 401) {
    onUnauthorized();
    return;
  }
  onError(readError(error));
}

function readError(error: unknown): string {
  if (error instanceof Error) {
    return error.message;
  }
  return "Неизвестная ошибка";
}

function formatDateRange(startDate: string, endDate: string) {
  if (startDate === endDate) {
    return formatDate(startDate);
  }
  return `${formatDate(startDate)} — ${formatDate(endDate)}`;
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("ru-RU").format(new Date(`${value}T00:00:00`));
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("ru-RU", {
    dateStyle: "short",
    timeStyle: "short"
  }).format(new Date(value));
}
