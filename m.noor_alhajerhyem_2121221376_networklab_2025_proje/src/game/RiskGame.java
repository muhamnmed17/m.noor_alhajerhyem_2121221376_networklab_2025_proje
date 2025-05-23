package game; 

import network.ClientHandler;
import java.util.*; 
import java.util.stream.Collectors; 

/**
 * Risk oyun mantığını yöneten ve oyun durumunu tutan sınıf
 */
public class RiskGame { 

    private Map<String, Territory> territories = new HashMap<>(); // Tüm bölgeleri (territory) tutan HashMap
    private Map<String, List<String>> adjacencyMap = new HashMap<>();  // Komşuluk haritası - hangi bölgelerin komşu olduğunu tutar
    private Map<String, String> territoryToContinentMap = new HashMap<>();  // Bölge-kıta ilişkisi - her bölgenin hangi kıtada olduğunu tutar
    private Map<String, List<String>> continentTerritories = new HashMap<>();  // Kıta-bölgeler ilişkisi - her kıtanın hangi bölgeleri içerdiğini tutar
    private Map<String, Integer> continentBonus = new HashMap<>();  // Kıta bonusları - her kıtayı kontrol eden oyuncunun alacağı bonus asker sayısı
private ClientHandler player1; // Birinci oyuncunun client handler'ı
private ClientHandler player2; // İkinci oyuncunun client handler'ı

    private Map<Integer, Integer> playerTroopsToPlace = new HashMap<>(); // Her oyuncunun yerleştirmesi gereken asker sayısını tutar
    private static final int INITIAL_TROOPS = 20;  // Her oyuncunun başlangıçta alacağı asker sayısı sabit değeri
    private static final int MIN_TROOPS_PER_TURN = 3;  // Bir turda minimum alınacak asker sayısı sabit değeri
    private boolean gameOver = false; // Oyunun bitip bitmediğini kontrol eden boolean değişken

 
/**
 * Oyunu başlatır ve başlangıç durumunu ayarlar
 */
public void initializeGame(ClientHandler player1, ClientHandler player2) { // Oyunu başlatan metod
    // ✅ ÖNCE TÜM OYUN DURUMUNU TEMİZLE
    territories.clear();                    // Eski bölge durumlarını temizle
    adjacencyMap.clear();                   // Komşuluk haritasını temizle  
    territoryToContinentMap.clear();        // Bölge-kıta ilişkisini temizle
    continentTerritories.clear();           // Kıta-bölgeler ilişkisini temizle
    continentBonus.clear();                 // Kıta bonuslarını temizle
    playerTroopsToPlace.clear();            // Oyuncu asker sayılarını temizle
    gameOver = false;                       // Oyun durumunu sıfırla
    
    // Şimdi temiz bir şekilde başlat
    this.player1 = player1; // Birinci oyuncuyu sınıf değişkenine atar
    this.player2 = player2; // İkinci oyuncuyu sınıf değişkenine atar
    createTerritories(); // Tüm bölgeleri oluşturan metodu çağırır
    defineAdjacencies(); // Bölgeler arası komşuluk ilişkilerini tanımlayan metodu çağırır
    defineContinents(); // Kıtaları ve kıta bonuslarını tanımlayan metodu çağırır
    distributeTerritories(player1.getPlayerId(), player2.getPlayerId()); // Bölgeleri oyuncular arasında dağıtan metodu çağırır
    playerTroopsToPlace.put(player1.getPlayerId(), INITIAL_TROOPS); // Birinci oyuncunun başlangıç asker sayısını ayarlar
    playerTroopsToPlace.put(player2.getPlayerId(), INITIAL_TROOPS); // İkinci oyuncunun başlangıç asker sayısını ayarlar
    
    System.out.println("✅ RiskGame tamamen temizlendi ve yeniden başlatıldı.");
} // initializeGame metodunun sonu


private int getOwnedTerritoryCount(int playerId) { // Belirli bir oyuncunun sahip olduğu bölge sayısını döndüren metod
    int count = 0; // Sayacı sıfır olarak başlatır
    for (Territory t : territories.values()) { // Tüm bölgeler üzerinde döngü başlatır
        if (t.getOwner() == playerId) count++; // Eğer bölgenin sahibi belirtilen oyuncu ise sayacı artırır
    } // for döngüsünün sonu
    return count; // Toplam bölge sayısını döndürür
} // getOwnedTerritoryCount metodunun sonu

    /**
     * Bölgeleri oluşturur
     */
    private void createTerritories() { // Tüm bölgeleri oluşturan metod
        // Kuzey Amerika
        territories.put("Alaska", new Territory("Alaska", -1, 0)); // Alaska bölgesini oluşturur ve haritaya ekler
        territories.put("Kuzeybatı Toprakları", new Territory("Kuzeybatı Toprakları", -1, 0)); // Kuzeybatı Toprakları bölgesini oluşturur
        territories.put("Grönland", new Territory("Grönland", -1, 0)); // Grönland bölgesini oluşturur
        territories.put("Alberta", new Territory("Alberta", -1, 0)); // Alberta bölgesini oluşturur
        territories.put("Ontario", new Territory("Ontario", -1, 0)); // Ontario bölgesini oluşturur
        territories.put("Quebec", new Territory("Quebec", -1, 0)); // Quebec bölgesini oluşturur
        territories.put("Batı ABD", new Territory("Batı ABD", -1, 0)); // Batı ABD bölgesini oluşturur
        territories.put("Doğu ABD", new Territory("Doğu ABD", -1, 0)); // Doğu ABD bölgesini oluşturur
        territories.put("Orta Amerika", new Territory("Orta Amerika", -1, 0)); // Orta Amerika bölgesini oluşturur

        // Güney Amerika
        territories.put("Venezuela", new Territory("Venezuela", -1, 0)); // Venezuela bölgesini oluşturur
        territories.put("Peru", new Territory("Peru", -1, 0)); // Peru bölgesini oluşturur
        territories.put("Brezilya", new Territory("Brezilya", -1, 0)); // Brezilya bölgesini oluşturur
        territories.put("Arjantin", new Territory("Arjantin", -1, 0)); // Arjantin bölgesini oluşturur

        // Avrupa
        territories.put("İzlanda", new Territory("İzlanda", -1, 0)); // İzlanda bölgesini oluşturur
        territories.put("Britanya", new Territory("Britanya", -1, 0)); // Britanya bölgesini oluşturur
        territories.put("İskandinavya", new Territory("İskandinavya", -1, 0)); // İskandinavya bölgesini oluşturur
        territories.put("Batı Avrupa", new Territory("Batı Avrupa", -1, 0)); // Batı Avrupa bölgesini oluşturur
        territories.put("Güney Avrupa", new Territory("Güney Avrupa", -1, 0)); // Güney Avrupa bölgesini oluşturur
        territories.put("Kuzey Avrupa", new Territory("Kuzey Avrupa", -1, 0)); // Kuzey Avrupa bölgesini oluşturur
        territories.put("Ukrayna", new Territory("Ukrayna", -1, 0)); // Ukrayna bölgesini oluşturur

        // Afrika
        territories.put("Kuzey Afrika", new Territory("Kuzey Afrika", -1, 0)); // Kuzey Afrika bölgesini oluşturur
        territories.put("Mısır", new Territory("Mısır", -1, 0)); // Mısır bölgesini oluşturur
        territories.put("Doğu Afrika", new Territory("Doğu Afrika", -1, 0)); // Doğu Afrika bölgesini oluşturur
        territories.put("Kongo", new Territory("Kongo", -1, 0)); // Kongo bölgesini oluşturur
        territories.put("Güney Afrika", new Territory("Güney Afrika", -1, 0)); // Güney Afrika bölgesini oluşturur
        territories.put("Madagaskar", new Territory("Madagaskar", -1, 0)); // Madagaskar bölgesini oluşturur

        // Asya
        territories.put("Ural", new Territory("Ural", -1, 0)); // Ural bölgesini oluşturur
        territories.put("Sibirya", new Territory("Sibirya", -1, 0)); // Sibirya bölgesini oluşturur
        territories.put("Yakutsk", new Territory("Yakutsk", -1, 0)); // Yakutsk bölgesini oluşturur
        territories.put("Kamçatka", new Territory("Kamçatka", -1, 0)); // Kamçatka bölgesini oluşturur
        territories.put("Irkutsk", new Territory("Irkutsk", -1, 0)); // Irkutsk bölgesini oluşturur
        territories.put("Moğolistan", new Territory("Moğolistan", -1, 0)); // Moğolistan bölgesini oluşturur
        territories.put("Japonya", new Territory("Japonya", -1, 0)); // Japonya bölgesini oluşturur
        territories.put("Çin", new Territory("Çin", -1, 0)); // Çin bölgesini oluşturur
        territories.put("Hindistan", new Territory("Hindistan", -1, 0)); // Hindistan bölgesini oluşturur
        territories.put("Afganistan", new Territory("Afganistan", -1, 0)); // Afganistan bölgesini oluşturur
        territories.put("Orta Doğu", new Territory("Orta Doğu", -1, 0)); // Orta Doğu bölgesini oluşturur
        territories.put("Güneydoğu Asya", new Territory("Güneydoğu Asya", -1, 0)); // Güneydoğu Asya bölgesini oluşturur

        // Avustralya
        territories.put("Endonezya", new Territory("Endonezya", -1, 0)); // Endonezya bölgesini oluşturur
        territories.put("Yeni Gine", new Territory("Yeni Gine", -1, 0)); // Yeni Gine bölgesini oluşturur
        territories.put("Batı Avustralya", new Territory("Batı Avustralya", -1, 0)); // Batı Avustralya bölgesini oluşturur
        territories.put("Doğu Avustralya", new Territory("Doğu Avustralya", -1, 0)); // Doğu Avustralya bölgesini oluşturur
    } // createTerritories metodunun sonu

    /**
     * Komşuluk ilişkilerini tanımlar
     */
    private void defineAdjacencies() { // Bölgeler arası komşuluk ilişkilerini tanımlayan metod
        adjacencyMap.put("Alaska", Arrays.asList("Kamçatka", "Kuzeybatı Toprakları", "Alberta")); // Alaska'nın komşularını tanımlar
        adjacencyMap.put("Kuzeybatı Toprakları", Arrays.asList("Alaska", "Alberta", "Ontario", "Grönland")); // Kuzeybatı Toprakları'nın komşularını tanımlar
        adjacencyMap.put("Grönland", Arrays.asList("İzlanda", "Quebec", "Ontario", "Kuzeybatı Toprakları")); // Grönland'ın komşularını tanımlar
        adjacencyMap.put("Alberta", Arrays.asList("Alaska", "Kuzeybatı Toprakları", "Ontario", "Batı ABD")); // Alberta'nın komşularını tanımlar
        adjacencyMap.put("Ontario", Arrays.asList("Grönland", "Quebec", "Batı ABD", "Doğu ABD", "Alberta", "Kuzeybatı Toprakları")); // Ontario'nun komşularını tanımlar
        adjacencyMap.put("Quebec", Arrays.asList("Grönland", "Doğu ABD", "Ontario")); // Quebec'in komşularını tanımlar
        adjacencyMap.put("Batı ABD", Arrays.asList("Alberta", "Ontario", "Doğu ABD", "Orta Amerika")); // Batı ABD'nin komşularını tanımlar
        adjacencyMap.put("Doğu ABD", Arrays.asList("Quebec", "Ontario", "Batı ABD", "Orta Amerika")); // Doğu ABD'nin komşularını tanımlar
        adjacencyMap.put("Orta Amerika", Arrays.asList("Batı ABD", "Doğu ABD", "Venezuela")); // Orta Amerika'nın komşularını tanımlar

        adjacencyMap.put("Venezuela", Arrays.asList("Orta Amerika", "Peru", "Brezilya")); // Venezuela'nın komşularını tanımlar
        adjacencyMap.put("Peru", Arrays.asList("Venezuela", "Brezilya", "Arjantin")); // Peru'nun komşularını tanımlar
        adjacencyMap.put("Brezilya", Arrays.asList("Kuzey Afrika", "Arjantin", "Venezuela", "Peru")); // Brezilya'nın komşularını tanımlar
        adjacencyMap.put("Arjantin", Arrays.asList("Brezilya", "Peru")); // Arjantin'in komşularını tanımlar

        adjacencyMap.put("İzlanda", Arrays.asList("Grönland", "Britanya", "İskandinavya")); // İzlanda'nın komşularını tanımlar
        adjacencyMap.put("Britanya", Arrays.asList("İzlanda", "İskandinavya", "Kuzey Avrupa", "Batı Avrupa")); // Britanya'nın komşularını tanımlar
        adjacencyMap.put("İskandinavya", Arrays.asList("İzlanda", "Britanya", "Kuzey Avrupa", "Ukrayna")); // İskandinavya'nın komşularını tanımlar
        adjacencyMap.put("Batı Avrupa", Arrays.asList("Britanya", "Kuzey Avrupa", "Güney Avrupa", "Kuzey Afrika")); // Batı Avrupa'nın komşularını tanımlar
        adjacencyMap.put("Güney Avrupa", Arrays.asList("Batı Avrupa", "Kuzey Avrupa", "Ukrayna", "Orta Doğu", "Mısır", "Kuzey Afrika")); // Güney Avrupa'nın komşularını tanımlar
        adjacencyMap.put("Kuzey Avrupa", Arrays.asList("İskandinavya", "Britanya", "Batı Avrupa", "Güney Avrupa", "Ukrayna")); // Kuzey Avrupa'nın komşularını tanımlar
        adjacencyMap.put("Ukrayna", Arrays.asList("İskandinavya", "Kuzey Avrupa", "Güney Avrupa", "Orta Doğu", "Afganistan", "Ural")); // Ukrayna'nın komşularını tanımlar

        adjacencyMap.put("Kuzey Afrika", Arrays.asList("Batı Avrupa", "Güney Avrupa", "Mısır", "Brezilya", "Kongo", "Doğu Afrika")); // Kuzey Afrika'nın komşularını tanımlar
        adjacencyMap.put("Mısır", Arrays.asList("Kuzey Afrika", "Güney Avrupa", "Orta Doğu", "Doğu Afrika")); // Mısır'ın komşularını tanımlar
        adjacencyMap.put("Doğu Afrika", Arrays.asList("Mısır", "Orta Doğu", "Kuzey Afrika", "Kongo", "Güney Afrika", "Madagaskar")); // Doğu Afrika'nın komşularını tanımlar
        adjacencyMap.put("Kongo", Arrays.asList("Kuzey Afrika", "Doğu Afrika", "Güney Afrika")); // Kongo'nun komşularını tanımlar
        adjacencyMap.put("Güney Afrika", Arrays.asList("Kongo", "Doğu Afrika", "Madagaskar")); // Güney Afrika'nın komşularını tanımlar
        adjacencyMap.put("Madagaskar", Arrays.asList("Doğu Afrika", "Güney Afrika")); // Madagaskar'ın komşularını tanımlar

        adjacencyMap.put("Orta Doğu", Arrays.asList("Güney Avrupa", "Ukrayna", "Afganistan", "Hindistan", "Mısır", "Doğu Afrika")); // Orta Doğu'nun komşularını tanımlar
        adjacencyMap.put("Afganistan", Arrays.asList("Ukrayna", "Ural", "Çin", "Hindistan", "Orta Doğu")); // Afganistan'ın komşularını tanımlar
        adjacencyMap.put("Ural", Arrays.asList("Ukrayna", "Afganistan", "Çin", "Sibirya")); // Ural'ın komşularını tanımlar
        adjacencyMap.put("Sibirya", Arrays.asList("Ural", "Çin", "Moğolistan", "Irkutsk", "Yakutsk")); // Sibirya'nın komşularını tanımlar
        adjacencyMap.put("Yakutsk", Arrays.asList("Sibirya", "Irkutsk", "Kamçatka")); // Yakutsk'un komşularını tanımlar
        adjacencyMap.put("Kamçatka", Arrays.asList("Yakutsk", "Irkutsk", "Moğolistan", "Japonya", "Alaska")); // Kamçatka'nın komşularını tanımlar
        adjacencyMap.put("Irkutsk", Arrays.asList("Sibirya", "Moğolistan", "Kamçatka", "Yakutsk")); // Irkutsk'un komşularını tanımlar
        adjacencyMap.put("Moğolistan", Arrays.asList("Sibirya", "Çin", "Japonya", "Kamçatka", "Irkutsk")); // Moğolistan'ın komşularını tanımlar
        adjacencyMap.put("Japonya", Arrays.asList("Kamçatka", "Moğolistan")); // Japonya'nın komşularını tanımlar
        adjacencyMap.put("Çin", Arrays.asList("Ural", "Afganistan", "Hindistan", "Güneydoğu Asya", "Moğolistan", "Sibirya")); // Çin'in komşularını tanımlar
        adjacencyMap.put("Hindistan", Arrays.asList("Orta Doğu", "Afganistan", "Çin", "Güneydoğu Asya")); // Hindistan'ın komşularını tanımlar
        adjacencyMap.put("Güneydoğu Asya", Arrays.asList("Çin", "Hindistan", "Endonezya")); // Güneydoğu Asya'nın komşularını tanımlar

        adjacencyMap.put("Endonezya", Arrays.asList("Güneydoğu Asya", "Batı Avustralya", "Yeni Gine")); // Endonezya'nın komşularını tanımlar
        adjacencyMap.put("Batı Avustralya", Arrays.asList("Endonezya", "Doğu Avustralya", "Yeni Gine")); // Batı Avustralya'nın komşularını tanımlar
        adjacencyMap.put("Doğu Avustralya", Arrays.asList("Batı Avustralya", "Yeni Gine")); // Doğu Avustralya'nın komşularını tanımlar
        adjacencyMap.put("Yeni Gine", Arrays.asList("Endonezya", "Batı Avustralya", "Doğu Avustralya")); // Yeni Gine'nin komşularını tanımlar
    } // defineAdjacencies metodunun sonu

    /**
     * Kıta-bölge ilişkilerini ve bonusları tanımlar
     */
    private void defineContinents() { // Kıtaları ve bonuslarını tanımlayan metod
        // Kıtalar ve içerdiği bölgeler
        continentTerritories.put("Kuzey Amerika", Arrays.asList( // Kuzey Amerika kıtasının bölgelerini tanımlar
                "Alaska", "Kuzeybatı Toprakları", "Alberta", "Ontario", "Quebec", // Kuzey Amerika'ya ait bölgelerin listesi
                "Grönland", "Batı ABD", "Doğu ABD", "Orta Amerika" // Kuzey Amerika'ya ait bölgelerin devamı
        )); // Kuzey Amerika bölge listesinin sonu

        continentTerritories.put("Güney Amerika", Arrays.asList( // Güney Amerika kıtasının bölgelerini tanımlar
                "Venezuela", "Peru", "Brezilya", "Arjantin" // Güney Amerika'ya ait bölgelerin listesi
        )); // Güney Amerika bölge listesinin sonu

        continentTerritories.put("Avrupa", Arrays.asList( // Avrupa kıtasının bölgelerini tanımlar
                "İzlanda", "Britanya", "İskandinavya", "Batı Avrupa", // Avrupa'ya ait bölgelerin listesi
                "Kuzey Avrupa", "Güney Avrupa", "Ukrayna" // Avrupa'ya ait bölgelerin devamı
        )); // Avrupa bölge listesinin sonu

        continentTerritories.put("Afrika", Arrays.asList( // Afrika kıtasının bölgelerini tanımlar
                "Kuzey Afrika", "Mısır", "Doğu Afrika", // Afrika'ya ait bölgelerin listesi
                "Kongo", "Güney Afrika", "Madagaskar" // Afrika'ya ait bölgelerin devamı
        )); // Afrika bölge listesinin sonu

        continentTerritories.put("Asya", Arrays.asList( // Asya kıtasının bölgelerini tanımlar
                "Ural", "Sibirya", "Yakutsk", "Kamçatka", "Irkutsk", // Asya'ya ait bölgelerin listesi
                "Moğolistan", "Japonya", "Çin", "Hindistan", // Asya'ya ait bölgelerin devamı
                "Afganistan", "Orta Doğu", "Güneydoğu Asya" // Asya'ya ait bölgelerin sonu
        )); // Asya bölge listesinin sonu

        continentTerritories.put("Avustralya", Arrays.asList( // Avustralya kıtasının bölgelerini tanımlar
                "Endonezya", "Yeni Gine", "Batı Avustralya", "Doğu Avustralya" // Avustralya'ya ait bölgelerin listesi
        )); // Avustralya bölge listesinin sonu

        // Bölgelerin hangi kıtada olduğu
        for (Map.Entry<String, List<String>> entry : continentTerritories.entrySet()) { // Her kıta için döngü başlatır
            String continent = entry.getKey(); // Mevcut kıtanın ismini alır
            for (String territory : entry.getValue()) { // Kıtadaki her bölge için döngü başlatır
                territoryToContinentMap.put(territory, continent); // Bölge-kıta ilişkisini haritaya ekler
            } // iç for döngüsünün sonu
        } // dış for döngüsünün sonu

        // Kıta bonusları
        continentBonus.put("Kuzey Amerika", 5); // Kuzey Amerika'nın bonus asker sayısını 5 olarak ayarlar
        continentBonus.put("Güney Amerika", 2); // Güney Amerika'nın bonus asker sayısını 2 olarak ayarlar
        continentBonus.put("Avrupa", 5); // Avrupa'nın bonus asker sayısını 5 olarak ayarlar
        continentBonus.put("Afrika", 3); // Afrika'nın bonus asker sayısını 3 olarak ayarlar
        continentBonus.put("Asya", 7); // Asya'nın bonus asker sayısını 7 olarak ayarlar
        continentBonus.put("Avustralya", 2); // Avustralya'nın bonus asker sayısını 2 olarak ayarlar
    } // defineContinents metodunun sonu


    /**
     * Bölgeleri rastgele dağıtır
     */
  private void distributeTerritories(int player1Id, int player2Id) {
    List<String> territoryNames = new ArrayList<>(territories.keySet());
    Collections.shuffle(territoryNames);

    for (int i = 0; i < territoryNames.size(); i++) {
        String territory = territoryNames.get(i);
        // Her zaman: çift indeks → player1, tek indeks → player2
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
    private int countPlayerTerritories(int playerId) { // Belirli bir oyuncunun sahip olduğu bölge sayısını hesaplayan metod
        int count = 0; // Sayacı sıfır olarak başlatır
        for (Territory t : territories.values()) { // Tüm bölgeler üzerinde döngü başlatır
            if (t.getOwner() == playerId) { // Eğer bölgenin sahibi belirtilen oyuncu ise
                count++; // Sayacı bir artırır
            } // if koşulunun sonu
        } // for döngüsünün sonu
        return count; // Toplam bölge sayısını döndürür
    } // countPlayerTerritories metodunun sonu

    /**
     * Bir oyuncunun sahip olduğu kıtalardan gelen toplam bonusu hesaplar
     */
   private int calculateContinentBonus(int playerId) { // Oyuncunun kıta bonuslarını hesaplayan metod
        int bonus = 0; // Bonus sayacını sıfır olarak başlatır

        for (String continent : continentTerritories.keySet()) { // Her kıta için döngü başlatır
            boolean ownsContinent = true; // Kıtanın tamamen kontrol edilip edilmediğini tutan boolean
            for (String territory : continentTerritories.get(continent)) { // Kıtadaki her bölge için döngü başlatır
                if (territories.get(territory).getOwner() != playerId) { // Eğer bölgenin sahibi belirtilen oyuncu değilse
                    ownsContinent = false; // Kıta tamamen kontrol edilmiyor olarak işaretler
                    break; // Döngüden çıkar
                } // if koşulunun sonu
            } // iç for döngüsünün sonu

            if (ownsContinent) { // Eğer kıta tamamen kontrol ediliyorsa
                bonus += continentBonus.get(continent); // Kıta bonusunu toplama ekler
            } 
        } 

        return bonus; // Toplam bonusu döndürür
    } // calculateContinentBonus metodunun sonu

    /**
     * Bir oyuncunun bir bölgeye asker yerleştirmesini sağlar
     *
     * @return İşlem başarılı ise true, değilse false
     */
    public boolean placeTroops(int playerId, String territoryName, int troopCount) { // Asker yerleştirme metodu

        Territory territory = territories.get(territoryName); // Belirtilen bölgeyi alır
        if (territory == null || territory.getOwner() != playerId) { // Eğer bölge yoksa veya oyuncuya ait değilse
            return false; // İşlem başarısız olarak false döndürür
        } // if koşulunun sonu

        int availableTroops = playerTroopsToPlace.getOrDefault(playerId, 0); // Oyuncunun yerleştirebileceği asker sayısını alır
        if (troopCount <= 0 || troopCount > availableTroops) { // Eğer asker sayısı geçersizse veya elimizde o kadar asker yoksa
            return false; // İşlem başarısız olarak false döndürür
        } // if koşulunun sonu

        territory.addTroops(troopCount); // Bölgeye belirtilen sayıda asker ekler
        playerTroopsToPlace.put(playerId, availableTroops - troopCount); // Oyuncunun kalan asker sayısını günceller

        System.out.println("Oyuncu " + playerId + ", " + territoryName + "'ye " + troopCount // Konsola bilgi mesajı yazdırır
                + " asker yerleştirdi. Kalan: " + playerTroopsToPlace.get(playerId)); // Kalan asker sayısını da yazdırır

        return true; // İşlem başarılı olarak true döndürür
    } // placeTroops metodunun sonu

    /**
     * Saldırı işlemini gerçekleştirir
     *
     * @return [saldıran kayıp, savunan kayıp] dizisi veya null (başarısız
     * saldırı)
     */
    public int[] attack(int playerId, String fromTerritory, String toTerritory, int attackDice, StringBuilder errorMessage) { // Saldırı metodunu tanımlar

        Territory from = territories.get(fromTerritory); // Saldıran bölgeyi alır
        Territory to = territories.get(toTerritory); // Hedef bölgeyi alır

        if (from == null || to == null) { // Eğer bölgelerden biri yoksa
            errorMessage.append("Bölge bulunamadı."); // Hata mesajı ekler
            return null; // null döndürür
        } 

        if (from.getOwner() != playerId) { // Eğer saldıran bölge oyuncuya ait değilse
            errorMessage.append("Kaynak bölge size ait değil."); // Hata mesajı ekler
            return null; // null döndürür
        } 

        if (to.getOwner() == playerId) { // Eğer hedef bölge de aynı oyuncuya aitse
            errorMessage.append("Kendi bölgenize saldırı yapamazsınız."); // Hata mesajı ekler
            return null; // null döndürür
        } 

        if (!areNeighbors(fromTerritory, toTerritory)) { // Eğer bölgeler komşu değilse
            errorMessage.append("Bu bölgeler komşu değil."); // Hata mesajı ekler
            return null; // null döndürür
        } 

        if (from.getTroops() <= 1) { // Eğer saldıran bölgede yeterli asker yoksa
            errorMessage.append("Saldırı yapmak için en az 2 askere ihtiyacınız var."); // Hata mesajı ekler
            return null; // null döndürür
        } 

        if (attackDice < 1 || attackDice > 3) { // Eğer zar sayısı geçersizse
            errorMessage.append("Zar sayısı 1 ile 3 arasında olmalı."); // Hata mesajı ekler
            return null; // null döndürür
        } 

        if (attackDice >= from.getTroops()) { // Eğer atılacak zar sayısı asker sayısından fazlaysa
            errorMessage.append("Bu kadar zar atamazsınız. En fazla " + (from.getTroops() - 1) + " zar atabilirsiniz."); // Hata mesajı ekler
            return null; // null döndürür
        } 

        // Saldıran zarları
        List<Integer> attackerDice = rollDice(attackDice); // Saldıran için zar atar

        // Savunan zarları (en fazla 2 zar)
        int defendDice = Math.min(2, to.getTroops()); // Savunan zar sayısını hesaplar (en fazla 2, en az hedef bölgedeki asker sayısı)
        List<Integer> defenderDice = rollDice(defendDice); // Savunan için zar atar

        // Zarları büyükten küçüğe sırala
        Collections.sort(attackerDice, Collections.reverseOrder()); // Saldıran zarlarını büyükten küçüğe sıralar
        Collections.sort(defenderDice, Collections.reverseOrder()); // Savunan zarlarını büyükten küçüğe sıralar

        System.out.println("Saldıran zarlar: " + attackerDice); // Saldıran zarlarını konsola yazdırır
        System.out.println("Savunan zarlar: " + defenderDice); // Savunan zarlarını konsola yazdırır

        int comparisons = Math.min(attackerDice.size(), defenderDice.size()); // Kaç zar karşılaştırılacağını hesaplar
        int attackerLosses = 0; // Saldıran kayıplarını sıfır olarak başlatır
        int defenderLosses = 0; // Savunan kayıplarını sıfır olarak başlatır

        for (int i = 0; i < comparisons; i++) { // Her zar çifti için döngü başlatır
            if (attackerDice.get(i) > defenderDice.get(i)) { // Eğer saldıran zarı büyükse
                defenderLosses++; // Savunan kayıplarını artırır
            } else { // Değilse
                attackerLosses++; // Saldıran kayıplarını artırır
            } // if-else koşulunun sonu
        } // for döngüsünün sonu

        from.removeTroops(attackerLosses); // Saldıran bölgeden kayıp askerleri çıkarır
        to.removeTroops(defenderLosses); // Hedef bölgeden kayıp askerleri çıkarır

        System.out.println("Saldıran kayıp: " + attackerLosses + ", Savunan kayıp: " + defenderLosses); // Kayıpları konsola yazdırır

        if (to.getTroops() <= 0) { // Eğer hedef bölgede asker kalmadıysa
            to.setOwner(playerId); // Bölgenin sahibini saldıran oyuncu yapar
            to.setTroops(1); // Bölgeye 1 asker yerleştirir
            from.removeTroops(1); // Saldıran bölgeden 1 asker daha çıkarır
            System.out.println(toTerritory + " ele geçirildi!"); // Ele geçirme mesajını yazdırır
        } // if koşulunun sonu

        return new int[]{attackerLosses, defenderLosses}; // Kayıpları dizi olarak döndürür
    } // attack metodunun sonu

    /**
     * Güçlendirme işlemini gerçekleştirir
     */
    public boolean fortify(int playerId, String fromTerritory, String toTerritory, int troops) { // Güçlendirme metodunu tanımlar

        Territory from = territories.get(fromTerritory); // Kaynak bölgeyi alır
        Territory to = territories.get(toTerritory); // Hedef bölgeyi alır

        if (from == null || to == null) { // Eğer bölgelerden biri yoksa
            return false; // false döndürür
        } // if koşulunun sonu
        if (from.getOwner() != playerId || to.getOwner() != playerId) { // Eğer bölgelerden biri oyuncuya ait değilse
            return false; // false döndürür
        } // if koşulunun sonu
        if (!areNeighbors(fromTerritory, toTerritory)) { // Eğer bölgeler komşu değilse
            return false; // false döndürür
        } // if koşulunun sonu
        if (troops <= 0 || from.getTroops() <= troops) { // Eğer asker sayısı geçersizse veya kaynak bölgede yeterli asker yoksa
            return false; // false döndürür
        } // if koşulunun sonu

        from.removeTroops(troops); // Kaynak bölgeden askerleri çıkarır
        to.addTroops(troops); // Hedef bölgeye askerleri ekler

        System.out.println("Oyuncu " + playerId + " " + fromTerritory + " -> " + toTerritory + " bölgesine " + troops + " asker taşıdı."); // İşlem mesajını yazdırır
        return true; // true döndürür
    } 

    /**
     * Oyunun kazanılıp kazanılmadığını kontrol eder
     *
     * @return Kazanan oyuncunun ID'si, oyun devam ediyorsa -1
     */
    public int checkWinner() { // Kazanan kontrolü yapan metod
        int owner = -1; // İlk sahibi -1 olarak başlatır
        for (Territory t : territories.values()) { // Tüm bölgeler üzerinde döngü başlatır
            if (owner == -1) { // Eğer ilk sahip henüz belirlenmemişse
                owner = t.getOwner(); // İlk bölgenin sahibini kaydet
            } else if (t.getOwner() != owner) { // Eğer farklı bir sahip varsa
                return -1; // Oyun devam ediyor, -1 döndür
            } // if-else koşulunun sonu
        } // for döngüsünün sonu
        return owner; // Tek sahip varsa onun ID'sini döndür
    } 

    /**
     * Oyunun anlık harita durumunu string olarak döndürür
     */
    public String getMapState() { // Harita durumunu string olarak döndüren metod
        return territories.values().stream() // Tüm bölgeleri stream'e çevirir
                .map(Territory::toString) // Her bölgeyi string'e dönüştürür
                .collect(Collectors.joining(";")); // Noktalı virgülle birleştirip tek string yapar
    } 

    public int getTroopsToPlace(int playerId) { // Oyuncunun yerleştirmesi gereken asker sayısını döndüren metod
        return playerTroopsToPlace.getOrDefault(playerId, 0); // Oyuncunun asker sayısını döndürür, yoksa 0
    } 

    public int getTerritoryTroops(String territoryName) { // Belirli bir bölgedeki asker sayısını döndüren metod
        Territory t = territories.get(territoryName); // Bölgeyi alır
        return (t != null) ? t.getTroops() : 0; // Bölge varsa asker sayısını, yoksa 0 döndürür
    } 

    public Map<String, Territory> getTerritories() { // Tüm bölgeleri döndüren metod
        return territories; // Bölgeler haritasını döndürür
    } 

    public void calculateTroopsFor(int playerId) { // Oyuncunun bir sonraki tur için alacağı asker sayısını hesaplayan metod
        int territoryCount = countPlayerTerritories(playerId); // Oyuncunun sahip olduğu bölge sayısını hesaplar
        int continentBonus = calculateContinentBonus(playerId); // Oyuncunun kıta bonusunu hesaplar
        int troops = Math.max(MIN_TROOPS_PER_TURN, territoryCount / 3) + continentBonus; // Toplam asker sayısını hesaplar (minimum 3, bölge sayısı/3 + kıta bonusu)
        playerTroopsToPlace.put(playerId, troops); // Hesaplanan asker sayısını oyuncu için kaydeder
    } 

    /**
     * İki bölge komşu mu?
     */
    public boolean areNeighbors(String a, String b) { // İki bölgenin komşu olup olmadığını kontrol eden metod
        return adjacencyMap.getOrDefault(a, Collections.emptyList()).contains(b); // a bölgesinin komşuları listesinde b var mı kontrol eder
    } // areNeighbors metodunun sonu

    public Map<String, List<String>> getAdjacencyMap() { // Komşuluk haritasını döndüren metod
        return adjacencyMap; // Komşuluk haritasını döndürür
    } 

    /**
     * Belirli sayıda zar atar
     */
    private List<Integer> rollDice(int count) { // Zar atma metodu
        Random rand = new Random(); // Rastgele sayı üreteci oluşturur
        List<Integer> result = new ArrayList<>(); // Sonuç listesini oluşturur
        for (int i = 0; i < count; i++) { // Belirtilen sayıda zar atmak için döngü başlatır
            result.add(rand.nextInt(6) + 1); // 1-6 arası rastgele sayı üretip listeye ekler
        } // for döngüsünün sonu
        return result; // Zar sonuçlarını döndürür
    } 
} 