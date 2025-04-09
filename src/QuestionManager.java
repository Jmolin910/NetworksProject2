import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.security.SecureRandom;

public class QuestionManager {
    private List<String> questions;

    public QuestionManager(String filePath) {
        questions = new ArrayList<>();
        loadQuestionsFromFile(filePath);
    }

    // Read each line from the file and add to the list
    private void loadQuestionsFromFile(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                questions.add(line.trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Question loadQuestion(String question) {
        String[] parts = question.split("\\|");
        if (parts.length < 7) {
            throw new IllegalArgumentException("Invalid serialized question format.");
        }
        return new Question(parts[1], parts[2], parts[3], parts[4], parts[5], parts[6]);
    }

    public String getAndRemoveRandomQuestion() {
        if (questions.isEmpty()) {
            return null;
        }
        SecureRandom random = new SecureRandom();
        int index = random.nextInt(questions.size());
        return questions.remove(index);
    }
}