package printer;

public class PrinterState {
    public static volatile String nozzleCurrent = "--";
    public static volatile String nozzleTarget = "--";
    public static volatile String bedCurrent = "--";
    public static volatile String bedTarget = "--";
    
    public static volatile int progressPercent = 0;
    public static volatile String elapsedTime = "00:00:00";
    public static volatile String rawData = "Cihaz bekleniyor...";
}