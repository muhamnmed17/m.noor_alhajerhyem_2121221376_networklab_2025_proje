package final_project;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.List;
import javax.swing.border.*;
import java.util.Timer;
import java.util.TimerTask;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

/**
 * Risk oyunu için geliştirilmiş istemci uygulaması
 */
public class RiskClient extends JFrame {

    private JLabel troopsLeftLabel;
    private BufferedImage worldMap;
    private final int MAP_WIDTH = 1200;
    private final int MAP_HEIGHT = 700;

    private JLabel playerColorLabel;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Map<String, Set<String>> adjacencyMap = new HashMap<>();
    private Timer turnTimer;
    private int secondsLeft;
    private JLabel timerLabel;
    private String selectedTerritory = null;
    private List<String> highlightedTargets = new ArrayList<>();

    private int playerId = -1;

    private final Map<Integer, String> playerNames = new HashMap<>();

    private int currentTurn = -1;

    private Color getPlayerColor(int id) {
        return (id % 2 == 0)
                ? new Color(220, 20, 60) // Kırmızı
                : new Color(30, 144, 255); // Mavi
    }
    private static final Color SELECTED_COLOR = new Color(255, 215, 0, 200);
    private static final Color FRIENDLY_TARGET_COLOR = new Color(50, 205, 50, 200); // Yeşil - kendi bölgelerimize komşu
    private static final Color ENEMY_TARGET_COLOR = new Color(255, 99, 71, 200); // Kırmızımsı - rakip bölgelere komşu
    private static final Color TARGET_COLOR = new Color(50, 205, 50, 200); // Eski renk, uyumluluk için tutuldu

    private Map<String, Territory> territories = new HashMap<>();
    private Map<Integer, Integer> playerTroopsToPlace = new HashMap<>();
    private Map<String, String> continentOwners = new HashMap<>(); // Kıta sahipleri

    private final Map<String, Point> territoryPositions = new HashMap<>();
    private final Map<String, Color> continentColors = new HashMap<>();

    private JPanel mapPanel;
    private JLabel statusLabel;
    private JTextArea gameLogArea;
    private JButton placeTroopsButton, attackButton, fortifyButton, endTurnButton;

    private String targetTerritory = null;
    private boolean attackMode = false;
    private boolean fortifyMode = false;
    private boolean gameOver = false;

    private JDialog diceDialog;
    private JPanel attackerDicePanel;
    private JPanel defenderDicePanel;

    public RiskClient(String serverIp, int serverPort, String playerName) {
        super("Risk Oyunu");
        initializeTerritoryPositions();
        initializeContinentColors();
        initializeUI();
        enableButtons(false); // Başlangıçta butonlar pasif olsun

        try {
            socket = new Socket(serverIp, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            logToGameConsole("Sunucuya bağlandı: " + serverIp + ":" + serverPort);

            // İsim gönder
            sendCommand("SET_NAME " + playerName);
        } catch (IOException e) {
            logToGameConsole("Bağlantı hatası: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Sunucuya bağlanılamadı: " + e.getMessage(),
                    "Bağlantı Hatası", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        startListening();
    }

    /**
     * Konsola zaman damgalı mesaj yazdırır
     */
    private void logToGameConsole(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = String.format("[%tT] ", new Date());
            gameLogArea.append(timestamp + message + "\n");
            gameLogArea.setCaretPosition(gameLogArea.getDocument().getLength());
        });
    }

    private void setSelectedTerritory(String name) {
        selectedTerritory = name;
        highlightedTargets.clear();

        if (name != null && adjacencyMap.containsKey(name)) {
            highlightedTargets.addAll(adjacencyMap.get(name));
        }

        mapPanel.repaint(); // Görseli güncelle
    }

    private JPanel createRightPanel() {
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setPreferredSize(new Dimension(200, 0));

        // Durum paneli
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(BorderFactory.createTitledBorder("Durum"));

        statusLabel = new JLabel("Bağlanıyor...", SwingConstants.CENTER);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        troopsLeftLabel = new JLabel("Kalan Asker: 0", SwingConstants.CENTER);
        troopsLeftLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        playerColorLabel = new JLabel("Renginiz: -", SwingConstants.CENTER);
        playerColorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        timerLabel = new JLabel("Süre: --", SwingConstants.CENTER);
        timerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(timerLabel);

        infoPanel.add(statusLabel);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(troopsLeftLabel);
        infoPanel.add(playerColorLabel);
        
        // Renk bilgisi paneli
        JPanel colorInfoPanel = new JPanel();
        colorInfoPanel.setLayout(new BoxLayout(colorInfoPanel, BoxLayout.Y_AXIS));
        colorInfoPanel.setBorder(BorderFactory.createTitledBorder("Renk Bilgisi"));
        
        // Yeşil renk (kendi bölgelerimize komşu)
        JPanel friendlyColorPanel = new JPanel(new BorderLayout());
        JPanel friendlyColorBox = new JPanel();
        friendlyColorBox.setBackground(FRIENDLY_TARGET_COLOR);
        friendlyColorBox.setPreferredSize(new Dimension(20, 20));
        friendlyColorBox.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        JLabel friendlyLabel = new JLabel(" Kendi bölgelerinize komşu");
        friendlyColorPanel.add(friendlyColorBox, BorderLayout.WEST);
        friendlyColorPanel.add(friendlyLabel, BorderLayout.CENTER);
        
        // Kırmızımsı renk (rakip bölgelere komşu)
        JPanel enemyColorPanel = new JPanel(new BorderLayout());
        JPanel enemyColorBox = new JPanel();
        enemyColorBox.setBackground(ENEMY_TARGET_COLOR);
        enemyColorBox.setPreferredSize(new Dimension(20, 20));
        enemyColorBox.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        JLabel enemyLabel = new JLabel(" Rakip bölgelere komşu");
        enemyColorPanel.add(enemyColorBox, BorderLayout.WEST);
        enemyColorPanel.add(enemyLabel, BorderLayout.CENTER);
        
        // Sarı renk (seçili bölge)
        JPanel selectedColorPanel = new JPanel(new BorderLayout());
        JPanel selectedColorBox = new JPanel();
        selectedColorBox.setBackground(SELECTED_COLOR);
        selectedColorBox.setPreferredSize(new Dimension(20, 20));
        selectedColorBox.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        JLabel selectedLabel = new JLabel(" Seçili bölge");
        selectedColorPanel.add(selectedColorBox, BorderLayout.WEST);
        selectedColorPanel.add(selectedLabel, BorderLayout.CENTER);
        
        colorInfoPanel.add(friendlyColorPanel);
        colorInfoPanel.add(Box.createVerticalStrut(5));
        colorInfoPanel.add(enemyColorPanel);
        colorInfoPanel.add(Box.createVerticalStrut(5));
        colorInfoPanel.add(selectedColorPanel);

        // Buton paneli
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(4, 1, 5, 5));

        placeTroopsButton = new JButton("Asker Yerleştir");
        attackButton = new JButton("Saldır");
        fortifyButton = new JButton("Güçlendir");
        endTurnButton = new JButton("Sırayı Bitir");

        buttonPanel.add(placeTroopsButton);
        buttonPanel.add(attackButton);
        buttonPanel.add(fortifyButton);
        buttonPanel.add(endTurnButton);

        // ActionListener eklemeyi unutma
        placeTroopsButton.addActionListener(e -> handlePlaceTroops());
        attackButton.addActionListener(e -> toggleAttackMode());
        fortifyButton.addActionListener(e -> toggleFortifyMode());
        endTurnButton.addActionListener(e -> endTurn());

        // Tümünü ekle
        rightPanel.add(infoPanel);
        rightPanel.add(Box.createVerticalStrut(10));
        rightPanel.add(buttonPanel);
        rightPanel.add(Box.createVerticalGlue());

        return rightPanel;
    }

    private void startTurnTimer() {
        stopTurnTimer();
        secondsLeft = 60;
        timerLabel.setText("Süre: 60 sn");

        turnTimer = new Timer();
        turnTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    secondsLeft--;
                    timerLabel.setText("Süre: " + secondsLeft + " sn");

                    if (secondsLeft <= 0) {
                        stopTurnTimer();
                        logToGameConsole("Süre doldu! Otomatik sıra geçiliyor.");
                        endTurn(); // otomatik sırayı bitir
                    }
                });
            }
        }, 1000, 1000); // her saniye
    }

    private void stopTurnTimer() {
        if (turnTimer != null) {
            turnTimer.cancel();
            turnTimer = null;
        }
        timerLabel.setText("Süre: --");
    }

    /**
     * Güçlendirme modunu açar/kapatır
     */
    private void toggleFortifyMode() {
        fortifyMode = !fortifyMode;
        attackMode = false;
        selectedTerritory = null;
        targetTerritory = null;

        if (fortifyMode) {
            statusLabel.setText("Güçlendirme modu aktif. Kaynak bölgenizi seçin.");
            logToGameConsole("Güçlendirme modu başlatıldı.");
        } else {
            statusLabel.setText("Güçlendirme modu kapatıldı.");
            logToGameConsole("Güçlendirme modu kapatıldı.");
        }

        mapPanel.repaint();
    }

    /**
     * Oyuncunun sırasını bitirir
     */
    private void endTurn() {
    sendCommand("END_TURN");
    logToGameConsole("Turu bitirdiniz.");
    statusLabel.setText("Turu bitirdiniz. Rakip bekleniyor...");
    enableButtons(false);
    removeHighlights();
    mapPanel.repaint();
}

    /**
     * Asker yerleştirme modunu başlatır
     */
    private void handlePlaceTroops() {
        int remaining = playerTroopsToPlace.getOrDefault(playerId, 0);
        if (remaining <= 0) {
            logToGameConsole("Yerleştirecek askeriniz kalmadı.");
            return;
        }

        statusLabel.setText("Asker yerleştirmek için kendi bir bölgenizi seçin.");
        logToGameConsole("Asker yerleştirme modu aktif. Kalan: " + remaining);
    }

    /**
     * Saldırı modunu açar/kapatır
     */
    private void toggleAttackMode() {
        attackMode = !attackMode;
        fortifyMode = false;
        selectedTerritory = null;
        targetTerritory = null;

        if (attackMode) {
            statusLabel.setText("Saldırı modu aktif. Saldıran bölgeyi seçin.");
            logToGameConsole("Saldırı modu başlatıldı.");
        } else {
            statusLabel.setText("Saldırı modu kapatıldı.");
            logToGameConsole("Saldırı modu kapatıldı.");
        }

        mapPanel.repaint();
    }

    private void initializeTerritoryPositions() {
// KUZEY AMERİKA (Sarı Bölge)
        territoryPositions.put("Alaska", new Point(91, 106)); // ✓ DOĞRU
        territoryPositions.put("Kuzeybatı Toprakları", new Point(206, 109));
        territoryPositions.put("Grönland", new Point(418, 60));
        territoryPositions.put("Alberta", new Point(186, 160));
        territoryPositions.put("Ontario", new Point(272, 179));
        territoryPositions.put("Quebec", new Point(344, 170));
        territoryPositions.put("Batı ABD", new Point(188, 235));
        territoryPositions.put("Doğu ABD", new Point(279, 254));
        territoryPositions.put("Orta Amerika", new Point(204, 321));

// GÜNEY AMERİKA (Turuncu Bölge)
        territoryPositions.put("Venezuela", new Point(284, 385));
        territoryPositions.put("Peru", new Point(246, 453));
        territoryPositions.put("Brezilya", new Point(373, 446));
        territoryPositions.put("Arjantin", new Point(313, 564));

// AVRUPA (Mavi Bölge)
        territoryPositions.put("İzlanda", new Point(511, 142));
        territoryPositions.put("Britanya", new Point(500, 209));
        territoryPositions.put("İskandinavya", new Point(607, 135));
        territoryPositions.put("Kuzey Avrupa", new Point(596, 227));
        territoryPositions.put("Batı Avrupa", new Point(508, 317));
        territoryPositions.put("Güney Avrupa", new Point(614, 287));
        territoryPositions.put("Ukrayna", new Point(704, 187));

// AFRİKA (Kahverengi Bölge)
        territoryPositions.put("Kuzey Afrika", new Point(535, 409)); // ✓ DOĞRU
        territoryPositions.put("Mısır", new Point(644, 383)); // ✓ DOĞRU
        territoryPositions.put("Doğu Afrika", new Point(706, 464));
        territoryPositions.put("Kongo", new Point(643, 503));
        territoryPositions.put("Güney Afrika", new Point(655, 599));
        territoryPositions.put("Madagaskar", new Point(761, 592));

// ASYA (Yeşil Bölge)
        territoryPositions.put("Ural", new Point(818, 158));
        territoryPositions.put("Sibirya", new Point(889, 110));
        territoryPositions.put("Yakutsk", new Point(971, 94)); // ✓ DOĞRU
        territoryPositions.put("Kamçatka", new Point(1072, 99));
        territoryPositions.put("Irkutsk", new Point(956, 182));
        territoryPositions.put("Moğolistan", new Point(969, 237));
        territoryPositions.put("Çin", new Point(956, 308));
        territoryPositions.put("Japonya", new Point(1088, 250));
        territoryPositions.put("Afganistan", new Point(807, 254));
        territoryPositions.put("Hindistan", new Point(879, 391)); // ✓ DOĞRU
        territoryPositions.put("Orta Doğu", new Point(735, 364));
        territoryPositions.put("Güneydoğu Asya", new Point(962, 385));

// AVUSTRALYA (Mor Bölge)
        territoryPositions.put("Endonezya", new Point(985, 488));
        territoryPositions.put("Yeni Gine", new Point(1084, 467));
        territoryPositions.put("Batı Avustralya", new Point(1024, 580));
        territoryPositions.put("Doğu Avustralya", new Point(1136, 576));

    }

    private void initializeContinentColors() {
        continentColors.put("Avrupa", new Color(100, 149, 237, 80));
        continentColors.put("Afrika", new Color(255, 165, 0, 80));
        continentColors.put("Asya", new Color(144, 238, 144, 80));
    }

    private void initializeUI() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        try {
            worldMap = ImageIO.read(getClass().getResourceAsStream("/risk_map.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        setSize(1200, 700);
        setMinimumSize(new Dimension(900, 600));
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        mapPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;

                if (worldMap != null) {
                    g2d.drawImage(worldMap, 0, 0, getWidth(), getHeight(), null);
                }

                // Harita üstüne overlay'ler
                drawTerritories(g2d);
            }
        };

        mapPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int realX = e.getX() * 1200 / mapPanel.getWidth();
                int realY = e.getY() * 700 / mapPanel.getHeight();
                handleMapClick(new Point(realX, realY));
            }
        });

        mainPanel.add(mapPanel, BorderLayout.CENTER);

        JPanel rightPanel = createRightPanel();
        mainPanel.add(rightPanel, BorderLayout.EAST);

        JPanel logPanel = createLogPanel();
        mainPanel.add(logPanel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);

        createDiceDialog();
        JMenuBar menuBar = createMenuBar();
        setJMenuBar(menuBar);
        setupKeyBindings();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void updateMap(String data) {
        territories.clear();
        String[] entries = data.split(";");
        for (String entry : entries) {
            String[] parts = entry.split(":");
            if (parts.length >= 3) {
                String name = parts[0];
                int owner = Integer.parseInt(parts[1]);
                int troops = Integer.parseInt(parts[2]);

                Territory t = new Territory(name, owner, troops);
                territories.put(name, t);
            }
        }
        mapPanel.repaint();
    }

    /**
     * Oyun konsolu loglarını içeren alt paneli oluşturur
     */
    private JPanel createLogPanel() {
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("Oyun Konsolu"));
        logPanel.setPreferredSize(new Dimension(0, 100));

        gameLogArea = new JTextArea();
        gameLogArea.setEditable(false);
        gameLogArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        gameLogArea.setLineWrap(true);
        gameLogArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(gameLogArea);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        logPanel.add(scrollPane, BorderLayout.CENTER);

        return logPanel;
    }

    /**
     * Klavye kısayollarını tanımlar (P, A, F, E, ESC)
     */
    private void setupKeyBindings() {
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getRootPane().getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0), "placeTroops");
        actionMap.put("placeTroops", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (placeTroopsButton.isEnabled()) {
                    handlePlaceTroops();
                }
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0), "attack");
        actionMap.put("attack", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (attackButton.isEnabled()) {
                    toggleAttackMode();
                }
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0), "fortify");
        actionMap.put("fortify", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (fortifyButton.isEnabled()) {
                    toggleFortifyMode();
                }
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, 0), "endTurn");
        actionMap.put("endTurn", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (endTurnButton.isEnabled()) {
                    endTurn();
                }
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clearSelection");
        actionMap.put("clearSelection", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearSelectionAndModes();
            }
        });
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Oyun menüsü
        JMenu gameMenu = new JMenu("Oyun");

        JMenuItem exitItem = new JMenuItem("Çıkış");
        exitItem.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(this, "Oyundan çıkmak istiyor musunuz?",
                    "Çıkış", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                System.exit(0);
            }
        });

        gameMenu.add(exitItem);

        // Yardım menüsü
        JMenu helpMenu = new JMenu("Yardım");

        JMenuItem aboutItem = new JMenuItem("Hakkında");
        aboutItem.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    "Risk Oyunu v1.0\nGeliştirici: M. Noor", "Hakkında", JOptionPane.INFORMATION_MESSAGE);
        });

        JMenuItem shortcutItem = new JMenuItem("Kısayollar");
        shortcutItem.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    """
                P - Asker Yerleştir
                A - Saldırı
                F - Güçlendir
                E - Sırayı Bitir
                Esc - Seçimi Temizle
                """, "Klavye Kısayolları", JOptionPane.INFORMATION_MESSAGE);
        });

        helpMenu.add(shortcutItem);
        helpMenu.add(aboutItem);

        menuBar.add(gameMenu);
        menuBar.add(helpMenu);

        return menuBar;
    }

    private void createDiceDialog() {
        diceDialog = new JDialog(this, "Zar Atılıyor", true);
        diceDialog.setSize(400, 250);
        diceDialog.setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel dicePanel = new JPanel(new GridLayout(1, 2, 20, 0));

        attackerDicePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        attackerDicePanel.setBorder(BorderFactory.createTitledBorder("Saldıran"));

        defenderDicePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        defenderDicePanel.setBorder(BorderFactory.createTitledBorder("Savunan"));

        dicePanel.add(attackerDicePanel);
        dicePanel.add(defenderDicePanel);

        JLabel resultLabel = new JLabel("", SwingConstants.CENTER);
        resultLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        mainPanel.add(dicePanel, BorderLayout.CENTER);
        mainPanel.add(resultLabel, BorderLayout.SOUTH);

        diceDialog.add(mainPanel);
        diceDialog.setLocationRelativeTo(this);
    }

    private void drawMap(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawContinents(g2d);

        drawTerritories(g2d);
    }

    private void drawContinents(Graphics2D g2d) {
        drawContinent(g2d, new Point[]{
            territoryPositions.get("Türkiye"),
            territoryPositions.get("Almanya"),
            territoryPositions.get("Fransa"),
            territoryPositions.get("Ukrayna"),
            territoryPositions.get("İsveç"),
            territoryPositions.get("İtalya")
        }, continentColors.get("Avrupa"));

        drawContinent(g2d, new Point[]{
            territoryPositions.get("Mısır"),
            territoryPositions.get("Fas")
        }, continentColors.get("Afrika"));

        drawContinent(g2d, new Point[]{
            territoryPositions.get("Çin"),
            territoryPositions.get("Hindistan"),
            territoryPositions.get("Japonya"),
            territoryPositions.get("Rusya"),
            territoryPositions.get("Güney Kore"),
            territoryPositions.get("Suudi Arabistan")
        }, continentColors.get("Asya"));

    }

    private void drawContinent(Graphics2D g2d, Point[] points, Color color) {
        if (points.length < 3) {
            return;
        }

        int padding = 50;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;

        for (Point p : points) {
            minX = Math.min(minX, p.x);
            minY = Math.min(minY, p.y);
            maxX = Math.max(maxX, p.x);
            maxY = Math.max(maxY, p.y);
        }

        minX -= padding;
        minY -= padding;
        maxX += padding;
        maxY += padding;

        g2d.setColor(color);
        g2d.fillOval(minX, minY, maxX - minX, maxY - minY);
    }

    private void drawConnection(Graphics2D g2d, String t1, String t2) {
        Point p1 = territoryPositions.get(t1);
        Point p2 = territoryPositions.get(t2);
        if (p1 == null || p2 == null) {
            return;
        }

        int w = mapPanel.getWidth();
        int h = mapPanel.getHeight();

        int x1 = p1.x * w / MAP_WIDTH;
        int y1 = p1.y * h / MAP_HEIGHT;
        int x2 = p2.x * w / MAP_WIDTH;
        int y2 = p2.y * h / MAP_HEIGHT;

        g2d.drawLine(x1, y1, x2, y2);
    }

    private void drawTerritories(Graphics2D g2d) {
        for (Map.Entry<String, Territory> entry : territories.entrySet()) {
            String name = entry.getKey();
            Territory t = entry.getValue();
            if (territoryPositions.containsKey(name)) {
                drawTerritory(g2d, name, t);
            }
        }
    }

    private void drawDiceResults(List<Integer> attacker, List<Integer> defender) {
        attackerDicePanel.removeAll();
        defenderDicePanel.removeAll();

        for (int val : attacker) {
            JLabel die = new JLabel(String.valueOf(val));
            die.setFont(new Font("Arial", Font.BOLD, 24));
            die.setBorder(new EmptyBorder(10, 10, 10, 10));
            attackerDicePanel.add(die);
        }

        for (int val : defender) {
            JLabel die = new JLabel(String.valueOf(val));
            die.setFont(new Font("Arial", Font.BOLD, 24));
            die.setBorder(new EmptyBorder(10, 10, 10, 10));
            defenderDicePanel.add(die);
        }

        diceDialog.revalidate();
        diceDialog.repaint();
        diceDialog.setVisible(true);
    }

    private void drawTerritory(Graphics2D g2d, String name, Territory t) {
        Point pos = territoryPositions.get(name);
        if (pos == null) {
            return;
        }

        int panelWidth = mapPanel.getWidth();
        int panelHeight = mapPanel.getHeight();

        int scaledX = pos.x * panelWidth / MAP_WIDTH;
        int scaledY = pos.y * panelHeight / MAP_HEIGHT;
        int radius = 20 * panelWidth / MAP_WIDTH;

        boolean isSelected = name.equals(selectedTerritory);
        boolean isTarget = highlightedTargets.contains(name);

        Color background = getPlayerColor(t.getOwner());
        g2d.setColor(background);
        g2d.fillOval(scaledX - radius, scaledY - radius, radius * 2, radius * 2);

        if (isSelected) {
            g2d.setColor(SELECTED_COLOR);
            g2d.fillOval(scaledX - radius, scaledY - radius, radius * 2, radius * 2);
        } else if (isTarget) {
            // Seçilen bölgenin sahibine göre komşu bölgeleri farklı renklerde göster
            Territory selectedT = selectedTerritory != null ? territories.get(selectedTerritory) : null;
            if (selectedT != null) {
                // Seçilen bölge bizimse ve komşu bölge de bizimse yeşil, değilse kırmızımsı
                if (selectedT.getOwner() == playerId) {
                    // Komşu bölgenin sahibine göre renk belirle
                    if (t.getOwner() == playerId) {
                        g2d.setColor(FRIENDLY_TARGET_COLOR); // Kendi bölgemize komşu olan kendi bölgemiz
                    } else {
                        g2d.setColor(ENEMY_TARGET_COLOR); // Kendi bölgemize komşu olan rakip bölge
                    }
                } else {
                    // Seçilen bölge rakibinse ve komşu bölge bizimse yeşil, değilse kırmızımsı
                    if (t.getOwner() == playerId) {
                        g2d.setColor(FRIENDLY_TARGET_COLOR); // Rakip bölgeye komşu olan kendi bölgemiz
                    } else {
                        g2d.setColor(ENEMY_TARGET_COLOR); // Rakip bölgeye komşu olan rakip bölge
                    }
                }
            } else {
                g2d.setColor(TARGET_COLOR); // Seçili bölge yoksa eski rengi kullan
            }
            g2d.fillOval(scaledX - radius, scaledY - radius, radius * 2, radius * 2);
        }

        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.drawOval(scaledX - radius, scaledY - radius, radius * 2, radius * 2);

        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(name);
        g2d.drawString(name, scaledX - textWidth / 2, scaledY - radius - 5);

        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        fm = g2d.getFontMetrics();
        String troops = String.valueOf(t.getTroops());
        int tw = fm.stringWidth(troops);
        g2d.drawString(troops, scaledX - tw / 2, scaledY + 5);
    }

    private void handleMapClick(Point point) {
        if (gameOver) {
            return;
        }

        String clicked = findClickedTerritory(point);
        if (clicked == null) {
            return;
        }

        if (currentTurn != playerId) {
            logToGameConsole("Sıra sizde değil.");
            return;
        }

        int troopsLeft = playerTroopsToPlace.getOrDefault(playerId, 0);
        if (troopsLeft > 0) {
            Territory t = territories.get(clicked);
            if (t != null && t.getOwner() == playerId) {
                String input = JOptionPane.showInputDialog(this,
                        "Kaç asker yerleştirilsin? (max " + troopsLeft + ")", "Asker Yerleştir", JOptionPane.QUESTION_MESSAGE);
                try {
                    int num = Integer.parseInt(input);
                    if (num > 0 && num <= troopsLeft) {
                        sendCommand("PLACE_TROOPS " + clicked + " " + num);
                    } else {
                        logToGameConsole("Geçersiz asker sayısı.");
                    }
                } catch (NumberFormatException e) {
                    logToGameConsole("Geçersiz giriş.");
                }
            } else {
                logToGameConsole("Kendi bölgenizi seçmelisiniz.");
            }
            return;
        }

        if (attackMode) {
            handleAttackModeClick(clicked);
            return;
        }

        if (fortifyMode) {
            handleFortifyModeClick(clicked);
            return;
        }

        Territory t = territories.get(clicked);
        if (t != null && t.getOwner() == playerId) {
            setSelectedTerritory(clicked); // artık bu şekilde seçiliyor
            statusLabel.setText(clicked + " seçildi.");
        }
    }

    private String findClickedTerritory(Point point) {
        for (Map.Entry<String, Point> entry : territoryPositions.entrySet()) {
            Point p = entry.getValue();
            if (point.distance(p) <= 35) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void clearSelectionAndModes() {
        removeHighlights();
        statusLabel.setText("Seçim temizlendi.");
        logToGameConsole("Seçimler sıfırlandı.");
        mapPanel.repaint();
    }

    private void handleAttackModeClick(String clicked) {
        Territory t = territories.get(clicked);
        if (selectedTerritory == null) {
            if (t.getOwner() == playerId && t.getTroops() > 1) {
                setSelectedTerritory(clicked);

                statusLabel.setText("Hedef seçin");
                mapPanel.repaint();
            } else {
                logToGameConsole("Saldırmak için en az 2 askerli bir bölge seçin.");
            }
        } else {
            if (t.getOwner() != playerId && areNeighbors(selectedTerritory, clicked)) {
                targetTerritory = clicked;
                String input = JOptionPane.showInputDialog("Kaç zarla saldırı yapılacak? (1-3)");
                try {
                    int dice = Integer.parseInt(input);
                    sendCommand("ATTACK " + selectedTerritory + " " + targetTerritory + " " + dice);
                } catch (NumberFormatException e) {
                    logToGameConsole("Geçersiz zar sayısı.");
                }
            } else {
                logToGameConsole("Geçersiz hedef.");
            }
        }
    }

    private void handleFortifyModeClick(String clicked) {
        Territory t = territories.get(clicked);
        if (selectedTerritory == null) {
            if (t.getOwner() == playerId && t.getTroops() > 1) {
                selectedTerritory = clicked;
                statusLabel.setText("Hedef bölge seçin.");
                mapPanel.repaint();
            } else {
                logToGameConsole("Güçlendirmek için yeterli askeriniz yok.");
            }
        } else {
            if (t.getOwner() == playerId && areConnected(selectedTerritory, clicked)) {
                targetTerritory = clicked;
                Territory from = territories.get(selectedTerritory);
                int max = from.getTroops() - 1;
                String input = JOptionPane.showInputDialog("Kaç asker taşınsın? (1-" + max + ")");
                try {
                    int num = Integer.parseInt(input);
                    sendCommand("FORTIFY " + selectedTerritory + " " + targetTerritory + " " + num);
                } catch (NumberFormatException e) {
                    logToGameConsole("Geçersiz asker sayısı.");
                }
            } else {
                logToGameConsole("Sadece kendi bölgeleriniz arasında geçiş yapabilirsiniz.");
            }
        }
    }

    private boolean areNeighbors(String a, String b) {
        return adjacencyMap.getOrDefault(a, Collections.emptySet()).contains(b);
    }

    private boolean areConnected(String a, String b) {
        // Şimdilik sadece doğrudan komşuluk
        return areNeighbors(a, b);
    }

    private void sendCommand(String command) {
        out.println(command);
        logToGameConsole("Gönderildi: " + command);
    }

    private void handleAdjacencyMessage(String data) {
        adjacencyMap.clear();
        String[] parts = data.split(";");
        for (String entry : parts) {
            if (entry.isBlank()) {
                continue;
            }
            String[] tokens = entry.split(":");
            if (tokens.length != 2) {
                continue;
            }
            String territory = tokens[0];
            Set<String> neighbors = new HashSet<>(List.of(tokens[1].split(",")));
            adjacencyMap.put(territory, neighbors);
        }
        logToGameConsole("Komşuluk verileri güncellendi.");
    }

private void startListening() {
    executor.submit(() -> {
        try {
            String msg;
            while ((msg = in.readLine()) != null) {
                final String message = msg;
                SwingUtilities.invokeLater(() -> processMessage(message));
            }
        } catch (SocketException e) {
            SwingUtilities.invokeLater(() -> {
                logToGameConsole("Sunucu ile bağlantı kesildi: " + e.getMessage());
                JOptionPane.showMessageDialog(this, "Sunucu ile bağlantı kesildi. Oyun kapatılacak.",
                        "Bağlantı Hatası", JOptionPane.ERROR_MESSAGE);
                cleanup();
                System.exit(1);
            });
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> {
                logToGameConsole("Sunucu ile iletişim hatası: " + e.getMessage());
                JOptionPane.showMessageDialog(this, "Sunucu ile iletişim kesildi: " + e.getMessage(),
                        "Bağlantı Hatası", JOptionPane.ERROR_MESSAGE);
                cleanup();
                System.exit(1);
            });
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> {
                logToGameConsole("Beklenmeyen hata: " + e.getMessage());
                JOptionPane.showMessageDialog(this, "Beklenmeyen bir hata oluştu: " + e.getMessage(),
                        "Hata", JOptionPane.ERROR_MESSAGE);
                cleanup();
                System.exit(1);
            });
        }
    });
}

    private void processMessage(String message) {
        logToGameConsole("Alındı: " + message);
        String[] parts = message.split(" ", 2);
        String command = parts[0];
        String data = parts.length > 1 ? parts[1] : "";

        switch (command) {
            case "RESTART_PROMPT" -> {
                int result = JOptionPane.showConfirmDialog(this,
                        "Rakibiniz oyundan ayrıldı. Yeni bir rakiple oynamak ister misiniz?",
                        "Oyun Bitti", JOptionPane.YES_NO_OPTION);

                if (result == JOptionPane.YES_OPTION) {
                    sendCommand("RESTART");
                } else {
                    sendCommand("RESTART_DECLINE");
                    JOptionPane.showMessageDialog(this, "Oyun kapatılıyor. Görüşmek üzere!");
                    cleanup();
                    System.exit(0);
                }
            }

            case "INIT" ->
                handleInitCommand(data);

            case "MAP" ->
                updateMap(data);
            case "TURN" ->
                handleTurnCommand(data);
            case "PLACE_RESULT" ->
                handlePlaceResult(data);
            case "ATTACK_RESULT" ->
                handleAttackResult(data);
            case "FORTIFY_RESULT" ->
                handleFortifyResult(data);
            case "GAME_OVER" ->
                handleGameOverCommand(data);
            case "ADJACENCY" ->
                handleAdjacencyMessage(data);

            default -> {
                if (command.equals("ERROR")) {
                    logToGameConsole("Hata: " + data);
                } else if (command.equals("INFO")) {
                    logToGameConsole(data);
                    if (data.contains("Yeni bir rakip bekleniyor")) {
                        statusLabel.setText("Yeni bir rakip bekleniyor...");
                        enableButtons(false);
                    }

                    if (data.contains("Diğer oyuncudan yeniden başlatma isteği")) {
                        int answer = JOptionPane.showConfirmDialog(this,
                                "Rakip oyunu yeniden başlatmak istiyor. Kabul ediyor musunuz?",
                                "Yeniden Başlatma İsteği", JOptionPane.YES_NO_OPTION);

                        if (answer == JOptionPane.YES_OPTION) {
                            sendCommand("RESTART");
                        } else {
                            sendCommand("RESTART_DECLINE");
                            logToGameConsole("Yeniden başlatma isteğini reddettiniz.");
                        }
                    }

                } else if (command.equals("EXIT")) {
                    JOptionPane.showMessageDialog(this,
                            "Oyun kapatılıyor. Görüşmek üzere!",
                            "Çıkış", JOptionPane.INFORMATION_MESSAGE);
                    cleanup();
                    System.exit(0);
                } else if (command.equals("DISCONNECT")) {
                    JOptionPane.showMessageDialog(this,
                            data,
                            "Bağlantı Kesildi", JOptionPane.WARNING_MESSAGE);
                    cleanup();
                    System.exit(0);
                } else {
                    logToGameConsole("Bilinmeyen komut: " + command);
                }
            }
        }
    }

    private void handleInitCommand(String data) {
        String[] parts = data.split(":", 2);
        playerId = Integer.parseInt(parts[0]);
        String name = (parts.length > 1) ? parts[1] : "Oyuncu " + playerId;

        playerNames.put(playerId, name);

        logToGameConsole("Oyuncu kimliğiniz: " + playerId + " (" + name + ")");

        Color c = getPlayerColor(playerId);
        String colorName = getColorName(c);
        playerColorLabel.setText("Renginiz: " + colorName);
        playerColorLabel.setForeground(c);  // Rengi GUI'de göster
    }

    private String getColorName(Color c) {
        if (c.equals(new Color(220, 20, 60))) {
            return "Kırmızı";
        }
        if (c.equals(new Color(30, 144, 255))) {
            return "Mavi";
        }
        return "Bilinmeyen";
    }

    private void handleMapCommand(String data) {
        territories.clear();
        String[] tokens = data.split(";");
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            String[] fields = token.split(":");
            if (fields.length < 3) {
                continue;
            }
            String name = fields[0];
            int owner = Integer.parseInt(fields[1]);
            int troops = Integer.parseInt(fields[2]);
            territories.put(name, new Territory(name, owner, troops));
        }
        mapPanel.repaint();
    }

     private void handleTurnCommand(String data) {
        String[] parts = data.split(":");
        int turn = Integer.parseInt(parts[0]);
        int troops = Integer.parseInt(parts[1]);
        String name = (parts.length >= 3) ? parts[2] : "Oyuncu " + turn;

        currentTurn = turn;
        // Seçimleri sıfırla
        removeHighlights();
        mapPanel.repaint();
        playerTroopsToPlace.put(turn, troops);

        if (turn == playerId) {
            logToGameConsole("Sıra sizde! " + troops + " asker yerleştirin.");
            statusLabel.setText("Sıra sizde!");
            enableButtons(true);
            startTurnTimer();
        } else {
            logToGameConsole("Rakibin sırası.");
            statusLabel.setText("Sıra: " + name);
            enableButtons(false);
        }

        troopsLeftLabel.setText("Kalan Asker: " + troops);
        selectedTerritory = null;
        targetTerritory = null;
        mapPanel.repaint();
    }

  

    private void handlePlaceResult(String data) {
        String[] parts = data.split(":");
        String territory = parts[0];
        int troops = Integer.parseInt(parts[1]);

        Territory t = territories.get(territory);
        if (t != null) {
            t.setTroops(troops);
        }

        int updated = Integer.parseInt(parts[2]);
        playerTroopsToPlace.put(playerId, updated);
        troopsLeftLabel.setText("Kalan Asker: " + updated);
        mapPanel.repaint();

        if (updated == 0 && currentTurn == playerId) {
            enableButtons(true);
        }
    }

    private void handleAttackResult(String data) {
        String[] parts = data.split(":");
        String from = parts[0];
        String to = parts[1];
        int attackerLoss = Integer.parseInt(parts[2]);
        int defenderLoss = Integer.parseInt(parts[3]);

        Territory attacker = territories.get(from);
        Territory defender = territories.get(to);

        if (attacker != null) {
            attacker.removeTroops(attackerLoss);
        }
        if (defender != null) {
            defender.removeTroops(defenderLoss);
        }

        if (defender.getTroops() <= 0 && attacker != null) {
            defender.setOwner(attacker.getOwner()); // playerId yerine saldıran bölgenin sahibi
            defender.setTroops(1);
            attacker.removeTroops(1);
            logToGameConsole(to + " ele geçirildi!");
        }

        removeHighlights();
        mapPanel.repaint();
    }

    private void handleFortifyResult(String data) {
        String[] parts = data.split(":");
        String from = parts[0];
        String to = parts[1];
        int moved = Integer.parseInt(parts[2]);

        Territory src = territories.get(from);
        Territory dst = territories.get(to);
        if (src != null) {
            src.removeTroops(moved);
        }
        if (dst != null) {
            dst.addTroops(moved);
        }

        removeHighlights();
        mapPanel.repaint();
    }

    private void handleGameOverCommand(String data) {
        gameOver = true;
        removeHighlights();
        mapPanel.repaint();

        try {
            int winnerId = Integer.parseInt(data.trim());

            String message = (winnerId == playerId)
                    ? "Tebrikler, kazandınız!"
                    : "Oyunu kaybettiniz. Kazanan: " + playerNames.get(winnerId);

            int choice = JOptionPane.showConfirmDialog(this,
                    message + "\nYeniden başlatılsın mı?",
                    "Oyun Bitti", JOptionPane.YES_NO_OPTION);

            if (choice == JOptionPane.YES_OPTION) {
                sendCommand("RESTART");
                logToGameConsole("Yeniden başlatma istendi.");
            } else {
                sendCommand("RESTART_DECLINE");
                logToGameConsole("Yeniden başlatma isteğini reddettiniz.");
            }

        } catch (NumberFormatException e) {
            // Eğer gelen veri sayı değilse, mesaj olarak göster
            JOptionPane.showMessageDialog(this, data, "Oyun Bitti", JOptionPane.INFORMATION_MESSAGE);
            sendCommand("RESTART_DECLINE"); // güvenli çıkış
        }

        statusLabel.setText("Oyun bitti.");
        enableButtons(false);
    }

    private void enableButtons(boolean enable) {
        if (enable && currentTurn != playerId) {
            enable = false; // sıran değilse aktif etme
        }

        int troops = playerTroopsToPlace.getOrDefault(playerId, 0);
        placeTroopsButton.setEnabled(enable && troops > 0);
        attackButton.setEnabled(enable && troops == 0);
        fortifyButton.setEnabled(enable && troops == 0);
        endTurnButton.setEnabled(enable && troops == 0);
    }

    /**
     * Vurgulamaları kaldırır ve seçimleri sıfırlar
     */
    private void removeHighlights() {
        selectedTerritory = null;
        targetTerritory = null;
        attackMode = false;
        fortifyMode = false;
        highlightedTargets.clear();
    }
    
    private void cleanup() {
        try {
            if (socket != null) {
                socket.close();
            }
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        executor.shutdownNow();
    }

    @Override
    public void dispose() {
        cleanup();
        super.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String ip = JOptionPane.showInputDialog("Sunucu IP'si", "127.0.0.1");
            if (ip == null || ip.isBlank()) {
                ip = "127.0.0.1";
            }

            String playerName = null;
            boolean isValidName = false;
            
            while (!isValidName) {
                playerName = JOptionPane.showInputDialog("Oyuncu adınızı girin:");
                if (playerName != null && !playerName.trim().isEmpty()) {
                    isValidName = true;
                } else {
                    JOptionPane.showMessageDialog(null, "Lütfen geçerli bir isim girin!", 
                            "Hata", JOptionPane.ERROR_MESSAGE);
                }
            }

            new RiskClient(ip, 9090, playerName);
        });
    }

}
