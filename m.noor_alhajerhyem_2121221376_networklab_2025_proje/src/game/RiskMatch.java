package game; 

import RiskServer.RiskServer;
import game.RiskGame;
import network.Message;
import network.ClientHandler;
import java.util.*;

/**
 * İki oyuncu arasındaki eşleşmeyi ve oyun mantığını yöneten sınıf
 */ 
public class RiskMatch { // RiskMatch sınıfını tanımla

    private final int roomId; // Oda kimlik numarası
    private final RiskGame game; // Oyun mantığını yöneten RiskGame nesnesi
    private final ClientHandler player1; // Birinci oyuncunun istemci yöneticisi
    private final ClientHandler player2; // İkinci oyuncunun istemci yöneticisi
    private RiskServer server; // Oyun sunucusu referansı
private boolean initialPlacementPhase = true; // ✅ Başlangıç yerleştirme fazı
    private final Map<Integer, ClientHandler> idToPlayer = new HashMap<>(); // Oyuncu ID'den ClientHandler'a eşleme
    private final int[] playerOrder; // Oyuncu sırası dizisi
    private int currentPlayerIndex = 0; // Şu anki oyuncunun dizideki indeksi
    private int currentPlayer; // Şu anki oyuncunun ID'si
    private Set<Integer> playersFinishedPlacement = new HashSet<>(); // Yerleştirmeyi bitiren oyuncular
    private final Set<Integer> restartRequests = new HashSet<>(); // Yeniden başlatma isteği veren oyuncuların ID'leri
    
    private ClientHandler getPlayerById(int id) { // ID'ye göre oyuncu döndürme metodu
        if (playerOrder[0] == id) return player1; // ID ilk oyuncununsa player1'i döndür
        if (playerOrder[1] == id) return player2; // ID ikinci oyuncununsa player2'yi döndür
        return null; // ID hiçbir oyuncunun değilse null döndür
    }

    public RiskMatch(int roomId, ClientHandler p1, ClientHandler p2, RiskServer server) { // Constructor
        this.roomId = roomId; // Oda ID'sini ayarla
        this.player1 = p1; // Birinci oyuncuyu ayarla
        this.player2 = p2; // İkinci oyuncuyu ayarla
        this.server = server; // Sunucu referansını ayarla
        this.game = new RiskGame(); // Yeni bir RiskGame nesnesi oluştur

        p1.setRiskMatch(this); // Birinci oyuncunun match referansını ayarla
        p2.setRiskMatch(this); // İkinci oyuncunun match referansını ayarla

        idToPlayer.put(p1.getPlayerId(), p1); // Birinci oyuncuyu ID eşleme tablosuna ekle
        idToPlayer.put(p2.getPlayerId(), p2); // İkinci oyuncuyu ID eşleme tablosuna ekle

        playerOrder = new int[]{p1.getPlayerId(), p2.getPlayerId()}; // Oyuncu sıralamasını ayarla
        currentPlayer = playerOrder[0]; // Sıradaki oyuncuyu birinci oyuncu olarak ayarla
        
        System.out.println("Yeni RiskMatch oluşturuldu: Oda " + roomId + ", Oyuncular " + 
                p1.getPlayerId() + "(" + p1.getPlayerName() + ") ve " + 
                p2.getPlayerId() + "(" + p2.getPlayerName() + ")"); // Log mesajı yaz
    }

   public void start() { // Oyunu başlatan metot
    // ✅ ÖNCE RiskMatch durumunu temizle
    initialPlacementPhase = true;           // Başlangıç fazını aktif yap
    playersFinishedPlacement.clear();       // Yerleştirme tamamlayan oyuncuları temizle
    restartRequests.clear();                // Yeniden başlatma isteklerini temizle
    currentPlayerIndex = 0;                 // İlk oyuncudan başla
    currentPlayer = playerOrder[0];         // Sıradaki oyuncuyu ilk oyuncu yap
    
    System.out.println("✅ RiskMatch durumu temizlendi.");
    
    // Şimdi oyunu başlat
    game.initializeGame(player1, player2); // RiskGame nesnesini başlat (bu da kendi temizliğini yapacak)
    sendInit(); // Başlangıç bilgilerini gönder
    sendAdjacency(); // Komşuluk haritasını gönder
    sendMap(); // Harita durumunu gönder

    // İlk oyuncuya başlangıç takviyesi ile TUR ataması
    int initialTroops = game.getTroopsToPlace(playerOrder[0]); // İlk oyuncunun başlangıç asker sayısını al
    broadcastMessage(new Message("TURN", Map.of( // Tur bilgisini tüm oyunculara gönder
        "playerId", String.valueOf(playerOrder[0]), // Sıradaki oyuncu ID'si
        "troops", String.valueOf(initialTroops), // Yerleştirilecek asker sayısı
        "name", getCurrentPlayer().getPlayerName() // Sıradaki oyuncu adı
    )));
    
    System.out.println("✅ Yeni oyun başlatıldı: Oda " + roomId);
}

    private void sendInit() { // Başlangıç bilgilerini gönderen metot
        // Oyuncu ID ve isimlerini gönder
        player1.sendMessage(new Message("INIT", Map.of( // Birinci oyuncuya kimlik bilgilerini gönder
            "playerId", String.valueOf(player1.getPlayerId()), // Oyuncu ID'si
            "name", player1.getPlayerName() // Oyuncu adı
        )));
        
        player2.sendMessage(new Message("INIT", Map.of( // İkinci oyuncuya kimlik bilgilerini gönder
            "playerId", String.valueOf(player2.getPlayerId()), // Oyuncu ID'si
            "name", player2.getPlayerName() // Oyuncu adı
        )));
    }

public void handlePlaceTroops(int playerId, String territory, int troops) {
    if (playerId != currentPlayer) {
        return;
    }

    if (game.placeTroops(playerId, territory, troops)) {
        int remaining = game.getTroopsToPlace(playerId);
        
        getPlayer(playerId).sendMessage(new Message("PLACE_RESULT", Map.of(
            "territory", territory,
            "troops", String.valueOf(game.getTerritoryTroops(territory)),
            "remaining", String.valueOf(remaining)
        )));
        
        sendMap();
        
        // ✅ Başlangıç fazı kontrolü
        if (initialPlacementPhase && remaining == 0) {
            playersFinishedPlacement.add(playerId);
            
            // Her iki oyuncu da bitirdiyse
            if (playersFinishedPlacement.size() == 2) {
                finishInitialPlacement();
            } else {
                // Diğer oyuncuya geç (troops hesaplaması YOK)
                broadcastMessage(new Message("INFO", Map.of(
                    "msg", getCurrentPlayer().getPlayerName() + " askerlerini yerleştirdi. Sıradaki oyuncu: " + 
                           getPlayerById(playerOrder[(currentPlayerIndex + 1) % 2]).getPlayerName()
                )));
                nextTurn(); // Bu artık troops hesaplamayacak
            }
        }
    } else {
        getPlayer(playerId).sendMessage(new Message("ERROR", Map.of(
            "msg", "Asker yerleştirme başarısız"
        )));
    }
}
  
  private void finishInitialPlacement() {
    initialPlacementPhase = false; // Başlangıç fazını bitir
    
    broadcastMessage(new Message("INFO", Map.of(
        "msg", "Başlangıç yerleştirme fazı tamamlandı! Normal oyun başlıyor..."
    )));
    
    // Normal oyunu başlat - troops hesaplama ile
    currentPlayerIndex = 0; // İlk oyuncudan başla
    currentPlayer = playerOrder[0];
    
    game.calculateTroopsFor(currentPlayer); // ✅ İlk kez hesapla
    int troops = game.getTroopsToPlace(currentPlayer);
    
    broadcastMessage(new Message("TURN", Map.of(
        "playerId", String.valueOf(currentPlayer),
        "troops", String.valueOf(troops),
        "name", getCurrentPlayer().getPlayerName()
    )));
}

  public void handleAttack(int playerId, String from, String to, int dice) { // Saldırı işlemini yöneten metot
    if (playerId != currentPlayer) { // Eğer sıradaki oyuncu değilse
        return; // İşlem yapma
    }
    
    // ✅ Başlangıç fazında saldırı yasak
    if (initialPlacementPhase) {
        getPlayer(playerId).sendMessage(new Message("ERROR", Map.of(
            "msg", "Başlangıç fazında sadece asker yerleştirme yapabilirsiniz."
        )));
        return;
    }

    StringBuilder error = new StringBuilder(); // Hata mesajını tutacak StringBuilder
    int[] result = game.attack(playerId, from, to, dice, error); // Saldırı sonucunu al

    if (result != null) { // Saldırı başarılıysa
        broadcastMessage(new Message("ATTACK_RESULT", Map.of( // Saldırı sonucunu tüm oyunculara bildir
            "from", from, // Saldıran bölge
            "to", to, // Hedef bölge
            "attackerLoss", String.valueOf(result[0]), // Saldıran kaybı
            "defenderLoss", String.valueOf(result[1]) // Savunan kaybı
        )));
        
        sendMap(); // Harita durumunu güncelle
        checkGameOver(); // Oyunun bitip bitmediğini kontrol et
        
        // ✅ Saldırı sonrası tur bitmez, oyuncu isterse devam edebilir
        
    } else { // Saldırı başarısızsa
        getPlayer(playerId).sendMessage(new Message("ERROR", Map.of( // Hata mesajı gönder
            "msg", error.toString() // Hata mesajı
        )));
    }
}

   public void handleFortify(int playerId, String from, String to, int troops) { // Güçlendirme işlemini yöneten metot
    if (playerId != currentPlayer) { // Eğer sıradaki oyuncu değilse
        return; // İşlem yapma
    }
    
    // ✅ Başlangıç fazında güçlendirme yasak
    if (initialPlacementPhase) {
        getPlayer(playerId).sendMessage(new Message("ERROR", Map.of(
            "msg", "Başlangıç fazında sadece asker yerleştirme yapabilirsiniz."
        )));
        return;
    }

    if (game.fortify(playerId, from, to, troops)) { // Güçlendirme başarılıysa
        broadcastMessage(new Message("FORTIFY_RESULT", Map.of( // Güçlendirme sonucunu tüm oyunculara bildir
            "from", from, // Kaynak bölge
            "to", to, // Hedef bölge
            "troops", String.valueOf(troops) // Taşınan asker sayısı
        )));
        
        sendMap(); // Harita durumunu güncelle
        
        // ✅ Güçlendirme sonrası tur otomatik bitsin (Risk kuralları gereği)
        broadcastMessage(new Message("INFO", Map.of(
            "msg", getCurrentPlayer().getPlayerName() + " güçlendirme yaptı. Sıra geçiyor..."
        )));
        
        nextTurn(); // Sıradaki oyuncuya geç
        
    } else { // Güçlendirme başarısızsa
        getPlayer(playerId).sendMessage(new Message("ERROR", Map.of( // Hata mesajı gönder
            "msg", "Güçlendirme başarısız" // Hata mesajı
        )));
    }
}

    public void handleEndTurn(int playerId) { // Tur bitirme işlemini yöneten metot
        if (playerId != currentPlayer) { // Eğer sıradaki oyuncu değilse
            return; // İşlem yapma
        }
        nextTurn(); // Sıradaki oyuncuya geç
    }

 public void nextTurn() { // Sıradaki oyuncuya geçen metot
    currentPlayerIndex = (currentPlayerIndex + 1) % 2; // Oyuncu indeksini güncelle
    currentPlayer = playerOrder[currentPlayerIndex]; // Sıradaki oyuncu ID'sini ayarla

    // ✅ Sadece normal oyunda troops hesapla, başlangıç fazında değil
    if (!initialPlacementPhase) {
        game.calculateTroopsFor(currentPlayer); // Normal oyunda asker hesapla
    }
    
    int troops = game.getTroopsToPlace(currentPlayer); // Mevcut asker sayısını al

    broadcastMessage(new Message("TURN", Map.of( // Tur bilgisini tüm oyunculara gönder
        "playerId", String.valueOf(currentPlayer), // Sıradaki oyuncu ID
        "troops", String.valueOf(troops), // Yerleştirilecek asker sayısı
        "name", getCurrentPlayer().getPlayerName() // Sıradaki oyuncu adı
    )));
}

    private void checkGameOver() { // Oyunun bitip bitmediğini kontrol eden metot
        int winner = game.checkWinner(); // Kazananı belirle
        if (winner != -1) { // Eğer bir kazanan varsa
            broadcastMessage(new Message("GAME_OVER", Map.of( // Oyun sonu mesajını tüm oyunculara gönder
                "winnerId", String.valueOf(winner) // Kazanan oyuncu ID'si
            )));
        }
    }

    
// RiskMatch.java içinde handleRestartRequest metodunu şu şekilde değiştirin:

public void handleRestartRequest(int playerId) {
    if (game.checkWinner() == -1) {
        getPlayer(playerId).sendMessage(new Message("INFO", Map.of(
            "msg", "Oyun henüz bitmedi, yeniden başlatılamaz."
        )));
        return;
    }

    if (restartRequests.contains(playerId)) {
        return;
    }

    restartRequests.add(playerId);
    getPlayer(playerId).sendMessage(new Message("INFO", Map.of(
        "msg", "Yeniden başlatma isteğiniz alındı."
    )));

    // Eğer sadece 1 oyuncu kaldıysa → direkt başlat
    if (checkActivePlayerCount() == 1) {
        ClientHandler waitingPlayer = getPlayer(playerId);
        
        waitingPlayer.sendMessage(new Message("INFO", Map.of(
            "msg", "Yeni bir rakip bekleniyor..."
        )));
        
        server.addToWaiting(waitingPlayer);
        
        System.out.println("Oyuncu " + playerId + " tek kaldı, bekleme listesine eklendi.");
        return;
    }

    // Normal 2 oyuncu durumu
    if (restartRequests.size() == 2) {
        broadcastMessage(new Message("INFO", Map.of(
            "msg", "Her iki oyuncu da yeniden başlatmayı onayladı. Oyun sıfırlanıyor..."
        )));
        
        restartRequests.clear();
        
        // ✅ RiskMatch durumunu tamamen sıfırla
        initialPlacementPhase = true;
        playersFinishedPlacement.clear();
        currentPlayerIndex = 0;
        currentPlayer = playerOrder[0];
        
        // ✅ OYUNU TAMAMEN YENİDEN BAŞLAT
        game.initializeGame(player1, player2); // Bu artık sahiplikleri de sıfırlayacak
        
        // ✅ İstemcilere yeni durumu gönder
        sendInit();        // Oyuncu kimliklerini yeniden gönder
        sendAdjacency();   // Komşuluk haritasını gönder
        sendMap();         // Yenilenen harita durumunu gönder
        
        // İlk oyuncunun başlangıç askerlerini hesapla
        int initialTroops = game.getTroopsToPlace(playerOrder[0]);
        broadcastMessage(new Message("TURN", Map.of(
            "playerId", String.valueOf(playerOrder[0]),
            "troops", String.valueOf(initialTroops),
            "name", getCurrentPlayer().getPlayerName()
        )));
        
        System.out.println("✅ Oyun tamamen yeniden başlatıldı: Oda " + roomId);
        
    } else {
        getOtherPlayer(playerId).sendMessage(new Message("INFO", Map.of(
            "msg", "Diğer oyuncudan yeniden başlatma isteği geldi."
        )));
    }
}

    private int checkActivePlayerCount() { // Aktif oyuncu sayısını kontrol eden metot
        Set<Integer> active = new HashSet<>(); // Aktif oyuncu ID'lerini tutacak küme
        for (Territory t : game.getTerritories().values()) { // Tüm bölgeleri döngüye al
            active.add(t.getOwner()); // Bölge sahibini aktif oyuncular listesine ekle
        }
        return active.size(); // Aktif oyuncu sayısını döndür
    }

    public void handleRestartDecline(int playerId) { // Yeniden başlatma reddi işlemini yöneten metot
        int otherPlayerId = (playerId == playerOrder[0]) ? playerOrder[1] : playerOrder[0]; // Diğer oyuncunun ID'sini bul
        ClientHandler quitter = getPlayerById(playerId); // Reddeden oyuncuyu al
        ClientHandler waitingPlayer = getPlayerById(otherPlayerId); // Bekleyen oyuncuyu al

        quitter.sendMessage(new Message("EXIT", Map.of( // Reddeden oyuncuya çıkış mesajı gönder
            "msg", "show_popup_and_close" // Çıkış mesajı
        )));
        
        waitingPlayer.sendMessage(new Message("INFO", Map.of( // Bekleyen oyuncuya bilgi mesajı gönder
            "msg", "Diğer oyuncu yeniden başlatmak istemedi. Beklemeye alındınız." // Bilgi mesajı
        )));

        // Oyuncu beklemeye alınsın:
        server.addToWaiting(waitingPlayer); // Bekleyen oyuncuyu bekleme listesine ekle
    }

     public void handleDisconnect(int playerId) { // Bağlantı kopması durumunu yöneten metot
    int otherPlayerId = (playerId == playerOrder[0]) ? playerOrder[1] : playerOrder[0]; // Diğer oyuncunun ID'sini bul
    ClientHandler otherPlayer = getPlayerById(otherPlayerId); // Diğer oyuncuyu al

    if (otherPlayer == null) { // Eğer diğer oyuncu da bağlantısını kesmişse
        return; // İşlemi sonlandır
    }

    int loserId = playerId; // Kaybeden oyuncu ayrılan oyuncu
    int winnerId = otherPlayerId; // Kazanan oyuncu kalan oyuncu

    // Tüm bölgeleri kazanana geçir
    for (Territory t : game.getTerritories().values()) { // Tüm bölgeleri döngüye al
        if (t.getOwner() == loserId) { // Eğer bölge ayrılan oyuncunun ise
            t.setOwner(winnerId); // Bölgenin sahibini kalan oyuncu olarak ayarla
        }
    }

    // ✅ Ayrılan oyuncunun RiskMatch referansını temizle
    ClientHandler disconnectedPlayer = getPlayerById(playerId);
    if (disconnectedPlayer != null) {
        disconnectedPlayer.setRiskMatch(null);
    }

    otherPlayer.sendMessage(new Message("INFO", Map.of( // Kalan oyuncuya bilgi mesajı gönder
        "msg", "Rakip oyundan ayrıldı. Tebrikler kazandınız!" // Bilgi mesajı
    )));
    
    otherPlayer.sendMessage(new Message("RESTART_PROMPT", Map.of( // Yeniden başlatma sorgusu gönder
        "msg", "Rakip oyundan ayrıldı. Yeni bir oyuncuyla eşleşmek ister misiniz?" // Soru mesajı
    )));

    System.out.println("Oyuncu " + playerId + " ayrıldı, diğer oyuncuya RESTART_PROMPT gönderildi.");
}

    private ClientHandler getPlayer(int id) { // ID'ye göre oyuncu getiren yardımcı metot
        return idToPlayer.get(id); // ID'ye karşılık gelen oyuncuyu döndür
    }

    private ClientHandler getOtherPlayer(int id) { // Belirli bir oyuncunun rakibini getiren yardımcı metot
        for (int key : idToPlayer.keySet()) { // Tüm oyuncu ID'lerini döngüye al
            if (key != id) { // Eğer ID verilen ID değilse
                return idToPlayer.get(key); // Bu ID'ye sahip oyuncuyu döndür
            }
        }
        return null; // Eğer rakip bulunamazsa null döndür
    }

    private ClientHandler getCurrentPlayer() { // Sıradaki oyuncuyu getiren yardımcı metot
        return getPlayer(currentPlayer); // Sıradaki oyuncu ID'sine sahip oyuncuyu döndür
    }

    private void broadcastMessage(Message msg) { // Tüm oyunculara mesaj gönderen yardımcı metot
        player1.sendMessage(msg); // Birinci oyuncuya mesajı gönder
        player2.sendMessage(msg); // İkinci oyuncuya mesajı gönder
    }

    private void sendMap() { // Harita durumunu tüm oyunculara gönderen yardımcı metot
        Message mapMsg = new Message("MAP", Map.of( // Harita mesajı oluştur
            "data", game.getMapState() // Harita durumu verisini ekle
        ));
        broadcastMessage(mapMsg); // Harita mesajını tüm oyunculara gönder
    }

    private void sendAdjacency() { // Komşuluk haritasını tüm oyunculara gönderen yardımcı metot
        StringBuilder sb = new StringBuilder(); // String oluşturucu
        for (Map.Entry<String, List<String>> entry : game.getAdjacencyMap().entrySet()) { // Komşuluk haritasını döngüye al
            sb.append(entry.getKey()).append(":"); // Bölge adını ekle
            sb.append(String.join(",", entry.getValue())); // Komşu bölgeleri ekle
            sb.append(";"); // Ayırıcı ekle
        }
        
        Message adjMsg = new Message("ADJACENCY", Map.of( // Komşuluk mesajı oluştur
            "data", sb.toString() // Komşuluk verisi ekle
        ));
        broadcastMessage(adjMsg); // Komşuluk mesajını tüm oyunculara gönder
    }
}