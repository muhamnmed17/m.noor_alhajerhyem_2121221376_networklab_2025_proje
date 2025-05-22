package final_project; // Projenin paket tanımlaması

import javax.swing.*; // Swing GUI bileşenleri için import
import java.awt.*; // AWT grafik ve layout sınıfları için import
import java.awt.event.*; // Olay yönetimi sınıfları için import
import java.io.*; // Dosya ve I/O işlemleri için import
import java.util.*; // Java koleksiyon sınıfları için import
import java.util.List; // Liste interface'i için özel import
import javax.swing.border.*; // Swing kenarlık sınıfları için import

/**
 * Risk oyunu için ana istemci sınıfı
 */ // Sınıfın açıklaması
public class RiskClient extends JFrame { // Ana istemci sınıfının tanımlanması, JFrame'den miras alır
    private NetworkClient network; // Ağ bağlantısı yöneticisi
    private GameUI gameUI; // Oyun kullanıcı arayüzü
    private GameState gameState; // Oyun durumu yöneticisi
    private MessageHandler messageHandler; // Mesaj işleme yöneticisi
    
    public RiskClient(String serverIp, int serverPort, String playerName) { // Yapıcı metod - IP, port ve oyuncu adı parametreleri
        super("Risk Oyunu"); // Üst sınıfın (JFrame) yapıcısını çağırma ve pencere başlığını ayarlama
        
        // Bileşenleri başlat
        gameState = new GameState(); // Oyun durumu nesnesini oluşturma
        gameUI = new GameUI(this, gameState); // Oyun arayüzü nesnesini oluşturma
        messageHandler = new MessageHandler(this, gameState, gameUI); // Mesaj işleyici nesnesini oluşturma
        
        // UI'ı başlat
        initializeUI(); // Kullanıcı arayüzünü başlatma metodunu çağırma
        
        // Sunucuya bağlan
        connectToServer(serverIp, serverPort, playerName); // Sunucu bağlantısı kurma metodunu çağırma
    }
    
    private void initializeUI() { // Kullanıcı arayüzünü başlatma metodu
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE); // Pencere kapatma işlemini özel olarak yönetme
        
        addWindowListener(new WindowAdapter() { // Pencere olayları için dinleyici ekleme
            @Override // Üst sınıf metodunu geçersiz kılma
            public void windowClosing(WindowEvent e) { // Pencere kapanma olayı
                handleWindowClosing(); // Özel pencere kapatma metodunu çağırma
            }
        });
        
        setSize(1200, 700); // Pencere boyutunu ayarlama
        setMinimumSize(new Dimension(900, 600)); // Minimum pencere boyutunu ayarlama
        setLayout(new BorderLayout()); // BorderLayout layout manager'ını ayarlama
        
        // GameUI'dan ana paneli al ve ekle
        add(gameUI.getMainPanel(), BorderLayout.CENTER); // Ana paneli pencere merkezine ekleme
        
        // Menü çubuğunu ayarla
        setJMenuBar(gameUI.createMenuBar()); // GameUI'dan oluşturulan menü çubuğunu ayarlama
        
        // Başlangıçta butonları pasif yap
        gameUI.enableButtons(false); // Başlangıçta tüm butonları devre dışı bırakma
        gameUI.updateStatus("Sunucuya bağlanıyor..."); // Durum mesajını bağlanma olarak güncelleme
        
        setLocationRelativeTo(null); // Pencereyi ekran ortasında konumlandırma
        setVisible(true); // Pencereyi görünür yapma
    }
    
    private void connectToServer(String serverIp, int serverPort, String playerName) { // Sunucuya bağlanma metodu
        try { // Hata yakalama bloğu başlangıcı
            network = new NetworkClient(); // Ağ istemcisi nesnesini oluşturma
            network.connect(serverIp, serverPort); // Belirtilen IP ve porta bağlanma
            gameUI.logToConsole("Sunucuya bağlandı: " + serverIp + ":" + serverPort); // Başarılı bağlantı mesajını konsola yazma
            gameUI.updateStatus("Bağlantı kuruldu. Oyun başlatılıyor..."); // Durum mesajını güncelleme
            
            Message nameMsg = new Message("SET_NAME", Map.of("name", playerName)); // Oyuncu adını sunucuya gönderecek mesaj oluşturma
            network.sendMessage(nameMsg); // Oyuncu adı mesajını sunucuya gönderme
            
            // Mesaj dinlemeyi başlat
            network.startListening(messageHandler::processMessage); // Sunucudan gelen mesajları dinlemeye başlama
            
        } catch (IOException e) { // Giriş/çıkış hatası yakalama
            gameUI.logToConsole("Bağlantı hatası: " + e.getMessage()); // Hata mesajını konsola yazma
            gameUI.updateStatus("Bağlantı başarısız!"); // Durum mesajını hata olarak güncelleme
            JOptionPane.showMessageDialog(this, "Sunucuya bağlanılamadı: " + e.getMessage(), // Kullanıcıya hata dialog'u gösterme
                    "Bağlantı Hatası", JOptionPane.ERROR_MESSAGE);
            dispose(); // Pencereyi kapatma
        }
    }
    
    public void handleWindowClosing() { // Pencere kapatma işlemini yönetme metodu
        int response = JOptionPane.showConfirmDialog( // Kullanıcıya onay dialog'u gösterme
            this, // Bu pencereyi parent olarak ayarlama
            "Oyundan çıkmak istiyor musunuz?", // Dialog mesajı
            "Çıkış Onayı", // Dialog başlığı
            JOptionPane.YES_NO_OPTION, // Evet/Hayır seçenekleri
            JOptionPane.QUESTION_MESSAGE // Soru tipi ikon
        );

        if (response == JOptionPane.YES_OPTION) { // Kullanıcı evet seçerse
            if (network != null && network.isConnected()) { // Ağ bağlantısı varsa ve bağlıysa
                Message exitMsg = new Message("QUIT", Map.of("msg", "User quit")); // Çıkış mesajı oluşturma
                network.sendMessage(exitMsg); // Çıkış mesajını sunucuya gönderme
            }
            cleanup(); // Temizlik işlemlerini yapma
            SwingUtilities.invokeLater(() -> { // EDT'de asenkron olarak çalıştırma
                dispose(); // Pencereyi kapatma
                System.exit(0); // Uygulamayı sonlandırma
            });
        }
    }
    
    public void sendMessage(Message message) { // Mesaj gönderme metodu
        if (network != null) { // Ağ bağlantısı varsa
            network.sendMessage(message); // Mesajı ağ üzerinden gönderme
        }
    }
    
    private void cleanup() { // Temizlik işlemleri metodu
        gameUI.cleanup(); // GameUI temizlik işlemlerini yapma
        if (network != null) { // Ağ bağlantısı varsa
            network.close(); // Ağ bağlantısını kapatma
        }
    }
    
    @Override // Üst sınıf metodunu geçersiz kılma
    public void dispose() { // Pencere kapatma metodunu geçersiz kılma
        cleanup(); // Temizlik işlemlerini yapma
        SwingUtilities.invokeLater(() -> { // EDT'de asenkron olarak çalıştırma
            try { // Hata yakalama bloğu başlangıcı
                super.dispose(); // Üst sınıfın dispose metodunu çağırma
            } catch (Exception e) { // Herhangi bir hata yakalama
                System.err.println("Pencere kapatılırken hata oluştu: " + e.getMessage()); // Hata mesajını stderr'a yazdırma
            }
        });
    }
    
    public static void main(String[] args) { // Ana metod - uygulamanın giriş noktası
        SwingUtilities.invokeLater(() -> { // EDT'de asenkron olarak çalıştırma
            String ip = "127.0.0.1"; // Varsayılan sunucu IP adresi (localhost)
            String playerName = null; // Oyuncu adı değişkenini başlatma
            boolean isValidName = false; // Geçerli isim kontrolü için bayrak

            while (!isValidName) { // Geçerli bir isim girilene kadar döngü
                playerName = JOptionPane.showInputDialog("Oyuncu adınızı girin:"); // Kullanıcıdan isim alma
                if (playerName != null && !playerName.trim().isEmpty()) { // İsim null değil ve boş değilse
                    isValidName = true; // Geçerli isim bayrağını true yapma
                } else { // İsim geçersizse
                    JOptionPane.showMessageDialog(null, "Lütfen geçerli bir isim girin!", // Hata mesajı gösterme
                            "Hata", JOptionPane.ERROR_MESSAGE);
                }
            }

            new RiskClient(ip, 9090, playerName); // Yeni RiskClient nesnesi oluşturma ve uygulamayı başlatma
        });
    }
} 