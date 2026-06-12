package printer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GCodeParser {

    private static final Pattern TEMP_PATTERN   = Pattern.compile("T:(\\d+\\.\\d+)\\s+/(\\d+\\.\\d+)\\s+B:(\\d+\\.\\d+)\\s+/(\\d+\\.\\d+)");
    private static final Pattern SD_PATTERN     = Pattern.compile("SD printing byte (\\d+)/(\\d+)");
    private static final Pattern TIME_PATTERN   = Pattern.compile("echo:Print time:\\s*(.+)");
    private static final Pattern FILE_OPEN      = Pattern.compile("File opened: (.*?)\\s+Size:");
    private static final Pattern ERROR_PATTERN  = Pattern.compile("Error:(.+)");
    private static final Pattern FILAMENT_PATTERN = Pattern.compile("Filament used:\\s*([\\d.]+)m");

    private final PrinterState state;
    private double filamentUsedM = 0.0;

    public GCodeParser(PrinterState state) {
        this.state = state;
    }

    public void parseLine(String line) {
        if (line == null || line.trim().isEmpty() || line.equals("ok")) return;
        state.rawData = line;
        Matcher fileMatcher = FILE_OPEN.matcher(line);
        if (fileMatcher.find()) {
            state.currentPrintName = fileMatcher.group(1).trim();
            state.isPrinting       = true;
            state.progressPercent  = 0;
            state.elapsedTime      = "00:00:00";
            filamentUsedM          = 0.0;
            System.out.println("[" + state.displayName + "] Baskı başladı: " + state.currentPrintName);
            return;
        }
        if (line.contains("Done printing file") && state.isPrinting) {
            System.out.println("[" + state.displayName + "] Baskı tamamlandı.");
            DatabaseManager.logPrintJob(
                state.portName, state.displayName,
                state.currentPrintName, "BAŞARILI",
                state.elapsedTime, parseElapsedToSeconds(state.elapsedTime),
                filamentUsedM * 1000, // metre → mm
                null
            );
            state.isPrinting = false;
            return;
        }
        Matcher errorMatcher = ERROR_PATTERN.matcher(line);
        if (errorMatcher.find() && state.isPrinting) {
            String reason = errorMatcher.group(1).trim();
            System.out.println("[" + state.displayName + "] Hata: " + reason);
            DatabaseManager.logPrintJob(
                state.portName, state.displayName,
                state.currentPrintName, "HATALI",
                state.elapsedTime, parseElapsedToSeconds(state.elapsedTime),
                filamentUsedM * 1000,
                reason
            );
            state.isPrinting = false;
            return;
        }
        Matcher filamentMatcher = FILAMENT_PATTERN.matcher(line);
        if (filamentMatcher.find()) {
            filamentUsedM = Double.parseDouble(filamentMatcher.group(1));
            return;
        }
        Matcher tempMatcher = TEMP_PATTERN.matcher(line);
        if (tempMatcher.find()) {
            state.nozzleCurrent = tempMatcher.group(1);
            state.nozzleTarget  = tempMatcher.group(2);
            state.bedCurrent    = tempMatcher.group(3);
            state.bedTarget     = tempMatcher.group(4);
            return;
        }
        Matcher sdMatcher = SD_PATTERN.matcher(line);
        if (sdMatcher.find()) {
            double cur   = Double.parseDouble(sdMatcher.group(1));
            double total = Double.parseDouble(sdMatcher.group(2));
            if (total > 0) state.progressPercent = (int) ((cur / total) * 100);
            return;
        }
        Matcher timeMatcher = TIME_PATTERN.matcher(line);
        if (timeMatcher.find()) {
            state.elapsedTime = timeMatcher.group(1).trim();
        }
    }

    private int parseElapsedToSeconds(String elapsed) {
        if (elapsed == null || elapsed.equals("00:00:00")) return 0;
        try {
            if (elapsed.contains(":")) {
                String[] p = elapsed.split(":");
                return Integer.parseInt(p[0]) * 3600 + Integer.parseInt(p[1]) * 60 + Integer.parseInt(p[2]);
            }
            int secs = 0;
            Matcher h = Pattern.compile("(\\d+)h").matcher(elapsed);
            Matcher m = Pattern.compile("(\\d+)m").matcher(elapsed);
            Matcher s = Pattern.compile("(\\d+)s").matcher(elapsed);
            if (h.find()) secs += Integer.parseInt(h.group(1)) * 3600;
            if (m.find()) secs += Integer.parseInt(m.group(1)) * 60;
            if (s.find()) secs += Integer.parseInt(s.group(1));
            return secs;
        } catch (Exception e) {
            return 0;
        }
    }
}