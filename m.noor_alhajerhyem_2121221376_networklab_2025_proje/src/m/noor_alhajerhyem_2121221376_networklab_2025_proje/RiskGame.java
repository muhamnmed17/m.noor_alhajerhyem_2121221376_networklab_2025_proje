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
private ClientHandler player1;
private ClientHandler player2;

    private Map<Integer, Integer> playerTroopsToPlace = new HashMap<>();
    private static final int INITIAL_TROOPS = 20;  // Her oyuncunun başlangıçta alacağı asker sayısı
    private static final int MIN_TROOPS_PER_TURN = 3;  // Bir turda minimum alınacak asker sayısı
    private boolean gameOver = false;

    /**
     * Oyunu başlatır ve başlangıç durumunu ayarlar
     */
public void initializeGame(ClientHandler player1, ClientHandler player2) {
    this.player1 = player1;
    this.player2 = player2;
    createTerritories();
    defineAdjacencies();
    defineContinents();
    distributeTerritories(player1.getPlayerId(), player2.getPlayerId());
    playerTroopsToPlace.put(player1.getPlayerId(), INITIAL_TROOPS);
    playerTroopsToPlace.put(player2.getPlayerId(), INITIAL_TROOPS);
}


private int getOwnedTerritoryCount(int playerId) {
    int count = 0;
    for (Territory t : territories.values()) {
        if (t.getOwner() == playerId) count++;
    }
    return count;
}

    /**
     * Bölgeleri oluşturur
     */
    private void createTerritories() {
        // Kuzey Amerika
        territories.put("Alaska", new Territory("Alaska", -1, 0));
        territories.put("Kuzeybatı Toprakları", new Territory("Kuzeybatı Toprakları", -1, 0));
        territories.put("Grönland", new Territory("Grönland", -1, 0));
        territories.put("Alberta", new Territory("Alberta", -1, 0));
        territories.put("Ontario", new Territory("Ontario", -1, 0));
        territories.put("Quebec", new Territory("Quebec", -1, 0));
        territories.put("Batı ABD", new Territory("Batı ABD", -1, 0));
        territories.put("Doğu ABD", new Territory("Doğu ABD", -1, 0));
        territories.put("Orta Amerika", new Territory("Orta Amerika", -1, 0));

        // Güney Amerika
        territories.put("Venezuela", new Territory("Venezuela", -1, 0));
        territories.put("Peru", new Territory("Peru", -1, 0));
        territories.put("Brezilya", new Territory("Brezilya", -1, 0));
        territories.put("Arjantin", new Territory("Arjantin", -1, 0));

        // Avrupa
        territories.put("İzlanda", new Territory("İzlanda", -1, 0));
        territories.put("Britanya", new Territory("Britanya", -1, 0));
        territories.put("İskandinavya", new Territory("İskandinavya", -1, 0));
        territories.put("Batı Avrupa", new Territory("Batı Avrupa", -1, 0));
        territories.put("Güney Avrupa", new Territory("Güney Avrupa", -1, 0));
        territories.put("Kuzey Avrupa", new Territory("Kuzey Avrupa", -1, 0));
        territories.put("Ukrayna", new Territory("Ukrayna", -1, 0));

        // Afrika
        territories.put("Kuzey Afrika", new Territory("Kuzey Afrika", -1, 0));
        territories.put("Mısır", new Territory("Mısır", -1, 0));
        territories.put("Doğu Afrika", new Territory("Doğu Afrika", -1, 0));
        territories.put("Kongo", new Territory("Kongo", -1, 0));
        territories.put("Güney Afrika", new Territory("Güney Afrika", -1, 0));
        territories.put("Madagaskar", new Territory("Madagaskar", -1, 0));

        // Asya
        territories.put("Ural", new Territory("Ural", -1, 0));
        territories.put("Sibirya", new Territory("Sibirya", -1, 0));
        territories.put("Yakutsk", new Territory("Yakutsk", -1, 0));
        territories.put("Kamçatka", new Territory("Kamçatka", -1, 0));
        territories.put("Irkutsk", new Territory("Irkutsk", -1, 0));
        territories.put("Moğolistan", new Territory("Moğolistan", -1, 0));
        territories.put("Japonya", new Territory("Japonya", -1, 0));
        territories.put("Çin", new Territory("Çin", -1, 0));
        territories.put("Hindistan", new Territory("Hindistan", -1, 0));
        territories.put("Afganistan", new Territory("Afganistan", -1, 0));
        territories.put("Orta Doğu", new Territory("Orta Doğu", -1, 0));
        territories.put("Güneydoğu Asya", new Territory("Güneydoğu Asya", -1, 0));

        // Avustralya
        territories.put("Endonezya", new Territory("Endonezya", -1, 0));
        territories.put("Yeni Gine", new Territory("Yeni Gine", -1, 0));
        territories.put("Batı Avustralya", new Territory("Batı Avustralya", -1, 0));
        territories.put("Doğu Avustralya", new Territory("Doğu Avustralya", -1, 0));
    }

    /**
     * Komşuluk ilişkilerini tanımlar
     */
    private void defineAdjacencies() {
        adjacencyMap.put("Alaska", Arrays.asList("Kamçatka", "Kuzeybatı Toprakları", "Alberta"));
        adjacencyMap.put("Kuzeybatı Toprakları", Arrays.asList("Alaska", "Alberta", "Ontario", "Grönland"));
        adjacencyMap.put("Grönland", Arrays.asList("İzlanda", "Quebec", "Ontario", "Kuzeybatı Toprakları"));
        adjacencyMap.put("Alberta", Arrays.asList("Alaska", "Kuzeybatı Toprakları", "Ontario", "Batı ABD"));
        adjacencyMap.put("Ontario", Arrays.asList("Grönland", "Quebec", "Batı ABD", "Doğu ABD", "Alberta", "Kuzeybatı Toprakları"));
        adjacencyMap.put("Quebec", Arrays.asList("Grönland", "Doğu ABD", "Ontario"));
        adjacencyMap.put("Batı ABD", Arrays.asList("Alberta", "Ontario", "Doğu ABD", "Orta Amerika"));
        adjacencyMap.put("Doğu ABD", Arrays.asList("Quebec", "Ontario", "Batı ABD", "Orta Amerika"));
        adjacencyMap.put("Orta Amerika", Arrays.asList("Batı ABD", "Doğu ABD", "Venezuela"));

        adjacencyMap.put("Venezuela", Arrays.asList("Orta Amerika", "Peru", "Brezilya"));
        adjacencyMap.put("Peru", Arrays.asList("Venezuela", "Brezilya", "Arjantin"));
        adjacencyMap.put("Brezilya", Arrays.asList("Kuzey Afrika", "Arjantin", "Venezuela", "Peru"));
        adjacencyMap.put("Arjantin", Arrays.asList("Brezilya", "Peru"));

        adjacencyMap.put("İzlanda", Arrays.asList("Grönland", "Britanya", "İskandinavya"));
        adjacencyMap.put("Britanya", Arrays.asList("İzlanda", "İskandinavya", "Kuzey Avrupa", "Batı Avrupa"));
        adjacencyMap.put("İskandinavya", Arrays.asList("İzlanda", "Britanya", "Kuzey Avrupa", "Ukrayna"));
        adjacencyMap.put("Batı Avrupa", Arrays.asList("Britanya", "Kuzey Avrupa", "Güney Avrupa", "Kuzey Afrika"));
        adjacencyMap.put("Güney Avrupa", Arrays.asList("Batı Avrupa", "Kuzey Avrupa", "Ukrayna", "Orta Doğu", "Mısır", "Kuzey Afrika"));
        adjacencyMap.put("Kuzey Avrupa", Arrays.asList("İskandinavya", "Britanya", "Batı Avrupa", "Güney Avrupa", "Ukrayna"));
        adjacencyMap.put("Ukrayna", Arrays.asList("İskandinavya", "Kuzey Avrupa", "Güney Avrupa", "Orta Doğu", "Afganistan", "Ural"));

        adjacencyMap.put("Kuzey Afrika", Arrays.asList("Batı Avrupa", "Güney Avrupa", "Mısır", "Brezilya", "Kongo", "Doğu Afrika"));
        adjacencyMap.put("Mısır", Arrays.asList("Kuzey Afrika", "Güney Avrupa", "Orta Doğu", "Doğu Afrika"));
        adjacencyMap.put("Doğu Afrika", Arrays.asList("Mısır", "Orta Doğu", "Kuzey Afrika", "Kongo", "Güney Afrika", "Madagaskar"));
        adjacencyMap.put("Kongo", Arrays.asList("Kuzey Afrika", "Doğu Afrika", "Güney Afrika"));
        adjacencyMap.put("Güney Afrika", Arrays.asList("Kongo", "Doğu Afrika", "Madagaskar"));
        adjacencyMap.put("Madagaskar", Arrays.asList("Doğu Afrika", "Güney Afrika"));

        adjacencyMap.put("Orta Doğu", Arrays.asList("Güney Avrupa", "Ukrayna", "Afganistan", "Hindistan", "Mısır", "Doğu Afrika"));
        adjacencyMap.put("Afganistan", Arrays.asList("Ukrayna", "Ural", "Çin", "Hindistan", "Orta Doğu"));
        adjacencyMap.put("Ural", Arrays.asList("Ukrayna", "Afganistan", "Çin", "Sibirya"));
        adjacencyMap.put("Sibirya", Arrays.asList("Ural", "Çin", "Moğolistan", "Irkutsk", "Yakutsk"));
        adjacencyMap.put("Yakutsk", Arrays.asList("Sibirya", "Irkutsk", "Kamçatka"));
        adjacencyMap.put("Kamçatka", Arrays.asList("Yakutsk", "Irkutsk", "Moğolistan", "Japonya", "Alaska"));
        adjacencyMap.put("Irkutsk", Arrays.asList("Sibirya", "Moğolistan", "Kamçatka", "Yakutsk"));
        adjacencyMap.put("Moğolistan", Arrays.asList("Sibirya", "Çin", "Japonya", "Kamçatka", "Irkutsk"));
        adjacencyMap.put("Japonya", Arrays.asList("Kamçatka", "Moğolistan"));
        adjacencyMap.put("Çin", Arrays.asList("Ural", "Afganistan", "Hindistan", "Güneydoğu Asya", "Moğolistan", "Sibirya"));
        adjacencyMap.put("Hindistan", Arrays.asList("Orta Doğu", "Afganistan", "Çin", "Güneydoğu Asya"));
        adjacencyMap.put("Güneydoğu Asya", Arrays.asList("Çin", "Hindistan", "Endonezya"));

        adjacencyMap.put("Endonezya", Arrays.asList("Güneydoğu Asya", "Batı Avustralya", "Yeni Gine"));
        adjacencyMap.put("Batı Avustralya", Arrays.asList("Endonezya", "Doğu Avustralya", "Yeni Gine"));
        adjacencyMap.put("Doğu Avustralya", Arrays.asList("Batı Avustralya", "Yeni Gine"));
        adjacencyMap.put("Yeni Gine", Arrays.asList("Endonezya", "Batı Avustralya", "Doğu Avustralya"));
    }

    /**
     * Kıta-bölge ilişkilerini ve bonusları tanımlar
     */
    private void defineContinents() {
        // Kıtalar ve içerdiği bölgeler
        continentTerritories.put("Kuzey Amerika", Arrays.asList(
                "Alaska", "Kuzeybatı Toprakları", "Alberta", "Ontario", "Quebec",
                "Grönland", "Batı ABD", "Doğu ABD", "Orta Amerika"
        ));

        continentTerritories.put("Güney Amerika", Arrays.asList(
                "Venezuela", "Peru", "Brezilya", "Arjantin"
        ));

        continentTerritories.put("Avrupa", Arrays.asList(
                "İzlanda", "Britanya", "İskandinavya", "Batı Avrupa",
                "Kuzey Avrupa", "Güney Avrupa", "Ukrayna"
        ));

        continentTerritories.put("Afrika", Arrays.asList(
                "Kuzey Afrika", "Mısır", "Doğu Afrika",
                "Kongo", "Güney Afrika", "Madagaskar"
        ));

        continentTerritories.put("Asya", Arrays.asList(
                "Ural", "Sibirya", "Yakutsk", "Kamçatka", "Irkutsk",
                "Moğolistan", "Japonya", "Çin", "Hindistan",
                "Afganistan", "Orta Doğu", "Güneydoğu Asya"
        ));

        continentTerritories.put("Avustralya", Arrays.asList(
                "Endonezya", "Yeni Gine", "Batı Avustralya", "Doğu Avustralya"
        ));

        // Bölgelerin hangi kıtada olduğu
        for (Map.Entry<String, List<String>> entry : continentTerritories.entrySet()) {
            String continent = entry.getKey();
            for (String territory : entry.getValue()) {
                territoryToContinentMap.put(territory, continent);
            }
        }

        // Kıta bonusları
        continentBonus.put("Kuzey Amerika", 5);
        continentBonus.put("Güney Amerika", 2);
        continentBonus.put("Avrupa", 5);
        continentBonus.put("Afrika", 3);
        continentBonus.put("Asya", 7);
        continentBonus.put("Avustralya", 2);
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

    public Map<String, Territory> getTerritories() {
        return territories;
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
