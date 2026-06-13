package printer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class WifiPrinter {

    public static void main(String[] args) {
        System.out.println("=== WI-FI 3D PRINTER VERI ÇEKME VE ANALİZ TESTİ ===");

        String esp32Ip = "192.168.1.35"; 
        int port = 9100;

        System.out.println("[Test] Cihaza bağlanılıyor: " + esp32Ip + ":" + port);

        PrinterState testState = new PrinterState("WIFI_192.168.1.35", "Ender 3 Neo (Sahte Wi-Fi)");
        testState.connected = false;
        GCodeParser parser = new GCodeParser(testState);

        try (Socket socket = new Socket(esp32Ip, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            testState.connected = true;
            System.out.println("[Test] BAĞLANTI AKTİF. Döngüsel veri çekimi başlıyor...\n");

            for (int i = 1; i <= 3; i++) {
                System.out.println("=== DÖNGÜ #" + i + " ===");
                
                out.println("M105");
                String line = in.readLine();
                if (line != null) {
                    parser.parseLine(line);
                }

                out.println("M27");
                line = in.readLine();
                if (line != null) {
                    parser.parseLine(line);
                }
                if (in.ready()) in.readLine();

                out.println("M31");
                line = in.readLine();
                if (line != null) {
                    parser.parseLine(line);
                }
                if (in.ready()) in.readLine();

                System.out.println("[Analiz Sonucu - Nozzle]: " + testState.nozzleCurrent + " / " + testState.nozzleTarget + " °C");
                System.out.println("[Analiz Sonucu - Tabla ]: " + testState.bedCurrent + " / " + testState.bedTarget + " °C");
                System.out.println("[Analiz Sonucu - İlerleme]: %" + testState.progressPercent);
                System.out.println("[Analiz Sonucu - Zaman  ]: " + testState.elapsedTime);
                System.out.println("[Ham Çıktı Akışı        ]: " + testState.rawData);
                System.out.println("-----------------------------------------\n");
                Thread.sleep(4000); 
            }

            System.out.println("[Sonuç] Veri çekme ve kurumsal nesne modelleme testi başarıyla tamamlandı.");

        } catch (Exception e) {
            System.out.println("\n[Kritik Hata] ESP32 Simülatöründen veri çekilemedi!");
            System.out.println("Neden: " + e.getMessage());
        }
    }
}