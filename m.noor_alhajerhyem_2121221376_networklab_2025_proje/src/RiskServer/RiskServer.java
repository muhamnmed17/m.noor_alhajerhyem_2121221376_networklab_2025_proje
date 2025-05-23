package RiskServer; 

import network.PairingThread;
import network.Message;
import network.ClientHandler;
import java.io.*;                    
import java.net.*;                   
import java.util.*;                   
import java.util.concurrent.*;        

/**
 * Çok oyunculu Risk sunucusu – eşli eşleşmeli yapı
 */
public class RiskServer {

    private static final int PORT = 9090; // Sunucunun dinleyeceği port
    private int nextRoomId = 1;           // Oda ID sayacı
    private int nextPlayerId = 0;
    private ServerSocket serverSocket;                // Sunucu soketi
    private final List<ClientHandler> allClients = new ArrayList<>();    // Bağlı tüm istemciler
    private final List<ClientHandler> waitingClients = new ArrayList<>(); // Eşleşmeyi bekleyen oyuncular
    private final ExecutorService threadPool = Executors.newCachedThreadPool(); // Thread havuzu
    private boolean running = true;       // Sunucunun çalışıp çalışmadığını belirler
    private PairingThread pairingThread;  // Eşleştirme işlemlerini yürüten thread

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT); // Belirtilen portta sunucu başlatılır
            System.out.println("Risk sunucusu başlatıldı. Port: " + PORT);
            
            pairingThread = new PairingThread(this); // Eşleştirme thread’i oluştur
            pairingThread.start();                   // Eşleştirme başlat

            while (running) { // Sunucu çalıştığı sürece bağlantı bekle
                try {
                    Socket clientSocket = serverSocket.accept(); // Yeni istemci bağlantısı bekleniyor
                    handleNewConnection(clientSocket);           // Yeni bağlantıyı işle
                } catch (SocketException e) {
                    if (!running) break; // Sunucu kapanmışsa döngü sonlanır
                    System.err.println("Bağlantı hatası: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Sunucu hatası: " + e.getMessage());
        } finally {
            shutdownServer(); // Her durumda sunucuyu düzgün şekilde kapat
        }
    }

    /**
     * Yeni bir bağlantıyı yönetir
     */
private void handleNewConnection(Socket clientSocket) {
    try {
        System.out.println("Yeni istemci bağlandı: " + clientSocket.getInetAddress());
        int assignedId = nextPlayerId++; // ← DEĞİŞTİRİLDİ: benzersiz ID ataması
        System.out.println(" Assigned Player ID: " + assignedId); // ← DEBUG LOG
        
        ClientHandler handler = new ClientHandler(clientSocket, assignedId, this); // ← DEĞİŞTİRİLDİ

        synchronized (allClients) {
            allClients.add(handler); // Tüm istemci listesine ekle
        }

        synchronized (waitingClients) {
            waitingClients.add(handler); // Eşleşme bekleyen listesine ekle
        }

        threadPool.execute(handler); // Handler'ı thread havuzuna ver

    } catch (Exception e) {
        System.err.println("Bağlantı işlenirken hata: " + e.getMessage());
        try {
            clientSocket.close(); // Hata durumunda bağlantıyı kapat
        } catch (IOException closeError) {
            System.err.println("Socket kapatılamadı: " + closeError.getMessage());
        }
    }
}

    /**
     * Oyuncuyu bekleme listesine ekler
     */
public synchronized void addToWaiting(ClientHandler client) {
    synchronized (waitingClients) {
        if (!waitingClients.contains(client)) {
            // ✅ RiskMatch referansını temizle
            client.setRiskMatch(null);
            
            waitingClients.add(client);
            System.out.println("Oyuncu bekleme listesine eklendi: " + client.getPlayerId() + 
                             " (" + client.getPlayerName() + ")");
        }
    }
}

    /**
     * İstemciyi tüm listelerden kaldırır
     */
    public synchronized void removeClient(ClientHandler client) {
        synchronized (waitingClients) {
            waitingClients.remove(client); // Bekleme listesinden çıkar
        }
        synchronized (allClients) {
            allClients.remove(client);    // Tüm istemciler listesinden çıkar
        }
        System.out.println("İstemci temizlendi: Oyuncu " + client.getPlayerId());
    }

    /**
     * Bekleyen istemciler listesine referans döndürür
     */
    public List<ClientHandler> getWaitingClients() {
        return waitingClients; // Eşleşmeyi bekleyen istemciler
    }

    /**
     * Sonraki oda ID'sini alır ve sayacı arttırır
     */
    public synchronized int getNextRoomId() {
        return nextRoomId++; // Yeni oda numarası döndür ve sayacı arttır
    }

    /**
     * Sunucuyu kapatır
     */
    public void shutdownServer() {
        running = false; // Ana döngüyü durdur

        if (pairingThread != null) {
            pairingThread.stopPairing(); // Eşleştirme thread’ini durdur
        }

        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close(); // Sunucu soketini kapat
            } catch (IOException e) {
                System.err.println("ServerSocket kapatılamadı: " + e.getMessage());
            }
        }

        synchronized (allClients) {
            for (ClientHandler client : allClients) {
                try {
                    client.sendMessage(new Message("DISCONNECT", Map.of("msg", "Sunucu kapatılıyor."))); // Kapatma bildirimi
                    client.close(); // Bağlantıyı kapat
                } catch (IOException e) {
                    System.err.println("İstemci kapatılamadı: " + e.getMessage());
                }
            }
            allClients.clear(); // Listeyi temizle
        }

        threadPool.shutdown(); // Thread havuzunu durdur
        try {
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow(); // Zorla durdur
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow(); // Hata durumunda yine zorla durdur
        }

        System.out.println("Risk sunucusu kapatıldı.");
    }

    public static void main(String[] args) {
        RiskServer server = new RiskServer(); // Sunucu nesnesi oluştur

        // Program kapatılırken çalışacak hook ekle (örn. Ctrl+C)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Kapatma sinyali alındı. Sunucu kapatılıyor...");
            server.shutdownServer();
        }));

        server.start(); // Sunucuyu başlat
    }
}
