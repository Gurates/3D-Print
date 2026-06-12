package printer;

import com.fazecast.jSerialComm.SerialPort;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class PrinterManager {

    private static final List<PrinterState> printers = Collections.synchronizedList(new ArrayList<>());

    public static List<PrinterState> getPrinters() {
        return printers;
    }

    public static void startMonitoring() {
        SerialPort[] ports = SerialPort.getCommPorts();
        if (ports.length == 0) {
            System.out.println("[PrinterManager] Seri port bulunamadı. Simülasyon modu aktif.");
            PrinterState dummy = new PrinterState("SIM", "Simülasyon Yazıcısı");
            dummy.connected      = true;
            dummy.nozzleCurrent  = "210.5";
            dummy.nozzleTarget   = "215.0";
            dummy.bedCurrent     = "60.2";
            dummy.bedTarget      = "60.0";
            dummy.progressPercent = 45;
            dummy.elapsedTime    = "01:23:00";
            dummy.rawData        = "T:210.5 /215.0 B:60.2 /60.0";
            printers.add(dummy);
            return;
        }

        for (int i = 0; i < ports.length; i++) {
            final SerialPort port  = ports[i];
            final String displayName = "Yazıcı " + (i + 1) + " (" + port.getSystemPortName() + ")";
            final PrinterState state = new PrinterState(port.getSystemPortName(), displayName);
            printers.add(state);

            Thread t = new Thread(() -> monitorPort(port, state));
            t.setDaemon(true);
            t.setName("printer-" + port.getSystemPortName());
            t.start();
        }
    }

    private static void monitorPort(SerialPort port, PrinterState state) {
        port.setBaudRate(115200);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 1000, 0);

        if (!port.openPort()) {
            System.out.println("[" + state.displayName + "] Port açılamadı!");
            state.rawData = "Port açılamadı — başka program kullanıyor olabilir.";
            return;
        }

        state.connected = true;
        System.out.println("[" + state.displayName + "] Bağlandı.");

        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
        GCodeParser parser = new GCodeParser(state);
        Thread readerThread = new Thread(() -> {
            try (InputStream in = port.getInputStream();
                 Scanner scanner = new Scanner(in)) {
                while (port.isOpen()) {
                    if (scanner.hasNextLine()) {
                        parser.parseLine(scanner.nextLine());
                    }
                }
            } catch (Exception e) {
                System.out.println("[" + state.displayName + "] Okuma hatası: " + e.getMessage());
            }
        });
        readerThread.setDaemon(true);
        readerThread.start();
        try (OutputStream out = port.getOutputStream()) {
            while (true) {
                out.write("M105\n".getBytes()); Thread.sleep(500);
                out.write("M27\n".getBytes());  Thread.sleep(500);
                out.write("M31\n".getBytes());  Thread.sleep(1000);
            }
        } catch (Exception e) {
            System.out.println("[" + state.displayName + "] Bağlantı kesildi: " + e.getMessage());
        } finally {
            state.connected = false;
            port.closePort();
        }
    }
}