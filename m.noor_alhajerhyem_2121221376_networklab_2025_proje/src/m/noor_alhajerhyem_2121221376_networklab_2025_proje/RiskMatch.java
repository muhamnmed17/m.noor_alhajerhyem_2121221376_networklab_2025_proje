package final_project;

import java.util.*;

public class RiskMatch {

    private final int roomId;
    private final RiskGame game;
    private final ClientHandler player1;
    private final ClientHandler player2;

    private final Map<Integer, ClientHandler> idToPlayer = new HashMap<>();
    private final int[] playerOrder;
    private int currentPlayerIndex = 0;
    private int currentPlayer;

    private final Set<Integer> restartRequests = new HashSet<>();

    public RiskMatch(int roomId, ClientHandler p1, ClientHandler p2) {
        this.roomId = roomId;
        this.player1 = p1;
        this.player2 = p2;
        this.game = new RiskGame();

        p1.setRiskMatch(this);
        p2.setRiskMatch(this);

        idToPlayer.put(p1.getPlayerId(), p1);
        idToPlayer.put(p2.getPlayerId(), p2);

        playerOrder = new int[]{p1.getPlayerId(), p2.getPlayerId()};
        currentPlayer = playerOrder[0];
    }

    public void start() {
        // Gerçek oyuncu ID'lerini RiskGame'e aktar
        game.initializeGame(playerOrder[0], playerOrder[1]);

        sendInit();         // INIT 0:Ali, INIT 1:Veli gibi
        sendAdjacency();    // Komşuluk ilişkileri
        sendMap();          // Mevcut harita durumu
        nextTurn();         // İlk oyuncuya sıra gönder
    }

    private void sendInit() {
        player1.sendMessage("INIT " + player1.getPlayerId() + ":" + player1.getPlayerName());
        player2.sendMessage("INIT " + player2.getPlayerId() + ":" + player2.getPlayerName());
    }

    public void handlePlaceTroops(int playerId, String territory, int troops) {
        if (playerId != currentPlayer) {
            return;
        }

        if (game.placeTroops(playerId, territory, troops)) {
            int remaining = game.getTroopsToPlace(playerId);
            getPlayer(playerId).sendMessage("PLACE_RESULT " + territory + ":" + game.getTerritoryTroops(territory) + ":" + remaining);
            sendMap();
        } else {
            getPlayer(playerId).sendMessage("ERROR Asker yerleştirme başarısız");
        }
    }

    public void handleAttack(int playerId, String from, String to, int dice) {
        if (playerId != currentPlayer) {
            return;
        }

        StringBuilder error = new StringBuilder();
        int[] result = game.attack(playerId, from, to, dice, error);

        if (result != null) {
            broadcast("ATTACK_RESULT " + from + ":" + to + ":" + result[0] + ":" + result[1]);
            sendMap();
            checkGameOver();
        } else {
            getPlayer(playerId).sendMessage("ERROR " + error.toString());
        }
    }

    public void handleFortify(int playerId, String from, String to, int troops) {
        if (playerId != currentPlayer) {
            return;
        }

        if (game.fortify(playerId, from, to, troops)) {
            broadcast("FORTIFY_RESULT " + from + ":" + to + ":" + troops);
            sendMap();
        } else {
            getPlayer(playerId).sendMessage("ERROR Güçlendirme başarısız");
        }
    }

    public void handleEndTurn(int playerId) {
        if (playerId != currentPlayer) {
            return;
        }
        nextTurn();
    }

    public void nextTurn() {
        currentPlayerIndex = (currentPlayerIndex + 1) % 2;
        currentPlayer = playerOrder[currentPlayerIndex];

        game.calculateTroopsFor(currentPlayer); // 🔹 sadece burada hesaplanır
        int troops = game.getTroopsToPlace(currentPlayer);
        broadcast("TURN " + currentPlayer + ":" + troops + ":" + getCurrentPlayer().getPlayerName());
    }

    private void checkGameOver() {
        int winner = game.checkWinner();
        if (winner != -1) {
            broadcast("GAME_OVER " + winner);
        }
    }

    public void handleRestartRequest(int playerId) {
        if (game.checkWinner() == -1) {
            getPlayer(playerId).sendMessage("INFO Oyun henüz bitmedi, yeniden başlatılamaz.");
            return;
        }

        if (restartRequests.contains(playerId)) {
            return;
        }

        restartRequests.add(playerId);
        getPlayer(playerId).sendMessage("INFO Yeniden başlatma isteğiniz alındı.");

        if (restartRequests.size() == 2) {
            broadcast("INFO Her iki oyuncu da yeniden başlatmayı onayladı. Oyun sıfırlanıyor...");
            restartRequests.clear();
            game.initializeGame(playerOrder[0], playerOrder[1]);
            sendMap();
            sendAdjacency();
            currentPlayerIndex = 0;
            currentPlayer = playerOrder[0];
            nextTurn();
        } else {
            getOtherPlayer(playerId).sendMessage("INFO Diğer oyuncudan yeniden başlatma isteği geldi.");
        }
    }

    public void handleRestartDecline(int playerId) {
        broadcast("INFO Oyunculardan biri yeniden başlatmayı reddetti. Oyun kapatılıyor.");
        player1.sendMessage("EXIT");
        player2.sendMessage("EXIT");
    }

    public void handleDisconnect(int playerId) {
        getOtherPlayer(playerId).sendMessage("DISCONNECT Rakip oyundan ayrıldı.");
    }

    private ClientHandler getPlayer(int id) {
        return idToPlayer.get(id);
    }

    private ClientHandler getOtherPlayer(int id) {
        for (int key : idToPlayer.keySet()) {
            if (key != id) {
                return idToPlayer.get(key);
            }
        }
        return null;
    }

    private ClientHandler getCurrentPlayer() {
        return getPlayer(currentPlayer);
    }

    private void broadcast(String msg) {
        player1.sendMessage(msg);
        player2.sendMessage(msg);
    }

    private void sendMap() {
        broadcast("MAP " + game.getMapState());
    }

    private void sendAdjacency() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : game.getAdjacencyMap().entrySet()) {
            sb.append(entry.getKey()).append(":");
            sb.append(String.join(",", entry.getValue()));
            sb.append(";");
        }
        broadcast("ADJACENCY " + sb.toString());
    }
}
