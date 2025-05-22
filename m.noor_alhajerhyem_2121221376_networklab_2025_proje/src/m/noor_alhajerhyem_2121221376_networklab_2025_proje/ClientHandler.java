package final_project; 

import java.io.*; 
import java.net.*; 
import java.util.*; 

/**
 * İstemci bağlantılarını yöneten ve komutları işleyen sınıf
 */
public class ClientHandler implements Runnable { // ClientHandler sınıfı tanımı, Runnable arayüzünü uygular

    private Socket clientSocket; // İstemci ile iletişim için soket
    private ObjectOutputStream out; // Nesne gönderimi için çıkış akışı
    private ObjectInputStream in; // Nesne alımı için giriş akışı
    private RiskServer server; // Sunucu referansı
    private int playerId; // Oyuncu kimlik numarası
    private boolean running = true; // İş parçacığının çalışma durumu, başlangıçta true
    private String playerName = "Oyuncu"; // Oyuncu adı, varsayılan değer
    private RiskMatch riskMatch;  // Eşleşme referansı, oyuncunun katıldığı oyun

    public ClientHandler(Socket socket, int playerId, RiskServer server) { // Constructor, yeni bir istemci bağlantısı için
        this.clientSocket = socket; // Soket referansını kaydet
        this.playerId = playerId; // Oyuncu ID'sini kaydet
        this.server = server; // Sunucu referansını kaydet

        try { // Akış nesnelerini oluşturma bloğu
            // Önemli: Önce OutputStream, sonra InputStream (deadlock riskini azaltır)
            this.out = new ObjectOutputStream(socket.getOutputStream()); // Çıkış akışını oluştur
            this.out.flush(); // Başlık verilerini hemen gönder (buffer'ı temizle)
            
            this.in = new ObjectInputStream(socket.getInputStream()); // Giriş akışını oluştur
            
            System.out.println("ClientHandler oluşturuldu: Oyuncu " + playerId); // Log mesajı yaz
        } catch (IOException e) { // IO hatası durumunda
            System.err.println("ClientHandler oluşturma hatası: " + e.getMessage()); // Hata mesajını yazdır
        }
    }

    @Override
    public void run() { // Runnable arayüzünden Override edilen çalışma metodu
        try { // Ana çalışma bloğu
            Object inputObj; // Alınan nesneyi tutacak değişken
            while (running && (inputObj = in.readObject()) != null) { // İş parçacığı çalıştığı ve gelen nesne null olmadığı sürece
                // Gelen mesajların formatına göre işle
                if (inputObj instanceof Message) { // Eğer nesne Message türündeyse
                    processMessage((Message) inputObj); // Message işleme metodunu çağır
                } else if (inputObj instanceof String) { // Eğer nesne String türündeyse
                    // Geriye uyumluluk için string mesajları da destekle
                    processLegacyCommand((String) inputObj); // Eski format komutu işleme metodunu çağır
                }
            }
        } catch (SocketException | EOFException e) { // Soket hatası veya dosya sonu hatası durumunda
            System.out.println("İstemci bağlantısı kesildi: " + e.getMessage()); // Bilgi mesajı yazdır
        } catch (IOException | ClassNotFoundException e) { // IO hatası veya sınıf bulunamadı hatası durumunda
            System.err.println("İstemci ile iletişim kesildi: " + e.getMessage()); // Hata mesajı yazdır
        } finally { // Her durumda çalışacak blok
            handleDisconnect(); // Bağlantı kesme işlemini yönet
        }
    }

    /**
     * Bağlantı kesilince temizlik yapar
     */
    private void handleDisconnect() { // Bağlantı kesildiğinde temizlik yapan metot
        try { // Temizlik bloğu
            if (riskMatch != null) { // Eğer oyuncu bir eşleşmeye katılmışsa
                riskMatch.handleDisconnect(playerId); // Eşleşmeye bağlantı kesme bilgisini ilet
            } else { // Eğer henüz eşleşmeye katılmamışsa
                server.removeClient(this); // Sunucudan istemciyi kaldır
            }
            close(); // Kaynakları kapat
        } catch (IOException e) { // IO hatası durumunda
            System.err.println("Kapatma hatası: " + e.getMessage()); // Hata mesajı yazdır
        }
    }

    /**
     * Yeni mesaj formatında gelen mesajları işler
     */
    private void processMessage(Message message) { // Mesaj nesnelerini işleyen metot
        System.out.println("Oyuncu " + playerId + " mesajı: " + message); // Log mesajı yaz

        switch (message.type) { // Mesaj tipine göre işleme yap
            case "SET_NAME": // İsim ayarlama mesajı ise
                playerName = message.get("name"); // Oyuncu adını ayarla
                System.out.println("Oyuncu " + playerId + " yeni isim: " + playerName); // Log mesajı yaz
                break;

            case "PLACE_TROOPS": // Asker yerleştirme mesajı ise
                if (riskMatch != null) { // Eğer oyuncu bir eşleşmeye katılmışsa
                    String territory = message.get("territory"); // Bölge adını al
                    int troops = Integer.parseInt(message.get("troops")); // Asker sayısını al
                    riskMatch.handlePlaceTroops(playerId, territory, troops); // Eşleşmeye asker yerleştirme isteği gönder
                }
                break;

            case "ATTACK": // Saldırı mesajı ise
                if (riskMatch != null) { // Eğer oyuncu bir eşleşmeye katılmışsa
                    String from = message.get("from"); // Saldıran bölgeyi al
                    String to = message.get("to"); // Hedef bölgeyi al
                    int dice = Integer.parseInt(message.get("dice")); // Zar sayısını al
                    riskMatch.handleAttack(playerId, from, to, dice); // Eşleşmeye saldırı isteği gönder
                }
                break;

            case "FORTIFY": // Güçlendirme mesajı ise
                if (riskMatch != null) { // Eğer oyuncu bir eşleşmeye katılmışsa
                    String from = message.get("from"); // Kaynak bölgeyi al
                    String to = message.get("to"); // Hedef bölgeyi al
                    int troops = Integer.parseInt(message.get("troops")); // Asker sayısını al
                    riskMatch.handleFortify(playerId, from, to, troops); // Eşleşmeye güçlendirme isteği gönder
                }
                break;

            case "RESTART_DECLINE": // Yeniden başlatma reddi mesajı ise
                if (riskMatch != null) { // Eğer oyuncu bir eşleşmeye katılmışsa
                    riskMatch.handleRestartDecline(playerId); // Eşleşmeye yeniden başlatma reddi bilgisini ilet
                }
                break;

            case "END_TURN": // Tur bitirme mesajı ise
                if (riskMatch != null) { // Eğer oyuncu bir eşleşmeye katılmışsa
                    riskMatch.handleEndTurn(playerId); // Eşleşmeye tur bitirme isteği gönder
                }
                break;

            case "RESTART": // Yeniden başlatma mesajı ise
                if (riskMatch != null) { // Eğer oyuncu bir eşleşmeye katılmışsa
                    riskMatch.handleRestartRequest(playerId); // Eşleşmeye yeniden başlatma isteği gönder
                }
                break;

            default: // Bilinmeyen bir mesaj tipi ise
                sendErrorMessage("Bilinmeyen komut: " + message.type); // Hata mesajı gönder
                break;
        }
    }

    /**
     * Eski string formatındaki komutları işler (geriye uyumluluk için)
     */
    private void processLegacyCommand(String command) { // Eski format string komutları işleyen metot
        System.out.println("Oyuncu " + playerId + " eski format komutu: " + command); // Log mesajı yaz

        String[] parts = command.split(" ", 2); // Komutu boşluğa göre en fazla 2 parçaya böl
        String cmd = parts[0]; // İlk parça komut tipi
        String data = parts.length > 1 ? parts[1] : ""; // İkinci parça (varsa) komut verisi

        // Eski formatı yeni formata dönüştür ve öyle işle
        Message message = new Message(); // Yeni bir mesaj nesnesi oluştur
        message.type = cmd; // Mesaj tipini ayarla

        switch (cmd) { // Komut tipine göre işlem yap
            case "SET_NAME": // İsim ayarlama komutu ise
                message.put("name", data); // İsim verisini ekle
                break;
                
            case "PLACE_TROOPS": // Asker yerleştirme komutu ise
                if (!data.isBlank()) { // Eğer veri kısmı boş değilse
                    String[] subParts = data.trim().split(" "); // Veriyi boşluklara göre böl
                    if (subParts.length >= 2) { // En az 2 parça varsa
                        int lastIndex = subParts.length - 1; // Son parçanın indeksi
                        String troops = subParts[lastIndex]; // Son parça asker sayısı
                        String territory = String.join(" ", Arrays.copyOfRange(subParts, 0, lastIndex)); // Geri kalan parçalar bölge adı
                        
                        message.put("territory", territory); // Bölge adını ekle
                        message.put("troops", troops); // Asker sayısını ekle
                    }
                }
                break;
                
            case "ATTACK": // Saldırı komutu ise
                String[] attackParts = data.split(" "); // Veriyi boşluklara göre böl
                if (attackParts.length >= 3) { // En az 3 parça varsa
                    String dice = attackParts[attackParts.length - 1]; // Son parça zar sayısı
                    String territoryPart = String.join(" ", Arrays.copyOf(attackParts, attackParts.length - 1)); // Geri kalan kısım bölgeler
                    int sepIndex = territoryPart.lastIndexOf(' '); // Son boşluğun indeksi
                    
                    if (sepIndex != -1) { // Eğer boşluk varsa
                        String from = territoryPart.substring(0, sepIndex); // İlk kısım saldıran bölge
                        String to = territoryPart.substring(sepIndex + 1); // Son kısım hedef bölge
                        
                        message.put("from", from); // Saldıran bölgeyi ekle
                        message.put("to", to); // Hedef bölgeyi ekle
                        message.put("dice", dice); // Zar sayısını ekle
                    }
                }
                break;
                
            case "FORTIFY": // Güçlendirme komutu ise
                String[] fortifyParts = data.split(" "); // Veriyi boşluklara göre böl
                if (fortifyParts.length >= 3) { // En az 3 parça varsa
                    String troops = fortifyParts[fortifyParts.length - 1]; // Son parça asker sayısı
                    String territoryPart = String.join(" ", Arrays.copyOf(fortifyParts, fortifyParts.length - 1)); // Geri kalan kısım bölgeler
                    int sepIndex = territoryPart.lastIndexOf(' '); // Son boşluğun indeksi
                    
                    if (sepIndex != -1) { // Eğer boşluk varsa
                        String from = territoryPart.substring(0, sepIndex); // İlk kısım kaynak bölge
                        String to = territoryPart.substring(sepIndex + 1); // Son kısım hedef bölge
                        
                        message.put("from", from); // Kaynak bölgeyi ekle
                        message.put("to", to); // Hedef bölgeyi ekle
                        message.put("troops", troops); // Asker sayısını ekle
                    }
                }
                break;
                
            default: // Bilinmeyen bir komut tipi ise
                message.put("data", data); // Veriyi olduğu gibi ekle
                break;
        }
        
        processMessage(message); // Oluşturulan mesajı işle
    }

    /**
     * İstemciye mesaj gönderir
     */
    public void sendMessage(Message message) { // Mesaj gönderme metodu
        try { // Mesaj gönderme bloğu
            if (out != null && clientSocket != null && !clientSocket.isClosed()) { // Çıkış akışı ve soket hala açıksa
                out.writeObject(message); // Mesaj nesnesini gönder
                out.flush(); // Hemen gönderilmesini sağla (buffer'ı temizle)
                out.reset(); // Object cache'ini temizle (aynı nesneyi değiştirip tekrar gönderince sorun olmasın)
            }
        } catch (Exception e) { // Hata durumunda
            System.err.println("Mesaj gönderilirken hata: " + e.getMessage()); // Hata mesajı yazdır
        }
    }
    
    /**
     * Eski string formatında mesaj gönderir (geriye uyumluluk için)
     */
    public void sendLegacyMessage(String message) { // Eski format string mesaj gönderme metodu
        // İstemci eski formatı bekliyorsa, string mesaj olarak gönder
        // Yeni format için string'i Message nesnesine çevir
        try { // Mesaj gönderme bloğu
            if (out != null && clientSocket != null && !clientSocket.isClosed()) { // Çıkış akışı ve soket hala açıksa
                String[] parts = message.split(" ", 2); // Mesajı boşluğa göre en fazla 2 parçaya böl
                String type = parts[0]; // İlk parça mesaj tipi
                String data = parts.length > 1 ? parts[1] : ""; // İkinci parça (varsa) mesaj verisi
                
                Message msg = new Message(type, Map.of("data", data)); // Yeni format Message nesnesi oluştur
                sendMessage(msg); // Oluşturulan Message nesnesini gönder
            }
        } catch (Exception e) { // Hata durumunda
            System.err.println("Mesaj gönderilirken hata: " + e.getMessage()); // Hata mesajı yazdır
        }
    }
    
    /**
     * Hata mesajı gönderir
     */
    private void sendErrorMessage(String errorText) { // Hata mesajı gönderme metodu
        Message errorMsg = new Message("ERROR", Map.of("msg", errorText)); // Hata mesajı oluştur
        sendMessage(errorMsg); // Hata mesajını gönder
    }

    public String getPlayerName() { // Oyuncu adını getiren metot
        return playerName; // Oyuncu adını döndür
    }

    /**
     * Bağlantıyı kapatır
     */
    public void close() throws IOException { // Kaynakları kapatma metodu
        running = false; // İş parçacığını durdur
        if (in != null) { // Giriş akışı varsa
            in.close(); // Giriş akışını kapat
        }
        if (out != null) { // Çıkış akışı varsa
            out.close(); // Çıkış akışını kapat
        }
        if (clientSocket != null && !clientSocket.isClosed()) { // Soket varsa ve hala açıksa
            clientSocket.close(); // Soketi kapat
        }
    }

    public int getPlayerId() { // Oyuncu ID'sini getiren metot
        return playerId; // Oyuncu ID'sini döndür
    }

    // RiskMatch ataması ve erişimi
    public void setRiskMatch(RiskMatch match) { // Eşleşme ataması yapan metot
        this.riskMatch = match; // Eşleşme referansını kaydet
    }

    public RiskMatch getRiskMatch() { // Eşleşme referansını getiren metot
        return riskMatch; // Eşleşme referansını döndür
    }
} 