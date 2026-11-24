package app;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import data_access.FileAppDataAccessObject;
import entity.AbsenceNotice;
import entity.Announcement;
import entity.Event;
import entity.ParentPlayerLink;
import entity.Role;
import entity.Team;
import entity.User;

/** Builds repeatable presentation data without replacing an existing store. */
public final class DemoDataSeeder {
    private DemoDataSeeder() { }

    /** Creates one admin, two coaches, eight players, four parents and two weeks of events. */
    public static boolean seedIfEmpty(FileAppDataAccessObject store, Clock clock) {
        if (!store.isEmpty()) {
            return false;
        }
        final List<User> users = new ArrayList<>();
        users.add(new User("admin1", "pw", "Ada Admin", Role.ADMIN));
        for (int i = 1; i <= 2; i++) {
            users.add(new User("coach" + i, "pw", "Coach " + i, Role.COACH));
        }
        for (int i = 1; i <= 8; i++) {
            users.add(new User("player" + i, "pw", "Player " + i, Role.PLAYER));
        }
        final List<ParentPlayerLink> links = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            users.add(new User("parent" + i, "pw", "Parent " + i, Role.PARENT));
            links.add(new ParentPlayerLink("parent" + i, "player" + (i * 2 - 1)));
            links.add(new ParentPlayerLink("parent" + i, "player" + (i * 2)));
        }
        final Team first = new Team("demo-team", "Toronto Hawks", "Basketball", "coach1");
        final Team second = new Team("north-team", "Ottawa Bears", "Soccer", "coach2");
        final Team third = new Team("south-team", "Hamilton Falcons", "Volleyball", "coach1");
        final List<Team> teams = List.of(first, second, third);
        for (int i = 1; i <= 8; i++) {
            teams.get(i <= 3 ? 0 : i <= 6 ? 1 : 2).addPlayer("player" + i);
        }
        final String[] locations = {"Toronto, Ontario", "Ottawa, Ontario", "Hamilton, Ontario"};
        final LocalDate today = LocalDate.now(clock);
        final LocalDateTime createdAt = LocalDateTime.now(clock);
        final List<Event> events = new ArrayList<>();
        final List<Announcement> announcements = new ArrayList<>();
        for (int teamIndex = 0; teamIndex < teams.size(); teamIndex++) {
            final Team team = teams.get(teamIndex);
            announcements.add(new Announcement("demo-post-" + teamIndex, team.getTeamId(),
                    team.getCoachUsername(), "Welcome to the season. Check the schedule and bring water.", createdAt));
            for (int day = 1; day <= 14; day++) {
                final boolean game = day % 4 == 0;
                events.add(new Event("demo-" + team.getTeamId() + "-" + day, team.getTeamId(),
                        game ? Event.EventType.GAME : Event.EventType.PRACTICE,
                        today.plusDays(day).atTime(18 + teamIndex, 0), locations[teamIndex], game ? "Rivals" : null));
            }
        }
        final String parent = "parent1";
        final String player = "player1";
        final String eventId = "demo-demo-team-2";
        final String identity = parent.length() + ":" + parent + player.length() + ":" + player + eventId;
        final String noticeId = java.util.UUID.nameUUIDFromBytes(
                identity.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
        final AbsenceNotice notice = new AbsenceNotice(noticeId, parent, player, eventId,
                "Family appointment; please excuse this absence.", createdAt);
        return store.initializeDemoData(users, teams, events, links, announcements, List.of(notice));
    }

    /** Writes a separate demo store and refuses an existing destination. */
    public static void main(String[] args) {
        final Path target = Path.of(args.length == 0 ? "data/demo-data.json" : args[0]);
        if (Files.exists(target)) {
            throw new IllegalStateException("Choose a new demo file. The destination already exists: " + target);
        }
        seedIfEmpty(new FileAppDataAccessObject(target), Clock.systemDefaultZone());
        System.out.println("Demo data created at " + target.toAbsolutePath());
        System.out.println("Accounts use password pw: admin1, coach1-2, player1-8, parent1-4.");
    }
}
