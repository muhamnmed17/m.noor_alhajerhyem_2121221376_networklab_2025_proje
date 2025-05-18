package final_project;

/**
 * Risk oyununda bir bölgeyi temsil eden sınıf
 */
public class Territory {
    private String name;
    private int owner;
    private int troops;
private int troopCount;

    /**
     * Yeni bir bölge oluşturur
     * @param name Bölge adı
     * @param owner Bölge sahibi (-1 = sahipsiz)
     * @param troops Bölgedeki asker sayısı
     */
    public Territory(String name, int owner, int troops) {
        this.name = name;
        this.owner = owner;
        this.troops = troops;
    }

    /**
     * Bölgenin adını döndürür
     * @return Bölge adı
     */
    public String getName() {
        return name;
    }

    /**
     * Bölgenin sahibini döndürür
     * @return Oyuncu kimliği (-1 = sahipsiz)
     */
    public int getOwner() {
        return owner;
    }
public int getTroopCount() {
    return troopCount;
}

    /**
     * Bölgedeki asker sayısını döndürür
     * @return Asker sayısı
     */
    public int getTroops() {
        return troops;
    }

    /**
     * Bölgeye yeni bir sahip atar
     * @param owner Yeni sahip kimliği
     */
    public void setOwner(int owner) {
        this.owner = owner;
    }

    /**
     * Bölgedeki asker sayısını ayarlar
     * @param troops Yeni asker sayısı
     */
    public void setTroops(int troops) {
        this.troops = troops;
    }

    /**
     * Bölgeye asker ekler
     * @param amount Eklenecek asker sayısı
     */
    public void addTroops(int amount) {
        this.troops += amount;
    }

    /**
     * Bölgeden asker çıkarır (saldırı kaybı gibi durumlarda)
     * @param amount Çıkarılacak asker sayısı
     */
    public void removeTroops(int amount) {
        this.troops = Math.max(0, this.troops - amount);
    }

    @Override
    public String toString() {
        return name + ":" + owner + ":" + troops;
    }
}