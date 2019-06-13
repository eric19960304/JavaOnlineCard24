
import java.io.Serializable;
import java.util.ArrayList;

public class JPokerMessage implements Serializable {
	private static final long serialVersionUID = -1675867563027817666L;
	public String command;
	public String playerName;
	public String message;
	public Integer numberOfPlayer;
	public ArrayList<Player> players;
	public ArrayList<Player> allUsers;
	public Integer[] cardIds;
	
	public JPokerMessage(String command, String playerName, String message, Integer numberOfPlayer,
			ArrayList<Player> players, ArrayList<Player> allUsers, Integer[] cardIds) {
		this.command = command;
		this.playerName = playerName;
		this.message = message;
		this.numberOfPlayer = numberOfPlayer;
		this.players = players;
		this.allUsers = allUsers;
		this.cardIds = cardIds;
	}
}

