package printer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GCodeParser {

    private static final Pattern TEMP_PATTERN = Pattern.compile("T:(\\d+\\.\\d+)\\s+/(\\d+\\.\\d+)\\s+B:(\\d+\\.\\d+)\\s+/(\\d+\\.\\d+)");
    private static final Pattern SD_PATTERN = Pattern.compile("SD printing byte (\\d+)/(\\d+)");
    private static final Pattern TIME_PATTERN = Pattern.compile("echo:Print time:\\s*(.+)"); 
    
    // YENİ: Başlangıç, Bitiş ve Hata avcıları
    private static final Pattern FILE_OPEN_PATTERN = Pattern.compile("File opened: (.*?)\\s+Size:");
    private static final Pattern ERROR_PATTERN = Pattern.compile("Error:(.+)");

    // Canlı baskı takibi için anlık değişkenler
    private static String currentPrintName = "Bilinmiyor";
    private static boolean isPrinting = false;

    public static void parseLine(String line) {
        if (line == null || line.trim().isEmpty() || line.equals("ok")) return;
        PrinterState.rawData = line; 

        // 1. Dosya açıldı mı? (Baskı Başlangıcı)
        Matcher fileMatcher = FILE_OPEN_PATTERN.matcher(line);
        if (fileMatcher.find()) {
            currentPrintName = fileMatcher.group(1);
            isPrinting = true;
            PrinterState.progressPercent = 0;
            PrinterState.elapsedTime = "00:00:00";
            System.out.println(">>> YENİ BASKI BAŞLADI: " + currentPrintName);
            return;
        }

        // 2. Baskı bitti mi?
        if (line.contains("Done printing file") && isPrinting) {
            System.out.println(">>> BASKI BAŞARIYLA TAMAMLANDI!");
            DatabaseManager.logPrintJob(currentPrintName, "BAŞARILI", PrinterState.elapsedTime, "Yok");
            isPrinting = false;
            return;
        }

        // 3. Yazıcı hata mı verdi? (Kritik)
        Matcher errorMatcher = ERROR_PATTERN.matcher(line);
        if (errorMatcher.find() && isPrinting) {
            String errorReason = errorMatcher.group(1).trim();
            System.out.println(">>> BASKI HATASI: " + errorReason);
            DatabaseManager.logPrintJob(currentPrintName, "HATALI", PrinterState.elapsedTime, errorReason);
            isPrinting = false;
            return;
        }

        // --- Eski Isı ve Yüzde Okuma Mantığı Aşağıda Aynen Devam Ediyor ---
        Matcher tempMatcher = TEMP_PATTERN.matcher(line);
        if (tempMatcher.find()) {
            PrinterState.nozzleCurrent = tempMatcher.group(1);
            PrinterState.nozzleTarget = tempMatcher.group(2);
            PrinterState.bedCurrent = tempMatcher.group(3);
            PrinterState.bedTarget = tempMatcher.group(4);
            return;
        }

        Matcher sdMatcher = SD_PATTERN.matcher(line);
        if (sdMatcher.find()) {
            double currentBytes = Double.parseDouble(sdMatcher.group(1));
            double totalBytes = Double.parseDouble(sdMatcher.group(2));
            if (totalBytes > 0) {
                PrinterState.progressPercent = (int) ((currentBytes / totalBytes) * 100);
            }
            return;
        }

        Matcher timeMatcher = TIME_PATTERN.matcher(line);
        if (timeMatcher.find()) {
            PrinterState.elapsedTime = timeMatcher.group(1);
        }
    }
}