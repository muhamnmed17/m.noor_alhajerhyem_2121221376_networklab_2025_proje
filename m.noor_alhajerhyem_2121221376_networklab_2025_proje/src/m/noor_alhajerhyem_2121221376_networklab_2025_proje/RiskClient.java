package final_project; // Proje paket adı

import javax.swing.*; // Swing GUI bileşenlerini içe aktar
import java.awt.*; // AWT GUI bileşenlerini içe aktar
import java.awt.event.*; // AWT olay işleyicilerini içe aktar
import java.io.*; // Giriş/çıkış işlemleri için kütüphaneleri içe aktar
import java.net.*; // Ağ işlemleri için kütüphaneleri içe aktar
import java.util.*; // Genel koleksiyon ve yardımcı sınıfları içe aktar
import java.util.concurrent.*; // Eşzamanlı işlemler için kütüphaneleri içe aktar
import java.util.List; // List arayüzünü içe aktar
import javax.swing.border.*; // Swing kenarlık bileşenlerini içe aktar
import java.util.Timer; // Zamanlayıcı sınıfını içe aktar
import java.util.TimerTask; // Zamanlayıcı görevi sınıfını içe aktar
import javax.imageio.ImageIO; // Resim okuma/yazma işlemleri için kütüphaneyi içe aktar
import java.awt.image.BufferedImage; // Tamponlanmış resim sınıfını içe aktar

/**
 * Risk oyunu için geliştirilmiş istemci uygulaması
 */
    public class RiskClient extends JFrame { // Risk istemci sınıfını JFrame'den türet

    private JLabel troopsLeftLabel; // Kalan asker sayısını gösteren etiket
    private BufferedImage worldMap; // Dünya haritası görseli
    private final int MAP_WIDTH = 1200; // Harita genişliği (piksel)
    private final int MAP_HEIGHT = 700; // Harita yüksekliği (piksel)

    private JLabel playerColorLabel; // Oyuncu rengini gösteren etiket
    private NetworkClient network; // Ağ istemcisi nesnesi
    private final Map<String, Set<String>> adjacencyMap = new HashMap<>(); // Bölge komşuluk haritası
    private Timer turnTimer; // Tur zamanlayıcısı
    private int secondsLeft; // Kalan saniye sayısı
    private JLabel timerLabel; // Zamanlayıcı etiketi
    private String selectedTerritory = null; // Seçili bölge adı
    private List<String> highlightedTargets = new ArrayList<>(); // Vurgulanan hedef bölgeler listesi

    private int playerId = -1; // Oyuncunun kimlik numarası, başlangıçta tanımsız

    private final Map<Integer, String> playerNames = new HashMap<>(); // Oyuncu ID - ad eşlemesi

    private int currentTurn = -1; // Şu anki tur, başlangıçta tanımsız

    private Color getPlayerColor(int id) { // Oyuncu ID'sine göre renk döndüren metot
        return (id % 2 == 0) // ID çift sayı mı?
                ? new Color(220, 20, 60) // Kırmızı (çift ID için)
                : new Color(30, 144, 255); // Mavi (tek ID için)
    }
    private static final Color SELECTED_COLOR = new Color(255, 215, 0, 200); // Seçili bölge rengi (sarı, yarı saydam)
    private static final Color FRIENDLY_TARGET_COLOR = new Color(50, 205, 50, 200); // Yeşil - kendi bölgelerimize komşu
    private static final Color ENEMY_TARGET_COLOR = new Color(255, 99, 71, 200); // Kırmızımsı - rakip bölgelere komşu
    private static final Color TARGET_COLOR = new Color(50, 205, 50, 200); // Eski renk, uyumluluk için tutuldu

    private Map<String, Territory> territories = new HashMap<>(); // Bölge adı - Bölge nesnesi eşlemesi
    private Map<Integer, Integer> playerTroopsToPlace = new HashMap<>(); // Oyuncu ID - yerleştirilecek asker sayısı eşlemesi
    private Map<String, String> continentOwners = new HashMap<>(); // Kıta sahipleri

    private final Map<String, Point> territoryPositions = new HashMap<>(); // Bölge adı - koordinat eşlemesi
    private final Map<String, Color> continentColors = new HashMap<>(); // Kıta adı - renk eşlemesi

    private JPanel mapPanel; // Harita paneli
    private JLabel statusLabel; // Durum bilgisi etiketi
    private JTextArea gameLogArea; // Oyun log'u metin alanı
    private JButton placeTroopsButton, attackButton, fortifyButton, endTurnButton; // Oyun kontrol butonları

    private String targetTerritory = null; // Hedef bölge adı
    private boolean attackMode = false; // Saldırı modu aktif mi?
    private boolean fortifyMode = false; // Güçlendirme modu aktif mi?
    private boolean gameOver = false; // Oyun bitti mi?

    private JDialog diceDialog; // Zar atma diyaloğu
    private JPanel attackerDicePanel; // Saldıran zarları paneli
    private JPanel defenderDicePanel; // Savunan zarları paneli

    public RiskClient(String serverIp, int serverPort, String playerName) { // Constructor metodu
        super("Risk Oyunu"); // Üst sınıf constructor'ını çağır, pencere başlığını ayarla
        initializeTerritoryPositions(); // Bölge konumlarını başlat
        initializeContinentColors(); // Kıta renklerini başlat
        initializeUI(); // Kullanıcı arayüzünü başlat
        enableButtons(false); // Başlangıçta butonları pasif yap

        try { // Sunucuya bağlanma denemesi
            // NetworkClient'ı oluştur ve bağlan
            network = new NetworkClient(); // Yeni ağ istemcisi oluştur
            network.connect(serverIp, serverPort); // Belirtilen IP ve porta bağlan
            logToGameConsole("Sunucuya bağlandı: " + serverIp + ":" + serverPort); // Bağlantı bilgisini logla

            // İsim gönder (Message nesnesi ile)
            Message nameMsg = new Message("SET_NAME", Map.of("name", playerName)); // İsim ayarlama mesajı oluştur
            network.sendMessage(nameMsg); // Mesajı gönder

        } catch (IOException e) { // Bağlantı hatası durumunda
            logToGameConsole("Bağlantı hatası: " + e.getMessage()); // Hatayı logla
            JOptionPane.showMessageDialog(this, "Sunucuya bağlanılamadı: " + e.getMessage(),
                    "Bağlantı Hatası", JOptionPane.ERROR_MESSAGE); // Hata mesajı göster
            dispose(); // Pencereyi kapat
            return; // Constructor'dan çık
        }

        // NetworkClient'a mesaj işleme fonksiyonunu ver
        network.startListening(this::processMessage); // Mesaj dinlemeyi başlat, gelen mesajları processMessage metoduna yönlendir
    }

    /**
     * Konsola zaman damgalı mesaj yazdırır
     */
    private void logToGameConsole(String message) { // Oyun konsoluna mesaj yazdırma metodu
        SwingUtilities.invokeLater(() -> { // EDT üzerinde çalıştır
            String timestamp = String.format("[%tT] ", new Date()); // Zaman damgası oluştur
            gameLogArea.append(timestamp + message + "\n"); // Mesajı zamanlı olarak ekle
            gameLogArea.setCaretPosition(gameLogArea.getDocument().getLength()); // Kaydırmayı en alta getir
        });
    }

    private void setSelectedTerritory(String name) { // Seçili bölgeyi ayarlama metodu
        selectedTerritory = name; // Seçili bölge adını ayarla
        highlightedTargets.clear(); // Vurgulanan hedefleri temizle

        if (name != null && adjacencyMap.containsKey(name)) { // Eğer geçerli bir bölge seçildiyse
            highlightedTargets.addAll(adjacencyMap.get(name)); // Bu bölgenin tüm komşularını vurgulanan hedeflere ekle
        }

        mapPanel.repaint(); // Görseli güncelle
    }

    private JPanel createRightPanel() { // Sağ panel oluşturma metodu
        JPanel rightPanel = new JPanel(); // Yeni bir panel oluştur
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS)); // Dikey kutu yerleşimi kullan
        rightPanel.setPreferredSize(new Dimension(200, 0)); // Tercih edilen boyutu ayarla

        // Durum paneli
        JPanel infoPanel = new JPanel(); // Bilgi paneli oluştur
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS)); // Dikey kutu yerleşimi kullan
        infoPanel.setBorder(BorderFactory.createTitledBorder("Durum")); // Başlıklı kenarlık ekle

        statusLabel = new JLabel("Bağlanıyor...", SwingConstants.CENTER); // Durum etiketi oluştur
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Yatayda ortala
        troopsLeftLabel = new JLabel("Kalan Asker: 0", SwingConstants.CENTER); // Kalan asker etiketi oluştur
        troopsLeftLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Yatayda ortala
        playerColorLabel = new JLabel("Renginiz: -", SwingConstants.CENTER); // Oyuncu rengi etiketi oluştur
        playerColorLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Yatayda ortala
        timerLabel = new JLabel("Süre: --", SwingConstants.CENTER); // Zamanlayıcı etiketi oluştur
        timerLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Yatayda ortala
        infoPanel.add(Box.createVerticalStrut(5)); // Dikey boşluk ekle
        infoPanel.add(timerLabel); // Zamanlayıcı etiketini ekle

        infoPanel.add(statusLabel); // Durum etiketini ekle
        infoPanel.add(Box.createVerticalStrut(5)); // Dikey boşluk ekle
        infoPanel.add(troopsLeftLabel); // Kalan asker etiketini ekle
        infoPanel.add(playerColorLabel); // Oyuncu rengi etiketini ekle

        // Buton paneli
        JPanel buttonPanel = new JPanel(); // Buton paneli oluştur
        buttonPanel.setLayout(new GridLayout(4, 1, 5, 5)); // 4x1 ızgara yerleşimi, 5 piksel aralıklı

        placeTroopsButton = new JButton("Asker Yerleştir"); // Asker yerleştirme butonu oluştur
        attackButton = new JButton("Saldır"); // Saldırı butonu oluştur
        fortifyButton = new JButton("Güçlendir"); // Güçlendirme butonu oluştur
        endTurnButton = new JButton("Sırayı Bitir"); // Sıra bitirme butonu oluştur

        buttonPanel.add(placeTroopsButton); // Asker yerleştirme butonunu ekle
        buttonPanel.add(attackButton); // Saldırı butonunu ekle
        buttonPanel.add(fortifyButton); // Güçlendirme butonunu ekle
        buttonPanel.add(endTurnButton); // Sıra bitirme butonunu ekle

        // ActionListener eklemeyi unutma
        placeTroopsButton.addActionListener(e -> handlePlaceTroops()); // Asker yerleştirme işleyicisi ekle
        attackButton.addActionListener(e -> toggleAttackMode()); // Saldırı modu değişim işleyicisi ekle
        fortifyButton.addActionListener(e -> toggleFortifyMode()); // Güçlendirme modu değişim işleyicisi ekle
        endTurnButton.addActionListener(e -> endTurn()); // Sıra bitirme işleyicisi ekle

        // Tümünü ekle
        rightPanel.add(infoPanel); // Bilgi panelini ekle
        rightPanel.add(Box.createVerticalStrut(10)); // Dikey boşluk ekle
        rightPanel.add(buttonPanel); // Buton panelini ekle
        rightPanel.add(Box.createVerticalGlue()); // Esnek dikey boşluk ekle

        return rightPanel; // Oluşturulan paneli döndür
    }

    private void startTurnTimer() { // Tur zamanlayıcısını başlatan metot
        stopTurnTimer(); // Önceki zamanlayıcıyı durdur
        secondsLeft = 60; // Kalan saniyeyi 60'a ayarla
        timerLabel.setText("Süre: 60 sn"); // Zamanlayıcı etiketini güncelle

        turnTimer = new Timer(); // Yeni bir zamanlayıcı oluştur
        turnTimer.scheduleAtFixedRate(new TimerTask() { // Düzenli görev planla
            @Override
            public void run() { // Görev çalıştırma metodu
                SwingUtilities.invokeLater(() -> { // EDT üzerinde çalıştır
                    secondsLeft--; // Kalan saniyeyi azalt
                    timerLabel.setText("Süre: " + secondsLeft + " sn"); // Zamanlayıcı etiketini güncelle

                    if (secondsLeft <= 0) { // Süre dolduysa
                        stopTurnTimer(); // Zamanlayıcıyı durdur
                        logToGameConsole("Süre doldu! Otomatik sıra geçiliyor."); // Bilgi mesajı logla
                        endTurn(); // Otomatik sırayı bitir
                    }
                });
            }
        }, 1000, 1000); // her saniye (ilk 1000ms sonra, sonra her 1000ms'de bir)
    }

    private void stopTurnTimer() { // Tur zamanlayıcısını durduran metot
        if (turnTimer != null) { // Zamanlayıcı varsa
            turnTimer.cancel(); // Zamanlayıcıyı iptal et
            turnTimer = null; // Referansı temizle
        }
        timerLabel.setText("Süre: --"); // Zamanlayıcı etiketini sıfırla
    }

    /**
     * Güçlendirme modunu açar/kapatır
     */
    private void toggleFortifyMode() { // Güçlendirme modunu değiştiren metot
        fortifyMode = !fortifyMode; // Güçlendirme modunu tersine çevir
        attackMode = false; // Saldırı modunu kapat
        selectedTerritory = null; // Seçili bölgeyi temizle
        targetTerritory = null; // Hedef bölgeyi temizle

        if (fortifyMode) { // Eğer güçlendirme modu açıldıysa
            statusLabel.setText("Güçlendirme modu aktif. Kaynak bölgenizi seçin."); // Durum etiketini güncelle
            logToGameConsole("Güçlendirme modu başlatıldı."); // Bilgi mesajı logla
        } else { // Eğer güçlendirme modu kapatıldıysa
            statusLabel.setText("Güçlendirme modu kapatıldı."); // Durum etiketini güncelle
            logToGameConsole("Güçlendirme modu kapatıldı."); // Bilgi mesajı logla
        }

        mapPanel.repaint(); // Haritayı yeniden çiz
    }

    /**
     * Oyuncunun sırasını bitirir
     */
    private void endTurn() { // Turu bitiren metot
        Message endTurnMsg = new Message("END_TURN", Collections.emptyMap()); // Tur bitirme mesajı oluştur
        network.sendMessage(endTurnMsg); // Mesajı gönder

        logToGameConsole("Turu bitirdiniz."); // Bilgi mesajı logla
        statusLabel.setText("Turu bitirdiniz. Rakip bekleniyor..."); // Durum etiketini güncelle
        enableButtons(false); // Butonları devre dışı bırak
        removeHighlights(); // Vurgulamaları kaldır
        mapPanel.repaint(); // Haritayı yeniden çiz
    }

    /**
     * Asker yerleştirme modunu başlatır
     */
    private void handlePlaceTroops() { // Asker yerleştirme işlemini yöneten metot
        int remaining = playerTroopsToPlace.getOrDefault(playerId, 0); // Kalan asker sayısını al
        if (remaining <= 0) { // Yerleştirilecek asker kalmadıysa
            logToGameConsole("Yerleştirecek askeriniz kalmadı."); // Bilgi mesajı logla
            return; // Metoddan çık
        }

        statusLabel.setText("Asker yerleştirmek için kendi bir bölgenizi seçin."); // Durum etiketini güncelle
        logToGameConsole("Asker yerleştirme modu aktif. Kalan: " + remaining); // Bilgi mesajı logla
    }

    /**
     * Saldırı modunu açar/kapatır
     */
    private void toggleAttackMode() { // Saldırı modunu değiştiren metot
        attackMode = !attackMode; // Saldırı modunu tersine çevir
        fortifyMode = false; // Güçlendirme modunu kapat
        selectedTerritory = null; // Seçili bölgeyi temizle
        targetTerritory = null; // Hedef bölgeyi temizle

        if (attackMode) { // Eğer saldırı modu açıldıysa
            statusLabel.setText("Saldırı modu aktif. Saldıran bölgeyi seçin."); // Durum etiketini güncelle
            logToGameConsole("Saldırı modu başlatıldı."); // Bilgi mesajı logla
        } else { // Eğer saldırı modu kapatıldıysa
            statusLabel.setText("Saldırı modu kapatıldı."); // Durum etiketini güncelle
            logToGameConsole("Saldırı modu kapatıldı."); // Bilgi mesajı logla
        }

        mapPanel.repaint(); // Haritayı yeniden çiz
    }

    private void initializeTerritoryPositions() { // Bölge konumlarını başlatan metot
        // KUZEY AMERİKA (Sarı Bölge)
        territoryPositions.put("Alaska", new Point(91, 106)); // Alaska'nın konumunu ekle
        territoryPositions.put("Kuzeybatı Toprakları", new Point(206, 109)); // Kuzeybatı Toprakları'nın konumunu ekle
        territoryPositions.put("Grönland", new Point(418, 60)); // Grönland'ın konumunu ekle
        territoryPositions.put("Alberta", new Point(186, 160)); // Alberta'nın konumunu ekle
        territoryPositions.put("Ontario", new Point(272, 179)); // Ontario'nun konumunu ekle
        territoryPositions.put("Quebec", new Point(344, 170)); // Quebec'in konumunu ekle
        territoryPositions.put("Batı ABD", new Point(188, 235)); // Batı ABD'nin konumunu ekle
        territoryPositions.put("Doğu ABD", new Point(279, 254)); // Doğu ABD'nin konumunu ekle
        territoryPositions.put("Orta Amerika", new Point(204, 321)); // Orta Amerika'nın konumunu ekle

        // GÜNEY AMERİKA (Turuncu Bölge)
        territoryPositions.put("Venezuela", new Point(284, 385)); // Venezuela'nın konumunu ekle
        territoryPositions.put("Peru", new Point(246, 453)); // Peru'nun konumunu ekle
        territoryPositions.put("Brezilya", new Point(373, 446)); // Brezilya'nın konumunu ekle
        territoryPositions.put("Arjantin", new Point(313, 564)); // Arjantin'in konumunu ekle

        // AVRUPA (Mavi Bölge)
        territoryPositions.put("İzlanda", new Point(511, 142)); // İzlanda'nın konumunu ekle
        territoryPositions.put("Britanya", new Point(500, 209)); // Britanya'nın konumunu ekle
        territoryPositions.put("İskandinavya", new Point(607, 135)); // İskandinavya'nın konumunu ekle
        territoryPositions.put("Kuzey Avrupa", new Point(596, 227)); // Kuzey Avrupa'nın konumunu ekle
        territoryPositions.put("Batı Avrupa", new Point(508, 317)); // Batı Avrupa'nın konumunu ekle
        territoryPositions.put("Güney Avrupa", new Point(614, 287)); // Güney Avrupa'nın konumunu ekle
        territoryPositions.put("Ukrayna", new Point(704, 187)); // Ukrayna'nın konumunu ekle

        // AFRİKA (Kahverengi Bölge)
        territoryPositions.put("Kuzey Afrika", new Point(535, 409)); // Kuzey Afrika'nın konumunu ekle
        territoryPositions.put("Mısır", new Point(644, 383)); // Mısır'ın konumunu ekle
        territoryPositions.put("Doğu Afrika", new Point(706, 464)); // Doğu Afrika'nın konumunu ekle
        territoryPositions.put("Kongo", new Point(643, 503)); // Kongo'nun konumunu ekle
        territoryPositions.put("Güney Afrika", new Point(655, 599)); // Güney Afrika'nın konumunu ekle
        territoryPositions.put("Madagaskar", new Point(761, 592)); // Madagaskar'ın konumunu ekle

        // ASYA (Yeşil Bölge)
        territoryPositions.put("Ural", new Point(818, 158)); // Ural'ın konumunu ekle
        territoryPositions.put("Sibirya", new Point(889, 110)); // Sibirya'nın konumunu ekle
        territoryPositions.put("Yakutsk", new Point(971, 94)); // Yakutsk'un konumunu ekle
        territoryPositions.put("Kamçatka", new Point(1072, 99)); // Kamçatka'nın konumunu ekle
        territoryPositions.put("Irkutsk", new Point(956, 182)); // Irkutsk'un konumunu ekle
        territoryPositions.put("Moğolistan", new Point(969, 237)); // Moğolistan'ın konumunu ekle
        territoryPositions.put("Çin", new Point(956, 308)); // Çin'in konumunu ekle
        territoryPositions.put("Japonya", new Point(1088, 250)); // Japonya'nın konumunu ekle
        territoryPositions.put("Afganistan", new Point(807, 254)); // Afganistan'ın konumunu ekle
        territoryPositions.put("Hindistan", new Point(879, 391)); // Hindistan'ın konumunu ekle
        territoryPositions.put("Orta Doğu", new Point(735, 364)); // Orta Doğu'nun konumunu ekle
        territoryPositions.put("Güneydoğu Asya", new Point(962, 385)); // Güneydoğu Asya'nın konumunu ekle

        // AVUSTRALYA (Mor Bölge)
        territoryPositions.put("Endonezya", new Point(985, 488)); // Endonezya'nın konumunu ekle
        territoryPositions.put("Yeni Gine", new Point(1084, 467)); // Yeni Gine'nin konumunu ekle
        territoryPositions.put("Batı Avustralya", new Point(1024, 580)); // Batı Avustralya'nın konumunu ekle
        territoryPositions.put("Doğu Avustralya", new Point(1136, 576)); // Doğu Avustralya'nın konumunu ekle
    }

    private void initializeContinentColors() { // Kıta renklerini başlatan metot
        continentColors.put("Avrupa", new Color(100, 149, 237, 80)); // Avrupa rengi: açık mavi
        continentColors.put("Afrika", new Color(255, 165, 0, 80)); // Afrika rengi: turuncu
        continentColors.put("Asya", new Color(144, 238, 144, 80)); // Asya rengi: açık yeşil
    }

    private void initializeUI() { // Kullanıcı arayüzünü başlatan metot
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE); // Varsayılan kapanma işlemini devre dışı bırak

        // Kapanma isteğini düzgün yönet
        addWindowListener(new WindowAdapter() { // Pencere olaylarını dinleyici ekle
            @Override
            public void windowClosing(WindowEvent e) { // Pencere kapanırken
                handleWindowClosing(); // Özel kapanma işleyicisini çağır
            }
        });

        try { // Harita resmini yükleme denemesi
            worldMap = ImageIO.read(getClass().getResourceAsStream("/risk_map.png")); // Harita dosyasını yükle
        } catch (IOException e) { // Dosya okuma hatası durumunda
            e.printStackTrace(); // Hata izini yazdır
        }

        setSize(1200, 700); // Pencere boyutunu ayarla
        setMinimumSize(new Dimension(900, 600)); // Minimum pencere boyutunu ayarla
        setLayout(new BorderLayout()); // Kenar düzeni yerleşimi kullan

        JPanel mainPanel = new JPanel(new BorderLayout()); // Ana panel oluştur
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Boş kenarlık ekle

        mapPanel = new JPanel() { // Harita paneli oluştur
            @Override
            protected void paintComponent(Graphics g) { // Çizim metodu
                super.paintComponent(g); // Üst sınıf metodunu çağır
                Graphics2D g2d = (Graphics2D) g; // 2D grafikler için dönüştür

                if (worldMap != null) { // Harita resmi yüklendiyse
                    g2d.drawImage(worldMap, 0, 0, getWidth(), getHeight(), null); // Harita resmini çiz
                }

                // Harita üstüne overlay'ler
                drawTerritories(g2d); // Bölgeleri çiz
            }
        };

        mapPanel.addMouseListener(new MouseAdapter() { // Fare olayları dinleyicisi ekle
            @Override
            public void mouseClicked(MouseEvent e) { // Fare tıklandığında
                int realX = e.getX() * 1200 / mapPanel.getWidth(); // Gerçek X koordinatını hesapla
                int realY = e.getY() * 700 / mapPanel.getHeight(); // Gerçek Y koordinatını hesapla
                handleMapClick(new Point(realX, realY)); // Harita tıklama işleyicisini çağır
            }
        });

        mainPanel.add(mapPanel, BorderLayout.CENTER); // Harita panelini merkeze ekle

        JPanel rightPanel = createRightPanel(); // Sağ paneli oluştur
        mainPanel.add(rightPanel, BorderLayout.EAST); // Sağ paneli doğuya ekle

        JPanel logPanel = createLogPanel(); // Log panelini oluştur
        mainPanel.add(logPanel, BorderLayout.SOUTH); // Log panelini güneye ekle

        add(mainPanel, BorderLayout.CENTER); // Ana paneli pencereye ekle

        createDiceDialog(); // Zar diyaloğunu oluştur
        JMenuBar menuBar = createMenuBar(); // Menü çubuğunu oluştur
        setJMenuBar(menuBar); // Menü çubuğunu ayarla
        setupKeyBindings(); // Klavye kısayollarını ayarla

        setLocationRelativeTo(null); // Pencereyi ekranın ortasına yerleştir
        setVisible(true); // Pencereyi görünür yap
    }

    /**
     * Pencere kapatma isteğini düzgün şekilde yönetir
     */
  private void handleWindowClosing() { // Pencere kapatma işlemini yöneten metot
    int response = JOptionPane.showConfirmDialog( // Onay diyaloğu göster
        this, // Üst pencere
        "Oyundan çıkmak istiyor musunuz?", // Mesaj
        "Çıkış Onayı", // Başlık
        JOptionPane.YES_NO_OPTION, // Evet/Hayır seçenekli diyalog
        JOptionPane.QUESTION_MESSAGE // Soru tipi mesaj
    );

    if (response == JOptionPane.YES_OPTION) { // Eğer kullanıcı Evet'e tıkladıysa
        if (network != null && network.isConnected()) { // Ağ bağlantısı varsa
            Message exitMsg = new Message("QUIT", Map.of("msg", "User quit")); // Çıkış mesajı oluştur
            network.sendMessage(exitMsg); // Çıkış mesajını gönder
        }

        cleanup(); // Kaynakları temizle

        // GUI güvenli kapatma
        SwingUtilities.invokeLater(() -> { // EDT üzerinde çalıştır
            dispose(); // Pencereyi yok et
            System.exit(0); // Uygulamayı sonlandır
        });
    }
}


    private void updateMap(String data) { // Haritayı güncelleyen metot
        territories.clear(); // Bölge listesini temizle
        String[] entries = data.split(";"); // Veriyi bölgelere ayır
        for (String entry : entries) { // Her bölge için
            String[] parts = entry.split(":"); // Bölge verilerini ayır
            if (parts.length >= 3) { // Yeterli veri varsa
                String name = parts[0]; // Bölge adı
                int owner = Integer.parseInt(parts[1]); // Sahip ID'si
                int troops = Integer.parseInt(parts[2]); // Asker sayısı

                Territory t = new Territory(name, owner, troops); // Yeni bölge nesnesi oluştur
                territories.put(name, t); // Bölge listesine ekle
            }
        }
        mapPanel.repaint(); // Haritayı yeniden çiz
    }

    /**
     * Oyun konsolu loglarını içeren alt paneli oluşturur
     */
    private JPanel createLogPanel() { // Log paneli oluşturan metot
        JPanel logPanel = new JPanel(new BorderLayout()); // Yeni panel oluştur
        logPanel.setBorder(BorderFactory.createTitledBorder("Oyun Konsolu")); // Başlıklı kenarlık ekle
        logPanel.setPreferredSize(new Dimension(0, 100)); // Tercih edilen boyutu ayarla

        gameLogArea = new JTextArea(); // Metin alanı oluştur
        gameLogArea.setEditable(false); // Düzenlemeyi devre dışı bırak
        gameLogArea.setFont(new Font("Monospaced", Font.PLAIN, 12)); // Yazı tipini ayarla
        gameLogArea.setLineWrap(true); // Satır kaydırmayı etkinleştir
        gameLogArea.setWrapStyleWord(true); // Sözcük bazında kaydırmayı etkinleştir

        JScrollPane scrollPane = new JScrollPane(gameLogArea); // Kaydırma paneli oluştur
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS); // Dikey kaydırma çubuğunu her zaman göster

        logPanel.add(scrollPane, BorderLayout.CENTER); // Kaydırma panelini merkeze ekle

        return logPanel; // Oluşturulan paneli döndür
    }

    /**
     * Klavye kısayollarını tanımlar (P, A, F, E, ESC)
     */
    private void setupKeyBindings() { // Klavye kısayollarını ayarlayan metot
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW); // Giriş haritasını al
        ActionMap actionMap = getRootPane().getActionMap(); // Eylem haritasını al

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0), "placeTroops"); // P tuşunu asker yerleştirme eylemine bağla
        actionMap.put("placeTroops", new AbstractAction() { // Asker yerleştirme eylemi oluştur
            @Override
            public void actionPerformed(ActionEvent e) { // Eylem gerçekleştiğinde
                if (placeTroopsButton.isEnabled()) { // Buton etkinse
                    handlePlaceTroops(); // Asker yerleştirme işleyicisini çağır
                }
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0), "attack"); // A tuşunu saldırı eylemine bağla
        actionMap.put("attack", new AbstractAction() { // Saldırı eylemi oluştur
            @Override
            public void actionPerformed(ActionEvent e) { // Eylem gerçekleştiğinde
                if (attackButton.isEnabled()) { // Buton etkinse
                    toggleAttackMode(); // Saldırı modu değiştirme işleyicisini çağır
                }
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0), "fortify"); // F tuşunu güçlendirme eylemine bağla
        actionMap.put("fortify", new AbstractAction() { // Güçlendirme eylemi oluştur
            @Override
            public void actionPerformed(ActionEvent e) { // Eylem gerçekleştiğinde
                if (fortifyButton.isEnabled()) { // Buton etkinse
                    toggleFortifyMode(); // Güçlendirme modu değiştirme işleyicisini çağır
                }
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, 0), "endTurn"); // E tuşunu tur bitirme eylemine bağla
        actionMap.put("endTurn", new AbstractAction() { // Tur bitirme eylemi oluştur
            @Override
            public void actionPerformed(ActionEvent e) { // Eylem gerçekleştiğinde
                if (endTurnButton.isEnabled()) { // Buton etkinse
                    endTurn(); // Tur bitirme işleyicisini çağır
                }
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clearSelection"); // ESC tuşunu seçim temizleme eylemine bağla
        actionMap.put("clearSelection", new AbstractAction() { // Seçim temizleme eylemi oluştur
            @Override
            public void actionPerformed(ActionEvent e) { // Eylem gerçekleştiğinde
                clearSelectionAndModes(); // Seçim ve modları temizleme işleyicisini çağır
            }
        });
    }

    private JMenuBar createMenuBar() { // Menü çubuğu oluşturan metot
        JMenuBar menuBar = new JMenuBar(); // Yeni menü çubuğu oluştur

        // Oyun menüsü
        JMenu gameMenu = new JMenu("Oyun"); // Oyun menüsü oluştur

        JMenuItem exitItem = new JMenuItem("Çıkış"); // Çıkış menü öğesi oluştur
        exitItem.addActionListener(e -> handleWindowClosing()); // Çıkış eylemini ekle

        gameMenu.add(exitItem); // Çıkış öğesini oyun menüsüne ekle

        JMenuItem rulesItem = new JMenuItem("Kurallar"); // Kurallar menü öğesi oluştur
        rulesItem.addActionListener(e -> { // Kurallar eylemini ekle
            JOptionPane.showMessageDialog(this, // Mesaj diyaloğu göster
                    """
            🎲 Risk Oyunu Kuralları:

            1. Her tur başında oyuncuya asker verilir.
            2. Oyuncu askerlerini kendi bölgelerine yerleştirir.
            3. Ardından saldırı yapabilir:
               - Saldırı için en az 2 asker gerekir.
               - Komşu düşman bölgelere saldırılabilir.
            4. Tur sonunda asker taşıması (güçlendirme) yapılabilir.
            5. Tüm bölgeleri ele geçiren oyuncu oyunu kazanır.

            Renkler:
            🟩 Yeşil: Komşu kendi bölgen
            🟥 Kırmızı: Komşu düşman bölge
            🟨 Sarı: Seçili bölge
            """, // Kurallar metni
                    "Oyun Kuralları", JOptionPane.INFORMATION_MESSAGE); // Başlık ve mesaj tipi
        });

        // Yardım menüsü
        JMenu helpMenu = new JMenu("Yardım"); // Yardım menüsü oluştur

        JMenuItem aboutItem = new JMenuItem("Hakkında"); // Hakkında menü öğesi oluştur
        aboutItem.addActionListener(e -> { // Hakkında eylemini ekle
            JOptionPane.showMessageDialog(this, // Mesaj diyaloğu göster
                    "Risk Oyunu v1.0\nGeliştirici: M. Noor", "Hakkında", JOptionPane.INFORMATION_MESSAGE); // Mesaj, başlık ve mesaj tipi
        });

        JMenuItem shortcutItem = new JMenuItem("Kısayollar"); // Kısayollar menü öğesi oluştur
        shortcutItem.addActionListener(e -> { // Kısayollar eylemini ekle
            JOptionPane.showMessageDialog(this, // Mesaj diyaloğu göster
                    """
                P - Asker Yerleştir
                A - Saldırı
                F - Güçlendir
                E - Sırayı Bitir
                Esc - Seçimi Temizle
                """, "Klavye Kısayolları", JOptionPane.INFORMATION_MESSAGE); // Mesaj, başlık ve mesaj tipi

           
        });
helpMenu.add(rulesItem); // Kurallar öğesini yardım menüsüne ekle

        helpMenu.add(shortcutItem); // Kısayollar öğesini yardım menüsüne ekle
        helpMenu.add(aboutItem); // Hakkında öğesini yardım menüsüne ekle

        menuBar.add(gameMenu); // Oyun menüsünü menü çubuğuna ekle
        menuBar.add(helpMenu); // Yardım menüsünü menü çubuğuna ekle

        return menuBar; // Oluşturulan menü çubuğunu döndür
    }

    private void createDiceDialog() { // Zar diyaloğu oluşturan metot
        diceDialog = new JDialog(this, "Zar Atılıyor", true); // Yeni modal diyalog oluştur
        diceDialog.setSize(400, 250); // Boyutu ayarla
        diceDialog.setLayout(new BorderLayout()); // Kenar düzeni yerleşimi kullan

        JPanel mainPanel = new JPanel(new BorderLayout()); // Ana panel oluştur
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Boş kenarlık ekle

        JPanel dicePanel = new JPanel(new GridLayout(1, 2, 20, 0)); // Zar paneli oluştur

        attackerDicePanel = new JPanel(new FlowLayout(FlowLayout.CENTER)); // Saldıran zarları paneli oluştur
        attackerDicePanel.setBorder(BorderFactory.createTitledBorder("Saldıran")); // Başlıklı kenarlık ekle

        defenderDicePanel = new JPanel(new FlowLayout(FlowLayout.CENTER)); // Savunan zarları paneli oluştur
        defenderDicePanel.setBorder(BorderFactory.createTitledBorder("Savunan")); // Başlıklı kenarlık ekle

        dicePanel.add(attackerDicePanel); // Saldıran zarları panelini ekle
        dicePanel.add(defenderDicePanel); // Savunan zarları panelini ekle

        JLabel resultLabel = new JLabel("", SwingConstants.CENTER); // Sonuç etiketi oluştur
        resultLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0)); // Boş kenarlık ekle

        mainPanel.add(dicePanel, BorderLayout.CENTER); // Zar panelini merkeze ekle
        mainPanel.add(resultLabel, BorderLayout.SOUTH); // Sonuç etiketini güneye ekle

        diceDialog.add(mainPanel); // Ana paneli diyaloğa ekle
        diceDialog.setLocationRelativeTo(this); // Pencereye göre ortala
    }

    private void drawTerritories(Graphics2D g2d) { // Bölgeleri çizen metot
        for (Map.Entry<String, Territory> entry : territories.entrySet()) { // Her bölge için
            String name = entry.getKey(); // Bölge adı
            Territory t = entry.getValue(); // Bölge nesnesi
            if (territoryPositions.containsKey(name)) { // Konum tanımlıysa
                drawTerritory(g2d, name, t); // Bölgeyi çiz
            }
        }
    }

    private void drawDiceResults(List<Integer> attacker, List<Integer> defender) { // Zar sonuçlarını çizen metot
        attackerDicePanel.removeAll(); // Saldıran zarları panelini temizle
        defenderDicePanel.removeAll(); // Savunan zarları panelini temizle

        for (int val : attacker) { // Her saldıran zarı için
            JLabel die = new JLabel(String.valueOf(val)); // Zar etiketi oluştur
            die.setFont(new Font("Arial", Font.BOLD, 24)); // Yazı tipini ayarla
            die.setBorder(new EmptyBorder(10, 10, 10, 10)); // Boş kenarlık ekle
            attackerDicePanel.add(die); // Etiketi saldıran zarları paneline ekle
        }

        for (int val : defender) { // Her savunan zarı için
            JLabel die = new JLabel(String.valueOf(val)); // Zar etiketi oluştur
            die.setFont(new Font("Arial", Font.BOLD, 24)); // Yazı tipini ayarla
            die.setBorder(new EmptyBorder(10, 10, 10, 10)); // Boş kenarlık ekle
            defenderDicePanel.add(die); // Etiketi savunan zarları paneline ekle
        }

        diceDialog.revalidate(); // Diyaloğu yeniden doğrula
        diceDialog.repaint(); // Diyaloğu yeniden çiz
        diceDialog.setVisible(true); // Diyaloğu görünür yap
    }

    private void drawTerritory(Graphics2D g2d, String name, Territory t) { // Bölge çizen metot
        Point pos = territoryPositions.get(name); // Bölge konumunu al
        if (pos == null) { // Konum yoksa
            return; // Metoddan çık
        }

        int panelWidth = mapPanel.getWidth(); // Panel genişliğini al
        int panelHeight = mapPanel.getHeight(); // Panel yüksekliğini al

        int scaledX = pos.x * panelWidth / MAP_WIDTH; // Ölçeklendirilmiş X koordinatı
        int scaledY = pos.y * panelHeight / MAP_HEIGHT; // Ölçeklendirilmiş Y koordinatı
        int radius = 20 * panelWidth / MAP_WIDTH; // Ölçeklendirilmiş yarıçap

        boolean isSelected = name.equals(selectedTerritory); // Bölge seçili mi?
        boolean isTarget = highlightedTargets.contains(name); // Bölge vurgulanmış hedef mi?

        Color background = getPlayerColor(t.getOwner()); // Sahibin rengini al
        g2d.setColor(background); // Rengi ayarla
        g2d.fillOval(scaledX - radius, scaledY - radius, radius * 2, radius * 2); // Daireyi doldur

        if (isSelected) { // Eğer seçiliyse
            g2d.setColor(SELECTED_COLOR); // Seçim rengini ayarla
            g2d.fillOval(scaledX - radius, scaledY - radius, radius * 2, radius * 2); // Daireyi doldur
        } else if (isTarget) { // Eğer hedefse
            // Seçilen bölgenin sahibine göre komşu bölgeleri farklı renklerde göster
            Territory selectedT = selectedTerritory != null ? territories.get(selectedTerritory) : null; // Seçili bölgeyi al
            if (selectedT != null) { // Seçili bölge varsa
                // Seçilen bölge bizimse ve komşu bölge de bizimse yeşil, değilse kırmızımsı
                if (selectedT.getOwner() == playerId) { // Seçili bölge bizimse
                    // Komşu bölgenin sahibine göre renk belirle
                    if (t.getOwner() == playerId) { // Hedef bölge de bizimse
                        g2d.setColor(FRIENDLY_TARGET_COLOR); // Dost hedef rengi
                    } else { // Hedef bölge rakibinse
                        g2d.setColor(ENEMY_TARGET_COLOR); // Düşman hedef rengi
                    }
                } else { // Seçili bölge rakibinse
                    // Seçilen bölge rakibinse ve komşu bölge bizimse yeşil, değilse kırmızımsı
                    if (t.getOwner() == playerId) { // Hedef bölge bizimse
                        g2d.setColor(FRIENDLY_TARGET_COLOR); // Dost hedef rengi
                    } else { // Hedef bölge de rakibinse
                        g2d.setColor(ENEMY_TARGET_COLOR); // Düşman hedef rengi
                    }
                }
            } else { // Seçili bölge yoksa
                g2d.setColor(TARGET_COLOR); // Varsayılan hedef rengi
            }
            g2d.fillOval(scaledX - radius, scaledY - radius, radius * 2, radius * 2); // Daireyi doldur
        }

        g2d.setColor(Color.BLACK); // Siyah rengi ayarla
        g2d.setStroke(new BasicStroke(2.0f)); // Çizgi kalınlığını ayarla
        g2d.drawOval(scaledX - radius, scaledY - radius, radius * 2, radius * 2); // Daire çiz

        g2d.setColor(Color.BLACK); // Siyah rengi ayarla
        g2d.setFont(new Font("Arial", Font.BOLD, 12)); // Yazı tipini ayarla
        FontMetrics fm = g2d.getFontMetrics(); // Yazı metriklerini al
        int textWidth = fm.stringWidth(name); // Metin genişliğini hesapla
        g2d.drawString(name, scaledX - textWidth / 2, scaledY - radius - 5); // Bölge adını çiz

        g2d.setFont(new Font("Arial", Font.BOLD, 16)); // Yazı tipini ayarla
        fm = g2d.getFontMetrics(); // Yazı metriklerini al
        String troops = String.valueOf(t.getTroops()); // Asker sayısını string'e çevir
        int tw = fm.stringWidth(troops); // Metin genişliğini hesapla
        g2d.drawString(troops, scaledX - tw / 2, scaledY + 5); // Asker sayısını çiz
    }

    private void handleMapClick(Point point) { // Harita tıklamasını işleyen metot
        if (gameOver) { // Oyun bittiyse
            return; // Metoddan çık
        }

        String clicked = findClickedTerritory(point); // Tıklanan bölgeyi bul
        if (clicked == null) { // Tıklanan bölge yoksa
            return; // Metoddan çık
        }

        if (currentTurn != playerId) { // Sıra bizde değilse
            logToGameConsole("Sıra sizde değil."); // Bilgi mesajı logla
            return; // Metoddan çık
        }

        int troopsLeft = playerTroopsToPlace.getOrDefault(playerId, 0); // Kalan asker sayısını al
        if (troopsLeft > 0) { // Yerleştirilecek asker varsa
            Territory t = territories.get(clicked); // Tıklanan bölgeyi al
            if (t != null && t.getOwner() == playerId) { // Tıklanan bölge bizimse
                String input = JOptionPane.showInputDialog(this, // Giriş diyaloğu göster
                        "Kaç asker yerleştirilsin? (max " + troopsLeft + ")", "Asker Yerleştir", JOptionPane.QUESTION_MESSAGE); // Mesaj, başlık, mesaj tipi
                try { // Girişi dönüştürme denemesi
                    int num = Integer.parseInt(input); // Girişi sayıya çevir
                    if (num > 0 && num <= troopsLeft) { // Geçerli bir değerse
                        Message placeTroopsMsg = new Message("PLACE_TROOPS", Map.of( // Asker yerleştirme mesajı oluştur
                                "territory", clicked, // Bölge adı
                                "troops", String.valueOf(num) // Asker sayısı
                        ));
                        network.sendMessage(placeTroopsMsg); // Mesajı gönder
                    } else { // Geçersiz bir değerse
                        logToGameConsole("Geçersiz asker sayısı."); // Hata mesajı logla
                    }
                } catch (NumberFormatException e) { // Sayıya çevirme hatası durumunda
                    logToGameConsole("Geçersiz giriş."); // Hata mesajı logla
                }
            } else { // Tıklanan bölge bizim değilse
                logToGameConsole("Kendi bölgenizi seçmelisiniz."); // Hata mesajı logla
            }
            return; // Metoddan çık
        }

        if (attackMode) { // Saldırı modundaysak
            handleAttackModeClick(clicked); // Saldırı tıklamasını işle
            return; // Metoddan çık
        }

        if (fortifyMode) { // Güçlendirme modundaysak
            handleFortifyModeClick(clicked); // Güçlendirme tıklamasını işle
            return; // Metoddan çık
        }

        Territory t = territories.get(clicked); // Tıklanan bölgeyi al
        if (t != null && t.getOwner() == playerId) { // Tıklanan bölge bizimse
            setSelectedTerritory(clicked); // Bölgeyi seç
            statusLabel.setText(clicked + " seçildi."); // Durum etiketini güncelle
        }
    }

    private String findClickedTerritory(Point point) { // Tıklanan bölgeyi bulan metot
        for (Map.Entry<String, Point> entry : territoryPositions.entrySet()) { // Her bölge için
            Point p = entry.getValue(); // Bölge konumunu al
            if (point.distance(p) <= 35) { // Tıklama noktası bölgeye yeterince yakınsa
                return entry.getKey(); // Bölge adını döndür
            }
        }
        return null; // Tıklanan bölge yoksa null döndür
    }

    private void clearSelectionAndModes() { // Seçimleri ve modları temizleyen metot
        removeHighlights(); // Vurgulamaları kaldır
        statusLabel.setText("Seçim temizlendi."); // Durum etiketini güncelle
        logToGameConsole("Seçimler sıfırlandı."); // Bilgi mesajı logla
        mapPanel.repaint(); // Haritayı yeniden çiz
    }

    private void handleAttackModeClick(String clicked) { // Saldırı modunda tıklamayı işleyen metot
        Territory t = territories.get(clicked); // Tıklanan bölgeyi al
        if (selectedTerritory == null) { // Henüz saldıran bölge seçilmediyse
            if (t.getOwner() == playerId && t.getTroops() > 1) { // Bölge bizimse ve yeterli asker varsa
                setSelectedTerritory(clicked); // Bölgeyi seç
                statusLabel.setText("Hedef seçin"); // Durum etiketini güncelle
                mapPanel.repaint(); // Haritayı yeniden çiz
            } else { // Bölge bizim değilse veya yeterli asker yoksa
                logToGameConsole("Saldırmak için en az 2 askerli bir bölge seçin."); // Hata mesajı logla
            }
        } else { // Saldıran bölge seçildiyse
            if (t.getOwner() != playerId && areNeighbors(selectedTerritory, clicked)) { // Hedef düşman bölgesi ve komşuysa
                targetTerritory = clicked; // Hedef bölgeyi ayarla
                String input = JOptionPane.showInputDialog("Kaç zarla saldırı yapılacak? (1-3)"); // Zar sayısı sor
                try { // Girişi dönüştürme denemesi
                    int dice = Integer.parseInt(input); // Girişi sayıya çevir
                    Message attackMsg = new Message("ATTACK", Map.of( // Saldırı mesajı oluştur
                            "from", selectedTerritory, // Saldıran bölge
                            "to", targetTerritory, // Hedef bölge
                            "dice", String.valueOf(dice) // Zar sayısı
                    ));
                    network.sendMessage(attackMsg); // Mesajı gönder
                } catch (NumberFormatException e) { // Sayıya çevirme hatası durumunda
                    logToGameConsole("Geçersiz zar sayısı."); // Hata mesajı logla
                }
            } else { // Hedef geçersizse
                logToGameConsole("Geçersiz hedef."); // Hata mesajı logla
            }
        }
    }

    private void handleFortifyModeClick(String clicked) { // Güçlendirme modunda tıklamayı işleyen metot
        Territory t = territories.get(clicked); // Tıklanan bölgeyi al
        if (selectedTerritory == null) { // Henüz kaynak bölge seçilmediyse
            if (t.getOwner() == playerId && t.getTroops() > 1) { // Bölge bizimse ve yeterli asker varsa
                selectedTerritory = clicked; // Bölgeyi seç
                statusLabel.setText("Hedef bölge seçin."); // Durum etiketini güncelle
                mapPanel.repaint(); // Haritayı yeniden çiz
            } else { // Bölge bizim değilse veya yeterli asker yoksa
                logToGameConsole("Güçlendirmek için yeterli askeriniz yok."); // Hata mesajı logla
            }
        } else { // Kaynak bölge seçildiyse
            if (t.getOwner() == playerId && areConnected(selectedTerritory, clicked)) { // Hedef bizim bölgemiz ve bağlantılıysa
                targetTerritory = clicked; // Hedef bölgeyi ayarla
                Territory from = territories.get(selectedTerritory); // Kaynak bölgeyi al
                int max = from.getTroops() - 1; // Maksimum taşınabilecek asker sayısı
                String input = JOptionPane.showInputDialog("Kaç asker taşınsın? (1-" + max + ")"); // Asker sayısı sor
                try { // Girişi dönüştürme denemesi
                    int num = Integer.parseInt(input); // Girişi sayıya çevir
                    Message fortifyMsg = new Message("FORTIFY", Map.of( // Güçlendirme mesajı oluştur
                            "from", selectedTerritory, // Kaynak bölge
                            "to", targetTerritory, // Hedef bölge
                            "troops", String.valueOf(num) // Asker sayısı
                    ));
                    network.sendMessage(fortifyMsg); // Mesajı gönder
                } catch (NumberFormatException e) { // Sayıya çevirme hatası durumunda
                    logToGameConsole("Geçersiz asker sayısı."); // Hata mesajı logla
                }
            } else { // Hedef geçersizse
                logToGameConsole("Sadece kendi bölgeleriniz arasında geçiş yapabilirsiniz."); // Hata mesajı logla
            }
        }
    }

  private boolean areNeighbors(String a, String b) { // İki bölgenin komşu olup olmadığını kontrol eden metot
        return adjacencyMap.getOrDefault(a, Collections.emptySet()).contains(b); // a bölgesinin komşuları arasında b var mı?
    }

    private boolean areConnected(String a, String b) { // İki bölgenin bağlantılı olup olmadığını kontrol eden metot
        // Şimdilik sadece doğrudan komşuluk
        return areNeighbors(a, b); // Komşuluk kontrolünü kullan
    }

    private void handleAdjacencyMessage(String data) { // Komşuluk verilerini işleyen metot
        adjacencyMap.clear(); // Komşuluk haritasını temizle
        String[] parts = data.split(";"); // Veriyi bölgelere ayır
        for (String entry : parts) { // Her bölge için
            if (entry.isBlank()) { // Boş girişi atla
                continue;
            }
            String[] tokens = entry.split(":"); // Bölge ve komşularını ayır
            if (tokens.length != 2) { // Geçersiz formatı atla
                continue;
            }
            String territory = tokens[0]; // Bölge adı
            Set<String> neighbors = new HashSet<>(List.of(tokens[1].split(","))); // Komşu bölgeler kümesi
            adjacencyMap.put(territory, neighbors); // Komşuluk haritasına ekle
        }
        logToGameConsole("Komşuluk verileri güncellendi."); // Bilgi mesajı logla
    }

    /**
     * Sunucudan gelen mesajları işler (Message nesnesi)
     */
    private void processMessage(Message message) { // Sunucudan gelen mesajları işleyen metot
        String type = message.type; // Mesaj tipini al
        logToGameConsole("Alındı: " + message); // Mesajı logla

        switch (type) { // Mesaj tipine göre işlem yap
            case "RESTART_PROMPT" -> { // Yeniden başlatma sorgusu
                int result = JOptionPane.showConfirmDialog(this, // Onay diyaloğu göster
                        "Rakibiniz oyundan ayrıldı. Yeni bir rakiple oynamak ister misiniz?", // Mesaj
                        "Oyun Bitti", JOptionPane.YES_NO_OPTION); // Başlık ve seçenekler

                if (result == JOptionPane.YES_OPTION) { // Evet seçildiyse
                    Message restartMsg = new Message("RESTART", Collections.emptyMap()); // Yeniden başlatma mesajı oluştur
                    network.sendMessage(restartMsg); // Mesajı gönder
                } else { // Hayır seçildiyse
                    Message declineMsg = new Message("RESTART_DECLINE", Collections.emptyMap()); // Reddetme mesajı oluştur
                    network.sendMessage(declineMsg); // Mesajı gönder
                    JOptionPane.showMessageDialog(this, "Oyun kapatılıyor. Görüşmek üzere!"); // Bilgi mesajı göster
                    cleanup(); // Kaynakları temizle
                    dispose(); // Pencereyi kapat
                }
            }

            case "INIT" -> { // Başlatma mesajı
                playerId = Integer.parseInt(message.get("playerId")); // Oyuncu ID'sini ayarla
                String name = message.get("name"); // İsmi al
                if (name == null || name.isEmpty()) { // İsim yoksa
                    name = "Oyuncu " + playerId; // Varsayılan isim oluştur
                }

                playerNames.put(playerId, name); // İsmi kaydet
                logToGameConsole("Oyuncu kimliğiniz: " + playerId + " (" + name + ")"); // Bilgi mesajı logla

                Color c = getPlayerColor(playerId); // Oyuncu rengini al
                String colorName = getColorName(c); // Renk adını al
                playerColorLabel.setText("Renginiz: " + colorName); // Renk etiketini güncelle
                playerColorLabel.setForeground(c); // Etiket rengini ayarla
            }

            case "MAP" -> // Harita güncellemesi
                updateMap(message.get("data")); // Haritayı güncelle

            case "TURN" -> { // Tur değişimi
                int turn = Integer.parseInt(message.get("playerId")); // Sıradaki oyuncu ID'sini al
                int troops = Integer.parseInt(message.get("troops")); // Asker sayısını al
                String name = message.get("name"); // İsmi al
                if (name == null || name.isEmpty()) { // İsim yoksa
                    name = "Oyuncu " + turn; // Varsayılan isim oluştur
                }

                currentTurn = turn; // Mevcut turu güncelle
                removeHighlights(); // Vurgulamaları kaldır
                mapPanel.repaint(); // Haritayı yeniden çiz
                playerTroopsToPlace.put(turn, troops); // Yerleştirilecek asker sayısını kaydet

                if (turn == playerId) { // Sıra bizdeyse
                    logToGameConsole("Sıra sizde! " + troops + " asker yerleştirin."); // Bilgi mesajı logla
                    statusLabel.setText("Sıra sizde!"); // Durum etiketini güncelle
                    enableButtons(true); // Butonları etkinleştir
                    startTurnTimer(); // Zamanlayıcıyı başlat
                } else { // Sıra rakipteyse
                    logToGameConsole("Rakibin sırası: " + name); // Bilgi mesajı logla
                    statusLabel.setText("Sıra: " + name); // Durum etiketini güncelle
                    enableButtons(false); // Butonları devre dışı bırak
                }

                troopsLeftLabel.setText("Kalan Asker: " + troops); // Kalan asker etiketini güncelle
                selectedTerritory = null; // Seçili bölgeyi temizle
                targetTerritory = null; // Hedef bölgeyi temizle
                mapPanel.repaint(); // Haritayı yeniden çiz
            }

            case "PLACE_RESULT" -> { // Asker yerleştirme sonucu
                String territory = message.get("territory"); // Bölgeyi al
                int troops = Integer.parseInt(message.get("troops")); // Asker sayısını al
                int remaining = Integer.parseInt(message.get("remaining")); // Kalan asker sayısını al

                Territory t = territories.get(territory); // Bölge nesnesini al
                if (t != null) { // Bölge varsa
                    t.setTroops(troops); // Asker sayısını güncelle
                }

                playerTroopsToPlace.put(playerId, remaining); // Kalan asker sayısını güncelle
                troopsLeftLabel.setText("Kalan Asker: " + remaining); // Kalan asker etiketini güncelle
                mapPanel.repaint(); // Haritayı yeniden çiz

                if (remaining == 0 && currentTurn == playerId) { // Asker kalmadıysa ve sıra bizdeyse
                    enableButtons(true); // Butonları etkinleştir
                }
            }

            case "ATTACK_RESULT" -> { // Saldırı sonucu
                String from = message.get("from"); // Saldıran bölgeyi al
                String to = message.get("to"); // Hedef bölgeyi al
                int attackerLoss = Integer.parseInt(message.get("attackerLoss")); // Saldıran kaybını al
                int defenderLoss = Integer.parseInt(message.get("defenderLoss")); // Savunan kaybını al

                Territory attacker = territories.get(from); // Saldıran bölge nesnesini al
                Territory defender = territories.get(to); // Savunan bölge nesnesini al

                if (attacker != null) { // Saldıran bölge varsa
                    attacker.removeTroops(attackerLoss); // Asker kaybını uygula
                }
                if (defender != null) { // Savunan bölge varsa
                    defender.removeTroops(defenderLoss); // Asker kaybını uygula
                }

                if (defender.getTroops() <= 0 && attacker != null) { // Savunan bölge ele geçirildiyse
                    defender.setOwner(attacker.getOwner()); // Sahip değiştir
                    defender.setTroops(1); // 1 asker yerleştir
                    attacker.removeTroops(1); // Saldıran bölgeden asker azalt
                    logToGameConsole(to + " ele geçirildi!"); // Bilgi mesajı logla
                }

                removeHighlights(); // Vurgulamaları kaldır
                mapPanel.repaint(); // Haritayı yeniden çiz
            }

            case "FORTIFY_RESULT" -> { // Güçlendirme sonucu
                String from = message.get("from"); // Kaynak bölgeyi al
                String to = message.get("to"); // Hedef bölgeyi al
                int moved = Integer.parseInt(message.get("troops")); // Taşınan asker sayısını al

                Territory src = territories.get(from); // Kaynak bölge nesnesini al
                Territory dst = territories.get(to); // Hedef bölge nesnesini al
                if (src != null) { // Kaynak bölge varsa
                    src.removeTroops(moved); // Asker sayısını azalt
                }
                if (dst != null) { // Hedef bölge varsa
                    dst.addTroops(moved); // Asker sayısını artır
                }

                removeHighlights(); // Vurgulamaları kaldır
                mapPanel.repaint(); // Haritayı yeniden çiz
            }

            case "GAME_OVER" -> { // Oyun sonu
                gameOver = true; // Oyun bitti bayrağını ayarla
                removeHighlights(); // Vurgulamaları kaldır
                mapPanel.repaint(); // Haritayı yeniden çiz

                try { // Kazananı belirleme denemesi
                    int winnerId = Integer.parseInt(message.get("winnerId")); // Kazanan ID'sini al

                    String winMessage = (winnerId == playerId) // Kazanan mesajını oluştur
                            ? "Tebrikler, kazandınız!" // Biz kazandıysak
                            : "Oyunu kaybettiniz. Kazanan: " + playerNames.get(winnerId); // Rakip kazandıysa

                    int choice = JOptionPane.showConfirmDialog(this, // Onay diyaloğu göster
                            winMessage + "\nYeniden başlatılsın mı?", // Mesaj
                            "Oyun Bitti", JOptionPane.YES_NO_OPTION); // Başlık ve seçenekler

                    if (choice == JOptionPane.YES_OPTION) { // Evet seçildiyse
                        Message restartMsg = new Message("RESTART", Collections.emptyMap()); // Yeniden başlatma mesajı oluştur
                        network.sendMessage(restartMsg); // Mesajı gönder
                        logToGameConsole("Yeniden başlatma istendi."); // Bilgi mesajı logla
                    } else { // Hayır seçildiyse
                        Message declineMsg = new Message("RESTART_DECLINE", Collections.emptyMap()); // Reddetme mesajı oluştur
                        network.sendMessage(declineMsg); // Mesajı gönder
                        logToGameConsole("Yeniden başlatma isteğini reddettiniz."); // Bilgi mesajı logla
                    }

                } catch (NumberFormatException e) { // Sayıya çevirme hatası durumunda
                    // Eğer gelen veri sayı değilse, mesaj olarak göster
                    JOptionPane.showMessageDialog(this, message.get("msg"), "Oyun Bitti", JOptionPane.INFORMATION_MESSAGE); // Mesaj göster
                    Message declineMsg = new Message("RESTART_DECLINE", Collections.emptyMap()); // Reddetme mesajı oluştur
                    network.sendMessage(declineMsg); // Mesajı gönder
                }

                statusLabel.setText("Oyun bitti."); // Durum etiketini güncelle
                enableButtons(false); // Butonları devre dışı bırak
            }

            case "ADJACENCY" -> // Komşuluk verisi
                handleAdjacencyMessage(message.get("data")); // Komşuluk verilerini işle

            case "ERROR" -> // Hata mesajı
                logToGameConsole("Hata: " + message.get("msg")); // Hata mesajını logla

            case "INFO" -> { // Bilgi mesajı
                String info = message.get("msg"); // Bilgiyi al
                logToGameConsole(info); // Bilgiyi logla
                if (info.contains("Yeni bir rakip bekleniyor")) { // Eğer yeni rakip bekleniyorsa
                    statusLabel.setText("Yeni bir rakip bekleniyor..."); // Durum etiketini güncelle
                    enableButtons(false); // Butonları devre dışı bırak
                }

                if (info.contains("Diğer oyuncudan yeniden başlatma isteği")) { // Eğer yeniden başlatma isteği varsa
                    int answer = JOptionPane.showConfirmDialog(this, // Onay diyaloğu göster
                            "Rakip oyunu yeniden başlatmak istiyor. Kabul ediyor musunuz?", // Mesaj
                            "Yeniden Başlatma İsteği", JOptionPane.YES_NO_OPTION); // Başlık ve seçenekler

                    if (answer == JOptionPane.YES_OPTION) { // Evet seçildiyse
                        Message restartMsg = new Message("RESTART", Collections.emptyMap()); // Yeniden başlatma mesajı oluştur
                        network.sendMessage(restartMsg); // Mesajı gönder
                    } else { // Hayır seçildiyse
                        Message declineMsg = new Message("RESTART_DECLINE", Collections.emptyMap()); // Reddetme mesajı oluştur
                        network.sendMessage(declineMsg); // Mesajı gönder
                        logToGameConsole("Yeniden başlatma isteğini reddettiniz."); // Bilgi mesajı logla
                    }
                }
            }

            case "EXIT" -> { // Çıkış mesajı
                JOptionPane.showMessageDialog(this, // Mesaj diyaloğu göster
                        "Oyun kapatılıyor. Görüşmek üzere!", // Mesaj
                        "Çıkış", JOptionPane.INFORMATION_MESSAGE); // Başlık ve mesaj tipi
                cleanup(); // Kaynakları temizle
                dispose(); // Pencereyi kapat
            }

            case "DISCONNECT" -> { // Bağlantı kesme mesajı
                JOptionPane.showMessageDialog(this, // Mesaj diyaloğu göster
                        message.get("msg"), // Mesaj
                        "Bağlantı Kesildi", JOptionPane.WARNING_MESSAGE); // Başlık ve mesaj tipi
                cleanup(); // Kaynakları temizle
                dispose(); // Pencereyi kapat
            }

            default -> // Bilinmeyen mesaj tipi
                logToGameConsole("Bilinmeyen komut: " + type); // Bilgi mesajı logla
        }
    }

    private String getColorName(Color c) { // Renk adını döndüren metot
        if (c.equals(new Color(220, 20, 60))) { // Kırmızı ise
            return "Kırmızı";
        }
        if (c.equals(new Color(30, 144, 255))) { // Mavi ise
            return "Mavi";
        }
        return "Bilinmeyen"; // Diğer durumlar
    }

    private void enableButtons(boolean enable) { // Butonları etkinleştiren/devre dışı bırakan metot
        if (enable && currentTurn != playerId) { // Etkinleştirme istendi ama sıra bizde değilse
            enable = false; // Etkinleştirme
        }

        int troops = playerTroopsToPlace.getOrDefault(playerId, 0); // Kalan asker sayısını al
        placeTroopsButton.setEnabled(enable && troops > 0); // Asker yerleştir butonunu güncelle
        attackButton.setEnabled(enable && troops == 0); // Saldırı butonunu güncelle
        fortifyButton.setEnabled(enable && troops == 0); // Güçlendirme butonunu güncelle
        endTurnButton.setEnabled(enable && troops == 0); // Sıra bitirme butonunu güncelle
    }

    /**
     * Vurgulamaları kaldırır ve seçimleri sıfırlar
     */
    private void removeHighlights() { // Vurgulamaları ve seçimleri sıfırlayan metot
        selectedTerritory = null; // Seçili bölgeyi temizle
        targetTerritory = null; // Hedef bölgeyi temizle
        attackMode = false; // Saldırı modunu kapat
        fortifyMode = false; // Güçlendirme modunu kapat
        highlightedTargets.clear(); // Vurgulanan hedefleri temizle
    }

    /**
     * Kaynakları temizler ve bağlantıyı kapatır
     */
    private void cleanup() { // Kaynakları temizleyen metot
        stopTurnTimer(); // Zamanlayıcıyı durdur
        if (network != null) { // Ağ bağlantısı varsa
            network.close(); // Bağlantıyı kapat
        }
    }

 @Override
public void dispose() { // Pencereyi yok eden metot (override)
    cleanup(); // Kaynakları temizle
    SwingUtilities.invokeLater(() -> { // EDT üzerinde çalıştır
        try {
            super.dispose(); // Üst sınıf metodunu çağır
        } catch (Exception e) { // Hata durumunda
            System.err.println("Pencere kapatılırken hata oluştu: " + e.getMessage()); // Hata mesajı yazdır
        }
    });
}


    public static void main(String[] args) { // Ana metot
        SwingUtilities.invokeLater(() -> { // EDT üzerinde çalıştır
            String ip = "127.0.0.1"; // veya gerçek sunucu IP'si
            if (ip == null || ip.isBlank()) { // IP boşsa
                ip = "127.0.0.1"; // Localhost kullan
            }

            String playerName = null; // Oyuncu adı
            boolean isValidName = false; // Geçerli isim bayrağı

            while (!isValidName) { // Geçerli isim alana kadar
                playerName = JOptionPane.showInputDialog("Oyuncu adınızı girin:"); // İsim sor
                if (playerName != null && !playerName.trim().isEmpty()) { // İsim geçerliyse
                    isValidName = true; // Geçerli isim bayrağını ayarla
                } else { // İsim geçerli değilse
                    JOptionPane.showMessageDialog(null, "Lütfen geçerli bir isim girin!", // Hata mesajı göster
                            "Hata", JOptionPane.ERROR_MESSAGE); // Başlık ve mesaj tipi
                }
            }

            new RiskClient(ip, 9090, playerName); // Yeni Risk istemcisi oluştur
        });
    }
} 