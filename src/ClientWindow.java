import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.security.SecureRandom;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.*;

public class ClientWindow implements ActionListener {
	private JButton poll;
	private JButton submit;
	private JRadioButton options[];
	private ButtonGroup optionGroup;
	private JTextArea questionArea;
	private JLabel timer;
	private JLabel score;
	private TimerTask clock;

	private JFrame window;

	private static SecureRandom random = new SecureRandom();

	private TCPClient tcpClient;
	private String clientId;
	private Question currentQuestion;
	private String selectedOption = null;
	private int scoreValue = 0;
	private boolean isAnsweringAllowed = false;

	public ClientWindow() {
		tcpClient = new TCPClient("127.0.0.1", 55632);
		clientId = tcpClient.getClientId();

		JOptionPane.showMessageDialog(window, "This is a trivia game");

		window = new JFrame("Trivia");
		window.setLayout(null);

		questionArea = new JTextArea("Waiting for question from server...");
		questionArea.setLineWrap(true);
		questionArea.setWrapStyleWord(true);
		questionArea.setEditable(false);
		questionArea.setFocusable(false);
		questionArea.setOpaque(false);
		questionArea.setBounds(10, 5, 360, 60);
		window.add(questionArea);

		options = new JRadioButton[4];
		optionGroup = new ButtonGroup();
		for (int index = 0; index < options.length; index++) {
			options[index] = new JRadioButton("Option " + (index + 1));
			options[index].addActionListener(this);
			options[index].setBounds(10, 80 + (index * 20), 350, 20);
			window.add(options[index]);
			optionGroup.add(options[index]);
			options[index].setEnabled(false);
		}

		timer = new JLabel("TIMER");
		timer.setBounds(250, 250, 100, 20);
		clock = new TimerCode(15);
		Timer t = new Timer();
		t.schedule(clock, 0, 1000);
		window.add(timer);

		score = new JLabel("SCORE");
		score.setBounds(50, 250, 100, 20);
		window.add(score);

		poll = new JButton("Poll");
		poll.setBounds(10, 300, 100, 20);
		poll.addActionListener(this);
		window.add(poll);

		submit = new JButton("Submit");
		submit.setBounds(200, 300, 100, 20);
		submit.addActionListener(this);
		window.add(submit);
		submit.setEnabled(false);

		window.setSize(400, 400);
		window.setBounds(50, 50, 400, 400);
		window.setVisible(true);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setResizable(true);

		new Thread(() -> {
			while (true) {
				String msg = tcpClient.receiveMessageTCP();
				if (msg != null) {
					System.out.println("Background thread received: " + msg);

					if (msg.startsWith("QUESTION|")) {
						String[] parts = msg.split("\\|");
						if (parts.length >= 7) {
							currentQuestion = new Question(parts[1], parts[2], parts[3], parts[4], parts[5], parts[6]);
							SwingUtilities.invokeLater(() -> {
								questionArea.setText(currentQuestion.getQuestionText());
								String[] optionTexts = {
										currentQuestion.getOptionA(),
										currentQuestion.getOptionB(),
										currentQuestion.getOptionC(),
										currentQuestion.getOptionD()
								};
								for (int i = 0; i < options.length; i++) {
									options[i].setText(optionTexts[i]);
									options[i].setSelected(false);
									options[i].setEnabled(false);
								}
								optionGroup.clearSelection();
								submit.setEnabled(false);
								poll.setEnabled(true);
								isAnsweringAllowed = false;
								selectedOption = null;
								clock.cancel();
								clock = new TimerCode(15);
								new Timer().schedule(clock, 0, 1000);
							});
						}
					} else if (msg.equalsIgnoreCase("GAMEOVER")) {
						SwingUtilities.invokeLater(() -> {
							JOptionPane.showMessageDialog(window, "Game Over! Final Score: " + scoreValue);
							poll.setEnabled(false);
							submit.setEnabled(false);
						});
						break;
					} else if (msg.equalsIgnoreCase("ack")) {
						SwingUtilities.invokeLater(() -> {
							isAnsweringAllowed = true;
							poll.setEnabled(false);
							for (JRadioButton option : options) option.setEnabled(true);
							submit.setEnabled(true);
							System.out.println("Received ack: Answering enabled.");
						});
					} else if (msg.equalsIgnoreCase("negative-ack")) {
						SwingUtilities.invokeLater(() -> {
							poll.setEnabled(false);
							submit.setEnabled(false);
							isAnsweringAllowed = false;
							for (JRadioButton option : options) option.setEnabled(false);
							JOptionPane.showMessageDialog(window, "You were late in polling");
							System.out.println("Received negative-ack: Answering disabled.");
						});
					} else if (msg.equalsIgnoreCase("correct")) {
						SwingUtilities.invokeLater(() -> {
							scoreValue += 10;
							JOptionPane.showMessageDialog(window, "Correct! +10 points");
						});
					} else if (msg.equalsIgnoreCase("wrong")) {
						SwingUtilities.invokeLater(() -> {
							scoreValue -= 10;
							JOptionPane.showMessageDialog(window, "Wrong! -10 points");
						});
					} else if (msg.equalsIgnoreCase("KILL")) {
						SwingUtilities.invokeLater(() -> {
							JOptionPane.showMessageDialog(window, "The host has kicked you out the game.");
							System.exit(0);
						});
						break;
					}
				}
			}
		}).start();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		System.out.println("You clicked " + e.getActionCommand());
		String input = e.getActionCommand();
		if (input.equals(options[0].getText())) {
			selectedOption = "A";
		} else if (input.equals(currentQuestion.getOptionB())) {
			selectedOption = "B";
		} else if (input.equals(currentQuestion.getOptionC())) {
			selectedOption = "C";
		} else if (input.equals(currentQuestion.getOptionD())) {
			selectedOption = "D";
		} else if (input.equals("Poll")) {
			tcpClient.sendPollMessage(clientId);
			clock.cancel();
			clock = new TimerCode(10);
			new Timer().schedule(clock, 0, 1000);
		} else if (input.equals("Submit")) {
			if (!isAnsweringAllowed) return;
			if (selectedOption != null) {
				tcpClient.sendAnswer(selectedOption);
			}
			submit.setEnabled(false);
			poll.setEnabled(false);
			selectedOption = null;
		} else {
			System.out.println("Incorrect Option");
		}
	}

	public class TimerCode extends TimerTask {
		private int duration;

		public TimerCode(int duration) {
			this.duration = duration;
		}

		@Override
		public void run() {
			if (duration < 0) {
				timer.setText("Timer expired");
				window.repaint();
				this.cancel();
				return;
			}
			timer.setForeground(duration < 6 ? Color.red : Color.black);
			timer.setText(duration + "");
			duration--;
			window.repaint();
		}
	}
}
