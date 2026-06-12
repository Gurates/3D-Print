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

            // 2. YENİ: Ayarlar Tablosu
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS settings (" +
                "  setting_key      TEXT PRIMARY KEY," +
                "  setting_value    TEXT" +
                ");"
            );

            System.out.println("[DB] Veritabanı ve tablolar hazır.");
        } catch (Exception e) {
            System.out.println("[DB Hata] " + e.getMessage());
        }
    }

    public static void saveSetting(String key, String value) {
        String sql = "INSERT OR REPLACE INTO settings (setting_key, setting_value) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
            System.out.println("[DB] Ayar kaydedildi: " + key);
        } catch (Exception e) {
            System.out.println("[DB Hata] Ayar kaydedilemedi: " + e.getMessage());
        }
    }

    public static String getSetting(String key, String defaultValue) {
        String sql = "SELECT setting_value FROM settings WHERE setting_key = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("setting_value");
            }
        } catch (Exception e) {
            System.out.println("[DB Hata] Ayar okunamadı: " + e.getMessage());
        }
        return defaultValue;
    }

    public static void logPrintJob(String printerPort, String printerName, String printName, String status, String durationText, int durationSeconds, double filamentMm, String errorReason) {
        String sql = "INSERT INTO print_jobs (printer_port, printer_name, print_name, status, duration_text, duration_seconds, filament_mm, error_reason) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, printerPort); ps.setString(2, printerName); ps.setString(3, printName); ps.setString(4, status); ps.setString(5, durationText); ps.setInt(6, durationSeconds); ps.setDouble(7, filamentMm); ps.setString(8, errorReason); ps.executeUpdate();
        } catch (Exception e) { System.out.println("[DB Hata] Log yazılamadı: " + e.getMessage()); }
    }

    public static class PrinterSummary {
        public String printerName; public int totalJobs; public int successJobs; public int failJobs; public long totalSeconds; public double totalFilamentM;
    }

    public static List<PrinterSummary> getPrinterSummaries() {
        List<PrinterSummary> list = new ArrayList<>();
        String sql = "SELECT printer_name, COUNT(*) AS total, SUM(CASE WHEN status='BAŞARILI' THEN 1 ELSE 0 END) AS success, SUM(CASE WHEN status='HATALI' THEN 1 ELSE 0 END) AS fail, SUM(duration_seconds) AS total_secs, SUM(filament_mm)/1000.0 AS total_m FROM print_jobs GROUP BY printer_name";
        try (Connection conn = DriverManager.getConnection(DB_URL); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) { PrinterSummary s = new PrinterSummary(); s.printerName = rs.getString("printer_name"); s.totalJobs = rs.getInt("total"); s.successJobs = rs.getInt("success"); s.failJobs = rs.getInt("fail"); s.totalSeconds = rs.getLong("total_secs"); s.totalFilamentM = rs.getDouble("total_m"); list.add(s); }
        } catch (Exception e) {} return list;
    }

    public static PrinterSummary getSinglePrinterSummary(String printerName) {
        String sql = "SELECT printer_name, COUNT(*) AS total, SUM(CASE WHEN status='BAŞARILI' THEN 1 ELSE 0 END) AS success, SUM(CASE WHEN status='HATALI' THEN 1 ELSE 0 END) AS fail, SUM(duration_seconds) AS total_secs, SUM(filament_mm)/1000.0 AS total_m FROM print_jobs WHERE printer_name=? GROUP BY printer_name";
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, printerName); ResultSet rs = ps.executeQuery();
            if (rs.next()) { PrinterSummary s = new PrinterSummary(); s.printerName = rs.getString("printer_name"); s.totalJobs = rs.getInt("total"); s.successJobs = rs.getInt("success"); s.failJobs = rs.getInt("fail"); s.totalSeconds = rs.getLong("total_secs"); s.totalFilamentM = rs.getDouble("total_m"); return s; }
        } catch (Exception e) {} return null;
    }

    public static List<String[]> getModelSummaries(String printerName) {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT print_name, COUNT(*) as total_cnt, SUM(CASE WHEN status='BAŞARILI' THEN 1 ELSE 0 END) as success_cnt, SUM(CASE WHEN status='HATALI' THEN 1 ELSE 0 END) as fail_cnt FROM print_jobs ";
        if (printerName != null) sql += "WHERE printer_name=? "; sql += "GROUP BY print_name ORDER BY total_cnt DESC";
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (printerName != null) ps.setString(1, printerName); ResultSet rs = ps.executeQuery();
            while (rs.next()) { rows.add(new String[]{ rs.getString("print_name"), String.valueOf(rs.getInt("total_cnt")), String.valueOf(rs.getInt("success_cnt")), String.valueOf(rs.getInt("fail_cnt")) }); }
        } catch (Exception e) {} return rows;
    }

    public static List<String[]> getRecentJobs(int limit, String printerName) {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT printer_name, print_name, status, duration_text, filament_mm, created_at FROM print_jobs ";
        if (printerName != null) sql += "WHERE printer_name=? "; sql += "ORDER BY id DESC LIMIT ?";
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement ps = conn.prepareStatement(sql)) {
            int paramIdx = 1; if (printerName != null) ps.setString(paramIdx++, printerName); ps.setInt(paramIdx, limit); ResultSet rs = ps.executeQuery();
            while (rs.next()) { rows.add(new String[]{ rs.getString("printer_name"), rs.getString("print_name"), rs.getString("status"), rs.getString("duration_text"), String.format("%.1f m", rs.getDouble("filament_mm") / 1000.0), rs.getString("created_at") }); }
        } catch (Exception e) {} return rows;
    }

    public static List<String[]> getErrorBreakdown(String printerName) {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT error_reason, COUNT(*) as cnt FROM print_jobs WHERE status='HATALI' AND error_reason IS NOT NULL ";
        if (printerName != null) sql += "AND printer_name=? "; sql += "GROUP BY error_reason ORDER BY cnt DESC LIMIT 10";
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (printerName != null) ps.setString(1, printerName); ResultSet rs = ps.executeQuery();
            while (rs.next()) { rows.add(new String[]{ rs.getString("error_reason"), String.valueOf(rs.getInt("cnt")) }); }
        } catch (Exception e) {} return rows;
    }
}