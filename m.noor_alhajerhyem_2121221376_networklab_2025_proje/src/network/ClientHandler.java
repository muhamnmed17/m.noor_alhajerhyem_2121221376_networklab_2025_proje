package network; 

import RiskServer.RiskServer;
import game.RiskMatch;
import java.io.*; 
import java.net.*; 
import java.util.*; 

/**
 * İstemci bağlantılarını yöneten ve komutları işleyen sınıf
 */
public class ClientHandler implements Runnable { // ClientHandler sınıfı tanımı, Runnable arayüzünü uygular

    private Socket clientSocket; // İstemci ile iletişim için soket
    private ObjectOutputStream out; // Nesne gönderimi için çıkış akışı
    private ObjectInputStream in; // Nesne alımı için giriş akışı
    private RiskServer server; // Sunucu referansı
    private int playerId; // Oyuncu kimlik numarası
    private boolean running = true; // İş parçacığının çalışma durumu, başlangıçta true
    private String playerName = "Oyuncu"; // Oyuncu adı, varsayılan değer
    private RiskMatch riskMatch;  // Eşleşme referansı, oyuncunun katıldığı oyun

    public ClientHandler(Socket socket, int playerId, RiskServer server) { // Constructor, yeni bir istemci bağlantısı için
        this.clientSocket = socket; // Soket referansını kaydet
        this.playerId = playerId; // Oyuncu ID'sini kaydet
        this.server = server; // Sunucu referansını kaydet

        try { // Akış nesnelerini oluşturma bloğu
            // Önemli: Önce OutputStream, sonra InputStream (deadlock riskini azaltır)
            this.out = new ObjectOutputStream(socket.getOutputStream()); // Çıkış akışını oluştur
            this.out.flush(); // Başlık verilerini hemen gönder (buffer'ı temizle)
            
            this.in = new ObjectInputStream(socket.getInputStream()); // Giriş akışını oluştur
            
            System.out.println("ClientHandler oluşturuldu: Oyuncu " + playerId); // Log mesajı yaz
        } catch (IOException e) { // IO hatası durumunda
            System.err.println("ClientHandler oluşturma hatası: " + e.getMessage()); // Hata mesajını yazdır
        }
    }

    
    @Override
public void run() {
    try {
        Object inputObj; // Gelen mesajı tutacak değişken
        
        // Thread çalıştığı ve mesaj geldiği sürece döngüde kal
        while (running && (inputObj = in.readObject()) != null) {
            
            // Gelen nesnenin tipine göre işle
            if (inputObj instanceof Message) {
                // Yeni mesaj formatı (Message nesnesi)
                processMessage((Message) inputObj);
            } else if (inputObj instanceof String) {
                // Eski mesaj formatı (String) - geriye uyumluluk için
                processLegacyCommand((String) inputObj);
            }
        }
        
    } catch (SocketException | EOFException e) {
        // Normal bağlantı kesme durumları
        // SocketException: Oyuncu programı kapattı
        // EOFException: Bağlantının diğer ucu kapandı
        // Bu normal durumlar, çok detaylı log yapmaya gerek yok
        System.out.println(" Oyuncu " + playerId + " (" + playerName + ") bağlantısını sonlandırdı.");
        
    } catch (IOException e) {
        // Giriş/çıkış hataları - ağ problemleri
        if (e.getMessage() != null && 
            (e.getMessage().contains("Connection reset") || 
             e.getMessage().contains("connection was aborted"))) {
            // Bu hatalar oyuncunun beklenmeyen şekilde çıktığını gösterir
            // (program crash, ağ kesilmesi vs.)
            System.out.println(" Oyuncu " + playerId + " (" + playerName + ") beklenmeyen şekilde bağlantısını kesti.");
        } else {
            // Diğer I/O hataları - daha genel ağ problemleri
            System.out.println(" Oyuncu " + playerId + " (" + playerName + ") ile iletişim hatası.");
        }
        
    } catch (ClassNotFoundException e) {
        // Serileştirme hatası - gelen nesne tanınamadı
        // Bu oyuncunun yanlış format mesaj gönderdiğini gösterir
        System.out.println("⚠️ Oyuncu " + playerId + " bilinmeyen mesaj formatı gönderdi.");
        
    } catch (Exception e) {
        // Tüm diğer beklenmeyen hatalar - bunlar ciddi olabilir
        // Bu tür hatalar debug edilmeli
        System.err.println(" Oyuncu " + playerId + " beklenmeyen hata: " + e.getMessage());
        
    } finally {
        // Her durumda (hata olsun olmasın) temizlik yap
        // finally bloğu try-catch'den çıkarken her zaman çalışır
        handleDisconnect();
    }
}

    /**
     * Bağlantı kesilince temizlik yapar
     */
private void handleDisconnect() {
    try {
        // Eğer oyuncu bir eşleşmede (match) ise
        if (riskMatch != null) {
            // Match'e oyuncunun ayrıldığını bildir (rakibe haber ver, oyunu sonlandır vs.)
            riskMatch.handleDisconnect(playerId);
        } else {
            // Henüz eşleşmede değilse (bekleme listesinde), direkt sunucudan çıkar
            server.removeClient(this);
        }
        
        // Tüm network kaynaklarını kapat (stream'ler, socket vs.)
        close();
        
        // Başarılı ayrılma log'u - kullanıcı dostu mesaj
        System.out.println(" Oyuncu " + playerId + " (" + playerName + ") oyundan ayrıldı.");
        
    } catch (IOException e) {
        // Kapatma sırasında oluşan hatalar - genelde önemli değil
        if (e.getMessage().contains("Socket closed")) {
            // Socket zaten kapalıydı - bu normal
            System.out.println(" Oyuncu " + playerId + " bağlantısı zaten kapatılmıştı.");
        } else {
            // Diğer I/O hataları - detaya girmeyerek genel bilgi ver
            System.out.println(" Oyuncu " + playerId + " temizlenirken küçük bir hata oluştu.");
        }
    }
}


    /**
     * Yeni mesaj formatında gelen mesajları işler
     */
    private void processMessage(Message message) { // Mesaj nesnelerini işleyen metot
        System.out.println("Oyuncu " + playerId + " mesajı: " + message); // Log mesajı yaz

        switch (message.type) { // Mesaj tipine göre işleme yap
            case "SET_NAME": // İsim ayarlama mesajı ise
                playerName = message.get("name"); // Oyuncu adını ayarla
                System.out.println("Oyuncu " + playerId + " yeni isim: " + playerName); // Log mesajı yaz
                break;

            case "PLACE_TROOPS": // Asker yerleştirme mesajı ise
                if (riskMatch != null) { // Eğer oyuncu bir eşleşmeye katılmışsa
                    String territory = message.get("territory"); // Bölge adını al
                    int troops = Integer.parseInt(message.get("troops")); // Asker sayısını al
                    riskMatch.handlePlaceTroops(playerId, territory, troops); // Eşleşmeye asker yerleştirme isteği gönder
                }
                break;

            case "ATTACK": // Saldırı mesajı ise
                if (riskMatch != null) { // Eğer oyuncu bir eşleşmeye katılmışsa
                    String from = message.get("from"); // Saldıran bölgeyi al
                    String to = message.get("to"); // Hedef bölgeyi al
                    int dice = Integer.parseInt(message.get("dice")); // Zar sayısını al
                    riskMatch.handleAttack(playerId, from, to, dice); // Eşleşmeye saldırı isteği gönder
                }
                break;

            case "FORTIFY": // Güçlendirme mesajı ise
                if (riskMatch != null) { // Eğer oyuncu bir eşleşmeye katılmışsa
                    String from = message.get("from"); // Kaynak bölgeyi al
                    String to = message.get("to"); // Hedef bölgeyi al
                    int troops = Integer.parseInt(message.get("troops")); // Asker sayısını al
                    riskMatch.handleFortify(playerId, from, to, troops); // Eşleşmeye güçlendirme isteği gönder
                }
                break;

            case "RESTART_DECLINE": // Yeniden başlatma reddi mesajı ise
                if (riskMatch != null) { // Eğer oyuncu bir eşleşmeye katılmışsa
                    riskMatch.handleRestartDecline(playerId); // Eşleşmeye yeniden başlatma reddi bilgisini ilet
                }
                break;

            case "END_TURN": // Tur bitirme mesajı ise
                if (riskMatch != null) { // Eğer oyuncu bir eşleşmeye katılmışsa
                    riskMatch.handleEndTurn(playerId); // Eşleşmeye tur bitirme isteği gönder
                }
                break;

            case "RESTART": // Yeniden başlatma mesajı ise
                if (riskMatch != null) { // Eğer oyuncu bir eşleşmeye katılmışsa
                    riskMatch.handleRestartRequest(playerId); // Eşleşmeye yeniden başlatma isteği gönder
                }
                break;

            default: // Bilinmeyen bir mesaj tipi ise
                sendErrorMessage("Bilinmeyen komut: " + message.type); // Hata mesajı gönder
                break;
        }
    }

    /**
     * Eski string formatındaki komutları işler (geriye uyumluluk için)
     */
    private void processLegacyCommand(String command) { // Eski format string komutları işleyen metot
        System.out.println("Oyuncu " + playerId + " eski format komutu: " + command); // Log mesajı yaz

        String[] parts = command.split(" ", 2); // Komutu boşluğa göre en fazla 2 parçaya böl
        String cmd = parts[0]; // İlk parça komut tipi
        String data = parts.length > 1 ? parts[1] : ""; // İkinci parça (varsa) komut verisi

        // Eski formatı yeni formata dönüştür ve öyle işle
        Message message = new Message(); // Yeni bir mesaj nesnesi oluştur
        message.type = cmd; // Mesaj tipini ayarla

        switch (cmd) { // Komut tipine göre işlem yap
            case "SET_NAME": // İsim ayarlama komutu ise
                message.put("name", data); // İsim verisini ekle
                break;
                
            case "PLACE_TROOPS": // Asker yerleştirme komutu ise
                if (!data.isBlank()) { // Eğer veri kısmı boş değilse
                    String[] subParts = data.trim().split(" "); // Veriyi boşluklara göre böl
                    if (subParts.length >= 2) { // En az 2 parça varsa
                        int lastIndex = subParts.length - 1; // Son parçanın indeksi
                        String troops = subParts[lastIndex]; // Son parça asker sayısı
                        String territory = String.join(" ", Arrays.copyOfRange(subParts, 0, lastIndex)); // Geri kalan parçalar bölge adı
                        
                        message.put("territory", territory); // Bölge adını ekle
                        message.put("troops", troops); // Asker sayısını ekle
                    }
                }
                break;
                
            case "ATTACK": // Saldırı komutu ise
                String[] attackParts = data.split(" "); // Veriyi boşluklara göre böl
                if (attackParts.length >= 3) { // En az 3 parça varsa
                    String dice = attackParts[attackParts.length - 1]; // Son parça zar sayısı
                    String territoryPart = String.join(" ", Arrays.copyOf(attackParts, attackParts.length - 1)); // Geri kalan kısım bölgeler
                    int sepIndex = territoryPart.lastIndexOf(' '); // Son boşluğun indeksi
                    
                    if (sepIndex != -1) { // Eğer boşluk varsa
                        String from = territoryPart.substring(0, sepIndex); // İlk kısım saldıran bölge
                        String to = territoryPart.substring(sepIndex + 1); // Son kısım hedef bölge
                        
                        message.put("from", from); // Saldıran bölgeyi ekle
                        message.put("to", to); // Hedef bölgeyi ekle
                        message.put("dice", dice); // Zar sayısını ekle
                    }
                }
                break;
                
            case "FORTIFY": // Güçlendirme komutu ise
                String[] fortifyParts = data.split(" "); // Veriyi boşluklara göre böl
                if (fortifyParts.length >= 3) { // En az 3 parça varsa
                    String troops = fortifyParts[fortifyParts.length - 1]; // Son parça asker sayısı
                    String territoryPart = String.join(" ", Arrays.copyOf(fortifyParts, fortifyParts.length - 1)); // Geri kalan kısım bölgeler
                    int sepIndex = territoryPart.lastIndexOf(' '); // Son boşluğun indeksi
                    
                    if (sepIndex != -1) { // Eğer boşluk varsa
                        String from = territoryPart.substring(0, sepIndex); // İlk kısım kaynak bölge
                        String to = territoryPart.substring(sepIndex + 1); // Son kısım hedef bölge
                        
                        message.put("from", from); // Kaynak bölgeyi ekle
                        message.put("to", to); // Hedef bölgeyi ekle
                        message.put("troops", troops); // Asker sayısını ekle
                    }
                }
                break;
                
            default: // Bilinmeyen bir komut tipi ise
                message.put("data", data); // Veriyi olduğu gibi ekle
                break;
        }
        
        processMessage(message); // Oluşturulan mesajı işle
    }

    /**
     * İstemciye mesaj gönderir
     */
  public void sendMessage(Message message) {
    try {
        // Bağlantı kontrolü: stream'ler ve socket açık mı?
        if (out != null && clientSocket != null && !clientSocket.isClosed()) {
            out.writeObject(message); // Mesajı nesne olarak gönder
            out.flush(); // Buffer'ı hemen boşalt (mesaj anında gitsin)
            out.reset(); // Object cache temizle (aynı nesne tekrar gönderilirse sorun olmasın)
        }
    } catch (SocketException e) {
        // SocketException: Bağlantı kesildi - bu normal bir durum (oyuncu çıktı)
        // Çok detaylı log yapmaya gerek yok, sadece bilgilendir
        System.out.println(" Oyuncu " + playerId + " bağlantısı kesildi, mesaj gönderilemedi.");
    } catch (IOException e) {
        // IOException: Giriş/çıkış hataları - bağlantı problemleri
        // Hata mesajına göre daha spesifik bilgi ver
        if (e.getMessage().contains("Connection reset") || 
            e.getMessage().contains("connection was aborted") ||
            e.getMessage().contains("Broken pipe")) {
            // Bu hatalar genelde oyuncunun aniden programı kapattığını gösterir
            System.out.println(" Oyuncu " + playerId + " beklenmeyen şekilde bağlantısını kesti.");
        } else {
            // Diğer I/O hataları - daha genel ağ problemleri
            System.out.println(" Oyuncu " + playerId + " ile iletişim hatası: " + e.getMessage());
        }
    } catch (Exception e) {
        // Diğer tüm beklenmeyen hatalar - bunlar ciddi olabilir
        System.err.println("⚠️ Oyuncu " + playerId + " mesaj gönderiminde beklenmeyen hata: " + e.getMessage());
    }
}
    
    /**
     * Eski string formatında mesaj gönderir (geriye uyumluluk için)
     */
    public void sendLegacyMessage(String message) { // Eski format string mesaj gönderme metodu
        // İstemci eski formatı bekliyorsa, string mesaj olarak gönder
        // Yeni format için string'i Message nesnesine çevir
        try { // Mesaj gönderme bloğu
            if (out != null && clientSocket != null && !clientSocket.isClosed()) { // Çıkış akışı ve soket hala açıksa
                String[] parts = message.split(" ", 2); // Mesajı boşluğa göre en fazla 2 parçaya böl
                String type = parts[0]; // İlk parça mesaj tipi
                String data = parts.length > 1 ? parts[1] : ""; // İkinci parça (varsa) mesaj verisi
                
                Message msg = new Message(type, Map.of("data", data)); // Yeni format Message nesnesi oluştur
                sendMessage(msg); // Oluşturulan Message nesnesini gönder
            }
        } catch (Exception e) { // Hata durumunda
            System.err.println("Mesaj gönderilirken hata: " + e.getMessage()); // Hata mesajı yazdır
        }
    }
    
    /**
     * Hata mesajı gönderir
     */
    private void sendErrorMessage(String errorText) { // Hata mesajı gönderme metodu
        Message errorMsg = new Message("ERROR", Map.of("msg", errorText)); // Hata mesajı oluştur
        sendMessage(errorMsg); // Hata mesajını gönder
    }

    public String getPlayerName() { // Oyuncu adını getiren metot
        return playerName; // Oyuncu adını döndür
    }

    /**
     * Bağlantıyı kapatır
     */
    public void close() throws IOException {
    running = false; // Thread döngüsünü durdur
    
    // Input Stream'i kapat
    try {
        if (in != null) {
            in.close(); // Gelen mesajları okuyan stream'i kapat
        }
    } catch (IOException e) {
        // Input stream kapatma hatası - genelde önemli değil (zaten kapalı olabilir)
        System.out.println("🔌 Oyuncu " + playerId + " giriş akışı kapatılırken ufak hata oluştu.");
    }
    
    // Output Stream'i kapat
    try {
        if (out != null) {
            out.close(); // Giden mesajları gönderen stream'i kapat
        }
    } catch (IOException e) {
        // Output stream kapatma hatası - genelde önemli değil
        System.out.println(" Oyuncu " + playerId + " çıkış akışı kapatılırken ufak hata oluştu.");
    }
    
    // Socket'i kapat (ana bağlantı)
    try {
        if (clientSocket != null && !clientSocket.isClosed()) {
            clientSocket.close(); // Ağ bağlantısını tamamen kes
        }
    } catch (IOException e) {
        // Socket kapatma hatası - bu da normal olabilir (zaten kapalı)
        if (e.getMessage().contains("Socket closed")) {
            System.out.println(" Oyuncu " + playerId + " bağlantısı zaten kapatılmıştı.");
        } else {
            // Başka bir socket hatası
            System.out.println(" Oyuncu " + playerId + " bağlantısı kapatılırken hata: " + e.getMessage());
        }
    }}
    public int getPlayerId() { // Oyuncu ID'sini getiren metot
        return playerId; // Oyuncu ID'sini döndür
    }

    // RiskMatch ataması ve erişimi
    public void setRiskMatch(RiskMatch match) { // Eşleşme ataması yapan metot
        this.riskMatch = match; // Eşleşme referansını kaydet
    }

    public RiskMatch getRiskMatch() { // Eşleşme referansını getiren metot
        return riskMatch; // Eşleşme referansını döndür
    }
} 