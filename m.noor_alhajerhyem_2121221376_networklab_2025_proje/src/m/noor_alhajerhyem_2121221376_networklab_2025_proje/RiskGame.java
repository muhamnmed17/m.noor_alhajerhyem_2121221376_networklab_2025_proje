package final_project;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Risk oyun mantığını yöneten ve oyun durumunu tutan sınıf
 */
public class RiskGame {

    private Map<String, Territory> territories = new HashMap<>();
    private Map<String, List<String>> adjacencyMap = new HashMap<>();  // Komşuluk haritası
    private Map<String, String> territoryToContinentMap = new HashMap<>();  // Bölge-kıta ilişkisi
    private Map<String, List<String>> continentTerritories = new HashMap<>();  // Kıta-bölgeler ilişkisi
    private Map<String, Integer> continentBonus = new HashMap<>();  // Kıta bonusları

    private Map<Integer, Integer> playerTroopsToPlace = new HashMap<>();
    private static final int INITIAL_TROOPS = 20;  // Her oyuncunun başlangıçta alacağı asker sayısı
    private static final int MIN_TROOPS_PER_TURN = 3;  // Bir turda minimum alınacak asker sayısı
    private boolean gameOver = false;

    /**
     * Oyunu başlatır ve başlangıç durumunu ayarlar
     */
    public void initializeGame(int playerId1, int playerId2) {
        createTerritories();
        defineAdjacencies();
        defineContinents();
        distributeTerritories(playerId1, playerId2);

        playerTroopsToPlace.put(playerId1, INITIAL_TROOPS);
        playerTroopsToPlace.put(playerId2, INITIAL_TROOPS);
    }

    /**
     * Bölgeleri oluşturur
     */
    private void createTerritories() {
        // Avrupa
        territories.put("Türkiye", new Territory("Türkiye", -1, 0));
        territories.put("Almanya", new Territory("Almanya", -1, 0));
        territories.put("Fransa", new Territory("Fransa", -1, 0));

        // Afrika
        territories.put("Mısır", new Territory("Mısır", -1, 0));
        territories.put("Fas", new Territory("Fas", -1, 0));

        // Asya
        territories.put("Çin", new Territory("Çin", -1, 0));
        territories.put("Hindistan", new Territory("Hindistan", -1, 0));
        territories.put("Japonya", new Territory("Japonya", -1, 0));

        // Avrupa
        territories.put("Ukrayna", new Territory("Ukrayna", -1, 0));
        territories.put("İsveç", new Territory("İsveç", -1, 0));
        territories.put("İtalya", new Territory("İtalya", -1, 0));

// Asya
        territories.put("Rusya", new Territory("Rusya", -1, 0));
        territories.put("Güney Kore", new Territory("Güney Kore", -1, 0));
        territories.put("Suudi Arabistan", new Territory("Suudi Arabistan", -1, 0));

    }

    /**
     * Komşuluk ilişkilerini tanımlar
     */
    private void defineAdjacencies() {
        // Türkiye'nin komşuları
        adjacencyMap.put("Türkiye", Arrays.asList("Almanya", "Fransa", "Mısır", "Hindistan", "Ukrayna", "İtalya", "Suudi Arabistan"));

        // Almanya'nın komşuları
        adjacencyMap.put("Almanya", Arrays.asList("Türkiye", "Fransa", "Ukrayna", "İsveç"));

        // Fransa'nın komşuları
        adjacencyMap.put("Fransa", Arrays.asList("Türkiye", "Almanya", "Fas", "İtalya"));

        // Mısır'ın komşuları
        adjacencyMap.put("Mısır", Arrays.asList("Fas", "Türkiye", "Suudi Arabistan"));

        // Fas'ın komşuları
        adjacencyMap.put("Fas", Arrays.asList("Fransa", "Mısır"));

        // Hindistan'ın komşuları
        adjacencyMap.put("Hindistan", Arrays.asList("Türkiye", "Çin", "Suudi Arabistan"));

        // Çin'in komşuları
        adjacencyMap.put("Çin", Arrays.asList("Hindistan", "Japonya", "Rusya", "Güney Kore"));

        // Japonya'nın komşuları
        adjacencyMap.put("Japonya", Arrays.asList("Çin", "Güney Kore"));

        // Yeni Komşuluklar
        adjacencyMap.put("Ukrayna", Arrays.asList("Türkiye", "Almanya", "Rusya"));
        adjacencyMap.put("İsveç", Arrays.asList("Almanya", "Rusya"));
        adjacencyMap.put("İtalya", Arrays.asList("Fransa", "Türkiye"));

        adjacencyMap.put("Rusya", Arrays.asList("Ukrayna", "İsveç", "Çin"));
        adjacencyMap.put("Güney Kore", Arrays.asList("Japonya", "Çin"));
        adjacencyMap.put("Suudi Arabistan", Arrays.asList("Hindistan", "Mısır", "Türkiye"));

    }

    /**
     * Kıta-bölge ilişkilerini ve bonusları tanımlar
     */
    private void defineContinents() {
        // Kıtalar ve içerdiği bölgeler
        continentTerritories.put("Avrupa", Arrays.asList("Türkiye", "Almanya", "Fransa", "Ukrayna", "İsveç", "İtalya"));
        continentTerritories.put("Afrika", Arrays.asList("Mısır", "Fas"));
        continentTerritories.put("Asya", Arrays.asList("Çin", "Hindistan", "Japonya", "Rusya", "Güney Kore", "Suudi Arabistan"));

        // Bölgelerin hangi kıtada olduğu
        territoryToContinentMap.put("Türkiye", "Avrupa");
        territoryToContinentMap.put("Almanya", "Avrupa");
        territoryToContinentMap.put("Fransa", "Avrupa");
        territoryToContinentMap.put("Mısır", "Afrika");
        territoryToContinentMap.put("Fas", "Afrika");
        territoryToContinentMap.put("Çin", "Asya");
        territoryToContinentMap.put("Hindistan", "Asya");
        territoryToContinentMap.put("Japonya", "Asya");
        territoryToContinentMap.put("Ukrayna", "Avrupa");
        territoryToContinentMap.put("İsveç", "Avrupa");
        territoryToContinentMap.put("İtalya", "Avrupa");

        territoryToContinentMap.put("Rusya", "Asya");
        territoryToContinentMap.put("Güney Kore", "Asya");
        territoryToContinentMap.put("Suudi Arabistan", "Asya");

        // Kıta bonusları
        continentBonus.put("Avrupa", 3);  // Avrupa'nın tüm bölgelerine sahip olan 3 asker bonus alır
        continentBonus.put("Afrika", 2);  // Afrika'nın tüm bölgelerine sahip olan 2 asker bonus alır
        continentBonus.put("Asya", 4);    // Asya'nın tüm bölgelerine sahip olan 4 asker bonus alır
    }

    /**
     * Bölgeleri rastgele dağıtır
     */
    private void distributeTerritories(int player1Id, int player2Id) {
        List<String> territoryNames = new ArrayList<>(territories.keySet());
        Collections.shuffle(territoryNames);

        for (int i = 0; i < territoryNames.size(); i++) {
            String territory = territoryNames.get(i);
            int owner = (i % 2 == 0) ? player1Id : player2Id;
            territories.get(territory).setOwner(owner);
            territories.get(territory).setTroops(1);
        }
    }

    /**
     * Sırayı bir sonraki oyuncuya geçirir ve asker dağıtımını hesaplar
     */
    /**
     * Bir oyuncunun sahip olduğu bölge sayısını döndürür
     */
    private int countPlayerTerritories(int playerId) {
        int count = 0;
        for (Territory t : territories.values()) {
            if (t.getOwner() == playerId) {
                count++;
            }
        }
        return count;
    }

    /**
     * Bir oyuncunun sahip olduğu kıtalardan gelen toplam bonusu hesaplar
     */
    private int calculateContinentBonus(int playerId) {
        int bonus = 0;

        for (String continent : continentTerritories.keySet()) {
            boolean ownsContinent = true;
            for (String territory : continentTerritories.get(continent)) {
                if (territories.get(territory).getOwner() != playerId) {
                    ownsContinent = false;
                    break;
                }
            }

            if (ownsContinent) {
                bonus += continentBonus.get(continent);
            }
        }

        return bonus;
    }

    /**
     * Bir oyuncunun bir bölgeye asker yerleştirmesini sağlar
     *
     * @return İşlem başarılı ise true, değilse false
     */
    public boolean placeTroops(int playerId, String territoryName, int troopCount) {

        Territory territory = territories.get(territoryName);
        if (territory == null || territory.getOwner() != playerId) {
            return false;
        }

        int availableTroops = playerTroopsToPlace.getOrDefault(playerId, 0);
        if (troopCount <= 0 || troopCount > availableTroops) {
            return false;
        }

        territory.addTroops(troopCount);
        playerTroopsToPlace.put(playerId, availableTroops - troopCount);

        System.out.println("Oyuncu " + playerId + ", " + territoryName + "'ye " + troopCount
                + " asker yerleştirdi. Kalan: " + playerTroopsToPlace.get(playerId));

        return true;
    }

    /**
     * Saldırı işlemini gerçekleştirir
     *
     * @return [saldıran kayıp, savunan kayıp] dizisi veya null (başarısız
     * saldırı)
     */
    public int[] attack(int playerId, String fromTerritory, String toTerritory, int attackDice, StringBuilder errorMessage) {

        Territory from = territories.get(fromTerritory);
        Territory to = territories.get(toTerritory);

        if (from == null || to == null) {
            errorMessage.append("Bölge bulunamadı.");
            return null;
        }

        if (from.getOwner() != playerId) {
            errorMessage.append("Kaynak bölge size ait değil.");
            return null;
        }

        if (to.getOwner() == playerId) {
            errorMessage.append("Kendi bölgenize saldırı yapamazsınız.");
            return null;
        }

        if (!areNeighbors(fromTerritory, toTerritory)) {
            errorMessage.append("Bu bölgeler komşu değil.");
            return null;
        }

        if (from.getTroops() <= 1) {
            errorMessage.append("Saldırı yapmak için en az 2 askere ihtiyacınız var.");
            return null;
        }

        if (attackDice < 1 || attackDice > 3) {
            errorMessage.append("Zar sayısı 1 ile 3 arasında olmalı.");
            return null;
        }

        if (attackDice >= from.getTroops()) {
            errorMessage.append("Bu kadar zar atamazsınız. En fazla " + (from.getTroops() - 1) + " zar atabilirsiniz.");
            return null;
        }

        // Saldıran zarları
        List<Integer> attackerDice = rollDice(attackDice);

        // Savunan zarları (en fazla 2 zar)
        int defendDice = Math.min(2, to.getTroops());
        List<Integer> defenderDice = rollDice(defendDice);

        // Zarları büyükten küçüğe sırala
        Collections.sort(attackerDice, Collections.reverseOrder());
        Collections.sort(defenderDice, Collections.reverseOrder());

        System.out.println("Saldıran zarlar: " + attackerDice);
        System.out.println("Savunan zarlar: " + defenderDice);

        int comparisons = Math.min(attackerDice.size(), defenderDice.size());
        int attackerLosses = 0;
        int defenderLosses = 0;

        for (int i = 0; i < comparisons; i++) {
            if (attackerDice.get(i) > defenderDice.get(i)) {
                defenderLosses++;
            } else {
                attackerLosses++;
            }
        }

        from.removeTroops(attackerLosses);
        to.removeTroops(defenderLosses);

        System.out.println("Saldıran kayıp: " + attackerLosses + ", Savunan kayıp: " + defenderLosses);

        if (to.getTroops() <= 0) {
            to.setOwner(playerId);
            to.setTroops(1);
            from.removeTroops(1);
            System.out.println(toTerritory + " ele geçirildi!");
        }

        return new int[]{attackerLosses, defenderLosses};
    }

    /**
     * Güçlendirme işlemini gerçekleştirir
     */
    public boolean fortify(int playerId, String fromTerritory, String toTerritory, int troops) {

        Territory from = territories.get(fromTerritory);
        Territory to = territories.get(toTerritory);

        if (from == null || to == null) {
            return false;
        }
        if (from.getOwner() != playerId || to.getOwner() != playerId) {
            return false;
        }
        if (!areNeighbors(fromTerritory, toTerritory)) {
            return false;
        }
        if (troops <= 0 || from.getTroops() <= troops) {
            return false;
        }

        from.removeTroops(troops);
        to.addTroops(troops);

        System.out.println("Oyuncu " + playerId + " " + fromTerritory + " -> " + toTerritory + " bölgesine " + troops + " asker taşıdı.");
        return true;
    }

    /**
     * Oyunun kazanılıp kazanılmadığını kontrol eder
     *
     * @return Kazanan oyuncunun ID'si, oyun devam ediyorsa -1
     */
    public int checkWinner() {
        int owner = -1;
        for (Territory t : territories.values()) {
            if (owner == -1) {
                owner = t.getOwner();
            } else if (t.getOwner() != owner) {
                return -1;
            }
        }
        return owner;
    }

    /**
     * Oyunun anlık harita durumunu string olarak döndürür
     */
    public String getMapState() {
        return territories.values().stream()
                .map(Territory::toString)
                .collect(Collectors.joining(";"));
    }

    public int getTroopsToPlace(int playerId) {
        return playerTroopsToPlace.getOrDefault(playerId, 0);
    }

    public int getTerritoryTroops(String territoryName) {
        Territory t = territories.get(territoryName);
        return (t != null) ? t.getTroops() : 0;
    }

    public void calculateTroopsFor(int playerId) {
        int territoryCount = countPlayerTerritories(playerId);
        int continentBonus = calculateContinentBonus(playerId);
        int troops = Math.max(MIN_TROOPS_PER_TURN, territoryCount / 3) + continentBonus;
        playerTroopsToPlace.put(playerId, troops);
    }

    /**
     * İki bölge komşu mu?
     */
    public boolean areNeighbors(String a, String b) {
        return adjacencyMap.getOrDefault(a, Collections.emptyList()).contains(b);
    }

    public Map<String, List<String>> getAdjacencyMap() {
        return adjacencyMap;
    }

    /**
     * Belirli sayıda zar atar
     */
    private List<Integer> rollDice(int count) {
        Random rand = new Random();
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(rand.nextInt(6) + 1);
        }
        return result;
    }
}
