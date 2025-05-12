package final_project;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * İstemci bağlantılarını yöneten ve komutları işleyen sınıf
 */
public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private RiskServer server;
    private int playerId;
    private boolean running = true;
    
    public ClientHandler(Socket socket, int playerId, RiskServer server) {
        this.clientSocket = socket;
        this.playerId = playerId;
        this.server = server;
        
        try {
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            System.err.println("ClientHandler oluşturma hatası: " + e.getMessage());
        }
    }
    
    @Override
    public void run() {
        try {
            String inputLine;
            while (running && (inputLine = in.readLine()) != null) {
                processCommand(inputLine);
            }
        } catch (IOException e) {
            System.err.println("İstemci ile iletişim kesildi: " + e.getMessage());
        } finally {
            try {
                close();
            } catch (IOException e) {
                System.err.println("Kapatma hatası: " + e.getMessage());
            }
        }
    }
    
    /**
     * İstemciden gelen komutları işler
     */
    private void processCommand(String command) {
        System.out.println("Oyuncu " + playerId + " komutu: " + command);
        
        String[] parts = command.split(" ", 2);
        String cmd = parts[0];
        String data = parts.length > 1 ? parts[1] : "";
        
        switch (cmd) {
            case "PLACE_TROOPS":
                // PLACE_TROOPS BölgeAdı AskerSayısı
                try {
                    String[] subParts = data.split(" ");
                    String territory = subParts[0];
                    int troops = Integer.parseInt(subParts[1]);
                    server.handlePlaceTroops(playerId, territory, troops);
                } catch (Exception e) {
                    sendMessage("ERROR Geçersiz komut formatı");
                }
                break;
                
            case "ATTACK":
                // ATTACK KaynakBölge HedefBölge ZarSayısı
                try {
                    String[] subParts = data.split(" ");
                    String from = subParts[0];
                    String to = subParts[1];
                    int dice = Integer.parseInt(subParts[2]);
                    server.handleAttack(playerId, from, to, dice);
                } catch (Exception e) {
                    sendMessage("ERROR Geçersiz komut formatı");
                }
                break;
                
            case "FORTIFY":
                // FORTIFY KaynakBölge HedefBölge AskerSayısı
                try {
                    String[] subParts = data.split(" ");
                    String from = subParts[0];
                    String to = subParts[1];
                    int troops = Integer.parseInt(subParts[2]);
                    server.handleFortify(playerId, from, to, troops);
                } catch (Exception e) {
                    sendMessage("ERROR Geçersiz komut formatı");
                }
                break;
                
            case "END_TURN":
                server.handleEndTurn(playerId);
                break;
                
            default:
                sendMessage("ERROR Bilinmeyen komut: " + cmd);
                break;
        }
    }
    
    /**
     * İstemciye mesaj gönderir
     */
    public void sendMessage(String message) {
        if (out != null && !clientSocket.isClosed()) {
            out.println(message);
        }
    }
    
    /**
     * Bağlantıyı kapatır
     */
    public void close() throws IOException {
        running = false;
        if (in != null) in.close();
        if (out != null) out.close();
        if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
    }
    
    public int getPlayerId() {
        return playerId;
    }
}