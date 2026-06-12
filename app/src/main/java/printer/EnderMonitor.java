package printer;

import com.fazecast.jSerialComm.SerialPort;
import java.io.OutputStream;
import java.io.InputStream;
import java.util.Scanner;

public class EnderMonitor {
    
    // Arayüzün (UI) anlık olarak okuyabilmesi için güncel yazıcı çıktısını tutan değişken
    public static volatile String sonYaziciYaniti = "";

    public static void veriCekmeyeBasla() {
        SerialPort[] ports = SerialPort.getCommPorts();
        
        System.out.println("=== 3D FARM YAZICI TARAMA ===");
        if (ports.length == 0) {
            System.out.println("Hata: Bilgisayara bağlı herhangi bir seri cihaz bulunamadı!");
            return;
        }

        // Bilgisayara bağlı ilk seri cihazı otomatik seçiyoruz
        SerialPort printerPort = ports[0]; 
        printerPort.setBaudRate(115200); 
        printerPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 2000, 0);

        if (!printerPort.openPort()) {
            System.out.println("Hata: Port açılamadı! Dilimleyici (Cura, Prusa vb.) programları kapatın.");
            return;
        }

        // Bağlantı ilk kurulduğunda kartın kendine gelmesi için 2 saniye bekleme
        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        try (OutputStream out = printerPort.getOutputStream();
             InputStream in = printerPort.getInputStream();
             Scanner scanner = new Scanner(in)) {

            System.out.println("[Başarılı] Yazıcı bağlantısı aktif. Veri akışı başladı...");

            while (true) {
                // Sıcaklık komutunu gönderiyoruz
                out.write("M105\n".getBytes());
                out.flush();

                if (scanner.hasNextLine()) {
                    String response = scanner.nextLine();
                    // Arayüzün yakalaması için veriyi köprü değişkene yazıyoruz
                    sonYaziciYaniti = response;
                }
                
                // Yazıcıyı yormamak için 2 saniyede bir sorgula
                Thread.sleep(2000); 
            }

        } catch (Exception e) {
            System.out.println("Haberleşme sırasında bir hata oluştu:");
            e.printStackTrace();
        } finally {
            printerPort.closePort();
            System.out.println("[Bağlantı] Port kapatıldı.");
        }
    }
}