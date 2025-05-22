package network; 

import java.io.Serializable;
import java.util.HashMap;   
import java.util.Map;        

/**
 * Risk oyunu için mesajlaşma sınıfı.
 * Ağ üzerinden serileştirilebilir mesajlar göndermek için kullanılır.
 */
public class Message implements Serializable { // Nesnelerin ağ üzerinden gönderilebilmesi için Serializable

    private static final long serialVersionUID = 1L; // Serileştirme versiyon numarası

    public String type; // Mesajın tipi (örneğin "ATTACK", "PLACE_TROOPS", "END_TURN" gibi)
    public Map<String, String> data; // Mesaja ait anahtar-değer biçiminde bilgiler

    /**
     * Boş constructor
     * data map'i boş şekilde başlatılır
     */
    public Message() {
        this.data = new HashMap<>();
    }

    /**
     * Tip ve veri ile constructor
     *  type Mesaj tipi
     *  data Mesajın içeriğini taşıyan key-value haritası
     */
    public Message(String type, Map<String, String> data) {
        this.type = type;
        // null gelirse boş bir map oluştur, aksi halde gelen map'in bir kopyasını al
        this.data = data != null ? new HashMap<>(data) : new HashMap<>();
    }

    /**
     * Veri haritasından değer almak için yardımcı metot
     *  key Aranacak anahtar
     *  Anahtara karşılık gelen değer (yoksa null)
     */
    public String get(String key) {
        return data != null ? data.get(key) : null;
    }

    /**
     * Veri haritasına değer eklemek için yardımcı metot
     *  key Eklenecek anahtar
     *  value Anahtara karşılık gelecek değer
     */
    public void put(String key, String value) {
        if (data == null) {
            data = new HashMap<>(); // Eğer null ise, map oluştur
        }
        data.put(key, value); // Anahtar-değer çifti ekle
    }

    /**
     * Mesaj nesnesini okunabilir metin formatında döndürür
     *  type ve data'yı içeren string
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(); // String birleştirme işlemi için
        sb.append("Message{type='").append(type).append("', data={");

        if (data != null) {
            boolean first = true;
            for (Map.Entry<String, String> entry : data.entrySet()) {
                if (!first) {
                    sb.append(", "); // İlk giriş hariç her girişten sonra virgül koy
                }
                first = false;
                sb.append(entry.getKey()).append("='").append(entry.getValue()).append("'");
            }
        }

        sb.append("}}"); // Kapanış
        return sb.toString(); // Sonuç olarak okunabilir mesaj döndür
    }
}
