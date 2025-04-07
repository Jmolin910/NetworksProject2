import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.security.SecureRandom;
import java.util.TimerTask;
import java.util.Timer;
import javax.swing.*;

public class ClientWindow implements ActionListener
{
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

	QuestionManager qm = new QuestionManager("src/Questions.txt");
	Question currentQuestion = qm.getAndRemoveRandomQuestion();
	String[] optionTexts = { currentQuestion.getOptionA(), currentQuestion.getOptionB(), currentQuestion.getOptionC(), currentQuestion.getOptionD() };

	// write setters and getters as you need
	
	public ClientWindow()
	{
		JOptionPane.showMessageDialog(window, "This is a trivia game");
		
		window = new JFrame("Trivia");
		question = new JLabel(currentQuestion.getQuestionText()); // represents the question
		window.add(question);
		question.setBounds(10, 5, 350, 100);;
		
		options = new JRadioButton[4];
		optionGroup = new ButtonGroup();
		for(int index=0; index<options.length; index++)
		{
			options[index] = new JRadioButton(optionTexts[index]);  // represents an option
			// if a radio button is clicked, the event would be thrown to this class to handle
			options[index].addActionListener(this);
			options[index].setBounds(10, 110+(index*20), 350, 20);
			window.add(options[index]);
			optionGroup.add(options[index]);
			options[index].setEnabled(false);
		}

		timer = new JLabel("TIMER");  // represents the countdown shown on the window
		timer.setBounds(250, 250, 100, 20);
		clock = new TimerCode(30);  // represents clocked task that should run after X seconds
		Timer t = new Timer();  // event generator
		t.schedule(clock, 0, 1000); // clock is called every second
		window.add(timer);
		
		
		score = new JLabel("SCORE"); // represents the score
		score.setBounds(50, 250, 100, 20);
		window.add(score);

		poll = new JButton("Poll");  // button that use clicks/ like a buzzer
		poll.setBounds(10, 300, 100, 20);
		poll.addActionListener(this);  // calls actionPerformed of this class
		window.add(poll);
		
		submit = new JButton("Submit");  // button to submit their answer
		submit.setBounds(200, 300, 100, 20);
		submit.addActionListener(this);  // calls actionPerformed of this class
		window.add(submit);
		submit.setEnabled(false);
		
		
		window.setSize(400,400);
		window.setBounds(50, 50, 400, 400);
		window.setLayout(null);
		window.setVisible(true);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setResizable(false);
	}

	// this method is called when you check/uncheck any radio button
	// this method is called when you press either of the buttons- submit/poll
	@Override
	public void actionPerformed(ActionEvent e)
	{
		System.out.println("You clicked " + e.getActionCommand());
		
		// input refers to the radio button you selected or button you clicked
		String input = e.getActionCommand();  
		switch(input)
		{
			case "Option 1":	// Your code here
								break;
			case "Option 2":	// Your code here
								break;
			case "Option 3":	// Your code here
								break;
			case "Option 4":	// Your code here
								break;
			case "Poll":		poll.setEnabled(false);
								submit.setEnabled(true);
								for(int index=0; index<options.length; index++)
								{
									options[index].setEnabled(true);  // enable the radio buttons
								}
								break;
			case "Submit":
								// Disable submit button after clicking submit
								submit.setEnabled(false);
								poll.setEnabled(true);
							
								// Retrieve the next question
								currentQuestion = qm.getAndRemoveRandomQuestion();
								if (currentQuestion == null) {
									// No more questions—end the game or show final score
									JOptionPane.showMessageDialog(window, "Game Over! Final Score: " + score.getText());
									// Optionally disable further interaction
									poll.setEnabled(false);
									break;
								}
								
								// Update the question label with the new question
								question.setText(currentQuestion.getQuestionText());
								
								// Update radio buttons with new options and clear selection
								String[] optionTexts = { currentQuestion.getOptionA(), currentQuestion.getOptionB(), currentQuestion.getOptionC(), currentQuestion.getOptionD() };
								for (int index = 0; index < options.length; index++) {
									options[index].setText(optionTexts[index]);
									options[index].setSelected(false);
									options[index].setEnabled(false); // keep disabled until poll button is pressed
								}
								
								// Reset timer for new question (if needed, cancel and start a new TimerTask)
								// For example:
								clock.cancel();  // cancel the current TimerTask
								clock = new TimerCode(15);  // restart timer for polling phase (15 seconds)
								new Timer().schedule(clock, 0, 1000);
								
								break;
			default:
								System.out.println("Incorrect Option");
		}
		
	}
	
	// this class is responsible for running the timer on the window
	public class TimerCode extends TimerTask
	{
		private int duration;  // write setters and getters as you need
		public TimerCode(int duration)
		{
			this.duration = duration;
		}
		@Override
		public void run()
		{
			if(duration < 0)
			{
				timer.setText("Timer expired");
				window.repaint();
				this.cancel();  // cancel the timed task
				return;
				// you can enable/disable your buttons for poll/submit here as needed
			}
			
			if(duration < 6)
				timer.setForeground(Color.red);
			else
				timer.setForeground(Color.black);
			
			timer.setText(duration+"");
			duration--;
			window.repaint();
		}
	}
	
}
