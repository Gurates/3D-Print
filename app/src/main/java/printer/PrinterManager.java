package printer;

import com.fazecast.jSerialComm.SerialPort;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Scanner;

public class PrinterManager {

    public static void startMonitoring() {
        SerialPort[] ports = SerialPort.getCommPorts();
        if (ports.length == 0) {
            System.out.println("Hata: Seri port bulunamadı!");
            return;
        }

        SerialPort printerPort = ports[0]; 
        printerPort.setBaudRate(115200);
        printerPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 1000, 0);

        if (!printerPort.openPort()) {
            System.out.println("Hata: Port açılamadı!");
            return;
        }

        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        // Yazıcıdan okuma yapacak ayrı bir dinleyici Thread (Buffer şişmesini önler)
        Thread readerThread = new Thread(() -> {
            try (InputStream in = printerPort.getInputStream();
                 Scanner scanner = new Scanner(in)) {
                while (printerPort.isOpen()) {
                    if (scanner.hasNextLine()) {
                        String line = scanner.nextLine();
                        GCodeParser.parseLine(line); // Gelen her satırı ayrıştırıcıya yolla
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        readerThread.setDaemon(true);
        readerThread.start();

        // Komut gönderici ana döngü
        try (OutputStream out = printerPort.getOutputStream()) {
            while (true) {
                // Komutları sırayla gönderiyoruz (Sıcaklık, SD Durumu, Süre)
                out.write("M105\n".getBytes());
                Thread.sleep(500); // Marlin'i boğmamak için aralarda kısa esler veriyoruz
                
                out.write("M27\n".getBytes());
                Thread.sleep(500);
                
                out.write("M31\n".getBytes());
                Thread.sleep(1000); // Toplam döngü süresi 2 saniye
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            printerPort.closePort();
        }
    }
}