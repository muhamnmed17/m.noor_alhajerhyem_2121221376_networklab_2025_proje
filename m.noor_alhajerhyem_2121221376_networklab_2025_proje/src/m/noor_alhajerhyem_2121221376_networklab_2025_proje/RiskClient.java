package final_project;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Risk oyunu için ana uygulama sınıfı
 */
public class RiskClient {
    private final RiskGameModel model;
    private final RiskUIView view;
    private final RiskNetworkManager network;
    private final RiskGameController controller;
    
    /**
     * Risk oyunu istemcisini başlatır
     */
    public RiskClient(String serverIp, int serverPort) {
        // Model ve görünüm bileşenlerini oluştur
        model = new RiskGameModel();
        view = new RiskUIView(model);
        
        // Ağ yöneticisi ve kontrolcüyü oluştur
        network = new RiskNetworkManager(serverIp, serverPort, model, view);
        controller = new RiskGameController(model, view, network);
        
        // Sunucudan gelen mesajları dinlemeye başla
        network.startListening();
        
        // Arayüzü göster
        view.setVisible(true);
    }
    
    /**
     * Ana metod, uygulamayı başlatır
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String ip = JOptionPane.showInputDialog("Sunucu IP'si", "127.0.0.1");
            if (ip == null || ip.isBlank()) {
                ip = "127.0.0.1";
            }
            new RiskClient(ip, 9090);
        });
    }
}