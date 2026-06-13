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
                socket = new Socket(ip, port);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            }
        } catch (Exception e) {
            socket = null;
        }
    }

    @Override
    public void updateState(PrinterState state) {
        if (parser == null) {
            parser = new GCodeParser(state);
        }

        try {
            if (socket == null || socket.isClosed()) {
                state.connected = false;
                state.rawData = "Bağlantı aranıyor...";
                connect();
                return; 
            }
            
            state.connected = true;

            out.println("M105");
            String line = in.readLine();
            if (line != null) parser.parseLine(line);

            out.println("M27");
            line = in.readLine();
            if (line != null) parser.parseLine(line);
            if (in.ready()) in.readLine();

            out.println("M31");
            line = in.readLine();
            if (line != null) parser.parseLine(line);
            if (in.ready()) in.readLine();

        } catch (Exception e) {
            state.connected = false;
            disconnect();
        }
    }

    @Override
    public void disconnect() {
        try {
            if (socket != null) socket.close();
            if (out != null) out.close();
            if (in != null) in.close();
        } catch (Exception e) {}
        socket = null;
    }

    @Override
    public void sendCommand(String cmd) {
        if (out != null) out.println(cmd);
    }
}