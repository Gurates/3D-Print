package printer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GCodeParser {
    private static final Pattern TEMP_PATTERN     = Pattern.compile("T:(\\d+\\.\\d+)\\s+/(\\d+\\.\\d+)\\s+B:(\\d+\\.\\d+)\\s+/(\\d+\\.\\d+)");
    private static final Pattern SD_PATTERN       = Pattern.compile("SD printing byte (\\d+)/(\\d+)");
    private static final Pattern TIME_PATTERN     = Pattern.compile("echo:Print time:\\s*(.+)");
    private static final Pattern FILE_OPEN        = Pattern.compile("File opened: (.*?)\\s+Size:");
    private static final Pattern ERROR_PATTERN    = Pattern.compile("Error:(.+)");
    private static final Pattern FILAMENT_PATTERN = Pattern.compile("Filament used:\\s*([\\d.]+)m");

    private final PrinterState   state;
    private final CostCalculator calc;
    private double filamentUsedM = 0.0;

    public GCodeParser(PrinterState state) {
        this.state = state;
        CostConfig cfg = DatabaseManager.loadCostConfig();
        this.calc = new CostCalculator(cfg);
    }

    public void parseLine(String line) {
        if (line == null || line.trim().isEmpty() || line.equals("ok")) return;
        state.rawData = line;

        Matcher fileMatcher = FILE_OPEN.matcher(line);
        if (fileMatcher.find()) {
            state.currentPrintName  = fileMatcher.group(1).trim();
            state.isPrinting        = true;
            state.progressPercent   = 0;
            state.elapsedTime       = "00:00:00";
            state.usedFilamentMm    = 0.0;
            state.lastCostBreakdown = null;
            filamentUsedM           = 0.0;
            System.out.printf("[%s] Baskı başladı: %s%n", state.displayName, state.currentPrintName);
            return;
        }

        if (line.contains("Done printing file") && state.isPrinting) {
            CostCalculator.CostBreakdown cb = calc.calculate(state.usedFilamentMm, parseElapsedToSeconds(state.elapsedTime));
            cb.isEstimate = false;
            state.lastCostBreakdown = cb;
            DatabaseManager.logPrintJob(
                state.portName, state.displayName,
                state.currentPrintName, "BAŞARILI",
                state.elapsedTime, parseElapsedToSeconds(state.elapsedTime),
                state.usedFilamentMm, null
            );
            String msg = "✅ <b>BASKI TAMAMLANDI!</b>\n\n" +
                         "🖨 <b>Yazıcı:</b> " + state.displayName + "\n" +
                         "📦 <b>Model:</b> " + state.currentPrintName + "\n" +
                         "🧵 <b>Filament:</b> " + String.format("%.2f m", filamentUsedM) + "\n" +
                         "⏱ <b>Süre:</b> " + state.elapsedTime + "\n" +
                         "💰 <b>Maliyet:</b> " + String.format("%.2f TL", cb.totalCostTL);
            TelegramNotifier.sendMessage(msg);
            state.isPrinting = false;
            return;
        }

        Matcher errorMatcher = ERROR_PATTERN.matcher(line);
        if (errorMatcher.find() && state.isPrinting) {
            String reason = errorMatcher.group(1).trim();
            CostCalculator.CostBreakdown cb = calc.calculate(state.usedFilamentMm, parseElapsedToSeconds(state.elapsedTime));
            cb.isEstimate = false;
            state.lastCostBreakdown = cb;
            DatabaseManager.logPrintJob(
                state.portName, state.displayName,
                state.currentPrintName, "HATALI",
                state.elapsedTime, parseElapsedToSeconds(state.elapsedTime),
                state.usedFilamentMm, reason
            );
            String msg = "❌ <b>BASKI HATALI!</b>\n\n" +
                         "🖨 <b>Yazıcı:</b> " + state.displayName + "\n" +
                         "📦 <b>Model:</b> " + state.currentPrintName + "\n" +
                         "⚠️ <b>Hata:</b> <code>" + reason + "</code>\n" +
                         "⏱ <b>Geçen Süre:</b> " + state.elapsedTime;
            TelegramNotifier.sendMessage(msg);
            state.isPrinting = false;
            return;
        }

        Matcher filamentMatcher = FILAMENT_PATTERN.matcher(line);
        if (filamentMatcher.find()) {
            filamentUsedM        = Double.parseDouble(filamentMatcher.group(1));
            state.usedFilamentMm = filamentUsedM * 1000.0;
            updateCostEstimate();
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
            double cur = Double.parseDouble(sdMatcher.group(1));
            double tot = Double.parseDouble(sdMatcher.group(2));
            if (tot > 0) { state.progressPercent = (int)((cur / tot) * 100); updateCostEstimate(); }
            return;
        }

        Matcher timeMatcher = TIME_PATTERN.matcher(line);
        if (timeMatcher.find()) {
            state.elapsedTime = timeMatcher.group(1).trim();
            updateCostEstimate();
        }
    }

    private void updateCostEstimate() {
        if (!state.isPrinting || state.progressPercent <= 0) return;
        int elapsed = parseElapsedToSeconds(state.elapsedTime);
        CostCalculator.CostBreakdown est = calc.projectFromProgress(
            state.progressPercent, state.usedFilamentMm, elapsed);
        state.lastCostBreakdown = est;
    }

    private int parseElapsedToSeconds(String elapsed) {
        if (elapsed == null || elapsed.isEmpty() || elapsed.equals("00:00:00")) return 0;
        try {
            if (elapsed.contains(":")) {
                String[] p = elapsed.split(":");
                return Integer.parseInt(p[0]) * 3600 + Integer.parseInt(p[1]) * 60 + Integer.parseInt(p[2]);
            }
            int s = 0;
            Matcher h = Pattern.compile("(\\d+)h").matcher(elapsed);
            Matcher m = Pattern.compile("(\\d+)m").matcher(elapsed);
            Matcher sec = Pattern.compile("(\\d+)s").matcher(elapsed);
            if (h.find()) s += Integer.parseInt(h.group(1)) * 3600;
            if (m.find()) s += Integer.parseInt(m.group(1)) * 60;
            if (sec.find()) s += Integer.parseInt(sec.group(1));
            return s;
        } catch (Exception e) { return 0; }
    }
}