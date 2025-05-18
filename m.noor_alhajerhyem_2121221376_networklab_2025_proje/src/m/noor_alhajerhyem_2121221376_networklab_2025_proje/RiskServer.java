package final_project;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Çok oyunculu Risk sunucusu – eşli eşleşmeli yapı
 */
public class RiskServer {

    private static final int PORT = 9090;
    private int roomCounter = 1;

    private ServerSocket serverSocket;
    private final List<ClientHandler> allClients = new ArrayList<>();
    private final List<ClientHandler> waitingClients = new ArrayList<>();
    private final Set<Integer> restartRequests = new HashSet<>();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    private int nextRoomId = 1;

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Risk sunucusu başlatıldı. Port: " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Yeni istemci bağlandı: " + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket, allClients.size(), this);
                allClients.add(handler);
                waitingClients.add(handler);
                threadPool.execute(handler);

                if (waitingClients.size() >= 2) {
                    ClientHandler p1 = waitingClients.remove(0);
                    ClientHandler p2 = waitingClients.remove(0);

                    System.out.println("Yeni eşleşme oluşturuluyor: Oyuncular "
                            + p1.getPlayerId() + " ve " + p2.getPlayerId());

                    RiskMatch match = new RiskMatch(nextRoomId++, p1, p2, this);
                    match.start();
                }
            }

        } catch (IOException e) {
            System.err.println("Sunucu hatası: " + e.getMessage());
        }
    }

    /**
     * (İsteğe bağlı) Tüm oyuncular onaylarsa restart logic buraya taşınabilir
     */
    public synchronized void handleRestartRequest(int playerId) {
        restartRequests.add(playerId);
        System.out.println("Oyuncu " + playerId + " yeniden başlatmak istiyor.");

        if (restartRequests.size() % 2 == 0) {
            System.out.println("2 oyuncudan restart geldi. İlgili RiskMatch üzerinden yeniden başlatılmalı.");
            // NOT: Bu sistem artık RiskMatch içinde yapılmalı!
            restartRequests.clear();
        }
    }

    public synchronized void addToWaiting(ClientHandler client) {
        waitingClients.add(client);
        System.out.println("Oyuncu yeniden bekleme listesine alındı: " + client);

        if (waitingClients.size() >= 2) {
            ClientHandler p1 = waitingClients.remove(0);
            ClientHandler p2 = waitingClients.remove(0);
            RiskMatch newMatch = new RiskMatch(roomCounter++, p1, p2, this);
            newMatch.start();
        }
    }

    public synchronized void removeClient(ClientHandler client) {
        waitingClients.remove(client);
        allClients.remove(client);
        System.out.println("İstemci temizlendi: " + client);
    }

    public static void main(String[] args) {
        RiskServer server = new RiskServer();
        server.start();
    }
}
