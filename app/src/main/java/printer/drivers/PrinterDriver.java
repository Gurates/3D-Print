package printer.drivers;
import printer.PrinterState;

public interface PrinterDriver {
    void connect();
    void disconnect();
    void updateState(PrinterState state);
    void sendCommand(String command);
}
