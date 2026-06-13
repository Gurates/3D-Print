package printer;

import printer.drivers.PrinterDriver;

public class PrinterState {
    public final String portName;
    public String displayName;

    public String printerModel = "Unknown Model"; 
    public PrinterDriver driver; 

    public volatile String nozzleCurrent = "--";
    public volatile String nozzleTarget  = "--";
    public volatile String bedCurrent    = "--";
    public volatile String bedTarget     = "--";

    public volatile int    progressPercent = 0;
    public volatile String elapsedTime     = "00:00:00";
    public volatile String rawData         = "Waiting...";
    public volatile boolean connected      = false;
    public volatile String  currentPrintName = "";
    public volatile boolean isPrinting       = false;
    public volatile double  usedFilamentMm    = 0.0;
    public volatile double  estimatedCostTL   = 0.0;
    public volatile double  estimatedPriceTL  = 0.0;
    
    public volatile CostCalculator.CostBreakdown lastCostBreakdown = null;

    public PrinterState(String portName, String displayName) {
        this.portName    = portName;
        this.displayName = displayName;
    }
}