
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class QuestionManager {
    private List<Question> questions;

    public QuestionManager(String filePath) {
        questions = new ArrayList<>();
        loadQuestionsFromFile(filePath);
    }

    private void loadQuestionsFromFile(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                /*  Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }*/
                // Recongnize the format: Question|Option A|Option B|Option C|Option D|Correct Answer
                String[] parts = line.split("\\|");
                /* 
                if (parts.length != 6) {
                    System.out.println("Invalid question format: " + line);
                    continue;
                }
                // Trim whitespace from each part
                for (int i = 0; i < parts.length; i++) {
                    parts[i] = parts[i].trim();
                }
                */
                // Create a new Question object
                Question q = new Question(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5]);
                questions.add(q);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // Returns and removes a random question from the list
    public Question getAndRemoveRandomQuestion() {
        if (questions.isEmpty()) {
            return null;
        }
        Random random = new Random();
        int index = random.nextInt(questions.size());
        return questions.remove(index);
    }

}
