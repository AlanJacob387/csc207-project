# Documentation

How the project is built. Diagrams marked **owed** are the deliverables named in `Plan.md` (task 1.7).

## 1. What the app is

Team Sports is a desktop application (Java 17 + Swing) that keeps a sports team schedule, attendance, stats and team information in one place. Four roles share one interface, and each role sees only the pages it may use.

## 2. Architecture

Clean Architecture, one package per layer:

| Layer | Package | Holds | Depends on |
|---|---|---|---|
| Entity | `entity` | `User`, `Role`, `Team`, `Event`, `AttendanceRecord`, `UserFactory` | nothing |
| Use case | `use_case/<name>` | input/output boundaries and data, the interactor, and the slice data-access interface | entity |
| Interface adapter | `interface_adapter/<name>` | controller, presenter, view model | use_case, entity |
| Interface adapter (shared) | `interface_adapter` | `ViewManagerModel` (which page is showing), `Session` (who is signed in) | the `CurrentUserInterface` port |
| Data access | `data_access` | `FileAppDataAccessObject`, `EntityJsonMapper`, `InMemoryUserDataAccessObject`, `OpenMeteoWeatherGateway` | entity, use-case interfaces |
| View | `view` | `LoginView`, `SignupView`, `ChangePasswordView`, `AppShell`, `CalendarView`, `CreateEventView`, `ViewManager`, `BackgroundImage` | interface_adapter |
| App | `app` | `AppBuilder` (builds the engine), `Main` (states what the app has) | everything |

- **Layer and package diagram** - L1 (task 1.7), below
- **Entity model diagram and CRC cards** - owed by L4
- **Class diagram for attendance/stats** - owed by L2

### Layer and package diagram (L1)

With every real dependency drawn in, one diagram was too crowded to read, so it is split in two:

1. **Layers and the dependency rule.** Package level. Shows every dependency that crosses a layer boundary.
2. **Dependencies inside the outer layers.** Class level. Shows which views and adapters reach sideways to the shared `Session` and `ViewManagerModel`, the two presenters that reach into the login slice's view model, and where the announcements page couples to use case output data.

#### 1. Layers and the dependency rule

Solid arrows mean "uses" (imports) and dotted arrows mean "implements". Both are dependencies. Every arrow points inward. Arrows that skip a layer, or that reach use case output data from outside the presenter, are labelled. The two that couple the announcements page to `ViewAnnouncementsOutputData` are marked FLAGGED.

```mermaid
graph TD
    subgraph INFRA["Infrastructure (frameworks and drivers)"]
        APP["app/<br/>AppBuilder, Main"]
        VIEW["view/<br/>Swing pages and dialogs"]
        DA["data_access/<br/>FileAppDataAccessObject, InMemoryUserDataAccessObject,<br/>FileScheduleWriter, EntityJsonMapper, OpenMeteoWeatherGateway"]
    end

    subgraph IA["Interface adapter"]
        CTRL["interface_adapter/{name}/<br/>Controller"]
        PRES["interface_adapter/{name}/<br/>Presenter, ViewModel"]
        SHARED["interface_adapter/<br/>Session, ViewManagerModel"]
    end

    subgraph UC["Use case"]
        INB["use_case/{name}/<br/>InputBoundary, InputData"]
        INT["use_case/{name}/<br/>Interactor"]
        OUTB["use_case/{name}/<br/>OutputBoundary, OutputData"]
        DAI["use_case/{name}/<br/>DataAccessInterface<br/>(export also: ScheduleFileWriter, ScheduleWriteException)"]
        CU["use_case/current_user/<br/>CurrentUserInterface"]
        ICS["use_case/export_schedule/<br/>IcsCalendarFormatter"]
    end

    subgraph ENT["Entity"]
        E["entity/<br/>User, Role, Team, Event,<br/>AttendanceRecord, Announcement, UserFactory"]
    end

    APP --> VIEW
    APP --> CTRL
    APP --> PRES
    APP --> SHARED
    APP --> DA
    APP --> INT
    APP -- "Role, Team, User, UserFactory (demo seed)" --> E

    VIEW --> CTRL
    VIEW --> PRES
    VIEW -- "ViewManager, ChangePasswordView, AnnouncementsView" --> SHARED
    VIEW -- "Role: AppShell, SignupView, AnnouncementsView" --> E
    VIEW -- "FLAGGED: AnnouncementsView reads ViewAnnouncementsOutputData, skips adapters" --> OUTB

    CTRL --> INB
    PRES -- "presenters read OutputData" --> OUTB
    PRES -- "FLAGGED: ViewAnnouncementsViewModel holds ViewAnnouncementsOutputData types" --> OUTB
    SHARED -- "Session uses Role, User" --> E

    INT --> OUTB
    INT --> DAI
    INT --> CU
    INT --> E
    DAI -- "Event (all seven L1 interfaces), User (login, signup, change_password), Announcement, Team, User (both announcement interfaces)" --> E
    INT -- "ExportScheduleInteractor calls format()" --> ICS
    ICS -- Event --> E
    CU -- User --> E

    DA --> E
    DA -- "FileScheduleWriter throws ScheduleWriteException" --> DAI

    INT -. implements InputBoundary .-> INB
    DA -. implements .-> DAI
    PRES -. implements .-> OUTB
    SHARED -. Session implements .-> CU
```

#### 2. Dependencies inside the outer layers

These arrows stay inside the outer two layers or point inward from them, so they do not break the dependency rule. They are drawn separately so diagram 1 stays readable.

```mermaid
graph LR
    subgraph VIEWS["view/"]
        VM_V["ViewManager"]
        CPV["ChangePasswordView"]
        AV["AnnouncementsView"]
    end

    subgraph ADAPTERS["interface_adapter/{name}/"]
        VAVM["ViewAnnouncementsViewModel"]
        LVM["LoginViewModel (login slice)"]
        LP["LoginPresenter"]
        SP["SignupPresenter"]
        LOP["LogoutPresenter"]
        PAC["PostAnnouncementController"]
        VAC["ViewAnnouncementsController"]
    end

    subgraph SHAREDPKG["interface_adapter/"]
        VMM["ViewManagerModel"]
        S["Session (concrete class)"]
    end

    subgraph UCPKG["use_case/view_announcements/"]
        VAOD["ViewAnnouncementsOutputData<br/>AnnouncementData, TeamData"]
    end

    VM_V -- listens for page changes --> VMM
    LP -- setState APP --> VMM
    SP -- setState LOGIN --> VMM
    LOP -- setState LOGIN --> VMM
    SP -- "cross-slice: writes welcome message" --> LVM
    LOP -- "cross-slice: clears it, or writes an error" --> LVM
    CPV --> S
    AV --> S
    PAC -- reads username --> S
    VAC -- reads username --> S
    AV --> VAVM
    VAVM -- "FLAGGED: stores and returns output data types" --> VAOD
    AV -- "FLAGGED: imports AnnouncementData, TeamData" --> VAOD
```

What the two diagrams show:

- **`use_case` has no outward arrows.** An interactor only knows its own boundaries, its own data-access interface, the `CurrentUserInterface` port and the entities. The data-access interfaces and the port also depend on entities (`Event` for every L1 interface, `User` for the port), which still points inward.
- **Data access is inverted.** Each interactor calls its slice's `...DataAccessInterface`, which lives in `use_case/<name>/`. `FileAppDataAccessObject` implements all of them, so the dependency runs from `data_access` into `use_case`, not the other way round. `FileScheduleWriter` does the same for the export slice's `ScheduleFileWriter`.
- **The boundaries are implemented across layers.** Every interactor implements its slice's `InputBoundary`, which the controller calls (it also reads the `InputData`). Interactors call `OutputBoundary` interfaces that presenters implement, and `CurrentUserInterface`, which `Session` implements. `OutputData` classes are not implemented by anything: interactors create them and presenters read them.
- **Views reach past the slice adapters in three ways.** `ViewManager` listens to `ViewManagerModel`. `ChangePasswordView` and `AnnouncementsView` take the concrete `Session`. `AppShell`, `SignupView` and `AnnouncementsView` import `entity.Role`. All of these point inward, so the rule holds, but they skip a layer.
- **The announcements page is coupled to use case output data (flagged).** The coupling starts in `ViewAnnouncementsViewModel` (L5), which stores and returns `ViewAnnouncementsOutputData.AnnouncementData` and `TeamData` instead of its own types. Because of that, `AnnouncementsView` imports those `use_case` classes too. They are the only view model and the only view that import anything from `use_case`. The other view models expose plain values or their own nested classes, such as `ViewScheduleViewModel.Entry`. Neither arrow breaks the inward rule, but the page now changes whenever the use case's output format does.
- **`data_access` also uses a use case class.** Besides implementing `ScheduleFileWriter`, `FileScheduleWriter` throws `ScheduleWriteException`, which is defined in `use_case/export_schedule`.
- **Some adapters use the shared state directly.** The login, signup and logout presenters set `ViewManagerModel` to change pages. `SignupPresenter` and `LogoutPresenter` also import `interface_adapter.login.LoginViewModel`, because both send the user back to the login page with a message or a cleared form. These are the only adapter-to-adapter dependencies between slices in the project; every other adapter imports only its own slice's classes plus the shared `Session` or `ViewManagerModel`. The two announcement controllers take the concrete `Session` and read the username themselves, instead of the interactor reading it through `CurrentUserInterface`.
- **`app` knows everything.** `AppBuilder` creates the concrete classes, passes them in through constructors, and uses `Role`, `Team`, `User` and `UserFactory` to seed the demo data.

### Package map

Every source package under `src/main/java`. Tests under `src/test/java` mirror the same package names.

| Package | Layer | Lane | What is in it |
|---|---|---|---|
| `entity` | Entity | L4 | `User`, `Role`, `Team`, `Event`, `AttendanceRecord`, `Announcement`, `UserFactory`. No imports from the rest of the project. |
| `use_case/current_user` | Use case | L3 | `CurrentUserInterface`, the "who is signed in" port that every interactor reads instead of asking the view. |
| `interface_adapter` | Interface adapter | L3 | `Session` (implements `CurrentUserInterface`) and `ViewManagerModel` (name of the page that should be showing). |
| `view` | Infrastructure | shared | Swing UI. L1: `CalendarView`, `CreateEventView`, `EventDetailsView` (modal dialog), `EventsListView`. L3: `LoginView`, `SignupView`, `ChangePasswordView`, `ViewManager`, `BackgroundImage`. L4: `AppShell`. L5: `AnnouncementsView`. |
| `data_access` | Infrastructure | shared | `FileAppDataAccessObject` (the JSON store, implements every slice's data-access interface), `EntityJsonMapper` (on-disk format), `InMemoryUserDataAccessObject` (test fake for the account slices), `FileScheduleWriter` (L1, writes the exported `.ics`), `OpenMeteoWeatherGateway` (L5, weather API). |
| `app` | Infrastructure | L1 | `AppBuilder` (creates and connects everything) and `Main` (lists the views and use cases, then shows the window). |

Each slice is a pair of packages with the same name:

| Slice | Lane | `use_case/<name>` | `interface_adapter/<name>` |
|---|---|---|---|
| `login` | L3 | Interactor, boundaries, input/output data, `LoginUserDataAccessInterface` | Controller, Presenter (navigates to the app), ViewModel |
| `signup` | L3 | Interactor, boundaries, input/output data, `SignupUserDataAccessInterface` | Controller, Presenter (navigates back to login), ViewModel |
| `logout` | L3 | Interactor, boundaries, output data. No input data and no data-access interface; it only clears `CurrentUserInterface` | Controller, Presenter (navigates to login). No view model |
| `change_password` | L3 | Interactor, boundaries, input/output data, `ChangePasswordUserDataAccessInterface` | Controller, Presenter, ViewModel |
| `create_event` | L1 | Interactor, boundaries, input/output data, `CreateEventDataAccessInterface` | Controller, Presenter, ViewModel |
| `view_schedule` | L1 | Interactor, boundaries, input/output data, `ViewScheduleDataAccessInterface` (read only) | Controller, Presenter, ViewModel |
| `view_event_details` | L1 | Interactor, boundaries, input/output data, `ViewEventDetailsDataAccessInterface` (read only) | Controller, Presenter, ViewModel |
| `edit_event` | L1 | Interactor, boundaries, input/output data, `EditEventDataAccessInterface` | Controller, Presenter, ViewModel |
| `cancel_event` | L1 | Interactor, boundaries, input/output data, `CancelEventDataAccessInterface` | Controller, Presenter, ViewModel |
| `browse_events` | L1 | Interactor, boundaries, input/output data, `BrowseEventsDataAccessInterface` (read only) | Controller, Presenter, ViewModel |
| `export_schedule` | L1 | Interactor, boundaries, input/output data, `ExportScheduleDataAccessInterface` (read), `ScheduleFileWriter` gateway (write), `ScheduleWriteException`, `IcsCalendarFormatter` | Controller, Presenter, ViewModel |
| `post_announcement` | L5 | Interactor, boundaries, input/output data, `PostAnnouncementDataAccessInterface` | Controller, Presenter, ViewModel |
| `view_announcements` | L5 | Interactor, boundaries, input/output data, `ViewAnnouncementsDataAccessInterface` | Controller, Presenter, ViewModel |

### AppBuilder wiring notes (L1)

`AppBuilder` creates the shared objects once, as fields: a single `FileAppDataAccessObject`, the `ViewManagerModel`, the `Session`, a `UserFactory` and the `ViewManager`. Its constructor also seeds the demo accounts and team if `coach1` is missing. `Main` then calls a chain of `add...` methods and finishes with `build()`.

**Where views are built.** Only three methods exist purely to build views: `addLoginView()`, `addSignupView()` and `addChangePasswordView()`. The other pages are built inside larger methods:

- `addAppShell()` builds the `AppShell`, `CalendarView` and `EventsListView`, plus placeholder panels for My Stats and Roster.
- `addViewEventDetailsUseCase()` builds the `EventDetailsView` panel. The panel is built once. Each call to `open()` creates a new modal `JDialog` around it and disposes that dialog when it closes.
- `addCreateEventUseCase()` builds `CreateEventView`, because that view takes its controller in its constructor.
- `addAnnouncementUseCases()` wires two use cases (`post_announcement` and `view_announcements`) and builds the `AnnouncementsView` that both of them share.

**How the L1 slices are assembled.** `addEditEventUseCase()` is a typical example. It creates the `EditEventViewModel`, a presenter that writes to that view model, an `EditEventInteractor` built from the shared `dataAccess`, the presenter and the `session`, and a controller around the interactor. Then it hands the controller and view model to a view that already exists (`eventDetailsView.setEditEventController(...)`). All seven L1 methods build view model, presenter, interactor and controller in that order. Each interactor receives the shared `FileAppDataAccessObject`, but its constructor only accepts that slice's interface, so it can only call what that interface declares. `session` is passed as a `CurrentUserInterface` in the same way. `addExportScheduleUseCase()` also passes a `new FileScheduleWriter()` as the second gateway.

**The other lanes' methods differ:**

- **`addLoginUseCase()`, `addSignupUseCase()`, `addLogoutUseCase()`** create no view model of their own. Their presenters take `viewManagerModel` plus view models built earlier (`loginViewModel`, `signupViewModel`).
- **Logout interactor:** gets no data-access object, only `session` and the presenter.
- **Signup interactor:** gets `dataAccess`, the presenter and the `userFactory`, but no session.
- **`addChangePasswordUseCase()`:** passes `dataAccess`, the presenter, `session` and `userFactory`.
- **`addAnnouncementUseCases()`:** passes the concrete `Session` to the two controllers rather than the port to the interactors. The controllers read the username and put it in the input data. Neither announcement interactor receives a session.

**Why the L1 slices each get their own method.** Each L1 method holds everything needed to build one slice, so the slice can be read, changed or removed in one place, and `Main` reads as a list of what the app has. Adding a feature means adding a method and a line in `Main` rather than editing a long shared block, which keeps merge conflicts between lanes small. The announcements method shows the alternative: two slices and a view built together.

**Navigation without depending on views.** `ViewManagerModel` only holds the name of the page that should be showing. A presenter that needs to change pages calls `setState(...)` and `firePropertyChanged()`. `ViewManager`, which is a view, listens and flips its `CardLayout`. The presenter depends only on the model in `interface_adapter` and never on a Swing class. In the current code only the login, signup and logout presenters do this; `AppBuilder`'s private `navigate()` helper uses the same mechanism for the "go to sign up" and "back to log in" links and for showing the login page in `build()`. None of the L1 presenters navigate. Their results show on the page or dialog that asked, so they only update a view model, and the view reacts to its property change.

**Ordering constraints.** These are real constraints in the code, not just conventions:

- **Views and the shell come first.** Use case methods read fields that earlier methods set, and when it fails depends on how the field is used.
  - **Crash at startup.** These fields are dereferenced as soon as the method runs, so a wrong order throws a `NullPointerException` while the app is being built:
    - `loginView` (`addLoginUseCase()`)
    - `signupView` (`addSignupUseCase()`)
    - `appShell` (`addLogoutUseCase()`, `addCreateEventUseCase()`, `addChangePasswordView()`, `addAnnouncementUseCases()`)
    - `changePasswordView` (`addChangePasswordUseCase()`)
    - `calendarView` (`addViewScheduleUseCase()`, `addViewEventDetailsUseCase()`, `addExportScheduleUseCase()`)
    - `eventsListView` (`addBrowseEventsUseCase()`)
    - `eventDetailsView` (`addEditEventUseCase()`, `addCancelEventUseCase()`)
  - **Crash later.** `addSignupUseCase()` and `addLogoutUseCase()` only pass `loginViewModel` into a presenter constructor, so a null there is stored without complaint. It fails later: at the first successful sign-up, when `SignupPresenter` writes the welcome message to it, and at the first logout attempt, when `LogoutPresenter` clears it or writes an error to it.
- **Sidebar order.** `AppShell` keeps its pages in a `LinkedHashMap`, so the sidebar shows pages in the order `addPage` was called, and `applyRole()` hides the ones the role may not see. `addAppShell()` adds the first four (Calendar, Events, My Stats, Roster), so those always lead. Pages added by later methods are appended in `Main`'s call order: New Event (`addCreateEventUseCase()`), Account (`addChangePasswordView()`), then Announcements. A coach currently sees Calendar, Events, Roster, New Event, Account, Announcements.
- **Edit, cancel and browse follow `addViewEventDetailsUseCase()`.** That method creates the one `EventDetailsView` panel and connects it to the calendar. `addEditEventUseCase()` and `addCancelEventUseCase()` call `eventDetailsView.set...Controller(...)` straight away, so they would fail with a `NullPointerException` if the panel did not exist yet. `addBrowseEventsUseCase()` only reads `eventDetailsView` inside its row-click callback, so it would not crash at startup, but the first click would fail if the panel were never created. It is kept after for the same reason, and so `Main` shows the dependency. What the three slices share is the panel and its controllers; the `JDialog` around it is new on every `open()`. The panel's close callback is always `calendarView::refresh`, so closing it reloads the calendar even when it was opened from the Events page. The Events page reloads itself separately once `open()` returns.
- **Announcements are last.** `addAnnouncementUseCases()` runs last in `Main`, so Announcements is the last item in the sidebar. Moving it up means moving that call earlier in `Main`.

## 3. Use cases

Every interaction a user can trigger is its own slice. The full catalogue (33 use cases with lane, sprint, priority and status) lives in `Plan.md`.

Built so far:

| Use case | Slice | View | Tests |
|---|---|---|---|
| `login` (UC-A1) | `use_case/login` + `interface_adapter/login` | `LoginView` | `LoginInteractorTest` (8) |
| `signup` (UC-A2) | `use_case/signup` + `interface_adapter/signup` | `SignupView` | `SignupInteractorTest` (8) |
| `logout` (UC-A3) | `use_case/logout` + `interface_adapter/logout` | Log out button in `AppShell` | `LogoutInteractorTest` (2) |
| `change_password` (UC-A4) | `use_case/change_password` + `interface_adapter/change_password` | `ChangePasswordView` (Account page) | `ChangePasswordInteractorTest` (8) |
| `create_event` (UC-B1) | `use_case/create_event` + `interface_adapter/create_event` | `CreateEventView` | owed by L2 (task 1.8) |

- **Sequence diagrams** - owed by each lane for its own use cases (task 3.8, coordinated by L5)

The account slices are the template the others copy: the view calls a controller, the controller builds input data, the interactor works through a narrow data-access interface and reports to a presenter, and the presenter updates a view model or asks the `ViewManagerModel` to change pages.

### Sequence diagrams: scheduling and events (L1)

One diagram per L1 use case, traced from the code. Shared conventions:

- Every interactor first asks `CurrentUserInterface` (the `Session`) who is signed in. If nobody is, it calls `prepareFailView` and stops. That check is shown once in full (in `edit_event`) and left out of the shorter diagrams.
- `FileAppDataAccessObject` is the only production implementation of every data-access interface. A `break` block means the interactor calls the presenter's fail view and returns.
- **Read-failure branches are left out.** Every L1 interactor wraps its store reads in `catch (IllegalStateException | UncheckedIOException ex)` and reports "Could not read ...". `export_schedule` caught only `IllegalStateException` until it was brought into line. These branches cannot be reached with `FileAppDataAccessObject`. It reads the file once, in its constructor, and after that every read method works on in-memory maps and never throws. Only the test fakes reach them.
- **Write failures and rollback.** `saveEvent` and `updateEvent` in `FileAppDataAccessObject` put the new event in the map, call `persist()`, and on any `RuntimeException` put the previous event back (or remove the new one) and rethrow. `persist()` reports a failed disk write as `UncheckedIOException`. The interactors catch `IllegalStateException | UncheckedIOException` around the write. Any other `RuntimeException` is still rolled back by the DAO, but it is not caught by the interactor, so it escapes `execute()` and no fail view appears.
- Every view model fires a property change when it is updated, and the view redraws from it.

#### edit_event (UC-B4)

Opened from the event details dialog. Only date/time, location and opponent can change; type and team cannot.

```mermaid
sequenceDiagram
    actor Coach
    box Infrastructure (view)
        participant V as EventDetailsView
    end
    box Interface adapter
        participant C as EditEventController
        participant P as EditEventPresenter
        participant VM as EditEventViewModel
    end
    box Use case
        participant ID as EditEventInputData
        participant I as EditEventInteractor
        participant CU as CurrentUserInterface
        participant DAI as EditEventDataAccessInterface
        participant OD as EditEventOutputData
    end
    box Infrastructure (data_access)
        participant DAO as FileAppDataAccessObject
    end
    box Entity
        participant E as Event
    end

    Coach->>V: Save
    V->>V: parse date/time text
    break text is not a valid date/time
        V-->>Coach: "Invalid date/time format." (no use case call)
    end
    V->>C: execute(eventId, dateTime, location, opponent)
    C->>ID: new EditEventInputData(...)
    C->>I: execute(inputData)

    I->>CU: getCurrentUsername()
    CU-->>I: username
    break nobody signed in
        I->>P: prepareFailView("Sign in to edit events.")
    end

    I->>DAI: getEvent(eventId)
    DAI->>DAO: getEvent(eventId)
    DAO-->>I: original Event, or null
    break event is null
        I->>P: prepareFailView("That event no longer exists.")
    end
    I->>DAI: getCoachUsername(original.teamId)
    DAI->>DAO: getCoachUsername(teamId)
    DAO-->>I: coach username

    Note over I: Authorization
    break username is not the team's coach
        I->>P: prepareFailView("Only the team's coach can edit this event.")
    end
    break original is cancelled (not reachable from the UI, Edit is hidden for cancelled events)
        I->>P: prepareFailView("Cancelled events can't be edited.")
    end
    break location blank, date/time missing, or a changed date/time is in the past
        I->>P: prepareFailView(reason)
    end
    Note over I: An unchanged date/time may be in the past, so a coach can fix the location of an event that already happened.

    I->>I: copyOf(original)
    I->>E: setDateTime(newDateTime), setLocation(trimmed) on the copy
    I->>I: opponentFor(type, opponent), evaluated as the argument to setOpponent, null for practices or blank
    I->>E: setOpponent(opponentFor(...)) on the copy
    I->>DAI: updateEvent(updated)
    DAI->>DAO: updateEvent(updated)
    DAO->>DAO: previous = events.put(eventId, updated)
    DAO->>DAO: persist() writes temp file, then moves it over app-data.json

    alt write succeeds
        DAO-->>I: return
        I->>OD: new EditEventOutputData(eventId, "Event updated.")
        I->>P: prepareSuccessView(outputData)
        P->>VM: setSuccessMessage("Event updated.")
        VM-->>V: property change
        V->>V: leave edit mode, reload via ViewEventDetailsController
    else persist() throws IllegalStateException or UncheckedIOException (a failed disk write is UncheckedIOException)
        DAO->>DAO: catch RuntimeException, events.put(eventId, previous)
        DAO-->>I: rethrow
        I->>I: catch (IllegalStateException | UncheckedIOException ex)
        I->>P: prepareFailView("Could not save the change: ...")
        P->>VM: setErrorMessage(...)
        VM-->>V: property change
        V->>V: stay in the form and show the error
    else persist() throws any other RuntimeException
        DAO->>DAO: catch RuntimeException, events.put(eventId, previous)
        DAO-->>I: rethrow
        Note over I: not caught here, so the exception escapes execute() and no fail view is shown
    end
```

Because the interactor edits a copy and the DAO puts the previous event back when the write fails, the event in memory always matches what is on disk.

#### export_schedule (UC-B7)

Exports every dated event of the user's teams, past and future, cancelled ones included, as one `.ics` file. Events with no date are left out and counted. An iCalendar entry needs a start time, and inventing one would put the event on the wrong day, while failing the whole export would leave the user with nothing. Reading goes through the slice's data-access interface. Writing goes through a separate `ScheduleFileWriter` gateway. The `.ics` text is built by `IcsCalendarFormatter`, a package-private class in the use case layer that never touches the file system.

```mermaid
sequenceDiagram
    actor User
    box Infrastructure (view)
        participant V as CalendarView
    end
    box Interface adapter
        participant C as ExportScheduleController
        participant P as ExportSchedulePresenter
        participant VM as ExportScheduleViewModel
    end
    box Use case
        participant ID as ExportScheduleInputData
        participant I as ExportScheduleInteractor
        participant DAI as ExportScheduleDataAccessInterface
        participant F as IcsCalendarFormatter
        participant W as ScheduleFileWriter
        participant OD as ExportScheduleOutputData
    end
    box Infrastructure (data_access)
        participant DAO as FileAppDataAccessObject
        participant FW as FileScheduleWriter
    end
    box Entity
        participant E as Event
    end

    User->>V: Export
    V->>V: JFileChooser, add .ics if missing, confirm overwrite
    V->>C: execute(absolutePath)
    C->>ID: new ExportScheduleInputData(targetPath)
    C->>I: execute(inputData)
    break target path is blank
        I->>P: prepareFailView("Choose a file to export to.")
    end

    I->>DAI: getTeamIdsForMember(username)
    DAI->>DAO: getTeamIdsForMember(username)
    DAO-->>I: teamIds
    break no teams
        I->>P: prepareFailView("You're not on any team yet.")
    end
    I->>I: calendarName(teamIds)
    alt exactly one team
        I->>DAI: getTeamName(teamId)
        DAI->>DAO: getTeamName(teamId)
        DAO-->>I: team name, or null
        Note over I: null gives "Team Sports Schedule"
    else several teams
        Note over I: "Team Sports Schedule", no lookup
    end
    I->>DAI: getEventsForTeams(teamIds)
    DAI->>DAO: getEventsForTeams(teamIds)
    DAO-->>I: List of Event
    loop each Event
        I->>E: getDateTime()
        I->>I: no date: skipped++, otherwise keep it
    end
    break nothing kept
        I->>P: prepareNothingToExportView(message)
        P->>VM: setResult(INFO, message)
        Note over I,P: "There are no events to export yet." or, if every event was skipped, "None of your events have a date yet, so there is nothing to export."
        VM-->>V: property change
        V-->>User: information dialog
    end

    I->>I: sort kept events by date/time
    I->>F: format(calName, events)
    F->>E: getEventId, getDateTime, getType, getOpponent, getLocation, isCancelled
    F-->>I: .ics text (STATUS:CANCELLED and "Cancelled:" prefix for cancelled events)

    I->>W: write(targetPath, content)
    W->>FW: write(targetPath, content)
    FW->>FW: write temp file in the same folder, then move it over the target
    alt write succeeds
        FW-->>I: return
        I->>OD: new ExportScheduleOutputData(targetPath, eventCount, skippedCount)
        I->>P: prepareSuccessView(outputData)
        P->>VM: setResult(SUCCESS, message)
        Note over P: "Exported 1 event to ..." or "Exported N events to ...", then "Left out K event(s) with no date set." if K > 0, then "Import this file into Google Calendar, Outlook, or Apple Calendar."
        VM-->>V: property change
        V-->>User: information dialog
    else ScheduleWriteException
        FW-->>I: throw ScheduleWriteException
        Note over FW: invalid file name, target is a folder, access denied, folder missing, or any other I/O failure
        I->>P: prepareFailView("Export failed. " + message)
        P->>VM: setResult(ERROR, message)
        VM-->>V: property change
        V-->>User: error dialog
    end
```

Every path that reaches the presenter ends the same way: the view model fires a property change and `CalendarView` shows a dialog. SUCCESS and INFO results get an information dialog, and ERROR results (including the fail views not drawn above) get an error dialog.

#### browse_events (UC-B5)

The Events page sends its two filter choices as input data. The interactor does all the filtering, so the view never holds events it should not show.

```mermaid
sequenceDiagram
    actor User
    box Infrastructure (view)
        participant V as EventsListView
    end
    box Interface adapter
        participant C as BrowseEventsController
        participant P as BrowseEventsPresenter
        participant VM as BrowseEventsViewModel
    end
    box Use case
        participant ID as BrowseEventsInputData
        participant I as BrowseEventsInteractor
        participant DAI as BrowseEventsDataAccessInterface
        participant OD as BrowseEventsOutputData
    end
    box Infrastructure (data_access)
        participant DAO as FileAppDataAccessObject
    end
    box Entity
        participant E as Event
    end

    User->>V: pick type and team filter (or open the page)
    V->>C: execute(type, teamId)
    Note over V,C: type is null, "PRACTICE" or "GAME". teamId is null for all teams.
    C->>ID: new BrowseEventsInputData(type, teamId)
    C->>I: execute(inputData)

    opt type is not null
        I->>I: Event.EventType.valueOf(type)
        break not a known type
            I->>P: prepareFailView("Unknown event type: ...")
        end
    end

    I->>DAI: getTeamIdsForMember(username)
    DAI->>DAO: getTeamIdsForMember(username)
    DAO-->>I: teamIds
    break user is on no team
        I->>OD: new BrowseEventsOutputData(empty, empty, onAnyTeam=false, filtered)
        I->>P: prepareSuccessView(outputData)
        P->>VM: setEvents(empty, empty, "You're not on any team yet.")
    end
    break teamId filter is not one of the user's teams (forged filter)
        I->>P: prepareFailView("You're not on that team.")
        P->>VM: setError(message)
    end

    loop each of the user's teams
        I->>DAI: getTeamName(teamId)
        DAI->>DAO: getTeamName(teamId)
        DAO-->>I: name, or null (team id used instead)
    end
    I->>DAI: getEventsForTeams(team filter only, or all teamIds)
    DAI->>DAO: getEventsForTeams(teamIds)
    DAO-->>I: List of Event

    loop each Event
        I->>E: getType()
        I->>I: keep it if no type filter or the type matches
    end
    I->>I: sort by date/time, missing dates last
    I->>OD: new BrowseEventsOutputData(rows, teamOptions, true, filtered)
    I->>P: prepareSuccessView(outputData)
    P->>P: format date and time ("Date not set" and blank time when there is no date)
    P->>P: build titles with "Cancelled:" prefix
    P->>P: empty message: "No events match these filters." or "No events scheduled yet."
    P->>VM: setEvents(entries, teams, message)
    VM-->>V: property change
    V->>V: refill team filter, fill table
```

The team filter box is filled only from the user's own teams, so the forged-filter path is not reachable through the normal UI. It is checked in the interactor anyway, so that the rule does not depend on the view.

#### create_event (UC-B1)

```mermaid
sequenceDiagram
    actor Coach
    participant V as CreateEventView
    participant C as CreateEventController
    participant ID as CreateEventInputData
    participant I as CreateEventInteractor
    participant DAI as CreateEventDataAccessInterface
    participant DAO as FileAppDataAccessObject
    participant E as Event
    participant OD as CreateEventOutputData
    participant P as CreateEventPresenter
    participant VM as CreateEventViewModel

    Coach->>V: Create
    V->>V: onCreate() parses the date/time text
    break text is blank or not a valid date/time
        V-->>Coach: "Invalid date/time format." (no use case call)
    end
    V->>C: execute(teamId, eventType, dateTime, location, opponent)
    C->>ID: new CreateEventInputData(...)
    C->>I: execute(inputData)
    I->>DAI: teamExists(teamId)
    DAI->>DAO: teamExists(teamId)
    break team does not exist (reachable, Team ID is a free-text field)
        I->>P: prepareFailView("Team does not exist: ...")
    end
    I->>DAI: getCoachUsername(teamId)
    DAI->>DAO: getCoachUsername(teamId)
    DAO-->>I: coach username
    alt signed-in user is not the coach
        I->>P: prepareFailView("Only the team's coach can create events for this team.")
        P->>VM: setErrorMessage(...), which also clears any old success message
    else coach, date in the future, location set, type valid
        I->>I: opponentFor(type, opponent), null for practices or blank
        I->>E: new Event(random UUID, teamId, type, dateTime, location, opponentFor(...))
        I->>DAI: saveEvent(event)
        DAI->>DAO: saveEvent(event)
        DAO->>DAO: put in map, persist(), on RuntimeException restore and rethrow
        I->>OD: new CreateEventOutputData(eventId, "Event created.")
        I->>P: prepareSuccessView(outputData)
        P->>VM: setSuccessMessage("Event created."), which also clears any old error
    end
    VM-->>V: property change
    V->>V: show the error if there is one, otherwise the success message
```

Other fail views:
- a past date/time: "Event date/time must be in the future."
- blank location: "Location is required."
- save failure: "Could not save the event: ..."

Not reachable from the UI (the interactor still checks them, and the tests reach them):
- a missing date/time. It gives the same "must be in the future" message, but `onCreate()` already rejects blank text before the controller is called.
- missing type ("Choose an event type.") and unknown type ("Unknown event type: ..."). `typeBox` is a fixed combo box holding only PRACTICE and GAME.
- "Could not read the team: ..." when `teamExists` or `getCoachUsername` throws. Only a test fake can do that.

#### view_schedule (UC-B2)

```mermaid
sequenceDiagram
    participant V as CalendarView
    participant C as ViewScheduleController
    participant ID as ViewScheduleInputData
    participant I as ViewScheduleInteractor
    participant DAI as ViewScheduleDataAccessInterface
    participant DAO as FileAppDataAccessObject
    participant E as Event
    participant OD as ViewScheduleOutputData
    participant P as ViewSchedulePresenter
    participant VM as ViewScheduleViewModel

    Note over V: loadVisibleRange() asks for the dates on screen (6 weeks in month view, 7 days in week view) whenever that range is not already loaded. refresh() clears it when the page is shown or the details dialog closes.
    V->>C: execute(from, to)
    C->>ID: new ViewScheduleInputData(from, to)
    C->>I: execute(inputData)
    alt from is after to
        I->>P: prepareFailView("The start of the range is after the end.")
        P->>VM: setError(message)
    else valid range
        I->>DAI: getTeamIdsForMember(username)
        DAI->>DAO: getTeamIdsForMember(username)
        DAO-->>I: teamIds
        I->>DAI: getEventsForTeams(teamIds, from, to)
        DAI->>DAO: getEventsForTeams(teamIds, from, to)
        DAO->>E: getDateTime(), skip events with no date, keep those in the range
        DAO-->>I: List of Event
        I->>I: leave out events with no date, sort by date/time, build EventRows
        I->>OD: new ViewScheduleOutputData(rows, onAnyTeam=true)
        I->>P: prepareSuccessView(outputData)
        P->>P: title "Practice", "Game" (no opponent) or "Game vs X", with "Cancelled:" prefix
        P->>VM: setSchedule(entries, "")
    end
    VM-->>V: property change, redraw month or week grid
    opt view model holds an error
        V->>V: clear the loaded range, so the next redraw asks again
    end
```

Other paths: a missing `from` or `to` gives "No date range was given." A user on no team gets a success view with no events, and the presenter shows "You're not on any team yet." The interactor also drops any event with no date, since the calendar has no cell for it. The JSON store already leaves those out of a date-range query, so this only matters for other implementations such as test fakes.

#### view_event_details (UC-B3)

```mermaid
sequenceDiagram
    actor User
    participant CV as CalendarView or EventsListView
    participant V as EventDetailsView
    participant C as ViewEventDetailsController
    participant ID as ViewEventDetailsInputData
    participant I as ViewEventDetailsInteractor
    participant DAI as ViewEventDetailsDataAccessInterface
    participant DAO as FileAppDataAccessObject
    participant E as Event
    participant OD as ViewEventDetailsOutputData
    participant P as ViewEventDetailsPresenter
    participant VM as ViewEventDetailsViewModel

    User->>CV: click an event
    CV->>V: open(eventId, parent)
    V->>C: execute(eventId)
    C->>ID: new ViewEventDetailsInputData(eventId)
    C->>I: execute(inputData)
    I->>DAI: getEvent(eventId)
    DAI->>DAO: getEvent(eventId)
    DAO-->>I: Event, or null
    alt event is null
        I->>P: prepareFailView("That event no longer exists.")
        P->>VM: setErrorMessage(message)
    else event found
        I->>E: getTeamId()
        I->>DAI: getTeamIdsForMember(username)
        DAI->>DAO: getTeamIdsForMember(username)
        DAO-->>I: ids of teams the user coaches or plays on
        alt event's team is not in that list
            I->>P: prepareFailView("You're not on this event's team.")
            P->>VM: setErrorMessage(message)
        else user is on the event's team
            I->>DAI: getTeamName(teamId), getCoachUsername(teamId)
            DAI->>DAO: getTeamName, getCoachUsername
            DAO-->>I: team name, coach
            I->>E: read fields
            I->>OD: new ViewEventDetailsOutputData(..., canManage = user is the coach)
            I->>P: prepareSuccessView(outputData)
            P->>P: format date and time ("Date not set" and blank time when there is no date), build title
            P->>VM: setDetails(...)
        end
    end
    VM-->>V: property change
    V->>V: show details
    Note over V: Edit and Cancel event are shown only when the event loaded, canManage is true, the event is not cancelled, the dialog is not in edit mode, and that controller has been set
    Note over V,CV: open() is modal. On close, onClose always runs calendarView.refresh(), even when the dialog was opened from the Events page. The Events page also refreshes itself after open() returns.
```

Other fail view: a blank event id ("No event was selected."). The membership check runs in the interactor, so a user who is not the coach or a player on the event's team gets the fail view whatever event id reaches the controller, the same team scoping `view_schedule` and `browse_events` use. An event whose team no longer exists has no members, so nobody can open it. `canManage` only decides which buttons appear. `edit_event` and `cancel_event` check the coach again themselves.

#### cancel_event (UC-B6)

```mermaid
sequenceDiagram
    actor Coach
    participant V as EventDetailsView
    participant C as CancelEventController
    participant ID as CancelEventInputData
    participant I as CancelEventInteractor
    participant DAI as CancelEventDataAccessInterface
    participant DAO as FileAppDataAccessObject
    participant E as Event
    participant OD as CancelEventOutputData
    participant P as CancelEventPresenter
    participant VM as CancelEventViewModel

    Coach->>V: Cancel event, then Yes in the confirm dialog
    V->>C: execute(eventId)
    C->>ID: new CancelEventInputData(eventId)
    C->>I: execute(inputData)
    I->>DAI: getEvent(eventId)
    DAI->>DAO: getEvent(eventId)
    DAO-->>I: original Event, or null
    break event is null
        I->>P: prepareFailView("That event no longer exists.")
    end
    I->>DAI: getCoachUsername(original.teamId)
    DAI->>DAO: getCoachUsername(teamId)
    DAO-->>I: coach username
    break user is not the coach
        I->>P: prepareFailView("Only the team's coach can cancel this event.")
    end
    break original is already cancelled (not reachable from the UI)
        I->>P: prepareFailView("This event is already cancelled.")
        P->>VM: setErrorMessage(message)
        VM-->>V: property change
        V->>V: show the error, no reload
    end
    I->>I: copyOf(original)
    I->>E: cancel() on the copy
    I->>DAI: updateEvent(cancelled)
    DAI->>DAO: updateEvent(cancelled)
    DAO->>DAO: put in map, persist()
    alt write succeeds
        DAO-->>I: return
        I->>OD: new CancelEventOutputData(eventId, "Event cancelled.")
        I->>P: prepareSuccessView(outputData)
        P->>VM: setSuccessMessage("Event cancelled.")
        VM-->>V: property change
        V->>V: reload via ViewEventDetailsController, show status
    else persist() throws IllegalStateException or UncheckedIOException
        DAO->>DAO: catch RuntimeException, put previous back, rethrow
        I->>I: catch (IllegalStateException | UncheckedIOException ex)
        I->>P: prepareFailView("Could not save the cancellation: ...")
        P->>VM: setErrorMessage(...)
        VM-->>V: property change
        V->>V: show the error and return without reloading
    else persist() throws any other RuntimeException
        DAO->>DAO: catch RuntimeException, put previous back, rethrow
        Note over I: not caught here, so the exception escapes execute() and no fail view is shown
    end
```

The already-cancelled branch cannot be reached from the UI, because the Cancel event button is hidden for cancelled events. The event is not deleted. It stays in the store flagged as cancelled, and the calendar, Events page and export all show it with a "Cancelled:" prefix. On any fail view the dialog shows the error and does not reload the event.

## 4. Storage

One local JSON file, `data/app-data.json` (git-ignored, created on first write), holds users, teams, events and attendance. `EntityJsonMapper` is the single place the on-disk shape lives, so a hosted store can replace the file without touching any use case.

The signed-in user is deliberately not in the file: it lives in `Session`, behind the `use_case/current_user/CurrentUserInterface` port, so the interactors own the session transitions rather than the views.

**Security note:** passwords are stored in plaintext, matching course scope. This must be revisited before any real deployment.

## 5. How to build, test and run

```bash
./gradlew build            # compile
./gradlew test             # unit tests (network-tagged tests skipped, so it works offline)
./gradlew test -Pnetwork   # also runs the live Open-Meteo check
./gradlew run              # start the app
```

Demo accounts are created on first run (password `pw`): `coach1`, `player1`, `parent1`, `admin1`.

## 6. Known limitations

- Data is **per machine**: the JSON file is local, so nothing is shared between devices yet. A hosted store is a later one-class swap.
- The calendar still shows hard-coded demo events until `view_schedule` lands (task 2.1).
- `manage_users` is not built yet, so roles are chosen at sign-up.
- One role per user (decision D5).

## 7. API notes

Open-Meteo (no key required) is our external API. It was called successfully from Java on 2026-10-29 and is documented in the README API section in the course required format (link, how it is used, whether it was called). Alternatives we considered are listed in `Plan.md` decision D7.