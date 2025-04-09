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
	private JLabel question;
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
		question = new JLabel("Waiting for question from server...");
		window.add(question);
		question.setBounds(10, 5, 350, 100);

		options = new JRadioButton[4];
		optionGroup = new ButtonGroup();
		for (int index = 0; index < options.length; index++) {
			options[index] = new JRadioButton("Option " + (index + 1)); // represents an option
			// if a radio button is clicked, the event would be thrown to this class to
			// handle
			options[index].addActionListener(this);
			options[index].setBounds(10, 110 + (index * 20), 350, 20);
			window.add(options[index]);
			optionGroup.add(options[index]);
			options[index].setEnabled(false);
		}

		timer = new JLabel("TIMER"); // represents the countdown shown on the window
		timer.setBounds(250, 250, 100, 20);
		clock = new TimerCode(15); // represents clocked task that should run after X seconds
		Timer t = new Timer(); // event generator
		t.schedule(clock, 0, 1000); // clock is called every second
		window.add(timer);

		score = new JLabel("SCORE"); // represents the score
		score.setBounds(50, 250, 100, 20);
		window.add(score);

		poll = new JButton("Poll"); // button that use clicks/ like a buzzer
		poll.setBounds(10, 300, 100, 20);
		poll.addActionListener(this); // calls actionPerformed of this class
		window.add(poll);

		submit = new JButton("Submit"); // button to submit their answer
		submit.setBounds(200, 300, 100, 20);
		submit.addActionListener(this); // calls actionPerformed of this class
		window.add(submit);
		submit.setEnabled(false);

		window.setSize(400, 400);
		window.setBounds(50, 50, 400, 400);
		window.setLayout(null);
		window.setVisible(true);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setResizable(true);

		new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					String msg = tcpClient.receiveMessageTCP();
					if (msg != null) {
						System.out.println("Background thread received: " + msg);
						if (msg.startsWith("QUESTION|")) {
							String[] parts = msg.split("\\|");
							if (parts.length >= 7) {
								currentQuestion = new Question(parts[1], parts[2], parts[3], parts[4], parts[5],
										parts[6]);
								SwingUtilities.invokeLater(new Runnable() {
									@Override
									public void run() {
										question.setText(currentQuestion.getQuestionText());
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
									}
								});
							} else {
								System.out.println("Received invalid question string: " + msg);
							}
						} else if (msg.equalsIgnoreCase("GAMEOVER")) {
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									JOptionPane.showMessageDialog(window, "Game Over! Final Score: " + scoreValue);
									poll.setEnabled(false);
									submit.setEnabled(false);
								}
							});
							break; // Exit the thread loop
						} else if (msg.equalsIgnoreCase("ack")) {
							// Poll winner: allow answering
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									isAnsweringAllowed = true;
									poll.setEnabled(false);
									for (JRadioButton option : options) {
										option.setEnabled(true);
									}
									submit.setEnabled(true);
									System.out.println("Received ack: Answering enabled.");
								}
							});
						} else if (msg.equalsIgnoreCase("negative-ack")) {
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									poll.setEnabled(false);
									JOptionPane.showMessageDialog(window, "You were late in polling");
									System.out.println("Received negative-ack: Answering disabled.");
								}
							});
						} else if (msg.equalsIgnoreCase("correct")) {
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									scoreValue += 10;
									JOptionPane.showMessageDialog(window, "Correct! +10 points");
								}
							});
						} else if (msg.equalsIgnoreCase("wrong")) {
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									scoreValue -= 10;
									JOptionPane.showMessageDialog(window, "Wrong! -10 points");
								}
							});
						} else {
							System.out.println("Unhandled message: " + msg);
						}
					}
				}
			}
		}).start();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		System.out.println("You clicked " + e.getActionCommand());

		// input refers to the radio button you selected or button you clicked
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

			tcpClient.sendPollMessage(clientId); // sends the poll message to the server
			clock.cancel(); // cancel the current TimerTask
			clock = new TimerCode(10); // restart timer for polling phase (15 seconds)
			new Timer().schedule(clock, 0, 1000);

		} else if (input.equals("Submit")) {
			// Disable submit button after clicking submit
			submit.setEnabled(false);
			poll.setEnabled(true);

			// sends the answer
			if (selectedOption != null) {
				tcpClient.sendAnswer(selectedOption);
			}

			// Update the question label with the new question
			question.setText(currentQuestion.getQuestionText());

			// Update radio buttons with new options and clear selection
			String[] optionTexts = { currentQuestion.getOptionA(), currentQuestion.getOptionB(),
					currentQuestion.getOptionC(), currentQuestion.getOptionD() };
			for (int index = 0; index < options.length; index++) {
				options[index].setText(optionTexts[index]);
				options[index].setSelected(false);
				options[index].setEnabled(false); // keep disabled until poll button is pressed
			}

			// Resets selected option
			selectedOption = null;
			optionGroup.clearSelection();

			// Reset timer for new question (if needed, cancel and start a new TimerTask)
			// For example:
			clock.cancel(); // cancel the current TimerTask
			clock = new TimerCode(15); // restart timer for polling phase (15 seconds)
			new Timer().schedule(clock, 0, 1000);

		} else {
			System.out.println("Incorrect Option");
		}

	}

	// this class is responsible for running the timer on the window
	public class TimerCode extends TimerTask {
		private int duration; // write setters and getters as you need

		public TimerCode(int duration) {
			this.duration = duration;
		}

		@Override
		public void run() {
			if (duration < 0) {
				timer.setText("Timer expired");
				window.repaint();
				this.cancel(); // cancel the timed task
				return;
				// you can enable/disable your buttons for poll/submit here as needed
			}

			if (duration < 6)
				timer.setForeground(Color.red);
			else
				timer.setForeground(Color.black);

			timer.setText(duration + "");
			duration--;
			window.repaint();
		}
	}

}