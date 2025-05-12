package final_project;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Risk oyunu için merkezi sunucu uygulaması
 */
public class RiskServer {
    private static final int PORT = 9090;
    private static final int MAX_PLAYERS = 2;
    private final Map<String, Set<String>> adjacencyMap = new HashMap<>();

    private ServerSocket serverSocket;
    private List<ClientHandler> clients = new ArrayList<>();
    private final ExecutorService threadPool = Executors.newFixedThreadPool(MAX_PLAYERS);
    private RiskGame game;
    private boolean gameInProgress = false;
    
    public RiskServer() {
        game = new RiskGame();
    }
    
    /**
     * Sunucuyu başlatır ve bağlantıları dinler
     */
    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Risk sunucusu başlatıldı. Port: " + PORT);
            System.out.println("Oyuncular bekleniyor...");
            
            while (clients.size() < MAX_PLAYERS) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Yeni bağlantı: " + clientSocket.getInetAddress());
                
                ClientHandler handler = new ClientHandler(clientSocket, clients.size(), this);
                clients.add(handler);
                threadPool.execute(handler);
                
                System.out.println("Oyuncu " + clients.size() + " bağlandı. " + 
                    (MAX_PLAYERS - clients.size()) + " oyuncu daha bekleniyor...");
                
                // Oyuncu kimlik bilgisini gönder
                handler.sendMessage("INIT " + handler.getPlayerId());
            }
            
            System.out.println("Tüm oyuncular bağlandı. Oyun başlıyor...");
            startGame();
            
        } catch (IOException e) {
            System.err.println("Sunucu hatası: " + e.getMessage());
        }
    }
    
    /**
     * Oyunu başlatır
     */
 private void startGame() {
    gameInProgress = true;
    game.initializeGame();
    broadcastAdjacency();  
    broadcastMap();
    game.nextTurn();
    broadcastTurn();
}

    
public void broadcastAdjacency() {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, List<String>> entry : game.getAdjacencyMap().entrySet()) {
        sb.append(entry.getKey()).append(":");
        sb.append(String.join(",", entry.getValue()));
        sb.append(";");
    }
    broadcast("ADJACENCY " + sb.toString());
}


    
    /**
     * Tüm istemcilere harita bilgisini gönderir
     */
    public void broadcastMap() {
        String mapState = game.getMapState();
        broadcast("MAP " + mapState);
    }
    
    /**
     * Tüm istemcilere sıra bilgisini gönderir
     */
    public void broadcastTurn() {
        int currentPlayer = game.getCurrentPlayer();
        int troopsToPlace = game.getTroopsToPlace(currentPlayer);
        broadcast("TURN " + currentPlayer + ":" + troopsToPlace);
    }
    
    /**
     * Tüm bağlı istemcilere mesaj gönderir
     */
    public void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }
    
    /**
     * Belirli bir istemciye mesaj gönderir
     */
    public void sendTo(int playerId, String message) {
        if (playerId >= 0 && playerId < clients.size()) {
            clients.get(playerId).sendMessage(message);
        }
    }
    
    /**
     * Oyuncu bir bölgeye asker yerleştirmek istediğinde
     */
    public synchronized void handlePlaceTroops(int playerId, String territory, int troops) {
        if (game.getCurrentPlayer() != playerId) {
            sendTo(playerId, "ERROR Şu anda sizin sıranız değil");
            return;
        }
        
        if (game.placeTroops(playerId, territory, troops)) {
            int remaining = game.getTroopsToPlace(playerId);
            sendTo(playerId, "PLACE_RESULT " + territory + ":" + game.getTerritoryTroops(territory) + ":" + remaining);
            broadcastMap();
            
            // Tüm birlikleri yerleştirilmiş mi kontrol et
            if (remaining == 0) {
                System.out.println("Oyuncu " + playerId + " tüm askerlerini yerleştirdi");
            }
        } else {
            sendTo(playerId, "ERROR Asker yerleştirme başarısız");
        }
    }
    
    /**
     * Oyuncu bir saldırı yapmak istediğinde
     */
    public synchronized void handleAttack(int playerId, String from, String to, int dice) {
        if (game.getCurrentPlayer() != playerId) {
            sendTo(playerId, "ERROR Şu anda sizin sıranız değil");
            return;
        }
        
StringBuilder error = new StringBuilder();
int[] result = game.attack(playerId, from, to, dice, error);
        if (result != null) {
            // result[0] = saldıran kayıp, result[1] = savunan kayıp
            broadcast("ATTACK_RESULT " + from + ":" + to + ":" + result[0] + ":" + result[1]);
            broadcastMap();
            
            // Oyun bitti mi kontrol et
            checkGameOver();
        } else {
            sendTo(playerId, "ERROR Saldırı başarısız");
        }
    }
    
    /**
     * Oyuncu güçlendirme yapmak istediğinde
     */
    public synchronized void handleFortify(int playerId, String from, String to, int troops) {
        if (game.getCurrentPlayer() != playerId) {
            sendTo(playerId, "ERROR Şu anda sizin sıranız değil");
            return;
        }
        
        if (game.fortify(playerId, from, to, troops)) {
            broadcast("FORTIFY_RESULT " + from + ":" + to + ":" + troops);
            broadcastMap();
        } else {
            sendTo(playerId, "ERROR Güçlendirme başarısız");
        }
    }
    
    /**
     * Oyuncu turunu bitirmek istediğinde
     */
    public synchronized void handleEndTurn(int playerId) {
        if (game.getCurrentPlayer() != playerId) {
            sendTo(playerId, "ERROR Şu anda sizin sıranız değil");
            return;
        }
        
        game.nextTurn();
        broadcastTurn();
    }
    
    /**
     * Oyunun bitip bitmediğini kontrol eder
     */
    private void checkGameOver() {
        int winner = game.checkWinner();
        if (winner != -1) {
            broadcast("GAME_OVER " + winner);
            System.out.println("Oyun bitti! Kazanan: Oyuncu " + winner);
            gameInProgress = false;
        }
    }
    
    /**
     * Sunucuyu kapatır
     */
    public void shutdown() {
        gameInProgress = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            
            for (ClientHandler client : clients) {
                client.close();
            }
            
            threadPool.shutdown();
            System.out.println("Sunucu kapatıldı.");
        } catch (IOException e) {
            System.err.println("Kapatma hatası: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        RiskServer server = new RiskServer();
        server.start();
    }
}