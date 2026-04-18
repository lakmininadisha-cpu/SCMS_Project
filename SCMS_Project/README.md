# Smart Campus Management System (SCMS)
## Cardiff Metropolitan University – CMP7001 Advanced Programming

---

## Project Structure

```
SCMS_Project/
├── src/main/java/scms/
│   ├── Main.java                        ← Application entry point
│   ├── model/
│   │   ├── User.java                    ← Abstract base (OOP)
│   │   ├── UserRole.java                ← Enum: ADMIN, STAFF, STUDENT
│   │   ├── Admin.java                   ← Concrete user subclass
│   │   ├── Staff.java                   ← Concrete user subclass
│   │   ├── Student.java                 ← Concrete user subclass
│   │   ├── Room.java                    ← Room entity
│   │   ├── Booking.java                 ← Booking entity
│   │   ├── MaintenanceRequest.java      ← Maintenance entity
│   │   └── Notification.java           ← Notification entity
│   ├── exception/
│   │   ├── RoomAlreadyBookedException.java
│   │   ├── UnauthorizedActionException.java
│   │   ├── DuplicateEntityException.java
│   │   └── EntityNotFoundException.java
│   ├── pattern/
│   │   ├── NotificationObserver.java    ← BEHAVIOURAL: Observer interface
│   │   ├── NotificationService.java     ← CREATIONAL: Singleton + Observer subject
│   │   ├── RoomFactory.java             ← CREATIONAL: Factory Method
│   │   └── CampusFacade.java            ← STRUCTURAL: Facade
│   ├── service/
│   │   ├── UserService.java
│   │   ├── RoomService.java
│   │   ├── BookingService.java
│   │   └── MaintenanceService.java
│   ├── ui/
│   │   └── ConsoleUI.java               ← Console menu application
│   └── test/
│       ├── SCMSTests.java               ← JUnit 5 test class (15 tests)
│       └── TestRunner.java              ← Standalone test runner (no JUnit needed)
├── build.sh                             ← Linux/Mac build script
├── build_and_run.bat                    ← Windows build script
└── README.md
```

---

## Design Patterns Used

| Category    | Pattern           | Location                          |
|-------------|-------------------|-----------------------------------|
| Creational  | Singleton         | `NotificationService`             |
| Creational  | Factory Method    | `RoomFactory`                     |
| Structural  | Facade            | `CampusFacade`                    |
| Behavioural | Observer          | `NotificationObserver` + `NotificationService` |

---

## OOP Principles

- **Abstraction** – `User` is abstract; concrete roles extend it
- **Inheritance** – `Admin`, `Staff`, `Student` extend `User`
- **Encapsulation** – all fields private; access via getters/setters
- **Polymorphism** – `getPermissionSummary()` overridden per subclass
- **Interface** – `NotificationObserver` decouples notification delivery

---

## Exception Handling

| Exception                      | Trigger                                      |
|-------------------------------|----------------------------------------------|
| `RoomAlreadyBookedException`  | Overlapping booking for same room/time       |
| `UnauthorizedActionException` | Non-admin performing admin-only action       |
| `DuplicateEntityException`    | Adding room/user with existing ID            |
| `EntityNotFoundException`     | Referencing unknown room/user/booking/request|
| `IllegalArgumentException`    | Invalid time window (start >= end)           |

---

## Build & Run

### Prerequisites
- **JDK 17+** installed (`java` and `javac` on PATH)

### Linux / macOS

```bash
chmod +x build.sh
./build.sh
```

### Windows

```bat
build_and_run.bat
```

### Manual compile & run

```bash
# From project root – compile
find src -name "*.java" > sources.txt
javac --release 17 -d out @sources.txt

# Run the application
java -cp out scms.Main

# Run the standalone test runner
java -cp out scms.test.TestRunner
```

### With JUnit 5 (optional)

Download `junit-platform-console-standalone-1.10.0.jar` from Maven Central, then:

```bash
java -jar junit-platform-console-standalone-1.10.0.jar \
     --class-path out \
     --select-class=scms.test.SCMSTests \
     --details=verbose
```

---

## Demo Login Credentials (pre-seeded)

| Role    | Email                        | Password    |
|---------|------------------------------|-------------|
| Admin   | alice@cardiffmet.ac.uk       | admin123    |
| Staff   | bob@cardiffmet.ac.uk         | staff123    |
| Staff   | carol@cardiffmet.ac.uk       | staff123    |
| Student | dave@cardiffmet.ac.uk        | student123  |
| Student | eve@cardiffmet.ac.uk         | student123  |

---

## Test Plan Summary (15 Tests)

| ID    | Description                                           | Expected  |
|-------|-------------------------------------------------------|-----------|
| TC01  | Staff books available room                            | PASS      |
| TC02  | Double-booking same room/time                         | PASS      |
| TC03  | Non-overlapping bookings on same room                 | PASS      |
| TC04  | Owner cancels own booking                             | PASS      |
| TC05  | Booking deactivated room                              | PASS      |
| TC06  | Maintenance lifecycle PENDING→ASSIGNED→COMPLETED      | PASS      |
| TC07  | Booking confirmation notification delivered           | PASS      |
| TC08  | Maintenance assignment notification delivered         | PASS      |
| TC09  | **Student adds room – intentional failure**           | **FAIL**  |
| TC10  | Duplicate room ID rejected                            | PASS      |
| TC11  | Invalid login credentials rejected                   | PASS      |
| TC12  | Non-owner cannot cancel another's booking             | PASS      |
| TC13  | Admin can cancel any booking                          | PASS      |
| TC14  | RoomFactory creates correct equipment list            | PASS      |
| TC15  | Invalid time window (start >= end) rejected           | PASS      |

> **TC09** is deliberately written to call `assertDoesNotThrow` on an operation
> that *must* throw `UnauthorizedActionException`, proving the authorization
> guard is functioning correctly.
