package printer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:farm_stats.db";

    public static void initDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            // Ana baskı log tablosu — cost_tl sütunu eklendi
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
                "  filament_grams   REAL    DEFAULT 0," +
                "  cost_tl          REAL    DEFAULT 0," +   // Gerçek toplam maliyet TL
                "  selling_price_tl REAL    DEFAULT 0," +   // Önerilen satış fiyatı TL
                "  error_reason     TEXT," +
                "  created_at       DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ");"
            );

            // Maliyet ayarları tablosu — tek satır, her zaman id=1
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS cost_config (" +
                "  id                      INTEGER PRIMARY KEY DEFAULT 1," +
                "  filament_price_per_kg   REAL DEFAULT 600.0," +
                "  electricity_cost_per_hr REAL DEFAULT 0.70," +
                "  labor_cost_per_hr       REAL DEFAULT 0.0," +
                "  profit_margin_pct       REAL DEFAULT 30.0," +
                "  filament_diameter_mm    REAL DEFAULT 1.75," +
                "  filament_density        REAL DEFAULT 1.24" +
                ");"
            );

            // Ayar satırı yoksa varsayılanları ekle
            stmt.execute(
                "INSERT OR IGNORE INTO cost_config (id) VALUES (1);"
            );

            // Telegram / genel ayarlar tablosu
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS settings (" +
                "  setting_key   TEXT PRIMARY KEY," +
                "  setting_value TEXT" +
                ");"
            );

            // Eski DB'ye cost sütunları ekle (migration — varsa hata yutulur)
            tryAlter(conn, "ALTER TABLE print_jobs ADD COLUMN filament_grams   REAL DEFAULT 0");
            tryAlter(conn, "ALTER TABLE print_jobs ADD COLUMN cost_tl          REAL DEFAULT 0");
            tryAlter(conn, "ALTER TABLE print_jobs ADD COLUMN selling_price_tl REAL DEFAULT 0");

            System.out.println("[DB] Veritabanı hazır.");
        } catch (Exception e) {
            System.out.println("[DB Hata] " + e.getMessage());
        }
    }

    private static void tryAlter(Connection conn, String sql) {
        try (Statement s = conn.createStatement()) { s.execute(sql); }
        catch (Exception ignored) {}  // Sütun zaten varsa hata vermez
    }

    // ── Maliyet ayarları ─────────────────────────────────────────────────

    public static CostConfig loadCostConfig() {
        CostConfig cfg = new CostConfig();
        String sql = "SELECT * FROM cost_config WHERE id=1";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                cfg.filamentPricePerKg    = rs.getDouble("filament_price_per_kg");
                cfg.electricityCostPerHour = rs.getDouble("electricity_cost_per_hr");
                cfg.laborCostPerHour       = rs.getDouble("labor_cost_per_hr");
                cfg.profitMarginPercent    = rs.getDouble("profit_margin_pct");
                cfg.filamentDiameterMm     = rs.getDouble("filament_diameter_mm");
                cfg.filamentDensity        = rs.getDouble("filament_density");
            }
        } catch (Exception e) {
            System.out.println("[DB Hata] loadCostConfig: " + e.getMessage());
        }
        return cfg;
    }

    public static void saveCostConfig(CostConfig cfg) {
        String sql =
            "UPDATE cost_config SET " +
            "filament_price_per_kg=?, electricity_cost_per_hr=?, labor_cost_per_hr=?," +
            "profit_margin_pct=?, filament_diameter_mm=?, filament_density=? WHERE id=1";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, cfg.filamentPricePerKg);
            ps.setDouble(2, cfg.electricityCostPerHour);
            ps.setDouble(3, cfg.laborCostPerHour);
            ps.setDouble(4, cfg.profitMarginPercent);
            ps.setDouble(5, cfg.filamentDiameterMm);
            ps.setDouble(6, cfg.filamentDensity);
            ps.executeUpdate();
            System.out.println("[DB] Maliyet ayarları kaydedildi: " + cfg);
        } catch (Exception e) {
            System.out.println("[DB Hata] saveCostConfig: " + e.getMessage());
        }
    }

    // ── Baskı loglama ────────────────────────────────────────────────────

    public static void logPrintJob(String printerPort, String printerName,
                                   String printName, String status,
                                   String durationText, int durationSeconds,
                                   double filamentMm, String errorReason) {
        // Maliyet config'i yükle ve hesapla
        CostConfig cfg      = loadCostConfig();
        CostCalculator calc = new CostCalculator(cfg);
        CostCalculator.CostBreakdown cost = calc.calculate(filamentMm, durationSeconds);

        String sql = "INSERT INTO print_jobs " +
            "(printer_port, printer_name, print_name, status, duration_text, duration_seconds," +
            " filament_mm, filament_grams, cost_tl, selling_price_tl, error_reason) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, printerPort);
            ps.setString(2, printerName);
            ps.setString(3, printName);
            ps.setString(4, status);
            ps.setString(5, durationText);
            ps.setInt   (6, durationSeconds);
            ps.setDouble(7, filamentMm);
            ps.setDouble(8, cost.filamentGrams);
            ps.setDouble(9, cost.totalCostTL);
            ps.setDouble(10, cost.sellingPriceTL);
            ps.setString(11, errorReason);
            ps.executeUpdate();
            System.out.printf("[DB] Log: %s | %s | %s | Maliyet: %.2f TL%n",
                printerName, printName, status, cost.totalCostTL);
        } catch (Exception e) {
            System.out.println("[DB Hata] logPrintJob: " + e.getMessage());
        }
    }

    // ── İstatistik sorguları ─────────────────────────────────────────────

    public static class PrinterSummary {
        public String printerName;
        public int    totalJobs;
        public int    successJobs;
        public int    failJobs;
        public long   totalSeconds;
        public double totalFilamentM;
        public double totalCostTL;       // Yeni: toplam maliyet
        public double totalRevenueTL;    // Yeni: toplam satış fiyatı
    }

    public static List<PrinterSummary> getPrinterSummaries() {
        List<PrinterSummary> list = new ArrayList<>();
        String sql =
            "SELECT printer_name," +
            "  COUNT(*) AS total," +
            "  SUM(CASE WHEN status='BAŞARILI' THEN 1 ELSE 0 END) AS success," +
            "  SUM(CASE WHEN status='HATALI'   THEN 1 ELSE 0 END) AS fail," +
            "  SUM(duration_seconds)       AS total_secs," +
            "  SUM(filament_mm)/1000.0     AS total_m," +
            "  SUM(cost_tl)                AS total_cost," +
            "  SUM(selling_price_tl)       AS total_revenue" +
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
                s.totalCostTL    = rs.getDouble("total_cost");
                s.totalRevenueTL = rs.getDouble("total_revenue");
                list.add(s);
            }
        } catch (Exception e) {
            System.out.println("[DB Hata] getPrinterSummaries: " + e.getMessage());
        }
        return list;
    }

    public static List<String[]> getRecentJobs(int limit) {
        List<String[]> rows = new ArrayList<>();
        String sql =
            "SELECT printer_name, print_name, status, duration_text," +
            "       filament_grams, cost_tl, selling_price_tl, created_at " +
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
                    String.format("%.1f g", rs.getDouble("filament_grams")),
                    String.format("%.2f TL", rs.getDouble("cost_tl")),
                    String.format("%.2f TL", rs.getDouble("selling_price_tl")),
                    rs.getString("created_at")
                });
            }
        } catch (Exception e) {
            System.out.println("[DB Hata] getRecentJobs: " + e.getMessage());
        }
        return rows;
    }

    public static List<String[]> getErrorBreakdown() {
        return getErrorBreakdown(null);
    }

    /** Opsiyonel yazıcı filtresiyle hata dağılımı. null → tüm yazıcılar */
    public static List<String[]> getErrorBreakdown(String printerName) {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT error_reason, COUNT(*) as cnt FROM print_jobs " +
                     "WHERE status='HATALI' AND error_reason IS NOT NULL" +
                     (printerName != null ? " AND printer_name=?" : "") +
                     " GROUP BY error_reason ORDER BY cnt DESC LIMIT 10";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (printerName != null) ps.setString(1, printerName);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rows.add(new String[]{
                    rs.getString("error_reason"),
                    String.valueOf(rs.getInt("cnt"))
                });
            }
        } catch (Exception e) {
            System.out.println("[DB Hata] getErrorBreakdown: " + e.getMessage());
        }
        return rows;
    }

    /** Opsiyonel yazıcı filtresiyle son N baskı. null → tüm yazıcılar */
    public static List<String[]> getRecentJobs(int limit, String printerName) {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT printer_name, print_name, status, duration_text," +
                     "       filament_grams, cost_tl, selling_price_tl, created_at " +
                     "FROM print_jobs" +
                     (printerName != null ? " WHERE printer_name=?" : "") +
                     " ORDER BY id DESC LIMIT ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            if (printerName != null) ps.setString(idx++, printerName);
            ps.setInt(idx, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rows.add(new String[]{
                    rs.getString("printer_name"),
                    rs.getString("print_name"),
                    rs.getString("status"),
                    rs.getString("duration_text"),
                    String.format("%.1f g", rs.getDouble("filament_grams")),
                    String.format("%.2f TL", rs.getDouble("cost_tl")),
                    String.format("%.2f TL", rs.getDouble("selling_price_tl")),
                    rs.getString("created_at")
                });
            }
        } catch (Exception e) {
            System.out.println("[DB Hata] getRecentJobs: " + e.getMessage());
        }
        return rows;
    }

    /** Tek bir yazıcının özet istatistiği */
    public static PrinterSummary getSinglePrinterSummary(String printerName) {
        String sql =
            "SELECT printer_name," +
            "  COUNT(*) AS total," +
            "  SUM(CASE WHEN status='BAŞARILI' THEN 1 ELSE 0 END) AS success," +
            "  SUM(CASE WHEN status='HATALI'   THEN 1 ELSE 0 END) AS fail," +
            "  SUM(duration_seconds)   AS total_secs," +
            "  SUM(filament_mm)/1000.0 AS total_m," +
            "  SUM(cost_tl)            AS total_cost," +
            "  SUM(selling_price_tl)   AS total_revenue" +
            " FROM print_jobs WHERE printer_name=? GROUP BY printer_name";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, printerName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                PrinterSummary s = new PrinterSummary();
                s.printerName    = rs.getString("printer_name");
                s.totalJobs      = rs.getInt("total");
                s.successJobs    = rs.getInt("success");
                s.failJobs       = rs.getInt("fail");
                s.totalSeconds   = rs.getLong("total_secs");
                s.totalFilamentM = rs.getDouble("total_m");
                s.totalCostTL    = rs.getDouble("total_cost");
                s.totalRevenueTL = rs.getDouble("total_revenue");
                return s;
            }
        } catch (Exception e) {
            System.out.println("[DB Hata] getSinglePrinterSummary: " + e.getMessage());
        }
        return null;
    }

    /**
     * Model (dosya adı) bazında baskı dağılımı.
     * printerName null → tüm farm; değilse o yazıcıya özel.
     * Dönüş: [print_name, total, success, fail]
     */
    public static List<String[]> getModelSummaries(String printerName) {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT print_name," +
                     "  COUNT(*) AS total," +
                     "  SUM(CASE WHEN status='BAŞARILI' THEN 1 ELSE 0 END) AS success," +
                     "  SUM(CASE WHEN status='HATALI'   THEN 1 ELSE 0 END) AS fail" +
                     " FROM print_jobs" +
                     (printerName != null ? " WHERE printer_name=?" : "") +
                     " GROUP BY print_name ORDER BY total DESC";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (printerName != null) ps.setString(1, printerName);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rows.add(new String[]{
                    rs.getString("print_name"),
                    String.valueOf(rs.getInt("total")),
                    String.valueOf(rs.getInt("success")),
                    String.valueOf(rs.getInt("fail"))
                });
            }
        } catch (Exception e) {
            System.out.println("[DB Hata] getModelSummaries: " + e.getMessage());
        }
        return rows;
    }

    /** Telegram / genel key-value ayar okuma */
    public static String getSetting(String key, String defaultValue) {
        String sql = "SELECT setting_value FROM settings WHERE setting_key=?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("setting_value");
        } catch (Exception e) {
            System.out.println("[DB Hata] getSetting: " + e.getMessage());
        }
        return defaultValue;
    }

    /** Telegram / genel key-value ayar yazma */
    public static void saveSetting(String key, String value) {
        // settings tablosu yoksa oluştur
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS settings " +
                         "(setting_key TEXT PRIMARY KEY, setting_value TEXT)");
        } catch (Exception ignored) {}

        String sql = "INSERT OR REPLACE INTO settings (setting_key, setting_value) VALUES (?,?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (Exception e) {
            System.out.println("[DB Hata] saveSetting: " + e.getMessage());
        }
    }
}