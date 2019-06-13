
public class Player implements java.io.Serializable{
	public int player_rank;
	public String player_name;
	public int games_won;
	public int games_played;
	public Double total_winning_time;
	public String password;
	
	public Player(int player_rank, String player_name, int games_won, 
				int games_played, Double total_winning_time, String password) {
		this.player_rank = player_rank;
		this.player_name = player_name;
		this.games_won = games_won;
		this.games_played = games_played;
		this.total_winning_time = total_winning_time;
		this.password = password;
	}
}
