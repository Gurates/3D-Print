package printer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

public class DatabaseManager {
    // Proje klasöründe otomatik oluşacak veritabanı dosyası
    private static final String DB_URL = "jdbc:sqlite:farm_stats.db";

    public static void initDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            
            // Eğer tablo yoksa yeni bir istatistik tablosu yarat
            String sql = "CREATE TABLE IF NOT EXISTS print_jobs (" +
                         "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                         "print_name TEXT NOT NULL," +
                         "status TEXT NOT NULL," +
                         "duration TEXT," +
                         "error_reason TEXT" +
                         ");";
            stmt.execute(sql);
            System.out.println("[DB] Veritabanı ve tablolar hazır.");
            
        } catch (Exception e) {
            System.out.println("[DB Hata] Veritabanı oluşturulamadı: " + e.getMessage());
        }
    }

    // Yeni baskı başladığında veya bittiğinde veritabanına kaydet
    public static void logPrintJob(String printName, String status, String duration, String errorReason) {
        String sql = "INSERT INTO print_jobs (print_name, status, duration, error_reason) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, printName);
            pstmt.setString(2, status);
            pstmt.setString(3, duration);
            pstmt.setString(4, errorReason);
            pstmt.executeUpdate();
            
            System.out.println("[DB] Baskı loglandı: " + printName + " | Durum: " + status);
            
        } catch (Exception e) {
            System.out.println("[DB Hata] Log yazılamadı: " + e.getMessage());
        }
    }
}