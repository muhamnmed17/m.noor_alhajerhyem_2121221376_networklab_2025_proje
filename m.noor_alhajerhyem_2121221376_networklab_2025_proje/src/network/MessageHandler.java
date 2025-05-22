package network; 

import network.Message;
import client.GameUI;
import client.RiskClient;
import game.GameState;
import game.Territory;
import javax.swing.*; 
import java.awt.*; 
import java.util.*; 
import java.util.List; 

/**
 * Sunucudan gelen mesajları işleyen sınıf
 */ // Sınıfın açıklaması
public class MessageHandler { // Mesaj işleyici sınıfının tanımlanması
    private final RiskClient parent; // Ana istemci referansı (değiştirilemez)
    private final GameState gameState; // Oyun durumu referansı (değiştirilemez)
    private final GameUI gameUI; // Kullanıcı arayüzü referansı (değiştirilemez)
    
    public MessageHandler(RiskClient parent, GameState gameState, GameUI gameUI) { // Yapıcı metod
        this.parent = parent; // Ana istemci referansını atama
        this.gameState = gameState; // Oyun durumu referansını atama
        this.gameUI = gameUI; // Kullanıcı arayüzü referansını atama
    }
    
    public void processMessage(Message message) { // Gelen mesajları işleyen ana metod
        String type = message.type; // Mesaj tipini alma
        gameUI.logToConsole("Alındı: " + message); // Alınan mesajı konsola yazdırma

        switch (type) { // Mesaj tipine göre dallanma
            case "RESTART_PROMPT" -> handleRestartPrompt(); // Yeniden başlatma istemi işlemi
            case "INIT" -> handleInit(message); // Oyuncu başlatma işlemi
            case "MAP" -> handleMap(message); // Harita güncelleme işlemi
            case "TURN" -> handleTurn(message); // Tur değişimi işlemi
            case "PLACE_RESULT" -> handlePlaceResult(message); // Asker yerleştirme sonucu işlemi
            case "ATTACK_RESULT" -> handleAttackResult(message); // Saldırı sonucu işlemi
            case "FORTIFY_RESULT" -> handleFortifyResult(message); // Güçlendirme sonucu işlemi
            case "GAME_OVER" -> handleGameOver(message); // Oyun bitişi işlemi
            case "ADJACENCY" -> handleAdjacency(message); // Komşuluk verileri işlemi
            case "ERROR" -> handleError(message); // Hata mesajı işlemi
            case "INFO" -> handleInfo(message); // Bilgi mesajı işlemi
            case "EXIT" -> handleExit(message); // Çıkış işlemi
            case "DISCONNECT" -> handleDisconnect(message); // Bağlantı kopması işlemi
            default -> gameUI.logToConsole("Bilinmeyen komut: " + type); // Bilinmeyen mesaj tipi için varsayılan işlem
        }
    }
    
    private void handleRestartPrompt() { // Yeniden başlatma istemi işleme metodu
        int result = JOptionPane.showConfirmDialog(parent, // Kullanıcıya onay dialog'u gösterme
                "Rakibiniz oyundan ayrıldı. Yeni bir rakiple oynamak ister misiniz?",
                "Oyun Bitti", JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) { // Kullanıcı evet derse
            Message restartMsg = new Message("RESTART", Collections.emptyMap()); // Yeniden başlatma mesajı oluşturma
            parent.sendMessage(restartMsg); // Mesajı sunucuya gönderme
        } else { // Kullanıcı hayır derse
            Message declineMsg = new Message("RESTART_DECLINE", Collections.emptyMap()); // Red mesajı oluşturma
            parent.sendMessage(declineMsg); // Red mesajını sunucuya gönderme
            JOptionPane.showMessageDialog(parent, "Oyun kapatılıyor. Görüşmek üzere!"); // Veda mesajı gösterme
            parent.dispose(); // Ana pencereyi kapatma
        }
    }
    
    private void handleInit(Message message) { // Oyuncu başlatma işleme metodu
        int playerId = Integer.parseInt(message.get("playerId")); // Mesajdan oyuncu ID'sini alma ve sayıya çevirme
        gameState.setPlayerId(playerId); // Oyun durumuna oyuncu ID'sini kaydetme
        
        String name = message.get("name"); // Mesajdan oyuncu adını alma
        if (name == null || name.isEmpty()) { // Oyuncu adı boş veya null ise
            name = "Oyuncu " + playerId; // Varsayılan isim oluşturma
        }

        gameState.getPlayerNames().put(playerId, name); // Oyuncu adını haritaya kaydetme
        gameUI.logToConsole("Oyuncu kimliğiniz: " + playerId + " (" + name + ")"); // Konsola oyuncu bilgisi yazma

        Color c = gameState.getPlayerColor(playerId); // Oyuncu rengini alma
        String colorName = gameState.getColorName(c); // Renk adını alma
        gameUI.updatePlayerColor("Renginiz: " + colorName, c); // UI'da oyuncu rengini güncelleme
        gameUI.updateStatus("Rakip oyuncu bekleniyor..."); // Durum mesajını güncelleme
    }
    
    private void handleMap(Message message) { // Harita güncelleme işleme metodu
        gameState.updateMap(message.get("data")); // Oyun durumundaki haritayı güncelleme
        gameUI.repaintMap(); // UI'da haritayı yeniden çizme
    }
    
    private void handleTurn(Message message) { // Tur değişimi işleme metodu
        int turn = Integer.parseInt(message.get("playerId")); // Sıradaki oyuncu ID'sini alma
        int troops = Integer.parseInt(message.get("troops")); // Yerleştirilecek asker sayısını alma
        String name = message.get("name"); // Sıradaki oyuncunun adını alma
        if (name == null || name.isEmpty()) { // Oyuncu adı boş veya null ise
            name = "Oyuncu " + turn; // Varsayılan isim oluşturma
        }

        gameState.setCurrentTurn(turn); // Mevcut sırayı güncelleme
        gameState.clearSelections(); // Önceki seçimleri temizleme
        gameUI.repaintMap(); // Haritayı yeniden çizme
        gameState.getPlayerTroopsToPlace().put(turn, troops); // Oyuncunun yerleştirecegi asker sayısını kaydetme

        if (turn == gameState.getPlayerId()) { // Sıra bu oyuncudaysa
            gameUI.logToConsole("Sıra sizde! " + troops + " asker yerleştirin."); // Konsola bilgi yazma
            gameUI.updateStatus("Sıra sizde!"); // Durum mesajını güncelleme
            gameUI.enableButtons(true); // Butonları etkinleştirme
            gameUI.startTurnTimer(); // Tur zamanlayıcısını başlatma
        } else { // Sıra başka oyuncudaysa
            gameUI.logToConsole("Rakibin sırası: " + name); // Konsola rakip bilgisi yazma
            gameUI.updateStatus("Sıra: " + name); // Durum mesajını güncelleme
            gameUI.enableButtons(false); // Butonları devre dışı bırakma
        }

        gameUI.updateTroopsLeft("Kalan Asker: " + troops); // Kalan asker sayısını güncelleme
        gameState.clearSelections(); // Seçimleri tekrar temizleme
        gameUI.repaintMap(); // Haritayı tekrar yeniden çizme
    }
    
    private void handlePlaceResult(Message message) { // Asker yerleştirme sonucu işleme metodu
        String territory = message.get("territory"); // Mesajdan bölge adını alma
        int troops = Integer.parseInt(message.get("troops")); // Mesajdan toplam asker sayısını alma
        int remaining = Integer.parseInt(message.get("remaining")); // Mesajdan kalan asker sayısını alma

        Territory t = gameState.getTerritories().get(territory); // İlgili bölgeyi alma
        if (t != null) { // Bölge varsa
            t.setTroops(troops); // Bölgenin asker sayısını güncelleme
        }

        gameState.getPlayerTroopsToPlace().put(gameState.getPlayerId(), remaining); // Oyuncunun kalan asker sayısını güncelleme
        gameUI.updateTroopsLeft("Kalan Asker: " + remaining); // UI'da kalan asker sayısını güncelleme
        gameUI.repaintMap(); // Haritayı yeniden çizme

        if (remaining == 0 && gameState.getCurrentTurn() == gameState.getPlayerId()) { // Kalan asker yoksa ve sıra bu oyuncudaysa
            gameUI.enableButtons(true); // Butonları etkinleştirme
        }
    }
    
    private void handleAttackResult(Message message) { // Saldırı sonucu işleme metodu
        String from = message.get("from"); // Saldıran bölgeyi alma
        String to = message.get("to"); // Hedef bölgeyi alma
        int attackerLoss = Integer.parseInt(message.get("attackerLoss")); // Saldıranın kaybını alma
        int defenderLoss = Integer.parseInt(message.get("defenderLoss")); // Savunanın kaybını alma

        Territory attacker = gameState.getTerritories().get(from); // Saldıran bölge nesnesini alma
        Territory defender = gameState.getTerritories().get(to); // Savunan bölge nesnesini alma

        if (attacker != null) { // Saldıran bölge varsa
            attacker.removeTroops(attackerLoss); // Saldıranın askerini azaltma
        }
        if (defender != null) { // Savunan bölge varsa
            defender.removeTroops(defenderLoss); // Savunanın askerini azaltma
        }

        if (defender.getTroops() <= 0 && attacker != null) { // Savunanın askeri bitmiş ve saldıran varsa
            defender.setOwner(attacker.getOwner()); // Bölgenin sahibini saldıran yapma
            defender.setTroops(1); // Savunan bölgeye 1 asker koyma
            attacker.removeTroops(1); // Saldırandan 1 asker çıkarma
            gameUI.logToConsole(to + " ele geçirildi!"); // Konsola fetih mesajı yazma
        }

        gameState.clearSelections(); // Seçimleri temizleme
        gameUI.repaintMap(); // Haritayı yeniden çizme
    }
    
    private void handleFortifyResult(Message message) { // Güçlendirme sonucu işleme metodu
        String from = message.get("from"); // Kaynak bölgeyi alma
        String to = message.get("to"); // Hedef bölgeyi alma
        int moved = Integer.parseInt(message.get("troops")); // Taşınan asker sayısını alma

        Territory src = gameState.getTerritories().get(from); // Kaynak bölge nesnesini alma
        Territory dst = gameState.getTerritories().get(to); // Hedef bölge nesnesini alma
        if (src != null) { // Kaynak bölge varsa
            src.removeTroops(moved); // Kaynak bölgeden asker çıkarma
        }
        if (dst != null) { // Hedef bölge varsa
            dst.addTroops(moved); // Hedef bölgeye asker ekleme
        }

        gameState.clearSelections(); // Seçimleri temizleme
        gameUI.repaintMap(); // Haritayı yeniden çizme
    }
    
    private void handleGameOver(Message message) { // Oyun bitişi işleme metodu
        gameState.setGameOver(true); // Oyun durumunu bitiş olarak işaretleme
        gameState.clearSelections(); // Seçimleri temizleme
        gameUI.repaintMap(); // Haritayı yeniden çizme

        try { // Hata yakalama bloğu başlangıcı
            int winnerId = Integer.parseInt(message.get("winnerId")); // Kazanan oyuncu ID'sini alma

            String winMessage = (winnerId == gameState.getPlayerId()) // Kazanan bu oyuncu mu kontrolü
                    ? "Tebrikler, kazandınız!" // Kazandıysa tebrik mesajı
                    : "Oyunu kaybettiniz. Kazanan: " + gameState.getPlayerNames().get(winnerId); // Kaybettiyse mağlubiyet mesajı

            int choice = JOptionPane.showConfirmDialog(parent, // Kullanıcıya yeniden başlatma seçeneği sunma
                    winMessage + "\nYeniden başlatılsın mı?",
                    "Oyun Bitti", JOptionPane.YES_NO_OPTION);

            if (choice == JOptionPane.YES_OPTION) { // Kullanıcı evet derse
                Message restartMsg = new Message("RESTART", Collections.emptyMap()); // Yeniden başlatma mesajı oluşturma
                parent.sendMessage(restartMsg); // Mesajı sunucuya gönderme
                gameUI.logToConsole("Yeniden başlatma istendi."); // Konsola bilgi yazma
            } else { // Kullanıcı hayır derse
                Message declineMsg = new Message("RESTART_DECLINE", Collections.emptyMap()); // Red mesajı oluşturma
                parent.sendMessage(declineMsg); // Red mesajını sunucuya gönderme
                gameUI.logToConsole("Yeniden başlatma isteğini reddettiniz."); // Konsola red bilgisi yazma
            }

        } catch (NumberFormatException e) { // Sayı dönüştürme hatası yakalama
            JOptionPane.showMessageDialog(parent, message.get("msg"), "Oyun Bitti", JOptionPane.INFORMATION_MESSAGE); // Hata durumunda genel mesaj gösterme
            Message declineMsg = new Message("RESTART_DECLINE", Collections.emptyMap()); // Red mesajı oluşturma
            parent.sendMessage(declineMsg); // Red mesajını sunucuya gönderme
        }

        gameUI.updateStatus("Oyun bitti."); // Durum mesajını güncelleme
        gameUI.enableButtons(false); // Butonları devre dışı bırakma
    }
    
    private void handleAdjacency(Message message) { // Komşuluk verileri işleme metodu
        gameState.updateAdjacency(message.get("data")); // Oyun durumundaki komşuluk verilerini güncelleme
        gameUI.logToConsole("Komşuluk verileri güncellendi."); // Konsola güncelleme bilgisi yazma
    }
    
    private void handleError(Message message) { // Hata mesajı işleme metodu
        gameUI.logToConsole("Hata: " + message.get("msg")); // Konsola hata mesajını yazma
    }
    
    private void handleInfo(Message message) { // Bilgi mesajı işleme metodu
        String info = message.get("msg"); // Mesajdan bilgi metnini alma
        gameUI.logToConsole(info); // Konsola bilgi metnini yazma
        
        if (info.contains("Yeni bir rakip bekleniyor")) { // Belirli bir mesaj içeriği kontrolü
            gameUI.updateStatus("Yeni bir rakip bekleniyor..."); // Durum mesajını güncelleme
            gameUI.enableButtons(false); // Butonları devre dışı bırakma
        }

        if (info.contains("Diğer oyuncudan yeniden başlatma isteği")) { // Yeniden başlatma isteği mesajı kontrolü
            int answer = JOptionPane.showConfirmDialog(parent, // Kullanıcıya onay dialog'u gösterme
                    "Rakip oyunu yeniden başlatmak istiyor. Kabul ediyor musunuz?",
                    "Yeniden Başlatma İsteği", JOptionPane.YES_NO_OPTION);

            if (answer == JOptionPane.YES_OPTION) { // Kullanıcı evet derse
                Message restartMsg = new Message("RESTART", Collections.emptyMap()); // Yeniden başlatma mesajı oluşturma
                parent.sendMessage(restartMsg); // Mesajı sunucuya gönderme
            } else { // Kullanıcı hayır derse
                Message declineMsg = new Message("RESTART_DECLINE", Collections.emptyMap()); // Red mesajı oluşturma
                parent.sendMessage(declineMsg); // Red mesajını sunucuya gönderme
                gameUI.logToConsole("Yeniden başlatma isteğini reddettiniz."); // Konsola red bilgisi yazma
            }
        }
    }
    
    private void handleExit(Message message) { // Çıkış işleme metodu
        JOptionPane.showMessageDialog(parent, // Kullanıcıya veda mesajı gösterme
                "Oyun kapatılıyor. Görüşmek üzere!",
                "Çıkış", JOptionPane.INFORMATION_MESSAGE);
        parent.dispose(); // Ana pencereyi kapatma
    }
    
    private void handleDisconnect(Message message) { // Bağlantı kopması işleme metodu
        JOptionPane.showMessageDialog(parent, // Kullanıcıya bağlantı kopma mesajı gösterme
                message.get("msg"),
                "Bağlantı Kesildi", JOptionPane.WARNING_MESSAGE);
        parent.dispose(); // Ana pencereyi kapatma
    }
} 