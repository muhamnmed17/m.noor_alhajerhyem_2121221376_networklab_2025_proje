package final_project; 

import java.util.*; 
import java.util.concurrent.atomic.AtomicBoolean; 
/**
 * Bekleyen oyuncuları eşleştiren ve oyun başlatan thread
 */
public class PairingThread extends Thread {

    private final RiskServer server; // Sunucu referansı
    private final AtomicBoolean running = new AtomicBoolean(true); // Thread çalışıyor mu kontrolü için
    private final int checkIntervalMs = 1000; // Her 1 saniyede bir eşleştirme kontrolü

    // Constructor – Sunucudan referans alınır
    public PairingThread(RiskServer server) {
        super("PairingThread"); // Thread adı verilir
        this.server = server;
        setDaemon(true); // Uygulama kapanınca thread de kapansın
    }

    @Override
    public void run() {
        System.out.println("Eşleştirme işlemi başlatıldı..."); // Bilgilendirme çıktısı

        // Thread aktif olduğu sürece eşleştirme yap
        while (running.get()) {
            try {
                checkAndPairPlayers(); // Oyuncuları eşleştir
                Thread.sleep(checkIntervalMs); // Bekle
            } catch (InterruptedException e) {
                // Thread kesintiye uğradıysa çık
                System.out.println("PairingThread kesintiye uğradı: " + e.getMessage());
                break;
            } catch (Exception e) {
                // Diğer hatalar loglanır
                System.err.println("Eşleştirme hatası: " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("Eşleştirme işlemi durduruldu."); // Thread durunca yaz
    }

    /**
     * Bekleyen oyuncuları kontrol eder ve eşleşme yapar
     */
    private void checkAndPairPlayers() {
        List<ClientHandler> waitingClients = server.getWaitingClients(); // Bekleyen oyuncuları al

        synchronized (waitingClients) { // Listeye eşzamanlı erişim
            if (waitingClients.size() >= 2) { // En az 2 oyuncu varsa
                ClientHandler player1 = waitingClients.remove(0); // İlk oyuncuyu al
                ClientHandler player2 = waitingClients.remove(0); // İkinci oyuncuyu al

                // Oyuncular aktif mi kontrol et
                if (isPlayerActive(player1) && isPlayerActive(player2)) {
                    createMatch(player1, player2); // Eşleştir
                } else {
                    // Aktif olmayan oyuncuları çıkar
                    if (!isPlayerActive(player1)) {
                        server.removeClient(player1);
                    } else {
                        waitingClients.add(player1); // Aktifse geri ekle
                    }

                    if (!isPlayerActive(player2)) {
                        server.removeClient(player2);
                    } else {
                        waitingClients.add(player2); // Aktifse geri ekle
                    }
                }
            }
        }
    }

    /**
     * Oyuncunun aktif olup olmadığını kontrol eder
     */
    private boolean isPlayerActive(ClientHandler player) {
        try {
            // Eğer oyuncu eşleşmemişse ve bağlantısı varsa aktiftir
            return player.getRiskMatch() == null;
        } catch (Exception e) {
            System.err.println("Oyuncu durumu kontrol edilirken hata: " + e.getMessage());
            return false;
        }
    }

    /**
     * İki oyuncuyu eşleştirir ve oyun başlatır
     */
    private void createMatch(ClientHandler player1, ClientHandler player2) {
        try {
            int roomId = server.getNextRoomId(); // Yeni oda ID'si al

            sendPairingInfo(player1, player2, roomId); // Oyunculara eşleştirme bilgisi gönder

            RiskMatch match = new RiskMatch(roomId, player1, player2, server); // Yeni eşleşme oluştur
            match.start(); // Oyunu başlat

            System.out.println("Yeni eşleşme oluşturuldu: Oda " + roomId);
        } catch (Exception e) {
            // Hata varsa logla ve oyuncuları beklemeye al
            System.err.println("Eşleştirme oluşturulamadı: " + e.getMessage());
            e.printStackTrace();

            server.addToWaiting(player1);
            server.addToWaiting(player2);
        }
    }

    /**
     * Eşleşme bilgilerini oyunculara gönderir
     */
    private void sendPairingInfo(ClientHandler player1, ClientHandler player2, int roomId) {
        // İlk oyuncuya ikinci oyuncunun bilgileri gönderilir
        Message pairingMsg1 = new Message("PAIRING_INFO", Map.of(
            "roomId", String.valueOf(roomId),
            "opponentId", String.valueOf(player2.getPlayerId()),
            "opponentName", player2.getPlayerName()
        ));

        // İkinci oyuncuya ilk oyuncunun bilgileri gönderilir
        Message pairingMsg2 = new Message("PAIRING_INFO", Map.of(
            "roomId", String.valueOf(roomId),
            "opponentId", String.valueOf(player1.getPlayerId()),
            "opponentName", player1.getPlayerName()
        ));

        // Mesajları gönder
        player1.sendMessage(pairingMsg1);
        player2.sendMessage(pairingMsg2);
    }

    /**
     * Thread'i durdurur
     */
    public void stopPairing() {
        running.set(false); // Döngü dursun
        this.interrupt(); // Bekleyen thread'i kes
    }
}
