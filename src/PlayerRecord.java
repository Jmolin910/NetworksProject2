public class PlayerRecord {

    private int playerId;
    private int score;

    public PlayerRecord(int playerId) {
        this.playerId = playerId;
        this.score = 0;
    }

    public int getScore() {
        return score;
    }
    
    public void addScore(int score) {
        this.score += score;
    }



}
