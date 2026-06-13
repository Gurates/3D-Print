package printer.drivers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import printer.PrinterState; 
import printer.GCodeParser;  

public class MarlinSocketDriver implements PrinterDriver {
    private String ip;
    private int port;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private GCodeParser parser;

    public MarlinSocketDriver(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    @Override
    public void connect() {
        try {
            if (socket == null || socket.isClosed()) {
                socket = new Socket();
                socket.connect(new java.net.InetSocketAddress(ip, port), 2000);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                System.out.println("[Driver] " + ip + " adresine başarıyla soket açıldı.");
            }
        } catch (Exception e) {
            socket = null;
        }
    }

    @Override
    public void updateState(PrinterState state) {
        parser = new GCodeParser(state);
        if (socket == null || socket.isClosed() || !socket.isConnected()) {
            setPrinterOffline(state, "Bağlantı aranıyor...");
            connect();
            return; 
        }

        try {
            socket.setSoTimeout(1500);
            
            out.println("M105");
            String line = in.readLine();
            
            if (line == null) {
                throw new java.io.IOException("Cihaz hattan düştü (Stream kapatıldı).");
            }
            
            state.connected = true;
            state.rawData = line;
            parser.parseLine(line);

            out.println("M27");
            line = in.readLine();
            if (line == null) throw new java.io.IOException("M27 yanıtı alınamadı.");
            parser.parseLine(line);
            if (in.ready()) in.readLine(); 

            out.println("M31");
            line = in.readLine();
            if (line == null) throw new java.io.IOException("M31 yanıtı alınamadı.");
            parser.parseLine(line);
            if (in.ready()) in.readLine();

        } catch (Exception e) {
            System.out.println("[Driver] Bağlantı kaybı algılandı: " + e.getMessage());
            setPrinterOffline(state, "Bağlantı koptu!");
            disconnect();
        }
    }

    private void setPrinterOffline(PrinterState state, String statusMessage) {
        state.connected = false;
        state.rawData = statusMessage;
        state.nozzleCurrent = "--";
        state.nozzleTarget  = "--";
        state.bedCurrent    = "--";
        state.bedTarget     = "--";
        state.progressPercent = 0;
        state.elapsedTime     = "00:00:00";
    }

    @Override
    public void disconnect() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (Exception e) {}
        socket = null;
    }

    @Override
    public void sendCommand(String cmd) {
        if (out != null) out.println(cmd);
    }
}