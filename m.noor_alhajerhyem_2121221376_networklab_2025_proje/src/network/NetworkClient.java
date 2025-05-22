package network; 

import java.io.*; 
import java.net.*; 
import java.util.concurrent.*;
import java.util.function.Consumer; 
import java.util.Map; 

/**
 * Risk oyunu için ağ iletişimini yöneten sınıf.
 * Java nesne serileştirme ile mesaj gönderip alır.
 */
public class NetworkClient { // NetworkClient sınıfı tanımı başlangıcı
    private Socket socket; // Sunucu ile iletişim için soket
    private ObjectOutputStream out; // Nesne gönderimi için çıkış akışı
    private ObjectInputStream in; // Nesne alımı için giriş akışı
    private final ExecutorService executor = Executors.newSingleThreadExecutor(); // Mesaj dinleme için tek iş parçacıklı yürütücü
    private boolean connected = false; // Bağlantı durumu, başlangıçta bağlı değil
    
    /**
     * Belirtilen IP ve porta bağlanır
     * 
     * serverIp Sunucu IP adresi
     *  serverPort Sunucu port numarası
     *
     */
    public void connect(String serverIp, int serverPort) throws IOException { // Bağlantı kurma metodu
        try { // Hata yakalama bloğu başlangıcı
            socket = new Socket(serverIp, serverPort); // Verilen IP ve porta bir soket oluştur
            
            //  Önce output stream sonra input stream oluşturulmalı (deadlock önlemi)
            out = new ObjectOutputStream(socket.getOutputStream()); // Nesne çıkış akışını oluştur
            out.flush(); // Başlık bilgilerini hemen gönder (buffer'ı boşalt)
            
            in = new ObjectInputStream(socket.getInputStream()); // Nesne giriş akışını oluştur
            connected = true; // Bağlantı durumunu true olarak ayarla
        } catch (IOException e) { // IO hatası durumunda
            throw new IOException("Sunucuya bağlanılamadı: " + e.getMessage(), e); // Daha açıklayıcı hata fırlat
        }
    }
    
    /**
     * Sunucuya Message nesnesini gönderir
     * 
     *  message Gönderilecek mesaj
     */
    public void sendMessage(Message message) { // Mesaj gönderme metodu
        if (connected && out != null) { // Eğer bağlıysak ve çıkış akışı oluşturulmuşsa
            try { // Hata yakalama bloğu başlangıcı
                out.writeObject(message); // Mesaj nesnesini gönder
                out.flush(); // Hemen gönderilmesini sağla (buffer'ı boşalt)
                out.reset(); // Object cache'ini temizle (aynı nesneyi değiştirip tekrar gönderince sorun olmasın)
            } catch (IOException e) { // IO hatası durumunda
                System.err.println("Mesaj gönderilirken hata: " + e.getMessage()); // Hata mesajını konsola yaz
            }
        }
    }
    
    /**
     * Eski tip string komut için destek (geçiş sürecinde)
     * Bu metod sadece geriye uyumluluk içindir
     */
    public void sendCommand(String command) { // Eski string formatında komut gönderme metodu
        // Eski formattan yeni formata dönüştür
        String[] parts = command.split(" ", 2); // Komutu boşluğa göre en fazla 2 parçaya böl
        String type = parts[0]; // İlk parça komut tipi
        String data = parts.length > 1 ? parts[1] : ""; // İkinci parça (varsa) komut verisi
        
        Message message = new Message(type, Map.of("data", data)); // Yeni format Message nesnesi oluştur
        sendMessage(message); // Oluşturulan Message nesnesini gönder
    }
    
    /**
     * Sunucudan gelen mesajları dinlemeye başlar
     * 
     * messageHandler Gelen mesajı işleyecek fonksiyon
     */
    public void startListening(Consumer<Message> messageHandler) {
    if (!connected) return; // Eğer bağlı değilsek, hiçbir şey yapma

    executor.submit(() -> { // Arka planda mesajları dinle
        try {
            Object obj; // Alınan nesneyi tutar

            // Bağlantı açık olduğu sürece mesaj bekle
            while (connected && (obj = in.readObject()) != null) {

                // Yeni formatta mesaj gelirse
                if (obj instanceof Message) {
                    Message message = (Message) obj;
                    messageHandler.accept(message); // İşleyiciye gönder

                // Eski tip String mesaj gelirse (geriye uyumluluk)
                } else if (obj instanceof String) {
                    String stringMessage = (String) obj;
                    String[] parts = stringMessage.split(" ", 2);
                    String type = parts[0];
                    String data = parts.length > 1 ? parts[1] : "";

                    Message message = new Message(type, Map.of("data", data));
                    messageHandler.accept(message);
                }
            }

        } catch (SocketException e) {
            // Bağlantı aniden kesildi (ör. sunucu kapattı)
            Message disconnectMessage = new Message("DISCONNECT",
                    Map.of("msg", "Sunucu ile bağlantı kesildi: " + e.getMessage()));
            messageHandler.accept(disconnectMessage);

        } catch (EOFException e) {
            // Dosya sonu: sunucu bağlantıyı kapattı
            Message disconnectMessage = new Message("DISCONNECT",
                    Map.of("msg", "Sunucu bağlantıyı kapattı"));
            messageHandler.accept(disconnectMessage);

        } catch (IOException e) {
            // Giriş/çıkış hatası (mesaj alınamıyor)
            Message errorMessage = new Message("DISCONNECT",
                    Map.of("msg", "Sunucu ile iletişim hatası: " + e.getMessage()));
            messageHandler.accept(errorMessage);

        } catch (ClassNotFoundException e) {
            // Serileştirme hatası
            Message errorMessage = new Message("ERROR",
                    Map.of("msg", "Serileştirme hatası: " + e.getMessage()));
            messageHandler.accept(errorMessage);

        } catch (Exception e) {
            // Diğer tüm hatalar
            Message errorMessage = new Message("DISCONNECT",
                    Map.of("msg", "Beklenmeyen hata: " + e.getMessage()));
            messageHandler.accept(errorMessage);
        }
    });
}

    
    /**
     * Bağlantıyı ve kaynakları kapatır
     */
    public void close() { // Kaynakları temizleme ve bağlantıyı kapatma metodu
        try { // Hata yakalama bloğu başlangıcı
            connected = false; // Bağlantı durumunu false olarak ayarla
            if (out != null) { // Eğer çıkış akışı oluşturulmuşsa
                out.close(); // Çıkış akışını kapat
            }
            if (in != null) { // Eğer giriş akışı oluşturulmuşsa
                in.close(); // Giriş akışını kapat
            }
            if (socket != null && !socket.isClosed()) { // Eğer soket varsa ve kapatılmamışsa
                socket.close(); // Soketi kapat
            }
        } catch (IOException e) { // IO hatası durumunda
            e.printStackTrace(); // Hata izini yazdır
        }
        executor.shutdownNow(); // Yürütücüyü hemen durdur
    }
    
    /**
     * Bağlantı durumunu döndürür
     * 
     *  Bağlantı durumu
     */
    public boolean isConnected() { // Bağlantı durumu sorgulama metodu
        return connected; // Bağlantı durumunu döndür
    }
} // NetworkClient sınıfı sonu