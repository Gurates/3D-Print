package printer;

import java.util.ArrayList;
import java.util.List;
import printer.drivers.*;

public class PrinterManager {

    private static final List<PrinterState> activePrinters = new ArrayList<>();

    public static List<PrinterState> getPrinters() {
        return activePrinters;
    }

    public static void startMonitoring() {
        System.out.println("[Manager] Kurumsal Sürücü (Driver) Mimarisi Başlatılıyor...");

        PrinterState p1 = new PrinterState("WIFI_192.168.1.35", "Ender 3 Neo (Wi-Fi)");
        p1.printerModel = "Creality Ender (Marlin)";
        p1.driver = new MarlinSocketDriver("192.168.1.35", 9100); 
        activePrinters.add(p1);


        Thread monitorThread = new Thread(() -> {
            while (true) {
                for (PrinterState printer : activePrinters) {
                    if (printer.driver != null) {
                        printer.driver.updateState(printer);
                    }
                }
                
                try {
                    Thread.sleep(2000); 
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        
        monitorThread.setDaemon(true);
        monitorThread.start();
    }
}