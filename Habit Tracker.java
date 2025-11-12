import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class HabitTracker {
    private static final String FILE = "habits.csv"; // format: habit,date (e.g., "Read,2025-11-12")
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    // habit -> set of completion dates
    private static final Map<String, Set<LocalDate>> data = new HashMap<>();

    public static void main(String[] args) {
        load();
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n=== HABIT TRACKER ===");
            System.out.println("1) Add habit");
            System.out.println("2) Mark today done");
            System.out.println("3) List habits (with streaks)");
            System.out.println("4) Show last 7 days heatmap");
            System.out.println("5) Exit");
            System.out.print("Choice: ");
            String ch = sc.nextLine().trim();

            switch (ch) {
                case "1" -> addHabit(sc);
                case "2" -> markToday(sc);
                case "3" -> listHabits();
                case "4" -> heatmap();
                case "5" -> { save(); System.out.println("Saved. Bye!"); return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static void addHabit(Scanner sc) {
        System.out.print("Habit name: ");
        String h = sc.nextLine().trim();
        if (h.isEmpty()) { System.out.println("Name required."); return; }
        data.putIfAbsent(cap(h), new HashSet<>());
        System.out.println("✅ Added: " + cap(h));
    }

    private static void markToday(Scanner sc) {
        if (data.isEmpty()) { System.out.println("No habits yet."); return; }
        String h = pickHabit(sc);
        if (h == null) return;
        LocalDate today = LocalDate.now();
        data.get(h).add(today);
        System.out.println("✅ Marked " + h + " for " + today);
    }

    private static void listHabits() {
        if (data.isEmpty()) { System.out.println("No habits yet."); return; }
        System.out.printf("%-20s %-8s %-12s %-12s%n", "Habit", "Days", "Streak", "BestStreak");
        System.out.println("-----------------------------------------------------------");
        for (var e : data.entrySet()) {
            String h = e.getKey();
            Set<LocalDate> days = e.getValue();
            System.out.printf("%-20s %-8d %-12d %-12d%n",
                    h, days.size(), currentStreak(days), bestStreak(days));
        }
    }

    private static void heatmap() {
        if (data.isEmpty()) { System.out.println("No habits yet."); return; }
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(6);

        System.out.println("\nLast 7 days (■ = done, □ = miss):");
        System.out.print("Dates : ");
        for (int i = 0; i < 7; i++) {
            LocalDate d = start.plusDays(i);
            System.out.print(d.getDayOfMonth() + (i == 6 ? "" : " "));
        }
        System.out.println();

        for (var e : data.entrySet()) {
            System.out.printf("%-6s: ", e.getKey().length() > 6 ? e.getKey().substring(0, 6) : e.getKey());
            for (int i = 0; i < 7; i++) {
                LocalDate d = start.plusDays(i);
                System.out.print(e.getValue().contains(d) ? "■ " : "□ ");
            }
            System.out.println();
        }
    }

    // ---------- Streak helpers ----------
    private static int currentStreak(Set<LocalDate> days) {
        int s = 0;
        LocalDate d = LocalDate.now();
        while (days.contains(d)) { s++; d = d.minusDays(1); }
        return s;
    }

    private static int bestStreak(Set<LocalDate> days) {
        if (days.isEmpty()) return 0;
        List<LocalDate> list = new ArrayList<>(days);
        Collections.sort(list);
        int best = 1, cur = 1;
        for (int i = 1; i < list.size(); i++) {
            if (list.get(i).minusDays(1).equals(list.get(i - 1))) cur++;
            else { best = Math.max(best, cur); cur = 1; }
        }
        return Math.max(best, cur);
    }

    // ---------- Storage ----------
    private static void load() {
        try {
            if (!Files.exists(Path.of(FILE))) return;
            List<String> lines = Files.readAllLines(Path.of(FILE));
            for (String line : lines) {
                String[] parts = line.split(",", 2);
                if (parts.length != 2) continue;
                String habit = cap(parts[0].trim());
                LocalDate date = LocalDate.parse(parts[1].trim(), FMT);
                data.putIfAbsent(habit, new HashSet<>());
                data.get(habit).add(date);
            }
        } catch (Exception e) {
            System.out.println("Could not load data: " + e.getMessage());
        }
    }

    private static void save() {
        try (PrintWriter out = new PrintWriter(new FileWriter(FILE))) {
            for (var e : data.entrySet()) {
                String h = e.getKey();
                for (LocalDate d : e.getValue()) {
                    out.println(h + "," + d.format(FMT));
                }
            }
        } catch (IOException e) {
            System.out.println("Could not save: " + e.getMessage());
        }
    }

    // ---------- Utils ----------
    private static String pickHabit(Scanner sc) {
        List<String> habits = new ArrayList<>(data.keySet());
        for (int i = 0; i < habits.size(); i++)
            System.out.println((i + 1) + ") " + habits.get(i));
        System.out.print("Select: ");
        try {
            int idx = Integer.parseInt(sc.nextLine().trim()) - 1;
            if (idx < 0 || idx >= habits.size()) { System.out.println("Invalid."); return null; }
            return habits.get(idx);
        } catch (NumberFormatException e) {
            System.out.println("Invalid.");
            return null;
        }
    }

    private static String cap(String s) {
        return s.isEmpty() ? s : s.substring(0,1).toUpperCase() + s.substring(1);
    }
}
