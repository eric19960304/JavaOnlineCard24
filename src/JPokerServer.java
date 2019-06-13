
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.rmi.*;
import java.rmi.server.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.naming.NamingException;

import com.singularsys.jep.EvaluationException;
import com.singularsys.jep.Jep;
import com.singularsys.jep.ParseException;


public class JPokerServer extends UnicastRemoteObject 
						 implements Auth {
	private static final String DB_HOST = "localhost";
	private static final String DB_USER = "comp3402_as3";
	private static final String DB_PASS = "comp3402_as3";
	private static final String DB_NAME = "comp3402_as3";
	
	private Connection dbConn;
	
	Random random = new Random();
	ArrayList<Integer> allowedNumbers = new ArrayList<>(Arrays.asList(1,2,3,4,5,6,7,8,9,10,11,12,13));
	Queue<JPokerMessage> queue = new LinkedList<>();
	
	private JMSHelper jmsHelper;

	public static void main(String[] args) {
		try {
			JPokerServer app = new JPokerServer();
			
			System.setSecurityManager(new SecurityManager());
			Naming.rebind("JPokerServer/Auth", app);
			System.out.println("Service registered");
			
			app.startJMS(); // handle game backend logic
			
		} catch(Exception e) {
			System.err.println("Exception thrown: "+e);
		}
	}
	
	public JPokerServer() throws RemoteException, SQLException, InstantiationException, 
		IllegalAccessException, ClassNotFoundException, NamingException, JMSException {
		try {
			Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
			dbConn = DriverManager.getConnection("jdbc:mysql://"+DB_HOST+
												"/"+DB_NAME+
												"?user="+DB_USER+
												"&password="+DB_PASS);
			System.out.println("DB connected");
			
			jmsHelper = new JMSHelper();
			
			resetOnlineUsers();
		}catch (IOException e) { e.printStackTrace(); }
		
	}
	
	private synchronized JPokerMessage getMessageFromQueue() {
		if(queue.isEmpty()) {
			return null;
		}else {
			return queue.remove();
		}
	}
	
	private synchronized void addMessageToQueue(JPokerMessage msg) {
		queue.add(msg);
	}
	
	public void startJMS() throws JMSException {
		
		MessageConsumer queueReader = jmsHelper.createQueueReader();
		MessageProducer topicSender = jmsHelper.createTopicSender();
		
		System.out.println("Started JMS");
		
		// create thread that keep consume the queue
		new Thread(new Runnable() {
			@Override
			public void run() {
				while(true) {
					try {
						JPokerMessage message = receiveMessage(queueReader);
						System.out.println("received message: "+message.command);
						addMessageToQueue(message);
					}catch(Exception e) {
						System.out.println(e);
					}
				}
			}
		}).start();
		
		int numberOfPlayers;
		int lostCount;
		long firstUserArrivedTime;
		long startGameTime;
		ArrayList<Player> players;
		Integer[] cardIds = new Integer[4];
		Integer[] cardValue = new Integer[4];
		// wait for users
		while(true) {
			numberOfPlayers = 0;
			firstUserArrivedTime = -1;
			players = new ArrayList<Player>();
			
			while(true) {
				boolean firstWaitedTooLong = numberOfPlayers >=2 && System.currentTimeMillis()-firstUserArrivedTime>10000;
				if(numberOfPlayers==4 || firstWaitedTooLong) {
					break;
				}
				
				JPokerMessage message = getMessageFromQueue();
				
				if(message!=null && message.command.equals("joinGame")) {
					// add player to list
					numberOfPlayers++;
					for(Player p : readUserInfo()) {
						if(p.player_name.equals(message.playerName)) {
							players.add(p);
							break;
						};
					}
					System.out.println(message.playerName+" has joined the game");
					
					if(numberOfPlayers==1) {
						firstUserArrivedTime = System.currentTimeMillis();
					}
				}
				
				try {
					Thread.sleep(1000);
				}catch(Exception e) {
					System.out.println(e);
				}
			}
				
			System.out.println("Start Game now");
			
			
			Collections.shuffle(allowedNumbers, random);
			System.out.print("Card: ");
	        for(int i=0; i<4; i++){
	        	cardValue[i] = allowedNumbers.get(i*2);
	        	System.out.print(cardValue[i]+" ");
	        }
	        System.out.println();
	        
	        for(int i=0; i<4; i++){
	            int suit = random_in_range(0, 3);
	            cardIds[i] = cardValue[i] + suit*13;
	        }
			
			JPokerMessage startGameMsg = new JPokerMessage(
					"startGame", "", null, numberOfPlayers, players, null, cardIds
			);
			Message startGameJmsMsg;
			try {
				startGameJmsMsg = jmsHelper.createMessage(startGameMsg);
				broadcastMessage(topicSender, startGameJmsMsg);
			} catch (JMSException e) {
				System.out.println("Error at broadcasting message: "+e);
			}
			
			startGameTime = System.currentTimeMillis();
			lostCount = 0;
			
			// wait for answers
			while(true) {
				if(lostCount==players.size()) {
					String hints = "<html><font size=\"20\">No winner,<br/>You all loser!</font></html>";
					
					JPokerMessage endGameMsg = new JPokerMessage(
							"endGame", "", hints, numberOfPlayers, players, readUserInfo(), null
					);
					Message endGameJmsMsg;
					try {
						endGameJmsMsg = jmsHelper.createMessage(endGameMsg);
						broadcastMessage(topicSender, endGameJmsMsg);
					} catch (JMSException e) {
						System.out.println("Error at broadcasting message: "+e);
					}
					break;
				}
				
				JPokerMessage message = getMessageFromQueue();
				
				if(message!=null && message.command.equals("submitAnswer")) {
					
					System.out.println(message.playerName+" submitted: "+message.message);
					Integer parsedAnswer = parseAnswer(message.message);
					System.out.println("parsed value: "+parsedAnswer);
					
					if(parsedAnswer != null) {
						if(parsedAnswer==24) {
							// player win
							System.out.println(message.playerName+": win game");
							for(int i=0; i<players.size(); i++) {
								Player p = players.get(i);
								if(p.player_name.equals(message.playerName)) {
									p.games_played += 1;
									p.games_won += 1;
									p.total_winning_time += (Double) ((System.currentTimeMillis() - startGameTime)/1000.0);
									updateUser(p);
									break;
								}
							}
							ArrayList<Player> allUsers = recalculateRanking();
							for(int i=0; i<players.size(); i++) {
								for(Player user: allUsers) {
									if(user.player_name.equals(players.get(i).player_name)) {
										players.set(i, user);
										break;
									}
								}
							}
							
							String hints = String.format(
									"<html><font size=\"20\">Winner: %s<br/>%s</font></html>", 
									message.playerName, message.message);
							
							JPokerMessage endGameMsg = new JPokerMessage(
									"endGame", "", hints, numberOfPlayers, players, allUsers, null
							);
							Message endGameJmsMsg;
							try {
								endGameJmsMsg = jmsHelper.createMessage(endGameMsg);
								broadcastMessage(topicSender, endGameJmsMsg);
							} catch (JMSException e) {
								System.out.println("Error at broadcasting message: "+e);
							}
							break;
						}else {
							// player lose
							lostCount++;
							System.out.println(message.playerName+": game over");
							for(int i=0; i<players.size(); i++) {
								Player p = players.get(i);
								if(p.player_name.equals(message.playerName)) {
									p.games_played += 1;
									updateUser(p);
									break;
								}
							}
							ArrayList<Player> allUsers = recalculateRanking();
							for(int i=0; i<players.size(); i++) {
								for(Player user: allUsers) {
									if(user.player_name.equals(players.get(i).player_name)) {
										players.set(i, user);
										break;
									}
								}
							}
							
							JPokerMessage gameOverMsg = new JPokerMessage(
									"gameOver", message.playerName, "="+parsedAnswer, numberOfPlayers, players, allUsers, null
							);
							Message gameOverJmsMsg;
							try {
								gameOverJmsMsg = jmsHelper.createMessage(gameOverMsg);
								broadcastMessage(topicSender, gameOverJmsMsg);
							} catch (JMSException e) {
								System.out.println("Error at broadcasting message: "+e);
							}
						}
					}else {
						// player lose
						lostCount++;
						System.out.println(message.playerName+": game over");
						for(int i=0; i<players.size(); i++) {
							Player p = players.get(i);
							if(p.player_name.equals(message.playerName)) {
								p.games_played += 1;
								updateUser(p);
								break;
							}
						}
						ArrayList<Player> allUsers = recalculateRanking();
						for(int i=0; i<players.size(); i++) {
							for(Player user: allUsers) {
								if(user.player_name.equals(players.get(i).player_name)) {
									players.set(i, user);
									break;
								}
							}
						}
						
						JPokerMessage gameOverMsg = new JPokerMessage(
								"gameOver", message.playerName, "invalid ans", numberOfPlayers, players, allUsers, null
						);
						Message gameOverJmsMsg;
						try {
							gameOverJmsMsg = jmsHelper.createMessage(gameOverMsg);
							broadcastMessage(topicSender, gameOverJmsMsg);
						} catch (JMSException e) {
							System.out.println("Error at broadcasting message: "+e);
						}
					}
					
				}
				
				
			}
		} // keep handling games

	}
	
	private int random_in_range(int min, int max){
        return random.nextInt(max - min + 1) + min;
    }
	
	private Integer parseAnswer(String answer) {
        Jep jep = new Jep();
        Object res;
        try {
            jep.parse(answer);
            res = jep.evaluate();
        } catch (ParseException e) {
            return null;
        } catch (EvaluationException e) {
            return null;
        }
        Double ca = (Double)res;
        Integer result = (int) Math.round(ca);
        return result;
    }
	
	public JPokerMessage receiveMessage(MessageConsumer queueReader) throws JMSException {
		try {
			Message jmsMessage = queueReader.receive();
			return (JPokerMessage)((ObjectMessage)jmsMessage).getObject();
		} catch(JMSException e) {
			System.err.println("Failed to receive message: "+e);
			throw e;
		}
	}
	
	public void broadcastMessage(MessageProducer topicSender, Message jmsMessage) throws JMSException {
		try {
			topicSender.send(jmsMessage);
		} catch(JMSException e) {
			System.err.println("Failed to boardcast message: "+e);
			throw e;
		}
	}
	
	
	/* Start of atomic operations */
	
	public synchronized void resetOnlineUsers() throws IOException {
		try {
			Statement statement = dbConn.createStatement();
			statement.executeUpdate("DELETE FROM online_user");
		} catch (SQLException | IllegalArgumentException e) {
			System.err.println("Error deleting record: "+e);
		}
    }
	
	private synchronized ArrayList<Player> readUserInfo() {
		ArrayList<Player> users = new ArrayList<Player>();
		try {
			Statement statement = dbConn.createStatement();
			
			ResultSet result = statement.executeQuery(
				"SELECT * FROM player"
			);
			while(result.next()) {
				users.add(new Player(
					result.getInt(1), 
					result.getString(2),
					result.getInt(3),
					result.getInt(4),
					result.getDouble(5),
					result.getString(6)
				));
			}
		}catch(SQLException e) {
			System.err.println("Error reading user info: "+e);
		}
		return users;
	}
	
	private synchronized ArrayList<String> readOnlineUser() {
		ArrayList<String> onlineUsers = new ArrayList<String>();
		try {
			Statement statement = dbConn.createStatement();
			ResultSet result = statement.executeQuery("SELECT online_player_name FROM online_user");
			while(result.next()) {
				onlineUsers.add(result.getString(1));
			}
		}catch(SQLException e) {
			System.err.println("Error reading online user: "+e);
		}
		return onlineUsers;
	}
	
	private synchronized void addUser(Player newUser) {
		try {
			int games_played = newUser.games_played;
			Double total_winning_time = newUser.total_winning_time;
			
			PreparedStatement addUserStatement = dbConn.prepareStatement(
					"INSERT INTO player (" +
					"player_rank, player_name, games_won, games_played, total_winning_time, password)" + 
					" VALUES (?, ?, ?, ?, ?, ?)"
			);
			addUserStatement.setInt(1, newUser.player_rank);
			addUserStatement.setString(2, newUser.player_name);
			addUserStatement.setInt(3, newUser.games_won);
			addUserStatement.setInt(4, games_played);
			addUserStatement.setDouble(5, total_winning_time);
			addUserStatement.setString(6, newUser.password);
			addUserStatement.execute();
		} catch (SQLException | IllegalArgumentException e) {
			System.err.println("Error adding user: "+e);
		}
	}
	
	private synchronized void addOnlineUser(String userName) {
		try {
			PreparedStatement addOnlineUserStatement = dbConn.prepareStatement(
					"INSERT INTO online_user (online_player_name) VALUES (?)"
			);
			addOnlineUserStatement.setString(1, userName);
			addOnlineUserStatement.execute();
		} catch (SQLException | IllegalArgumentException e) {
			System.err.println("Error adding online user: "+e);
		}
	}
	
	private synchronized void removeOnlineUser(String userName) {
		try {
			// delete from DB
			PreparedStatement removeOnlineUserStatement = dbConn.prepareStatement("DELETE FROM online_user WHERE online_player_name = ?");
			
			removeOnlineUserStatement.setString(1, userName);
			removeOnlineUserStatement.executeUpdate();
		} catch (SQLException | IllegalArgumentException e) {
			System.err.println("Error deleting record: "+e);
		}
	}
	
	private synchronized void updateUser(Player player) {
		
		try {
			PreparedStatement stmt = dbConn.prepareStatement(
					"UPDATE player SET games_won = ?, games_played = ?, total_winning_time = ? WHERE player_name = ?"
			);
			stmt.setInt(1, player.games_won);
			stmt.setInt(2, player.games_played);
			stmt.setDouble(3, player.total_winning_time);
			stmt.setString(4, player.player_name);
			stmt.executeUpdate();
		} catch (SQLException | IllegalArgumentException e) {
			System.err.println("Error deleting record: "+e);
		}
	}
	
	private synchronized ArrayList<Player> recalculateRanking() {
		ArrayList<Player> users = readUserInfo();
		Collections.sort(users, new Comparator<Player>() {
		    @Override
		    public int compare(Player lhs, Player rhs) {
		    	if(lhs.games_won > rhs.games_won) {
		    		return -1;
		    	}else if(lhs.games_won < rhs.games_won) {
		    		return 1;
		    	}else {
		    		return 0;
		    	}
		    }
		});
		try {
			PreparedStatement stmt = dbConn.prepareStatement(
					"UPDATE player SET player_rank = ? WHERE player_name = ?"
			);
			for(int i=0; i<users.size(); i++) {
				users.get(i).player_rank = i+1;
				stmt.setInt(1, users.get(i).player_rank);
				stmt.setString(2, users.get(i).player_name);
				stmt.executeUpdate();
			}
		} catch (SQLException | IllegalArgumentException e) {
			System.err.println("Error deleting record: "+e);
		}
		return users;
	}
	
	/* End of atomic operations */
	
	
	
	public ArrayList<Player> login(String userName, String password) throws RemoteException{
		boolean userNamePasswordCorrect = false;
		boolean userNotOnline = true;
		
		// validate user
		ArrayList<Player> users = readUserInfo();
		for(Player user : users) {
			if(userName.equals(user.player_name) && password.equals(user.password)){
				userNamePasswordCorrect = true;
				break;
			}
		}
		
		// avoid repeated login
		ArrayList<String> onlineUsers = readOnlineUser();
		for(String user : onlineUsers ) {
			if(user.equals(userName)) {
				userNotOnline = false;
				break;
			}
		}
		
		if(userNamePasswordCorrect && userNotOnline) {
			// allow login
			addOnlineUser(userName);
			return users;
		}else {
			// login fails
			return null;
		}
		
	}
	
	public ArrayList<Player> register(String userName, String password) throws RemoteException{
		// avoid duplicating user
		boolean userExist = false;
		ArrayList<Player> users = readUserInfo();
		for(Player user : users ) {
				if(userName.equals(user.player_name)){
					userExist = true;
					break;
				}
			}
		if(userExist) {
			// register fails
			return null;
		}else {
			// allow register
			Player newUser = new Player(users.size()+1, userName, 0, 0, 0.0, password);
			
			// update DB
			addOnlineUser(userName);
			addUser(newUser);
			
			users.add(newUser);
			return users;
		}
	}
	
	public void logout(String userName) throws RemoteException{
		removeOnlineUser(userName);
	}

}
