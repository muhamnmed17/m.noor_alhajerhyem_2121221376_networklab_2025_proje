package client;

import game.GameState;
import game.Territory;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.border.EmptyBorder;

/**
 * GameUI sınıfının yardımcı metodlarını içeren sınıf
 * Bu sınıf static metodlar ile UI işlemlerini destekler
 */
public class GameUIHelper {
    
    // Zamanlayıcı metodları
    
    /**
     * Oyuncu turunda zamanlayıcıyı başlatan metod
     *  timerLabel Zamanlayıcı gösterilecek etiket
     * timerRef Timer referansını tutan dizi
     * onTimeOut Süre dolduğunda çalışacak işlem
     */
    public static void startTurnTimer(JLabel timerLabel, Timer[] timerRef, Runnable onTimeOut) {
        stopTurnTimer(timerRef); // Önceki zamanlayıcıyı durdur
        final int[] secondsLeft = {60}; // Kalan saniye sayısını 60'a ayarla
        timerLabel.setText("Süre: 60 sn"); // Etiket metnini güncelle

        timerRef[0] = new Timer(); // Yeni zamanlayıcı oluştur
        timerRef[0].scheduleAtFixedRate(new TimerTask() { // Her saniye çalışacak görev planla
            @Override
            public void run() { // Görev çalıştırma metodu
                SwingUtilities.invokeLater(() -> { // EDT üzerinde güvenli çalıştır
                    secondsLeft[0]--; // Kalan saniyeyi bir azalt
                    timerLabel.setText("Süre: " + secondsLeft[0] + " sn"); // Etiketi güncelle

                    if (secondsLeft[0] <= 0) { // Süre dolduysa
                        stopTurnTimer(timerRef); // Zamanlayıcıyı durdur
                        onTimeOut.run(); // Zaman dolma işlemini çalıştır
                    }
                });
            }
        }, 1000, 1000); // İlk çalışma: 1 saniye sonra, tekrar aralığı: 1 saniye
    }

    /**
     * Aktif zamanlayıcıyı durduran metod
     *  timerRef Timer referansını tutan dizi
     */
    public static void stopTurnTimer(Timer[] timerRef) {
        if (timerRef[0] != null) { // Zamanlayıcı mevcutsa
            timerRef[0].cancel(); // Zamanlayıcıyı iptal et
            timerRef[0] = null; // Referansı temizle
        }
    }
    
    /**
     * Harita üzerindeki tüm bölgelerin koordinatlarını başlatan metod
     *  territoryPositions Bölge adı-koordinat eşlemesi haritası
     */
    public static void initializeTerritoryPositions(Map<String, Point> territoryPositions) {
        // KUZEY AMERİKA bölgelerinin koordinatları
        territoryPositions.put("Alaska", new Point(91, 106)); // Alaska bölgesinin konumu
        territoryPositions.put("Kuzeybatı Toprakları", new Point(206, 109)); // Kuzeybatı Toprakları konumu
        territoryPositions.put("Grönland", new Point(418, 60)); // Grönland'ın konumu
        territoryPositions.put("Alberta", new Point(186, 160)); // Alberta'nın konumu
        territoryPositions.put("Ontario", new Point(272, 179)); // Ontario'nun konumu
        territoryPositions.put("Quebec", new Point(344, 170)); // Quebec'in konumu
        territoryPositions.put("Batı ABD", new Point(188, 235)); // Batı ABD'nin konumu
        territoryPositions.put("Doğu ABD", new Point(279, 254)); // Doğu ABD'nin konumu
        territoryPositions.put("Orta Amerika", new Point(204, 321)); // Orta Amerika'nın konumu

        // GÜNEY AMERİKA bölgelerinin koordinatları
        territoryPositions.put("Venezuela", new Point(284, 385)); // Venezuela'nın konumu
        territoryPositions.put("Peru", new Point(246, 453)); // Peru'nun konumu
        territoryPositions.put("Brezilya", new Point(373, 446)); // Brezilya'nın konumu
        territoryPositions.put("Arjantin", new Point(313, 564)); // Arjantin'in konumu

        // AVRUPA bölgelerinin koordinatları
        territoryPositions.put("İzlanda", new Point(511, 142)); // İzlanda'nın konumu
        territoryPositions.put("Britanya", new Point(500, 209)); // Britanya'nın konumu
        territoryPositions.put("İskandinavya", new Point(607, 135)); // İskandinavya'nın konumu
        territoryPositions.put("Kuzey Avrupa", new Point(596, 227)); // Kuzey Avrupa'nın konumu
        territoryPositions.put("Batı Avrupa", new Point(508, 317)); // Batı Avrupa'nın konumu
        territoryPositions.put("Güney Avrupa", new Point(614, 287)); // Güney Avrupa'nın konumu
        territoryPositions.put("Ukrayna", new Point(704, 187)); // Ukrayna'nın konumu

        // AFRİKA bölgelerinin koordinatları
        territoryPositions.put("Kuzey Afrika", new Point(535, 409)); // Kuzey Afrika'nın konumu
        territoryPositions.put("Mısır", new Point(644, 383)); // Mısır'ın konumu
        territoryPositions.put("Doğu Afrika", new Point(706, 464)); // Doğu Afrika'nın konumu
        territoryPositions.put("Kongo", new Point(643, 503)); // Kongo'nun konumu
        territoryPositions.put("Güney Afrika", new Point(655, 599)); // Güney Afrika'nın konumu
        territoryPositions.put("Madagaskar", new Point(761, 592)); // Madagaskar'ın konumu

        // ASYA bölgelerinin koordinatları
        territoryPositions.put("Ural", new Point(818, 158)); // Ural'ın konumu
        territoryPositions.put("Sibirya", new Point(889, 110)); // Sibirya'nın konumu
        territoryPositions.put("Yakutsk", new Point(971, 94)); // Yakutsk'un konumu
        territoryPositions.put("Kamçatka", new Point(1072, 99)); // Kamçatka'nın konumu
        territoryPositions.put("Irkutsk", new Point(956, 182)); // Irkutsk'un konumu
        territoryPositions.put("Moğolistan", new Point(969, 237)); // Moğolistan'ın konumu
        territoryPositions.put("Çin", new Point(956, 308)); // Çin'in konumu
        territoryPositions.put("Japonya", new Point(1088, 250)); // Japonya'nın konumu
        territoryPositions.put("Afganistan", new Point(807, 254)); // Afganistan'ın konumu
        territoryPositions.put("Hindistan", new Point(879, 391)); // Hindistan'ın konumu
        territoryPositions.put("Orta Doğu", new Point(735, 364)); // Orta Doğu'nun konumu
        territoryPositions.put("Güneydoğu Asya", new Point(962, 385)); // Güneydoğu Asya'nın konumu

        // AVUSTRALYA bölgelerinin koordinatları
        territoryPositions.put("Endonezya", new Point(985, 488)); // Endonezya'nın konumu
        territoryPositions.put("Yeni Gine", new Point(1084, 467)); // Yeni Gine'nin konumu
        territoryPositions.put("Batı Avustralya", new Point(1024, 580)); // Batı Avustralya'nın konumu
        territoryPositions.put("Doğu Avustralya", new Point(1136, 576)); // Doğu Avustralya'nın konumu
    }
    
    /**
     * Kıtaların arka plan renklerini başlatan metod
     *  continentColors Kıta adı-renk eşlemesi haritası
     */
    public static void initializeContinentColors(Map<String, Color> continentColors) {
        continentColors.put("Avrupa", new Color(100, 149, 237, 80)); // Avrupa için açık mavi renk (yarı şeffaf)
        continentColors.put("Afrika", new Color(255, 165, 0, 80)); // Afrika için turuncu renk (yarı şeffaf)
        continentColors.put("Asya", new Color(144, 238, 144, 80)); // Asya için açık yeşil renk (yarı şeffaf)
    }
    
    /**
     * Harita üzerinde tıklanan noktanın hangi bölgeye ait olduğunu bulan metod
     *  point Tıklanan nokta koordinatları
     *  territoryPositions Bölge konumları haritası
     * @return Tıklanan bölgenin adı, yoksa null
     */
    public static String findClickedTerritory(Point point, Map<String, Point> territoryPositions) {
        for (Map.Entry<String, Point> entry : territoryPositions.entrySet()) { // Her bölge için kontrol et
            Point p = entry.getValue(); // Bölgenin konumunu al
            if (point.distance(p) <= 35) { // Tıklanan nokta bölgeye 35 piksel mesafeden yakınsa
                return entry.getKey(); // Bölge adını döndür
            }
        }
        return null; // Hiçbir bölgeye yakın değilse null döndür
    }
    
    /**
     * Harita üzerinde tek bir bölgeyi çizen metod
     * g2d Çizim için Graphics2D nesnesi
     * name Bölge adı
     *  t Bölge bilgileri nesnesi
     *  gameState Oyun durumu bilgileri
     * territoryPositions Bölge konumları haritası
     *  mapPanel Harita paneli (boyut hesaplamaları için)
     */
    public static void drawTerritory(Graphics2D g2d, String name, Territory t, GameState gameState, 
                                   Map<String, Point> territoryPositions, JPanel mapPanel) {
        Point pos = territoryPositions.get(name); // Bölgenin koordinatını al
        if (pos == null) return; // Konum yoksa çizim yapma

        int panelWidth = mapPanel.getWidth(); // Panel genişliğini al
        int panelHeight = mapPanel.getHeight(); // Panel yüksekliğini al

        // Koordinatları panel boyutuna göre ölçeklendir
        int scaledX = pos.x * panelWidth / 1200; // X koordinatını ölçeklendir
        int scaledY = pos.y * panelHeight / 700; // Y koordinatını ölçeklendir
        int radius = 20 * panelWidth / 1200; // Daire yarıçapını ölçeklendir

        // Bölgenin özel durumlarını kontrol et
        boolean isSelected = name.equals(gameState.getSelectedTerritory()); // Bu bölge seçili mi?
        boolean isTarget = gameState.getHighlightedTargets().contains(name); // Bu bölge hedef mi?

        // Bölge sahibinin rengini al ve arka planı çiz
        Color background = gameState.getPlayerColor(t.getOwner()); // Sahip oyuncunun rengini al
        g2d.setColor(background); // Çizim rengini ayarla
        g2d.fillOval(scaledX - radius, scaledY - radius, radius * 2, radius * 2); // Dolu daire çiz

        if (isSelected) { // Eğer bu bölge seçiliyse
            g2d.setColor(GameState.SELECTED_COLOR); // Seçim rengini kullan (sarı)
            g2d.fillOval(scaledX - radius, scaledY - radius, radius * 2, radius * 2); // Üzerine seçim rengi çiz
        } else if (isTarget) { // Eğer bu bölge hedef bölge ise
            // Seçilen bölgenin bilgilerini al
            Territory selectedT = gameState.getSelectedTerritory() != null ? 
                gameState.getTerritories().get(gameState.getSelectedTerritory()) : null;
            if (selectedT != null) { // Seçili bölge varsa
                if (selectedT.getOwner() == gameState.getPlayerId()) { // Seçili bölge bizimse
                    if (t.getOwner() == gameState.getPlayerId()) { // Hedef bölge de bizimse
                        g2d.setColor(GameState.FRIENDLY_TARGET_COLOR); // Yeşil renk kullan (dost hedef)
                    } else { // Hedef bölge düşmanınsa
                        g2d.setColor(GameState.ENEMY_TARGET_COLOR); // Kırmızı renk kullan (düşman hedef)
                    }
                } else { // Seçili bölge düşmanınsa
                    if (t.getOwner() == gameState.getPlayerId()) { // Hedef bölge bizimse
                        g2d.setColor(GameState.FRIENDLY_TARGET_COLOR); // Yeşil renk kullan
                    } else { // Hedef bölge de düşmanınsa
                        g2d.setColor(GameState.ENEMY_TARGET_COLOR); // Kırmızı renk kullan
                    }
                }
            } else { // Seçili bölge yoksa
                g2d.setColor(GameState.TARGET_COLOR); // Varsayılan hedef rengini kullan
            }
            g2d.fillOval(scaledX - radius, scaledY - radius, radius * 2, radius * 2); // Hedef rengi ile çiz
        }

        // Bölgenin dış çizgisini çiz
        g2d.setColor(Color.BLACK); // Siyah rengi ayarla
        g2d.setStroke(new BasicStroke(2.0f)); // Çizgi kalınlığını 2 piksel yap
        g2d.drawOval(scaledX - radius, scaledY - radius, radius * 2, radius * 2); // Daire çerçevesi çiz

        // Bölge adını çiz
        g2d.setColor(Color.BLACK); // Metin rengi siyah
        g2d.setFont(new Font("Arial", Font.BOLD, 12)); // Yazı tipi: Arial, kalın, 12 punto
        FontMetrics fm = g2d.getFontMetrics(); // Yazı metriklerini al
        int textWidth = fm.stringWidth(name); // Metin genişliğini hesapla
        g2d.drawString(name, scaledX - textWidth / 2, scaledY - radius - 5); // Bölge adını dairenin üstüne yaz

        // Asker sayısını çiz
        g2d.setFont(new Font("Arial", Font.BOLD, 16)); // Asker sayısı için daha büyük font
        fm = g2d.getFontMetrics(); // Yeni font metriklerini al
        String troops = String.valueOf(t.getTroops()); // Asker sayısını string'e çevir
        int tw = fm.stringWidth(troops); // Asker sayısı metninin genişliğini hesapla
        g2d.drawString(troops, scaledX - tw / 2, scaledY + 5); // Asker sayısını dairenin ortasına yaz
    }
    
    /**
     * Oyun butonlarının aktif/pasif durumlarını kontrol eden metod
     *  enable Butonların genel olarak aktif olup olmayacağı
     *  currentTurn Şu anki sıranın kimde olduğu
     *  playerId Bu oyuncunun ID'si
     *  troopsToPlace Yerleştirilecek asker sayısı
     *  buttons Kontrol edilecek butonlar (sırasıyla: asker yerleştir, saldır, güçlendir, sırayı bitir)
     */
    public static void enableButtons(boolean enable, int currentTurn, int playerId, 
                                   int troopsToPlace, JButton... buttons) {
        // Eğer oyun henüz başlamamışsa (currentTurn -1) veya sıra bizde değilse butonları pasif yap
        if (enable && (currentTurn == -1 || currentTurn != playerId)) {
            enable = false; // Butonları pasif hale getir
        }

        // Saldırı ve güçlendirme ancak asker yerleştirme bittikten sonra yapılabilir
        boolean canAttackFortify = enable && troopsToPlace == 0;
        
        buttons[0].setEnabled(enable && troopsToPlace > 0); // Asker yerleştir butonu: sadece asker varsa aktif
        buttons[1].setEnabled(canAttackFortify); // Saldır butonu: asker yerleştirme bittikten sonra aktif
        buttons[2].setEnabled(canAttackFortify); // Güçlendir butonu: asker yerleştirme bittikten sonra aktif
        buttons[3].setEnabled(canAttackFortify); // Sırayı bitir butonu: asker yerleştirme bittikten sonra aktif
    }
    
    /**
     * Saldırı sırasında zar sonuçlarını gösteren diyalog penceresi oluşturan metod
     * @param parent Ana pencere (diyaloğun üst penceresi)
     * @return Oluşturulan zar diyaloğu
     */
    public static JDialog createDiceDialog(JFrame parent) {
        JDialog diceDialog = new JDialog(parent, "Zar Atılıyor", true); // Modal diyalog oluştur
        diceDialog.setSize(400, 250); // Diyalog boyutunu ayarla
        diceDialog.setLayout(new BorderLayout()); // BorderLayout kullan

        JPanel mainPanel = new JPanel(new BorderLayout()); // Ana panel oluştur
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Kenar boşlukları ekle

        JPanel dicePanel = new JPanel(new GridLayout(1, 2, 20, 0)); // İki sütunlu ızgara layout (saldıran/savunan)

        JPanel attackerDicePanel = new JPanel(new FlowLayout(FlowLayout.CENTER)); // Saldıran zarları paneli
        attackerDicePanel.setBorder(BorderFactory.createTitledBorder("Saldıran")); // "Saldıran" başlığı ekle

        JPanel defenderDicePanel = new JPanel(new FlowLayout(FlowLayout.CENTER)); // Savunan zarları paneli
        defenderDicePanel.setBorder(BorderFactory.createTitledBorder("Savunan")); // "Savunan" başlığı ekle

        dicePanel.add(attackerDicePanel); // Saldıran panelini ekle
        dicePanel.add(defenderDicePanel); // Savunan panelini ekle

        JLabel resultLabel = new JLabel("", SwingConstants.CENTER); // Sonuç etiketi oluştur
        resultLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0)); // Üst boşluk ekle

        mainPanel.add(dicePanel, BorderLayout.CENTER); // Zar panelini merkeze ekle
        mainPanel.add(resultLabel, BorderLayout.SOUTH); // Sonuç etiketini alta ekle

        diceDialog.add(mainPanel); // Ana paneli diyaloğa ekle
        diceDialog.setLocationRelativeTo(parent); // Diyaloğu üst pencerenin ortasına yerleştir
        
        return diceDialog; // Oluşturulan diyaloğu döndür
    }
    
    /**
     * Saldırı sonucu zar değerlerini diyalog üzerinde gösteren metod
     * @param attacker Saldıranın zar değerleri listesi
     * @param defender Savunanın zar değerleri listesi
     * @param attackerDicePanel Saldıran zarlarının gösterileceği panel
     * @param defenderDicePanel Savunan zarlarının gösterileceği panel
     * @param diceDialog Zar diyaloğu penceresi
     */
    public static void drawDiceResults(List<Integer> attacker, List<Integer> defender, 
                                     JPanel attackerDicePanel, JPanel defenderDicePanel, 
                                     JDialog diceDialog) {
        attackerDicePanel.removeAll(); // Saldıran panelindeki eski zarları temizle
        defenderDicePanel.removeAll(); // Savunan panelindeki eski zarları temizle

        for (int val : attacker) { // Her saldıran zarı için
            JLabel die = new JLabel(String.valueOf(val)); // Zar değerini etiket olarak oluştur
            die.setFont(new Font("Arial", Font.BOLD, 24)); // Büyük, kalın yazı tipi kullan
            die.setBorder(new EmptyBorder(10, 10, 10, 10)); // Etrafına boşluk ekle
            attackerDicePanel.add(die); // Saldıran paneline zar etiketini ekle
        }

        for (int val : defender) { // Her savunan zarı için
            JLabel die = new JLabel(String.valueOf(val)); // Zar değerini etiket olarak oluştur
            die.setFont(new Font("Arial", Font.BOLD, 24)); // Büyük, kalın yazı tipi kullan
            die.setBorder(new EmptyBorder(10, 10, 10, 10)); // Etrafına boşluk ekle
            defenderDicePanel.add(die); // Savunan paneline zar etiketini ekle
        }

        diceDialog.revalidate(); // Diyaloğun bileşenlerini yeniden düzenle
        diceDialog.repaint(); // Diyaloğu yeniden çiz
        diceDialog.setVisible(true); // Diyaloğu görünür hale getir
    }
}