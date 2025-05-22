package final_project; 

import javax.swing.*; 
import java.awt.*; 
import java.awt.event.*; 
import java.awt.image.BufferedImage; 
import java.io.IOException; 
import java.util.*; 
import java.util.List; 
import java.util.Timer; 
import javax.imageio.ImageIO; 

/**
 * Oyun kullanıcı arayüzünü yöneten tamamlanmış sınıf
 */ // Sınıfın açıklaması
public class GameUI { // Ana UI sınıfının tanımlanması
    private final RiskClient parent; // Ana istemci referansı
    private final GameState gameState; // Oyun durumu referansı
    
    private BufferedImage worldMap; // Dünya haritası görüntüsü
    private final int MAP_WIDTH = 1200; // Harita genişliği sabiti
    private final int MAP_HEIGHT = 700; // Harita yüksekliği sabiti
    
    private JPanel mainPanel; // Ana panel bileşeni
    private JPanel mapPanel; // Harita paneli bileşeni
    private JLabel statusLabel; // Durum etiketi bileşeni
    private JLabel troopsLeftLabel; // Kalan asker etiketi bileşeni
    private JLabel playerColorLabel; // Oyuncu rengi etiketi bileşeni
    private JLabel timerLabel; // Zamanlayıcı etiketi bileşeni
    private JTextArea gameLogArea; // Oyun günlüğü metin alanı
    
    private JButton placeTroopsButton, attackButton, fortifyButton, endTurnButton; // Oyun butonları
    
    private Timer[] turnTimer = new Timer[1]; // Tur zamanlayıcısı için dizi wrapper'ı
    
    private JDialog diceDialog; // Zar dialog'u
    private JPanel attackerDicePanel; // Saldırgan zar paneli
    private JPanel defenderDicePanel; // Savunma zar paneli
    
    private final Map<String, Point> territoryPositions = new HashMap<>(); // Bölge konumları haritası
    private final Map<String, Color> continentColors = new HashMap<>(); // Kıta renkleri haritası
    
    public GameUI(RiskClient parent, GameState gameState) { // Yapıcı metod
        this.parent = parent; // Ana istemci referansını atama
        this.gameState = gameState; // Oyun durumu referansını atama
        
        GameUIHelper.initializeTerritoryPositions(territoryPositions); // Bölge konumlarını başlatma
        GameUIHelper.initializeContinentColors(continentColors); // Kıta renklerini başlatma
        loadWorldMap(); // Dünya haritasını yükleme
        createComponents(); // Bileşenleri oluşturma
        setupKeyBindings(); // Klavye kısayollarını kurma
        diceDialog = GameUIHelper.createDiceDialog(parent); // Zar dialog'unu oluşturma
        setupDiceDialogPanels(); // Zar dialog panellerini kurma
    }
    
    private void loadWorldMap() { // Dünya haritasını yükleme metodu
        try { // Hata yakalama bloğu başlangıcı
            worldMap = ImageIO.read(getClass().getResourceAsStream("/risk_map.png")); // Harita dosyasını okuma
        } catch (IOException e) { // Dosya okuma hatası yakalama
            e.printStackTrace(); // Hata izini yazdırma
        }
    }
    
    private void setupDiceDialogPanels() { // Zar dialog panellerini kurma metodu
        // Dice dialog panellerini al
        JPanel mainDicePanel = (JPanel) diceDialog.getContentPane().getComponent(0); // Ana zar panelini alma
        JPanel dicePanel = (JPanel) mainDicePanel.getComponent(0); // Zar panelini alma
        attackerDicePanel = (JPanel) dicePanel.getComponent(0); // Saldırgan zar panelini alma
        defenderDicePanel = (JPanel) dicePanel.getComponent(1); // Savunma zar panelini alma
    }
    
    private void createComponents() { // Bileşenleri oluşturma metodu
        mainPanel = new JPanel(new BorderLayout()); // Ana paneli BorderLayout ile oluşturma
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Ana panele kenarlık ekleme
        
        createMapPanel(); // Harita panelini oluşturma
        
        mainPanel.add(mapPanel, BorderLayout.CENTER); // Harita panelini merkeze ekleme
        mainPanel.add(createRightPanel(), BorderLayout.EAST); // Sağ paneli doğuya ekleme
        mainPanel.add(createLogPanel(), BorderLayout.SOUTH); // Log panelini güneye ekleme
    }
    
    private void createMapPanel() { // Harita panelini oluşturma metodu
        mapPanel = new JPanel() { // Özel JPanel oluşturma
            @Override // Üst sınıf metodunu geçersiz kılma
            protected void paintComponent(Graphics g) { // Çizim metodunu geçersiz kılma
                super.paintComponent(g); // Üst sınıfın çizim metodunu çağırma
                Graphics2D g2d = (Graphics2D) g; // 2D grafik nesnesine dönüştürme
                
                if (worldMap != null) { // Harita varsa kontrol
                    g2d.drawImage(worldMap, 0, 0, getWidth(), getHeight(), null); // Haritayı çizme
                }
                
                drawTerritories(g2d); // Bölgeleri çizme
            }
        };
        
        mapPanel.addMouseListener(new MouseAdapter() { // Fare dinleyicisi ekleme
            @Override // Üst sınıf metodunu geçersiz kılma
            public void mouseClicked(MouseEvent e) { // Fare tıklama olayı
                int realX = e.getX() * 1200 / mapPanel.getWidth(); // Gerçek X koordinatını hesaplama
                int realY = e.getY() * 700 / mapPanel.getHeight(); // Gerçek Y koordinatını hesaplama
                handleMapClick(new Point(realX, realY)); // Harita tıklama olayını işleme
            }
        });
    }
    
    private JPanel createRightPanel() { // Sağ paneli oluşturma metodu
        JPanel rightPanel = new JPanel(); // Sağ panel oluşturma
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS)); // Dikey kutu düzeni ayarlama
        rightPanel.setPreferredSize(new Dimension(200, 0)); // Tercih edilen boyut ayarlama
        
        // Durum paneli
        JPanel infoPanel = new JPanel(); // Bilgi paneli oluşturma
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS)); // Dikey kutu düzeni ayarlama
        infoPanel.setBorder(BorderFactory.createTitledBorder("Durum")); // Başlıklı kenarlık ekleme
        
        statusLabel = new JLabel("Sunucuya bağlanıyor...", SwingConstants.CENTER); // Durum etiketi oluşturma
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Merkez hizalama ayarlama
        troopsLeftLabel = new JLabel("Kalan Asker: 0", SwingConstants.CENTER); // Kalan asker etiketi oluşturma
        troopsLeftLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Merkez hizalama ayarlama
        playerColorLabel = new JLabel("Renginiz: -", SwingConstants.CENTER); // Oyuncu rengi etiketi oluşturma
        playerColorLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Merkez hizalama ayarlama
        timerLabel = new JLabel("Süre: --", SwingConstants.CENTER); // Zamanlayıcı etiketi oluşturma
        timerLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Merkez hizalama ayarlama
        
        infoPanel.add(Box.createVerticalStrut(5)); // Dikey boşluk ekleme
        infoPanel.add(timerLabel); // Zamanlayıcı etiketini ekleme
        infoPanel.add(statusLabel); // Durum etiketini ekleme
        infoPanel.add(Box.createVerticalStrut(5)); // Dikey boşluk ekleme
        infoPanel.add(troopsLeftLabel); // Kalan asker etiketini ekleme
        infoPanel.add(playerColorLabel); // Oyuncu rengi etiketini ekleme
        
        // Buton paneli
        JPanel buttonPanel = new JPanel(); // Buton paneli oluşturma
        buttonPanel.setLayout(new GridLayout(4, 1, 5, 5)); // 4x1 ızgara düzeni ayarlama
        
        placeTroopsButton = new JButton("Asker Yerleştir"); // Asker yerleştir butonu oluşturma
        attackButton = new JButton("Saldır"); // Saldır butonu oluşturma
        fortifyButton = new JButton("Güçlendir"); // Güçlendir butonu oluşturma
        endTurnButton = new JButton("Sırayı Bitir"); // Sırayı bitir butonu oluşturma
        
        buttonPanel.add(placeTroopsButton); // Asker yerleştir butonunu ekleme
        buttonPanel.add(attackButton); // Saldır butonunu ekleme
        buttonPanel.add(fortifyButton); // Güçlendir butonunu ekleme
        buttonPanel.add(endTurnButton); // Sırayı bitir butonunu ekleme
        
        // Action listeners
        placeTroopsButton.addActionListener(e -> handlePlaceTroops()); // Asker yerleştir butonu dinleyicisi
        attackButton.addActionListener(e -> toggleAttackMode()); // Saldır butonu dinleyicisi
        fortifyButton.addActionListener(e -> toggleFortifyMode()); // Güçlendir butonu dinleyicisi
        endTurnButton.addActionListener(e -> endTurn()); // Sırayı bitir butonu dinleyicisi
        
        rightPanel.add(infoPanel); // Bilgi panelini sağ panele ekleme
        rightPanel.add(Box.createVerticalStrut(10)); // Dikey boşluk ekleme
        rightPanel.add(buttonPanel); // Buton panelini ekleme
        rightPanel.add(Box.createVerticalGlue()); // Esnek dikey boşluk ekleme
        
        return rightPanel; // Sağ paneli döndürme
    }
    
    private JPanel createLogPanel() { // Log panelini oluşturma metodu
        JPanel logPanel = new JPanel(new BorderLayout()); // Log paneli oluşturma
        logPanel.setBorder(BorderFactory.createTitledBorder("Oyun Konsolu")); // Başlıklı kenarlık ekleme
        logPanel.setPreferredSize(new Dimension(0, 100)); // Tercih edilen boyut ayarlama
        
        gameLogArea = new JTextArea(); // Oyun günlüğü metin alanı oluşturma
        gameLogArea.setEditable(false); // Düzenlenemez yapma
        gameLogArea.setFont(new Font("Monospaced", Font.PLAIN, 12)); // Font ayarlama
        gameLogArea.setLineWrap(true); // Satır kaydırmayı aktifleştirme
        gameLogArea.setWrapStyleWord(true); // Kelime kaydırmayı aktifleştirme
        
        JScrollPane scrollPane = new JScrollPane(gameLogArea); // Kaydırma paneli oluşturma
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS); // Dikey kaydırma çubuğunu her zaman gösterme
        
        logPanel.add(scrollPane, BorderLayout.CENTER); // Kaydırma panelini merkeze ekleme
        return logPanel; // Log panelini döndürme
    }
    
    private void setupKeyBindings() { // Klavye kısayollarını kurma metodu
        InputMap inputMap = parent.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW); // Giriş haritasını alma
        ActionMap actionMap = parent.getRootPane().getActionMap(); // Eylem haritasını alma
        
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0), "placeTroops"); // P tuşu için kısayol tanımlama
        actionMap.put("placeTroops", new AbstractAction() { // Asker yerleştir eylemi tanımlama
            @Override // Üst sınıf metodunu geçersiz kılma
            public void actionPerformed(ActionEvent e) { // Eylem gerçekleştirildiğinde
                if (placeTroopsButton.isEnabled()) { // Buton etkinse
                    handlePlaceTroops(); // Asker yerleştirme işlemini çağırma
                }
            }
        });
        
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0), "attack"); // A tuşu için kısayol tanımlama
        actionMap.put("attack", new AbstractAction() { // Saldır eylemi tanımlama
            @Override // Üst sınıf metodunu geçersiz kılma
            public void actionPerformed(ActionEvent e) { // Eylem gerçekleştirildiğinde
                if (attackButton.isEnabled()) { // Buton etkinse
                    toggleAttackMode(); // Saldırı modunu açma/kapama
                }
            }
        });
        
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0), "fortify"); // F tuşu için kısayol tanımlama
        actionMap.put("fortify", new AbstractAction() { // Güçlendir eylemi tanımlama
            @Override // Üst sınıf metodunu geçersiz kılma
            public void actionPerformed(ActionEvent e) { // Eylem gerçekleştirildiğinde
                if (fortifyButton.isEnabled()) { // Buton etkinse
                    toggleFortifyMode(); // Güçlendirme modunu açma/kapama
                }
            }
        });
        
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, 0), "endTurn"); // E tuşu için kısayol tanımlama
        actionMap.put("endTurn", new AbstractAction() { // Sırayı bitir eylemi tanımlama
            @Override // Üst sınıf metodunu geçersiz kılma
            public void actionPerformed(ActionEvent e) { // Eylem gerçekleştirildiğinde
                if (endTurnButton.isEnabled()) { // Buton etkinse
                    endTurn(); // Sırayı bitirme işlemini çağırma
                }
            }
        });
        
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clearSelection"); // Escape tuşu için kısayol tanımlama
        actionMap.put("clearSelection", new AbstractAction() { // Seçimi temizle eylemi tanımlama
            @Override // Üst sınıf metodunu geçersiz kılma
            public void actionPerformed(ActionEvent e) { // Eylem gerçekleştirildiğinde
                clearSelectionAndModes(); // Seçim ve modları temizleme
            }
        });
    }
    
    private void drawTerritories(Graphics2D g2d) { // Bölgeleri çizme metodu
        for (Map.Entry<String, Territory> entry : gameState.getTerritories().entrySet()) { // Her bölge için döngü
            String name = entry.getKey(); // Bölge adını alma
            Territory t = entry.getValue(); // Bölge nesnesini alma
            if (territoryPositions.containsKey(name)) { // Bölge konumu varsa
                GameUIHelper.drawTerritory(g2d, name, t, gameState, territoryPositions, mapPanel); // Bölgeyi çizme
            }
        }
    }
    
    // Event handler metodları
    private void handlePlaceTroops() { // Asker yerleştirme işlemini ele alma metodu
        int remaining = gameState.getRemainingTroops(); // Kalan asker sayısını alma
        if (remaining <= 0) { // Kalan asker yoksa
            logToConsole("Yerleştirecek askeriniz kalmadı."); // Konsola mesaj yazma
            return; // Metoddan çıkış
        }
        statusLabel.setText("Asker yerleştirmek için kendi bir bölgenizi seçin."); // Durum etiketini güncelleme
        logToConsole("Asker yerleştirme modu aktif. Kalan: " + remaining); // Konsola mesaj yazma
    }
    
    private void toggleAttackMode() { // Saldırı modunu açma/kapama metodu
        gameState.setAttackMode(!gameState.isAttackMode()); // Saldırı modunu tersine çevirme
        gameState.setFortifyMode(false); // Güçlendirme modunu kapatma
        gameState.clearSelections(); // Seçimleri temizleme
        
        if (gameState.isAttackMode()) { // Saldırı modu açıksa
            statusLabel.setText("Saldırı modu aktif. Saldıran bölgeyi seçin."); // Durum etiketini güncelleme
            logToConsole("Saldırı modu başlatıldı."); // Konsola mesaj yazma
        } else { // Saldırı modu kapalıysa
            statusLabel.setText("Saldırı modu kapatıldı."); // Durum etiketini güncelleme
            logToConsole("Saldırı modu kapatıldı."); // Konsola mesaj yazma
        }
        
        mapPanel.repaint(); // Harita panelini yeniden çizme
    }
    
    private void toggleFortifyMode() { // Güçlendirme modunu açma/kapama metodu
        gameState.setFortifyMode(!gameState.isFortifyMode()); // Güçlendirme modunu tersine çevirme
        gameState.setAttackMode(false); // Saldırı modunu kapatma
        gameState.clearSelections(); // Seçimleri temizleme
        
        if (gameState.isFortifyMode()) { // Güçlendirme modu açıksa
            statusLabel.setText("Güçlendirme modu aktif. Kaynak bölgenizi seçin."); // Durum etiketini güncelleme
            logToConsole("Güçlendirme modu başlatıldı."); // Konsola mesaj yazma
        } else { // Güçlendirme modu kapalıysa
            statusLabel.setText("Güçlendirme modu kapatıldı."); // Durum etiketini güncelleme
            logToConsole("Güçlendirme modu kapatıldı."); // Konsola mesaj yazma
        }
        
        mapPanel.repaint(); // Harita panelini yeniden çizme
    }
    
    private void endTurn() { // Sırayı bitirme metodu
        Message endTurnMsg = new Message("END_TURN", Collections.emptyMap()); // Sıra bitirme mesajı oluşturma
        parent.sendMessage(endTurnMsg); // Mesajı gönderme
        
        logToConsole("Turu bitirdiniz."); // Konsola mesaj yazma
        statusLabel.setText("Turu bitirdiniz. Rakip bekleniyor..."); // Durum etiketini güncelleme
        enableButtons(false); // Butonları devre dışı bırakma
        gameState.clearSelections(); // Seçimleri temizleme
        mapPanel.repaint(); // Harita panelini yeniden çizme
    }
    
    private void clearSelectionAndModes() { // Seçim ve modları temizleme metodu
        gameState.clearSelections(); // Seçimleri temizleme
        statusLabel.setText("Seçim temizlendi."); // Durum etiketini güncelleme
        logToConsole("Seçimler sıfırlandı."); // Konsola mesaj yazma
        mapPanel.repaint(); // Harita panelini yeniden çizme
    }
    
    private void handleMapClick(Point point) { // Harita tıklama olayını işleme metodu
        if (gameState.isGameOver()) { // Oyun bittiyse
            return; // Metoddan çıkış
        }

        String clicked = GameUIHelper.findClickedTerritory(point, territoryPositions); // Tıklanan bölgeyi bulma
        if (clicked == null) { // Bölge bulunamadıysa
            return; // Metoddan çıkış
        }

        if (gameState.getCurrentTurn() != gameState.getPlayerId()) { // Sıra oyuncuda değilse
            logToConsole("Sıra sizde değil."); // Konsola mesaj yazma
            return; // Metoddan çıkış
        }

        int troopsLeft = gameState.getRemainingTroops(); // Kalan asker sayısını alma
        if (troopsLeft > 0) { // Yerleştirilecek asker varsa
            handleTroopPlacement(clicked, troopsLeft); // Asker yerleştirme işlemini ele alma
            return; // Metoddan çıkış
        }

        if (gameState.isAttackMode()) { // Saldırı modu açıksa
            handleAttackModeClick(clicked); // Saldırı modu tıklamasını işleme
            return; // Metoddan çıkış
        }

        if (gameState.isFortifyMode()) { // Güçlendirme modu açıksa
            handleFortifyModeClick(clicked); // Güçlendirme modu tıklamasını işleme
            return; // Metoddan çıkış
        }

        Territory t = gameState.getTerritories().get(clicked); // Tıklanan bölgeyi alma
        if (t != null && t.getOwner() == gameState.getPlayerId()) { // Bölge varsa ve oyuncunun ise
            gameState.setSelectedTerritory(clicked); // Seçili bölgeyi ayarlama
            statusLabel.setText(clicked + " seçildi."); // Durum etiketini güncelleme
            mapPanel.repaint(); // Harita panelini yeniden çizme
        }
    }
    
    private void handleTroopPlacement(String clicked, int troopsLeft) { // Asker yerleştirme işlemini ele alma metodu
        Territory t = gameState.getTerritories().get(clicked); // Tıklanan bölgeyi alma
        if (t != null && t.getOwner() == gameState.getPlayerId()) { // Bölge varsa ve oyuncunun ise
            String input = JOptionPane.showInputDialog(parent, // Kullanıcıdan giriş alma
                    "Kaç asker yerleştirilsin? (max " + troopsLeft + ")", 
                    "Asker Yerleştir", JOptionPane.QUESTION_MESSAGE);
            try { // Hata yakalama bloğu başlangıcı
                int num = Integer.parseInt(input); // Girişi sayıya dönüştürme
                if (num > 0 && num <= troopsLeft) { // Sayı geçerliyse
                    Message placeTroopsMsg = new Message("PLACE_TROOPS", Map.of( // Asker yerleştir mesajı oluşturma
                            "territory", clicked,
                            "troops", String.valueOf(num)
                    ));
                    parent.sendMessage(placeTroopsMsg); // Mesajı gönderme
                } else { // Sayı geçersizse
                    logToConsole("Geçersiz asker sayısı."); // Konsola mesaj yazma
                }
            } catch (NumberFormatException e) { // Sayı dönüştürme hatası
                logToConsole("Geçersiz giriş."); // Konsola mesaj yazma
            }
        } else { // Bölge geçersizse
            logToConsole("Kendi bölgenizi seçmelisiniz."); // Konsola mesaj yazma
        }
    }
    
    private void handleAttackModeClick(String clicked) { // Saldırı modu tıklamasını işleme metodu
        Territory t = gameState.getTerritories().get(clicked); // Tıklanan bölgeyi alma
        if (gameState.getSelectedTerritory() == null) { // Seçili bölge yoksa
            if (t.getOwner() == gameState.getPlayerId() && t.getTroops() > 1) { // Bölge oyuncunun ve yeterli askeri varsa
                gameState.setSelectedTerritory(clicked); // Seçili bölgeyi ayarlama
                statusLabel.setText("Hedef seçin"); // Durum etiketini güncelleme
                mapPanel.repaint(); // Harita panelini yeniden çizme
            } else { // Bölge uygun değilse
                logToConsole("Saldırmak için en az 2 askerli bir bölge seçin."); // Konsola mesaj yazma
            }
        } else { // Seçili bölge varsa
            if (t.getOwner() != gameState.getPlayerId() &&  // Bölge düşmana ait ve komşuysa
                gameState.areNeighbors(gameState.getSelectedTerritory(), clicked)) {
                gameState.setTargetTerritory(clicked); // Hedef bölgeyi ayarlama
                String input = JOptionPane.showInputDialog("Kaç zarla saldırı yapılacak? (1-3)"); // Kullanıcıdan zar sayısını alma
                try { // Hata yakalama bloğu başlangıcı
                    int dice = Integer.parseInt(input); // Girişi sayıya dönüştürme
                    Message attackMsg = new Message("ATTACK", Map.of( // Saldırı mesajı oluşturma
                            "from", gameState.getSelectedTerritory(),
                            "to", gameState.getTargetTerritory(),
                            "dice", String.valueOf(dice)
                    ));
                    parent.sendMessage(attackMsg); // Mesajı gönderme
                } catch (NumberFormatException e) { // Sayı dönüştürme hatası
                    logToConsole("Geçersiz zar sayısı."); // Konsola mesaj yazma
                }
            } else { // Hedef geçersizse
                logToConsole("Geçersiz hedef."); // Konsola mesaj yazma
            }
        }
    }
    
    private void handleFortifyModeClick(String clicked) { // Güçlendirme modu tıklamasını işleme metodu
        Territory t = gameState.getTerritories().get(clicked); // Tıklanan bölgeyi alma
        if (gameState.getSelectedTerritory() == null) { // Seçili bölge yoksa
            if (t.getOwner() == gameState.getPlayerId() && t.getTroops() > 1) { // Bölge oyuncunun ve yeterli askeri varsa
                gameState.setSelectedTerritory(clicked); // Seçili bölgeyi ayarlama
                statusLabel.setText("Hedef bölge seçin."); // Durum etiketini güncelleme
                mapPanel.repaint(); // Harita panelini yeniden çizme
            } else { // Bölge uygun değilse
                logToConsole("Güçlendirmek için yeterli askeriniz yok."); // Konsola mesaj yazma
            }
        } else { // Seçili bölge varsa
            if (t.getOwner() == gameState.getPlayerId() &&  // Bölge oyuncunun ve bağlantılıysa
                gameState.areConnected(gameState.getSelectedTerritory(), clicked)) {
                gameState.setTargetTerritory(clicked); // Hedef bölgeyi ayarlama
                Territory from = gameState.getTerritories().get(gameState.getSelectedTerritory()); // Kaynak bölgeyi alma
                int max = from.getTroops() - 1; // Maksimum taşınabilecek askeri hesaplama
                String input = JOptionPane.showInputDialog("Kaç asker taşınsın? (1-" + max + ")"); // Kullanıcıdan asker sayısını alma
                try { // Hata yakalama bloğu başlangıcı
                    int num = Integer.parseInt(input); // Girişi sayıya dönüştürme
                    Message fortifyMsg = new Message("FORTIFY", Map.of( // Güçlendirme mesajı oluşturma
                            "from", gameState.getSelectedTerritory(),
                            "to", gameState.getTargetTerritory(),
                            "troops", String.valueOf(num)
                    ));
                    parent.sendMessage(fortifyMsg); // Mesajı gönderme
                } catch (NumberFormatException e) { // Sayı dönüştürme hatası
                    logToConsole("Geçersiz asker sayısı."); // Konsola mesaj yazma
                }
            } else { // Hedef geçersizse
                logToConsole("Sadece kendi bölgeleriniz arasında geçiş yapabilirsiniz."); // Konsola mesaj yazma
            }
        }
    }
    
    public JMenuBar createMenuBar() { // Menü çubuğu oluşturma metodu
        JMenuBar menuBar = new JMenuBar(); // Menü çubuğu oluşturma
        
        JMenu gameMenu = new JMenu("Oyun"); // Oyun menüsü oluşturma
        JMenuItem exitItem = new JMenuItem("Çıkış"); // Çıkış menü öğesi oluşturma
        exitItem.addActionListener(e -> parent.handleWindowClosing()); // Çıkış dinleyicisi ekleme
        
        JMenuItem rulesItem = new JMenuItem("Kurallar"); // Kurallar menü öğesi oluşturma
        rulesItem.addActionListener(e -> { // Kurallar dinleyicisi ekleme
            JOptionPane.showMessageDialog(parent, // Kurallar dialog'unu gösterme
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
            """,
                    "Oyun Kuralları", JOptionPane.INFORMATION_MESSAGE);
        });
        
        JMenu helpMenu = new JMenu("Yardım"); // Yardım menüsü oluşturma
        JMenuItem aboutItem = new JMenuItem("Hakkında"); // Hakkında menü öğesi oluşturma
        aboutItem.addActionListener(e -> { // Hakkında dinleyicisi ekleme
            JOptionPane.showMessageDialog(parent, // Hakkında dialog'unu gösterme
                    "Risk Oyunu v1.0\nGeliştirici: M. Noor", "Hakkında", JOptionPane.INFORMATION_MESSAGE);
        });
        
        JMenuItem shortcutItem = new JMenuItem("Kısayollar"); // Kısayollar menü öğesi oluşturma
        shortcutItem.addActionListener(e -> { // Kısayollar dinleyicisi ekleme
            JOptionPane.showMessageDialog(parent, // Kısayollar dialog'unu gösterme
                    """
                P - Asker Yerleştir
                A - Saldırı
                F - Güçlendir
                E - Sırayı Bitir
                Esc - Seçimi Temizle
                """, "Klavye Kısayolları", JOptionPane.INFORMATION_MESSAGE);
        });
        
        helpMenu.add(rulesItem); // Kuralları yardım menüsüne ekleme
        helpMenu.add(shortcutItem); // Kısayolları yardım menüsüne ekleme
        helpMenu.add(aboutItem); // Hakkında'yı yardım menüsüne ekleme
        
        gameMenu.add(exitItem); // Çıkış'ı oyun menüsüne ekleme
        menuBar.add(gameMenu); // Oyun menüsünü menü çubuğuna ekleme
        menuBar.add(helpMenu); // Yardım menüsünü menü çubuğuna ekleme
        
        return menuBar; // Menü çubuğunu döndürme
    }
    
    // Public metodlar - diğer sınıflar tarafından kullanılacak
    public JPanel getMainPanel() { // Ana paneli alma metodu
        return mainPanel; // Ana paneli döndürme
    }
    
    public void logToConsole(String message) { // Konsola log yazma metodu
        SwingUtilities.invokeLater(() -> { // EDT'de çalıştırma
            String timestamp = String.format("[%tT] ", new Date()); // Zaman damgası oluşturma
            gameLogArea.append(timestamp + message + "\n"); // Mesajı log alanına ekleme
            gameLogArea.setCaretPosition(gameLogArea.getDocument().getLength()); // Cursor'u sona taşıma
        });
    }
    
    public void updateStatus(String status) { // Durum güncelleme metodu
        statusLabel.setText(status); // Durum etiketini güncelleme
    }
    
    public void updateTroopsLeft(String troops) { // Kalan asker güncelleme metodu
        troopsLeftLabel.setText(troops); // Kalan asker etiketini güncelleme
    }
    
    public void updatePlayerColor(String text, Color color) { // Oyuncu rengi güncelleme metodu
        playerColorLabel.setText(text); // Oyuncu rengi etiket metnini güncelleme
        playerColorLabel.setForeground(color); // Oyuncu rengi etiket rengini güncelleme
    }
    
    public void repaintMap() { // Haritayı yeniden çizme metodu
        mapPanel.repaint(); // Harita panelini yeniden çizme
    }
    
    public void enableButtons(boolean enable) { // Butonları etkinleştirme/devre dışı bırakma metodu
        GameUIHelper.enableButtons(enable, gameState.getCurrentTurn(), gameState.getPlayerId(), // Helper metodu çağırma
                gameState.getRemainingTroops(), placeTroopsButton, attackButton, fortifyButton, endTurnButton);
    }
    
    public void startTurnTimer() { // Tur zamanlayıcısını başlatma metodu
        GameUIHelper.startTurnTimer(timerLabel, turnTimer, () -> { // Helper metodu çağırma
            logToConsole("Süre doldu! Otomatik sıra geçiliyor."); // Konsola mesaj yazma
            endTurn(); // Sırayı bitirme
        });
    }
    
    public void drawDiceResults(List<Integer> attacker, List<Integer> defender) { // Zar sonuçlarını çizme metodu
        GameUIHelper.drawDiceResults(attacker, defender, attackerDicePanel, defenderDicePanel, diceDialog); // Helper metodu çağırma
    }
    
    public void cleanup() { // Temizleme metodu
        GameUIHelper.stopTurnTimer(turnTimer); // Zamanlayıcıyı durdurma
        if (diceDialog != null) { // Zar dialog'u varsa
            diceDialog.dispose(); // Dialog'u kapatma
        }
    }
} 