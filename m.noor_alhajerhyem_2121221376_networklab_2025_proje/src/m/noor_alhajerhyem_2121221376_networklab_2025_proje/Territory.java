package final_project; 

/**
 * Risk oyununda bir bölgeyi temsil eden sınıf
 */
public class Territory {

    private String name; // Bölgenin adı
    private int owner;   // Bölgenin sahibi (oyuncu ID'si) (-1 = sahipsiz)
    private int troops;  // Bölgedeki asker sayısı

    /**
     * Yeni bir bölge nesnesi oluşturur
     * name Bölge adı
     *  owner Bölge sahibi (-1 = sahipsiz)
     *  troops Bölgedeki asker sayısı
     */
    public Territory(String name, int owner, int troops) {
        this.name = name;       // Adı atar
        this.owner = owner;     // Sahibini atar
        this.troops = troops;   // Asker sayısını atar
    }

    /**
     * Bölgenin adını döndürür
     * @return Bölge adı
     */
    public String getName() {
        return name; // name alanını döndür
    }

    /**
     * Bölgenin sahibini döndürür
     * @return Oyuncu kimliği (-1 = sahipsiz)
     */
    public int getOwner() {
        return owner; // owner alanını döndür
    }

    /**
     * Bölgedeki asker sayısını döndürür
     * @return Asker sayısı
     */
    public int getTroops() {
        return troops; // troops alanını döndür
    }

    /**
     * Bölgeye yeni bir sahip atar
     *  owner Yeni sahip kimliği
     */
    public void setOwner(int owner) {
        this.owner = owner; // owner alanını güncelle
    }

    /**
     * Bölgedeki asker sayısını ayarlar
     *  troops Yeni asker sayısı
     */
    public void setTroops(int troops) {
        this.troops = troops; // troops alanını güncelle
    }

    /**
     * Bölgeye asker ekler
     *  amount Eklenecek asker sayısı
     */
    public void addTroops(int amount) {
        this.troops += amount; // asker sayısını artır
    }

    /**
     * Bölgeden asker çıkarır (saldırı kaybı gibi durumlarda)
     *  amount Çıkarılacak asker sayısı
     */
    public void removeTroops(int amount) {
        this.troops = Math.max(0, this.troops - amount); // negatif olmaması için 0 ile sınırla
    }

    @Override
    public String toString() {
        return name + ":" + owner + ":" + troops; // bölgeyi string olarak ifade eder
    }
}
