package game; 

import java.awt.*; 
import java.util.*; 
import java.util.List; 

/**
 * Oyunun mevcut durumunu tutan sınıf
 */ // Sınıfın açıklaması
public class GameState { // Oyun durumu sınıfının tanımlanması
    private int[] playerOrder = new int[2]; // Match'teki oyuncu sırası
    private int playerId = -1; // Mevcut oyuncunun ID'si, başlangıçta -1
    private int currentTurn = -1; // Şu anki sıranın sahibi oyuncu ID'si, başlangıçta -1
    private boolean gameOver = false; // Oyunun bitip bitmediğini gösteren bayrak
    private boolean attackMode = false; // Saldırı modunun aktif olup olmadığını gösteren bayrak
    private boolean fortifyMode = false; // Güçlendirme modunun aktif olup olmadığını gösteren bayrak
    
    private String selectedTerritory = null; // Seçili bölgenin adı, başlangıçta null
    private String targetTerritory = null; // Hedef bölgenin adı, başlangıçta null
    private List<String> highlightedTargets = new ArrayList<>(); // Vurgulanacak hedef bölgelerin listesi
    
    private Map<String, Territory> territories = new HashMap<>(); // Bölge adı -> Territory nesnesi haritası
    private Map<Integer, String> playerNames = new HashMap<>(); // Oyuncu ID -> Oyuncu adı haritası
    private Map<Integer, Integer> playerTroopsToPlace = new HashMap<>(); // Oyuncu ID -> Yerleştirilecek asker sayısı haritası
    private Map<String, String> continentOwners = new HashMap<>(); // Kıta adı -> Sahip oyuncu haritası
    private final Map<String, Set<String>> adjacencyMap = new HashMap<>(); // Bölge komşuluk haritası (değiştirilemez)
    
    // Sabit renkler
    public static final Color SELECTED_COLOR = new Color(255, 215, 0, 200); // Seçili bölge için altın sarısı renk (şeffaf)
    public static final Color FRIENDLY_TARGET_COLOR = new Color(50, 205, 50, 200); // Dost hedef bölgeler için yeşil renk (şeffaf)
    public static final Color ENEMY_TARGET_COLOR = new Color(255, 99, 71, 200); // Düşman hedef bölgeler için kırmızı renk (şeffaf)
    public static final Color TARGET_COLOR = new Color(50, 205, 50, 200); // Genel hedef bölgeler için yeşil renk (şeffaf)
    
    // Getter ve Setter metodları
    public int getPlayerId() { return playerId; } // Oyuncu ID'sini döndüren getter metodu
    public void setPlayerId(int playerId) { this.playerId = playerId; } // Oyuncu ID'sini ayarlayan setter metodu
    
    public int getCurrentTurn() { return currentTurn; } // Mevcut tur sahibini döndüren getter metodu
    public void setCurrentTurn(int currentTurn) { this.currentTurn = currentTurn; } // Mevcut tur sahibini ayarlayan setter metodu
    
    public boolean isGameOver() { return gameOver; } // Oyunun bitip bitmediğini döndüren getter metodu
    public void setGameOver(boolean gameOver) { this.gameOver = gameOver; } // Oyun bitiş durumunu ayarlayan setter metodu
    
    public boolean isAttackMode() { return attackMode; } // Saldırı modunu döndüren getter metodu
    public void setAttackMode(boolean attackMode) { this.attackMode = attackMode; } // Saldırı modunu ayarlayan setter metodu
    
    public boolean isFortifyMode() { return fortifyMode; } // Güçlendirme modunu döndüren getter metodu
    public void setFortifyMode(boolean fortifyMode) { this.fortifyMode = fortifyMode; } // Güçlendirme modunu ayarlayan setter metodu
    
    public String getSelectedTerritory() { return selectedTerritory; } // Seçili bölgeyi döndüren getter metodu
    public void setSelectedTerritory(String selectedTerritory) { // Seçili bölgeyi ayarlayan setter metodu
        this.selectedTerritory = selectedTerritory; // Seçili bölge değişkenine atama
        updateHighlightedTargets(); // Vurgulanacak hedefleri güncelleme metodu çağrısı
    }
    
    public String getTargetTerritory() { return targetTerritory; } // Hedef bölgeyi döndüren getter metodu
    public void setTargetTerritory(String targetTerritory) { this.targetTerritory = targetTerritory; } // Hedef bölgeyi ayarlayan setter metodu
    
    public List<String> getHighlightedTargets() { return highlightedTargets; } // Vurgulanan hedefleri döndüren getter metodu
    private Map<Integer, String> playerColors = new HashMap<>(); // Oyuncu renkleri

    public Map<String, Territory> getTerritories() { return territories; } // Bölgeler haritasını döndüren getter metodu
    public Map<Integer, String> getPlayerNames() { return playerNames; } // Oyuncu isimleri haritasını döndüren getter metodu
    public Map<Integer, Integer> getPlayerTroopsToPlace() { return playerTroopsToPlace; } // Yerleştirilecek askerler haritasını döndüren getter metodu
    public Map<String, String> getContinentOwners() { return continentOwners; } // Kıta sahipleri haritasını döndüren getter metodu
    public Map<String, Set<String>> getAdjacencyMap() { return adjacencyMap; } // Komşuluk haritasını döndüren getter metodu
    
    // Yardımcı metodlar
  public Color getPlayerColor(int playerId) {
    String colorName = playerColors.get(playerId);
    if ("RED".equals(colorName)) {
        return new Color(220, 20, 60); // Kırmızı
    } else if ("BLUE".equals(colorName)) {
        return new Color(30, 144, 255); // Mavi
    } else {
        return new Color(128, 128, 128); // Varsayılan gri
    }
}
  
    
public String getColorName(Color c) {
    if (c.equals(new Color(220, 20, 60))) return "Kırmızı";
    if (c.equals(new Color(30, 144, 255))) return "Mavi";
    return "Bilinmeyen";
}
    
    public boolean areNeighbors(String a, String b) { // İki bölgenin komşu olup olmadığını kontrol eden metod
        return adjacencyMap.getOrDefault(a, Collections.emptySet()).contains(b); // a bölgesinin komşuları arasında b'yi arama
    }
    
    public boolean areConnected(String a, String b) { // İki bölgenin bağlantılı olup olmadığını kontrol eden metod
        return areNeighbors(a, b); // Şu an komşuluk kontrolü ile aynı (gelecekte genişletilebilir)
    }
    
    public void updateMap(String data) { // Harita verilerini güncelleyen metod
        territories.clear(); // Mevcut bölgeler haritasını temizleme
        String[] entries = data.split(";"); // Veriyi noktalı virgülle bölerek dizi oluşturma
        for (String entry : entries) { // Her veri girişi için döngü
            String[] parts = entry.split(":"); // Veri girişini iki nokta üst üste ile bölme
            if (parts.length >= 3) { // En az 3 parça varsa (isim:sahip:asker)
                String name = parts[0]; // Bölge adını alma
                int owner = Integer.parseInt(parts[1]); // Sahip oyuncu ID'sini sayıya çevirme
                int troops = Integer.parseInt(parts[2]); // Asker sayısını sayıya çevirme
                Territory t = new Territory(name, owner, troops); // Yeni Territory nesnesi oluşturma
                territories.put(name, t); // Bölgeyi haritaya ekleme
            }
        }
    }
    
    public void updateAdjacency(String data) { // Komşuluk verilerini güncelleyen metod
        adjacencyMap.clear(); // Mevcut komşuluk haritasını temizleme
        String[] parts = data.split(";"); // Veriyi noktalı virgülle bölerek dizi oluşturma
        for (String entry : parts) { // Her veri girişi için döngü
            if (entry.isBlank()) continue; // Boş girişleri atlama
            String[] tokens = entry.split(":"); // Veri girişini iki nokta üst üste ile bölme
            if (tokens.length != 2) continue; // Tam olarak 2 parça yoksa atlama
            String territory = tokens[0]; // Bölge adını alma
            Set<String> neighbors = new HashSet<>(Arrays.asList(tokens[1].split(","))); // Komşuları virgülle bölerek set oluşturma
            adjacencyMap.put(territory, neighbors); // Bölge ve komşularını haritaya ekleme
        }
    }
    
    private void updateHighlightedTargets() { // Vurgulanacak hedefleri güncelleyen özel metod
        highlightedTargets.clear(); // Mevcut vurgulanan hedefleri temizleme
        if (selectedTerritory != null && adjacencyMap.containsKey(selectedTerritory)) { // Seçili bölge varsa ve komşuluk haritasında varsa
            highlightedTargets.addAll(adjacencyMap.get(selectedTerritory)); // Seçili bölgenin tüm komşularını vurgulanan hedeflere ekleme
        }
    }
    
    public void clearSelections() { // Tüm seçimleri temizleyen metod
        selectedTerritory = null; // Seçili bölgeyi null yapma
        targetTerritory = null; // Hedef bölgeyi null yapma
        attackMode = false; // Saldırı modunu kapatma
        fortifyMode = false; // Güçlendirme modunu kapatma
        highlightedTargets.clear(); // Vurgulanan hedefleri temizleme
    }
    
    public int getRemainingTroops() { // Kalan asker sayısını döndüren metod
        return playerTroopsToPlace.getOrDefault(playerId, 0); // Oyuncunun yerleştirilecek asker sayısını döndürme, yoksa 0
    }
    
    public void setPlayerOrder(int[] order) {
    this.playerOrder = order;
}
    
    public int[] getPlayerOrder() {
    return playerOrder;
}
    
public void setPlayerColor(int playerId, String color) {
    playerColors.put(playerId, color);
}
} 
