
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.TitledBorder;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;

import java.rmi.*;
import java.rmi.registry.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import javafx.util.Pair;


public class JPokerClient implements Runnable, MessageListener {
	private JFrame loginFrame, registerFrame, JPokerFrame;
	private JPanel loginPanel, registerPanel, JPokerPanel, userProfilePanel, playGamePanel, leaderBoardPanel;
	private JLabel name, winsCount, gamesCount, avgTimeToWin, rank, gamePlayHints, inputEvaluation;
	private JLabel[] playerStats, cards;
	private JButton newGameButton, submitButton, nextButton;
	private JTextField inputField;
	private JScrollPane scrollPane; // for storing the table in leader board page
	
	private Auth auth;
	private JMSHelper jmsHelper;
	private MessageProducer queueSender;
	private MessageConsumer topicReceiver;
	
	private ArrayList<Player> users;
	private Player userInfo;
	private String[] TABLE_COLUMN_NAME = { "Rank", "Player", "Games Won", "Games Played", "Avg. winning time" };
	
	public JPokerClient(String host) {
		try {
			Registry registry = LocateRegistry.getRegistry(host);
			auth = (Auth) registry.lookup("JPokerServer/Auth");
			System.out.println("Connected to Server");
			
			jmsHelper = new JMSHelper(host);
			queueSender = jmsHelper.createQueueSender();
			topicReceiver = jmsHelper.createTopicReader();
			topicReceiver.setMessageListener(this);
		} catch(Exception e) {
			System.err.println("Failed accessing RMI: "+e);
		}
	}
	
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new JPokerClient(args[0]));
	}
	
	public void run() {
		initFramesAndPanels();
		setLoginPanel();
		setRegisterPanel();
		setJPokerPanel();
		setFrames();
		setFrameVisibility(true, false, false);
	}
	
	private void initFramesAndPanels() {
		// create frames
		loginFrame = new JFrame("Login");
		registerFrame = new JFrame("registerFrame");
		JPokerFrame = new JFrame("JPoker 24-Game");
		
		loginPanel = new JPanel();
		registerPanel = new JPanel();
		JPokerPanel = new JPanel();
	}
	
	private void setLoginPanel() {
		// init component
		JPanel loginButtonPanel;
		JLabel loginNameLabel, passwordLabel;
		JButton loginButton, registerButton;
		JTextField loginName, password;
		
		loginButtonPanel = new JPanel();
		loginNameLabel = new JLabel("Login Name", SwingConstants.CENTER);
		passwordLabel = new JLabel("Password", SwingConstants.CENTER);
		loginName = new JTextField(15);
		password = new JTextField(15);
		loginButton = new JButton("Login");
		registerButton = new JButton("Register");
		
		
		// set panel
		loginButtonPanel.add(loginButton, BorderLayout.SOUTH);
		JPanel dummy = new JPanel();
		loginButtonPanel.add(dummy, BorderLayout.CENTER);
		loginButtonPanel.add(registerButton, BorderLayout.EAST);
		
		GridLayout loginlayout = new GridLayout(5,0);
		loginPanel.setLayout(loginlayout);
		loginPanel.add(loginNameLabel);
		loginPanel.add(loginName);
		loginPanel.add(passwordLabel);
		loginPanel.add(password);
		loginPanel.add(loginButtonPanel);
		
		
		// set size
		loginButtonPanel.setMinimumSize(new Dimension(400, 40));
		loginName.setPreferredSize(new Dimension(400, 30));
		password.setPreferredSize(new Dimension(400, 30));
		loginNameLabel.setPreferredSize(new Dimension(400, 30));
		passwordLabel.setPreferredSize(new Dimension(400, 30));
		dummy.setPreferredSize(new Dimension(100, 30));
		
		// add listener
		loginButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				if(loginName.getText().length() == 0 || 
					password.getText().length() == 0 ) {
					JOptionPane.showMessageDialog(loginFrame,
						    "No field can be empty",
						    "Error",
						    JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				String userNameEntered = loginName.getText();
				String passwordEntered = password.getText();
				
				loginName.setText("");
				password.setText("");
				new LoginWorker(userNameEntered, passwordEntered).execute();
			}
		});
		
		registerButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				setFrameVisibility(false, true, false);
			}
		});
	}
	
	private void setRegisterPanel() {
		// init component
		JPanel sumitButtonPanel;
		JLabel loginNameLabel, passwordLabel, comfirmPasswordLabel;
		JTextField loginName, password, confirmPassword;
		JButton submitButton, cancelButton;
		
		sumitButtonPanel = new JPanel();
		loginNameLabel = new JLabel("Login Name", SwingConstants.CENTER);
		passwordLabel = new JLabel("Password", SwingConstants.CENTER);
		comfirmPasswordLabel = new JLabel("Confirm Password", SwingConstants.CENTER);
		loginName = new JTextField(15);
		password = new JTextField(15);
		confirmPassword = new JTextField(15);
		submitButton = new JButton("Register");
		cancelButton = new JButton("Cancel");
		
		
		// init panel
		sumitButtonPanel.add(submitButton, BorderLayout.SOUTH);
		JPanel dummy = new JPanel();
		sumitButtonPanel.add(dummy, BorderLayout.CENTER);
		sumitButtonPanel.add(cancelButton, BorderLayout.EAST);
		
		GridLayout loginlayout = new GridLayout(7,0);
		registerPanel.setLayout(loginlayout);
		registerPanel.add(loginNameLabel);
		registerPanel.add(loginName);
		registerPanel.add(passwordLabel);
		registerPanel.add(password);
		registerPanel.add(comfirmPasswordLabel);
		registerPanel.add(confirmPassword);
		registerPanel.add(sumitButtonPanel);
		
		
		// set size
		sumitButtonPanel.setMinimumSize(new Dimension(400, 40));
		loginName.setPreferredSize(new Dimension(400, 30));
		password.setPreferredSize(new Dimension(400, 30));
		confirmPassword.setPreferredSize(new Dimension(400, 30));
		loginNameLabel.setPreferredSize(new Dimension(400, 30));
		passwordLabel.setPreferredSize(new Dimension(400, 30));
		comfirmPasswordLabel.setPreferredSize(new Dimension(400, 30));
		dummy.setPreferredSize(new Dimension(100, 30));
		
		// add listener
		submitButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				
				if(loginName.getText().length() == 0 || 
				   password.getText().length() == 0 ||
				   confirmPassword.getText().length() == 0) {
					JOptionPane.showMessageDialog(registerFrame,
						    "No field can be empty!",
						    "Error",
						    JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				if(! password.getText().equals( confirmPassword.getText() )) {
					JOptionPane.showMessageDialog(registerFrame,
						    "Confirm Password not match!",
						    "Error",
						    JOptionPane.ERROR_MESSAGE);
					return;
				}

				String loginNameEnterred = loginName.getText(), passwordEntered = password.getText();
				
				loginName.setText("");
				password.setText("");
				confirmPassword.setText("");
				
				new RegisterWorker(loginNameEnterred, passwordEntered).execute();
				
			}
		});
		
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				loginName.setText("");
				password.setText("");
				confirmPassword.setText("");
				
				setFrameVisibility(true, false, false);
			}
		});
	}
	
	private void setJPokerPanel() {
		JButton userProfile, playGame, leaderBoard, logout;
		userProfile = new JButton("User Profile");
		playGame = new JButton("Play Game");
		leaderBoard = new JButton("Leader Board");
		logout = new JButton("Logout");
		
		JPanel menuPanel = new JPanel();
		menuPanel.setLayout(new FlowLayout());
		menuPanel.add(userProfile);
		menuPanel.add(playGame);
		menuPanel.add(leaderBoard);
		menuPanel.add(logout);
		
		userProfile.setPreferredSize(new Dimension(190, 40));
		playGame.setPreferredSize(new Dimension(190, 40));
		leaderBoard.setPreferredSize(new Dimension(190, 40));
		logout.setPreferredSize(new Dimension(190, 40));
		menuPanel.setPreferredSize(new Dimension(800, 50));
		
		
		userProfilePanel = new JPanel(); 
		playGamePanel = new JPanel(); 
		leaderBoardPanel = new JPanel();
		
		setPokerPanelVisibility(true, false, false);
		
		userProfilePanel.setPreferredSize(new Dimension(800, 450));
		playGamePanel.setPreferredSize(new Dimension(800, 450));
		leaderBoardPanel.setPreferredSize(new Dimension(800, 450));
		
		setUserProfile();
		setLeaderBoard();
		setPlayGame();
		
		JPokerPanel.setLayout(new BoxLayout(JPokerPanel, BoxLayout.Y_AXIS));
		JPokerPanel.add(menuPanel);
		JPokerPanel.add(userProfilePanel);
		JPokerPanel.add(playGamePanel);
		JPokerPanel.add(leaderBoardPanel);
		
		// add listener
		userProfile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				setPokerPanelVisibility(true, false, false);
			}
		});
		
		playGame.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				setPokerPanelVisibility(false, true, false);
			}
		});
		
		leaderBoard.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				setPokerPanelVisibility(false, false, true);
			}
		});
		
		logout.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				setPokerPanelVisibility(true, false, false);
				setFrameVisibility(true, false, false);
				
				new LogoutWorker().execute();
			}
		});
	}
	
	private void setFrames() {
		// set login frame
		loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		loginFrame.add(loginPanel);
		loginFrame.pack();
		
		// set register frame
		registerFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		registerFrame.add(registerPanel);
		registerFrame.pack();	
		
		// set JPoker frame
		JPokerFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPokerFrame.add(JPokerPanel);
		JPokerFrame.pack();	
	}
	
	private void setFrameVisibility(boolean seeLogin, boolean seeRegister, boolean seeJPoker) {
		loginFrame.setVisible(seeLogin);
		registerFrame.setVisible(seeRegister);
		JPokerFrame.setVisible(seeJPoker);
	}

	private void updateUIAfterloginRegister(String userNameEntered, String passwordEntered) {
		Collections.sort(users, new Comparator<Player>() {
		    @Override
		    public int compare(Player lhs, Player rhs) {
		    	if(lhs.player_rank < rhs.player_rank) {
		    		return -1;
		    	}else if(lhs.player_rank > rhs.player_rank) {
		    		return 1;
		    	}else {
		    		return 0;
		    	}
		    }
		});
		
		
		for(Player user: users) {
			if(userNameEntered.equals(user.player_name)) {
				userInfo = user;
				break;
			}
		}
		
		name.setText(userInfo.player_name);
		name.invalidate();
		winsCount.setText("Number of wins: " + userInfo.games_won);
		winsCount.invalidate();
		gamesCount.setText("Number of games: " + userInfo.games_played);
		gamesCount.invalidate();
		Double avg = 0.0;
		if(userInfo.games_played>0) {
			avg = userInfo.total_winning_time/userInfo.games_won;
		}
		avgTimeToWin.setText("Average time to win: " + String.format("%.2f", avg));
		avgTimeToWin.invalidate();
		rank.setText("Rank #" + userInfo.player_rank);
		rank.invalidate();
		
		if(scrollPane!=null) {
			// remove previous user's content
			leaderBoardPanel.remove(scrollPane);
		}
		
		ArrayList<String[]> table_data = new ArrayList<String[]>();
		for(Player player : users) {
			Double temp = 0.0;
			if(player.games_won>0) {
				temp = player.total_winning_time/player.games_won;
			}
			
			String[] row = { 
					""+player.player_rank, 
					player.player_name, 
					""+player.games_won, 
					""+player.games_played,
					String.format("%.2f", temp)
			};
			table_data.add(row);
		}
		
		JTable table = new JTable(table_data.toArray(new String[table_data.size()][]), TABLE_COLUMN_NAME);
		table.setFillsViewportHeight(true);
		
		scrollPane = new JScrollPane(table);
		scrollPane.setPreferredSize(new Dimension(780, 380));
		scrollPane.setBounds(10, 10, 780, 380); 
		
		leaderBoardPanel.add(scrollPane);
		leaderBoardPanel.repaint();
		leaderBoardPanel.invalidate();
		leaderBoardPanel.revalidate();
		
		setFrameVisibility(false, false, true);
	}
	
	private Boolean login(String loginName, String password) {
		try {
			users = auth.login(loginName, password);
			
			if(users==null) {
				JOptionPane.showMessageDialog(loginFrame,
					    "Login fail",
					    "Error",
					    JOptionPane.ERROR_MESSAGE);
				return new Boolean(false);
			}
		}catch (RemoteException e) {
			System.err.println("Failed invoking RMI: "+e);
			return new Boolean(false);
		}
		return new Boolean(true);
	}
	
	private Boolean register(String loginName, String password) {
		try {
			users = auth.register(loginName, password);
			if(users == null) {
				JOptionPane.showMessageDialog(registerFrame,
					    "Register fail",
					    "Error",
					    JOptionPane.ERROR_MESSAGE);
				return new Boolean(false);
			}
		} catch (RemoteException e) {
			System.err.println("Failed invoking RMI: "+e);
			return new Boolean(false);
		}
		return new Boolean(true);
	}
	
	private Boolean logout() {
		try {
			auth.logout(userInfo.player_name);
		} catch (RemoteException e) {
			System.err.println("Failed invoking RMI: "+e);
			return new Boolean(false);
		}
		return new Boolean(true);
	}

	private void setUserProfile() {
		name = new JLabel();
		winsCount = new JLabel();
		gamesCount = new JLabel();
		avgTimeToWin = new JLabel();
		rank = new JLabel();
		
		userProfilePanel.setLayout(null);
		userProfilePanel.add(name);
		userProfilePanel.add(winsCount);
		userProfilePanel.add(gamesCount);
		userProfilePanel.add(avgTimeToWin);
		userProfilePanel.add(rank);
		
		int baseX = 80, baseY = 50;
		name.setFont(new Font("serif", Font.BOLD, 50));
		name.setBounds(baseX, 10 + baseY, 400, 50);
		winsCount.setFont(new Font("serif", Font.PLAIN, 18));
		winsCount.setBounds(baseX, 80 + baseY, 400, 30);
		gamesCount.setFont(new Font("serif", Font.PLAIN, 18));
		gamesCount.setBounds(baseX, 120 + baseY, 400, 30);
		avgTimeToWin.setFont(new Font("serif", Font.PLAIN, 18));
		avgTimeToWin.setBounds(baseX, 160 + baseY, 400, 30);
		rank.setFont(new Font("serif", Font.ITALIC, 36));
		rank.setBounds(baseX, 220 + baseY, 400, 40);
		
	}
	
	private void setLeaderBoard() {
		JTable table = new JTable();
		table = new JTable();
		table.setFillsViewportHeight(true);
		
		scrollPane = new JScrollPane(table);
		scrollPane.setPreferredSize(new Dimension(780, 380));
		scrollPane.setBounds(10, 10, 780, 380); 
		
		leaderBoardPanel.add(scrollPane);
	}

	private void setPlayGame() {
		// init components
		newGameButton = new JButton("New Game");
		submitButton = new JButton("Submit");
		nextButton = new JButton("Next");
		inputField = new JTextField(25);
		inputEvaluation = new JLabel("= 24", SwingConstants.CENTER);
		gamePlayHints = new JLabel();
		playerStats = new JLabel[4];
		cards = new JLabel[4];
		for(int i=0; i<4; i++) {
			playerStats[i] = new JLabel("", SwingConstants.CENTER);
			cards[i] = new JLabel();
		}
		
		// add to panel
		playGamePanel.setLayout(null);
		playGamePanel.add(gamePlayHints);
		playGamePanel.add(newGameButton);
		playGamePanel.add(submitButton);
		playGamePanel.add(nextButton);
		playGamePanel.add(inputField);
		playGamePanel.add(inputEvaluation);
		for(int i=0; i<4; i++) {
			playGamePanel.add(playerStats[i]);
			playGamePanel.add(cards[i]);
		}
		
		// set position
		int baseX = 15, baseY = 0;
		gamePlayHints.setFont(new Font("serif", Font.PLAIN, 24));
		gamePlayHints.setText("Waiting for players ...");
		gamePlayHints.setBounds(baseX+275, baseY+100, 400, 200);
		inputField.setBounds(baseX, baseY+405, 475, 35);
		inputEvaluation.setBounds(baseX+480, baseY+405, 130, 35);
		inputEvaluation.setFont(new Font("serif", Font.BOLD, 16));
		newGameButton.setBounds(baseX, baseY+10, 770, 420);
		submitButton.setBounds(baseX+480, baseY+405, 80, 35);
		nextButton.setBounds(baseX, baseY+405, 775, 35);
		for(int i=0; i<4; i++) {
			String title = "Player "+(i+1);
			if(i==0) {
				title += " (You)";
			}
			TitledBorder titled = new TitledBorder(title);
			String stat = getStatString("Player", 0, 0, 0.0);
			playerStats[i].setFont(new Font("serif", Font.PLAIN, 16));
			playerStats[i].setText(stat);
			playerStats[i].setBounds(baseX+580, baseY+(100*i), 200, 100);
			playerStats[i].setBorder(titled);
			
			int w=140, h=190;
			// get scaled image
			cards[i].setIcon(getCardIcon("back.png"));
			cards[i].setBounds(baseX+(143*i), baseY+110, w, h);
		}
		
		// set visibility
		gamePlayHints.setVisible(false);
		inputField.setVisible(false);
		inputEvaluation.setVisible(false);
		submitButton.setVisible(false);
		nextButton.setVisible(false);
		for(int i=0; i<4; i++) {
			playerStats[i].setVisible(false);
			cards[i].setVisible(false);
		}
		
		// add listener
		newGameButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				newGameButton.setVisible(false);
				gamePlayHints.setVisible(true);
				
				new NewGameWorker().execute();
			}
		});
		
		submitButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				submitButton.setVisible(false);
				
				String answer = inputField.getText();
				String[] letter = {"J", "Q", "K", "A"};
				String[] letterValue = {"11", "12", "13", "1"};
				for(int i=0; i<4; i++) {
					answer = answer.replaceAll(letter[i], letterValue[i]);
				}
				new SubmitAnswerWorker(answer).execute();
			}
		});
		
		nextButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				gamePlayHints.setText("Waiting for players ...");
				nextButton.setVisible(false);
				
				new NewGameWorker().execute();
			}
		});
	}
	
	private Boolean submitAnswerToServer(String answer) {
		try {
			JPokerMessage answerMessage = new JPokerMessage(
					"submitAnswer", userInfo.player_name, answer, null, null, null, null
			);
			Message message = jmsHelper.createMessage(answerMessage);
			if(message != null) {
				queueSender.send(message);
			}
		} catch (JMSException e) {
			System.err.println("Failed to send message");
		}
		return new Boolean(true);
	}
	
	private Boolean joinNewGame() {
		try {
			JPokerMessage joinGameMessage = new JPokerMessage(
					"joinGame", userInfo.player_name, null, null, null, null, null
			);
			Message message = jmsHelper.createMessage(joinGameMessage);
			if(message != null) {
				queueSender.send(message);
			}
		} catch (JMSException e) {
			System.err.println("Failed to send message");
		}
		return new Boolean(true);
	}
	
	private String getStatString(String name, int wonGame, int totalGame, Double avgTimeToWin) {
		return String.format("<html><font size=\"16\">%s</font><br/>Win: %d/%d | Avg: %.1fs</html>", 
				name, wonGame, totalGame, avgTimeToWin);
	}
	
	private ImageIcon getCardIcon(String fileName) {
		int w=140, h=190;
		ImageIcon icon = new ImageIcon(getClass().getResource("/images/"+fileName));
		Image image = icon.getImage(); // transform it 
		Image newimg = image.getScaledInstance(w, h,  java.awt.Image.SCALE_SMOOTH);
		icon = new ImageIcon(newimg);
		return icon;
	}
	
	private void setPokerPanelVisibility(boolean seeUserProfile, boolean seePlayGame, boolean seeLeaderBoard) {
		userProfilePanel.setVisible(seeUserProfile);
		playGamePanel.setVisible(seePlayGame);
		leaderBoardPanel.setVisible(seeLeaderBoard);
	}
	
	private class LoginWorker extends SwingWorker<Boolean, Void> {
		String loginName, password;
		
		LoginWorker(String loginName, String password){
			super();
			this.loginName = loginName;
			this.password = password;
		}
		@Override
		protected Boolean doInBackground() {
			return login(loginName, password);
		}
		@Override
       protected void done() {
           try {
        	   if(get()!=null && get()) {
        		   updateUIAfterloginRegister(loginName, password);
        	   }
           } catch (Exception e) {
        	   System.err.println("Failed login: "+e);
           }
       }
	}
	
	private class RegisterWorker extends SwingWorker<Boolean, Void> {
		String loginName, password;
		
		RegisterWorker(String loginName, String password){
			super();
			this.loginName = loginName;
			this.password = password;
		}
		@Override
		protected Boolean doInBackground() {
			return register(loginName, password);
		}
		@Override
		protected void done() {
			try {
				if(get()!=null && get()) {
					updateUIAfterloginRegister(loginName, password);
				}
			} catch (Exception e) {
				System.err.println("Failed login: "+e);
			}
		}
	}
	
	private class LogoutWorker extends SwingWorker<Boolean, Void> {
		@Override
		protected Boolean doInBackground() {
			return logout();
		}
		@Override
		protected void done() {
			try {
				if(get()!=null && get()) {
					setFrameVisibility(true, false, false);
				}
			} catch (Exception e) {
				System.err.println("Failed logout: "+e);
			}
		}
	}

	private class SubmitAnswerWorker extends SwingWorker<Boolean, Void> {
		String answer;
		
		SubmitAnswerWorker(String answer){
			super();
			this.answer = answer;
		}
		
		@Override
		protected Boolean doInBackground() {
			return submitAnswerToServer(answer);
		}
	}
	
	private class NewGameWorker extends SwingWorker<Boolean, Void> {
		@Override
		protected Boolean doInBackground() {
			return joinNewGame();
		}
	}
	
	private class StartGameWorker extends SwingWorker<Void, Void> {
		JPokerMessage message;
		
		public StartGameWorker(JPokerMessage message) {
			super();
			this.message = message;
		}
		
		@Override
		protected Void doInBackground() {
			return null;
		}
		@Override
		protected void done() {
			// set visibility
			gamePlayHints.setVisible(false);
			inputField.setVisible(true);
			submitButton.setVisible(true);
			inputField.setEditable(true);
			for(int i=0; i<4; i++) {
				String iconFileName = "card"+message.cardIds[i].toString()+".png";
				cards[i].setIcon(getCardIcon(iconFileName));
				cards[i].setVisible(true);
			}
			ArrayList<Player> othersPlayers = new ArrayList<>();
			for(Player p: message.players) {
				if(!p.player_name.equals(userInfo.player_name)) {
					othersPlayers.add(p);
				}
			}
			for(int i=0; i<message.numberOfPlayer; i++) {
				Player p;
				
				if(i==0) {
					p = userInfo;
				}else {
					p = othersPlayers.get(i-1);
				}
				
				playerStats[i].setText(getStatString(p.player_name, p.games_won, p.games_played, p.total_winning_time));
				playerStats[i].setVisible(true);
			}
		}
	}
	
	private class GameOverWorker extends SwingWorker<Void, Void> {
		JPokerMessage message;
		
		GameOverWorker(JPokerMessage message){
			super();
			this.message = message;
		}
		
		@Override
		protected Void doInBackground() {
			return null;
		}
		
		@Override
		protected void done() {
			inputEvaluation.setVisible(true);
			inputEvaluation.setText(message.message);
			inputField.setEditable(false);
		}
	}
	
	private class EndGameWorker extends SwingWorker<Void, Void> {
		JPokerMessage message;
		
		EndGameWorker(JPokerMessage message){
			super();
			this.message = message;
		}
		
		@Override
		protected Void doInBackground() {
			return null;
		}
		
		@Override
		protected void done() {
			gamePlayHints.setText(message.message);
			inputField.setVisible(false);
			inputField.setText("");
			inputEvaluation.setVisible(false);
			submitButton.setVisible(false);
			for(int i=0; i<4; i++) {
				playerStats[i].setVisible(false);
				cards[i].setVisible(false);
			}
			gamePlayHints.setVisible(true);
			nextButton.setVisible(true);
		}
	}
	
	private class UpdatePlayerStatWorker extends SwingWorker<Void, Void> {
		JPokerMessage message;
		
		UpdatePlayerStatWorker(JPokerMessage message){
			super();
			this.message = message;
		}
		
		@Override
		protected Void doInBackground() {
			return null;
		}
		
		@Override
		protected void done() {
			// update stat in game tab
			ArrayList<Player> othersPlayers = new ArrayList<>();
			for(Player p: message.players) {
				if(!p.player_name.equals(userInfo.player_name)) {
					othersPlayers.add(p);
				}else {
					userInfo=p;
				}
			}
			for(int i=0; i<message.numberOfPlayer; i++) {
				Player p;
				
				if(i==0) {
					p = userInfo;
				}else {
					p = othersPlayers.get(i-1);
				}
				
				playerStats[i].setText(getStatString(p.player_name, p.games_won, p.games_played, p.total_winning_time));
			}
			// update users ArrayList
			users = message.allUsers;
			
			Collections.sort(users, new Comparator<Player>() {
			    @Override
			    public int compare(Player lhs, Player rhs) {
			    	if(lhs.player_rank < rhs.player_rank) {
			    		return -1;
			    	}else if(lhs.player_rank > rhs.player_rank) {
			    		return 1;
			    	}else {
			    		return 0;
			    	}
			    }
			});
			
			// update table in leader board
			ArrayList<String[]> table_data = new ArrayList<String[]>();
			for(Player player : users) {
				Double temp = 0.0;
				if(player.games_won>0) {
					temp = player.total_winning_time/player.games_won;
				}
				
				String[] row = { 
						""+player.player_rank, 
						player.player_name, 
						""+player.games_won, 
						""+player.games_played,
						String.format("%.2f", temp)
				};
				table_data.add(row);
			}
			
			JTable table = new JTable(table_data.toArray(new String[table_data.size()][]), TABLE_COLUMN_NAME);
			table.setFillsViewportHeight(true);
			
			scrollPane = new JScrollPane(table);
			scrollPane.setPreferredSize(new Dimension(780, 380));
			scrollPane.setBounds(10, 10, 780, 380); 
			
			leaderBoardPanel.removeAll();
			leaderBoardPanel.add(scrollPane);
			leaderBoardPanel.repaint();
			leaderBoardPanel.invalidate();
			leaderBoardPanel.revalidate();
			
			name.setText(userInfo.player_name);
			name.invalidate();
			winsCount.setText("Number of wins: " + userInfo.games_won);
			winsCount.invalidate();
			gamesCount.setText("Number of games: " + userInfo.games_played);
			gamesCount.invalidate();
			Double avg = 0.0;
			if(userInfo.games_played>0) {
				avg = userInfo.total_winning_time/userInfo.games_won;
			}
			avgTimeToWin.setText("Average time to win: " + String.format("%.2f", avg));
			avgTimeToWin.invalidate();
			rank.setText("Rank #" + userInfo.player_rank);
			rank.invalidate();
		}
	}
	
	public void onMessage(Message jmsMessage) {
		if(userInfo==null) {
			System.out.println("userInfo==null");
			return;
		}
		
		try {
			JPokerMessage message = (JPokerMessage)((ObjectMessage)jmsMessage).getObject();
			
			System.out.println("received message: "+message.command);
			
			switch(message.command) {
				case "startGame":
					new StartGameWorker(message).execute();
					
					break;
				case "gameOver":
					if(message.playerName.equals(userInfo.player_name)) {
						new GameOverWorker(message).execute();
					}
					new UpdatePlayerStatWorker(message).execute();
					
					break;
				case "endGame":
					new EndGameWorker(message).execute();
					new UpdatePlayerStatWorker(message).execute();
					
					break;
				default:
			}
		} catch (JMSException e) {
			System.err.println("Failed to receive message");
		}
	}
	
}
