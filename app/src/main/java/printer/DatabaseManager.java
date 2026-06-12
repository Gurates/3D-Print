package printer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:farm_stats.db";

    public static void initDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS print_jobs (" +
                "  id               INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  printer_port     TEXT    NOT NULL," +
                "  printer_name     TEXT    NOT NULL," +
                "  print_name       TEXT    NOT NULL," +
                "  status           TEXT    NOT NULL," +
                "  duration_text    TEXT," +
                "  duration_seconds INTEGER DEFAULT 0," +
                "  filament_mm      REAL    DEFAULT 0," +
                "  error_reason     TEXT," +
                "  created_at       DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ");"
            );

            System.out.println("[DB] Veritabanı hazır.");
        } catch (Exception e) {
            System.out.println("[DB Hata] " + e.getMessage());
        }
    }

    public static void logPrintJob(String printerPort, String printerName,
                                   String printName, String status,
                                   String durationText, int durationSeconds,
                                   double filamentMm, String errorReason) {
        String sql = "INSERT INTO print_jobs " +
                     "(printer_port, printer_name, print_name, status, duration_text, duration_seconds, filament_mm, error_reason) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, printerPort);
            ps.setString(2, printerName);
            ps.setString(3, printName);
            ps.setString(4, status);
            ps.setString(5, durationText);
            ps.setInt   (6, durationSeconds);
            ps.setDouble(7, filamentMm);
            ps.setString(8, errorReason);
            ps.executeUpdate();
            System.out.println("[DB] Log eklendi: " + printerName + " | " + printName + " | " + status);
        } catch (Exception e) {
            System.out.println("[DB Hata] Log yazılamadı: " + e.getMessage());
        }
    }

    public static class PrinterSummary {
        public String printerName;
        public int totalJobs;
        public int successJobs;
        public int failJobs;
        public long totalSeconds;       // Toplam baskı süresi (saniye)
        public double totalFilamentM;   // Toplam filament metre cinsinden
    }

    public static List<PrinterSummary> getPrinterSummaries() {
        List<PrinterSummary> list = new ArrayList<>();
        String sql =
            "SELECT printer_name," +
            "  COUNT(*) AS total," +
            "  SUM(CASE WHEN status='BAŞARILI' THEN 1 ELSE 0 END) AS success," +
            "  SUM(CASE WHEN status='HATALI'   THEN 1 ELSE 0 END) AS fail," +
            "  SUM(duration_seconds)  AS total_secs," +
            "  SUM(filament_mm)/1000.0 AS total_m" +
            " FROM print_jobs GROUP BY printer_name";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                PrinterSummary s = new PrinterSummary();
                s.printerName    = rs.getString("printer_name");
                s.totalJobs      = rs.getInt("total");
                s.successJobs    = rs.getInt("success");
                s.failJobs       = rs.getInt("fail");
                s.totalSeconds   = rs.getLong("total_secs");
                s.totalFilamentM = rs.getDouble("total_m");
                list.add(s);
            }
        } catch (Exception e) {
            System.out.println("[DB Hata] getPrinterSummaries: " + e.getMessage());
        }
        return list;
    }

    public static List<String[]> getRecentJobs(int limit) {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT printer_name, print_name, status, duration_text, filament_mm, created_at " +
                     "FROM print_jobs ORDER BY id DESC LIMIT ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rows.add(new String[]{
                    rs.getString("printer_name"),
                    rs.getString("print_name"),
                    rs.getString("status"),
                    rs.getString("duration_text"),
                    String.format("%.1f m", rs.getDouble("filament_mm") / 1000.0),
                    rs.getString("created_at")
                });
            }
        } catch (Exception e) {
            System.out.println("[DB Hata] getRecentJobs: " + e.getMessage());
        }
        return rows;
    }

    public static List<String[]> getErrorBreakdown() {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT error_reason, COUNT(*) as cnt FROM print_jobs " +
                     "WHERE status='HATALI' AND error_reason IS NOT NULL " +
                     "GROUP BY error_reason ORDER BY cnt DESC LIMIT 10";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                rows.add(new String[]{ rs.getString("error_reason"), String.valueOf(rs.getInt("cnt")) });
            }
        } catch (Exception e) {
            System.out.println("[DB Hata] getErrorBreakdown: " + e.getMessage());
        }
        return rows;
    }
}