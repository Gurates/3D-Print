package printer;

import printer.drivers.*;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PrinterManager {

    private static final List<PrinterState> activePrinters = new CopyOnWriteArrayList<>();

    private static final ExecutorService farmThreadPool = Executors.newCachedThreadPool();

    public static List<PrinterState> getPrinters() {
        return activePrinters;
    }

    public static void startMonitoring() {
        System.out.println("[Manager] Multi-Thread (Çoklu İş Parçacığı) Mimarisi Başlatılıyor...");
        PrinterState p1 = new PrinterState("WIFI_192.168.1.35", "Ender 3 Neo (Bölge A)");
        p1.printerModel = "Creality Ender (Marlin)";
        p1.driver = new MarlinSocketDriver("192.168.1.35", 9100); 
        addAndStartPrinter(p1);

        PrinterState p2 = new PrinterState("WIFI_192.168.1.99", "Ender 3 Pro (Bölge B)");
        p2.printerModel = "Creality Ender (Marlin)";
        p2.driver = new MarlinSocketDriver("192.168.1.99", 9100); 
        addAndStartPrinter(p2);
    }
    public static void addAndStartPrinter(PrinterState printer) {
        activePrinters.add(printer);

        farmThreadPool.submit(() -> {
            System.out.println("[Thread Pool] " + printer.displayName + " için veri kanalı açıldı. (İşlemci Çekirdek ID: " + Thread.currentThread().getId() + ")");
            
            while (true) {
                if (printer.driver != null) {
                    try {
                        printer.driver.updateState(printer);
                    } catch (Exception e) {
                        System.out.println("[Hata] " + printer.displayName + " güncellenirken kritik hata: " + e.getMessage());
                    }
                }
                
                try {
                    Thread.sleep(2000); 
                } catch (InterruptedException e) {
                    System.out.println("[Sistem] " + printer.displayName + " izlemesi durduruldu.");
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }
}