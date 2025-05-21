package final_project;

import java.io.*;
import java.net.*;
import java.util.Arrays;

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
    private String playerName = "Oyuncu";
    private RiskMatch riskMatch;  // 🔹 Yeni: Eşleşme referansı

    public ClientHandler(Socket socket, int playerId, RiskServer server) {
        this.clientSocket = socket;
        this.playerId = playerId;
        this.server = server;

        try {
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
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
            if (riskMatch != null) {
                riskMatch.handleDisconnect(playerId);
            }

        } finally {
            try {
                server.removeClient(this);
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
    try {
        if (data == null || data.isBlank()) {
            sendMessage("ERROR Geçersiz komut formatı");
            break;
        }

        String[] subParts = data.trim().split(" ");
        if (subParts.length < 2) {
            sendMessage("ERROR Geçersiz komut formatı (eksik bilgi)");
            break;
        }

        int lastIndex = subParts.length - 1;
        int troops = Integer.parseInt(subParts[lastIndex]);
        String territory = String.join(" ", Arrays.copyOfRange(subParts, 0, lastIndex));
        riskMatch.handlePlaceTroops(playerId, territory, troops);
    } catch (Exception e) {
        sendMessage("ERROR Yerleştirme ayrıştırılamadı");
    }
    break;


            case "ATTACK":
    try {
                String[] subParts = data.split(" ");
                int dice = Integer.parseInt(subParts[subParts.length - 1]);

                String territoryPart = String.join(" ", Arrays.copyOf(subParts, subParts.length - 1));
                int sepIndex = territoryPart.lastIndexOf(' ');

                if (sepIndex == -1) {
                    sendMessage("ERROR Geçersiz komut formatı (from to dice)");
                    break;
                }

                String from = territoryPart.substring(0, sepIndex);
                String to = territoryPart.substring(sepIndex + 1);

                riskMatch.handleAttack(playerId, from, to, dice);
            } catch (Exception e) {
                sendMessage("ERROR Saldırı ayrıştırılamadı");
            }
            break;

            case "FORTIFY":
    try {
                String[] subParts = data.split(" ");
                int troops = Integer.parseInt(subParts[subParts.length - 1]);

                String territoryPart = String.join(" ", Arrays.copyOf(subParts, subParts.length - 1));
                int sepIndex = territoryPart.lastIndexOf(' ');

                if (sepIndex == -1) {
                    sendMessage("ERROR Geçersiz komut formatı (from to troops)");
                    break;
                }

                String from = territoryPart.substring(0, sepIndex);
                String to = territoryPart.substring(sepIndex + 1);

                riskMatch.handleFortify(playerId, from, to, troops);
            } catch (Exception e) {
                sendMessage("ERROR Güçlendirme ayrıştırılamadı");
            }
            break;

            case "RESTART_DECLINE":
                riskMatch.handleRestartDecline(playerId);  // 🔹 yeni metot
                break;

            case "END_TURN":
                riskMatch.handleEndTurn(playerId);
                break;

            case "RESTART":
                riskMatch.handleRestartRequest(playerId);
                break;

            case "SET_NAME":
                playerName = data.trim();
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
    try {
        if (out != null && clientSocket != null && !clientSocket.isClosed()) {
            out.println(message);
        }
    } catch (Exception e) {
        System.err.println("Mesaj gönderilirken hata: " + e.getMessage());
    }
}
    public String getPlayerName() {
        return playerName;
    }

    /**
     * Bağlantıyı kapatır
     */
    public void close() throws IOException {
        running = false;
        if (in != null) {
            in.close();
        }
        if (out != null) {
            out.close();
        }
        if (clientSocket != null && !clientSocket.isClosed()) {
            clientSocket.close();
        }
    }

    public int getPlayerId() {
        return playerId;
    }

    // 🔹 Yeni: RiskMatch ataması ve erişimi
    public void setRiskMatch(RiskMatch match) {
        this.riskMatch = match;
    }

    public RiskMatch getRiskMatch() {
        return riskMatch;
    }
}
