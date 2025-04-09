import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private static final int TCP_PORT = 55632;
    private static final int UDP_PORT = 55632;
    private static final int NumQuestions = 20;
    private static List<Integer> availableQuestions;

    // Map clientId to ClientHandler
    private static Map<Integer, ClientHandler> clientMap = new ConcurrentHashMap<>();
    private static ConcurrentLinkedQueue<Buzz> buzzQueue = new ConcurrentLinkedQueue<>();
    private static int nextClientId = 0;
    private static Integer currentBuzzWinner = null;

    // New fields for managing questions
    private static QuestionManager qm;
    private static String currentQuestion;
    private static Question currQ;

    public static void main(String[] args) {
        try {
            // Initialize a list of indices (if you need it for other purposes)
            availableQuestions = new ArrayList<>();
            for (int i = 0; i < NumQuestions; i++) {
                availableQuestions.add(i);
            }

            // Initialize QuestionManager with Questions.txt file and get the first question
            qm = new QuestionManager("src/Questions.txt");
            currentQuestion = qm.getAndRemoveRandomQuestion();
            currQ = qm.loadQuestion(currentQuestion);

            ServerSocket tcpServer = new ServerSocket(TCP_PORT);
            System.out.println("Server started on TCP port " + TCP_PORT);

            // Start UDP listener thread (for polls, etc.)
            UDPListener udpListener = new UDPListener(UDP_PORT, buzzQueue);
            udpListener.start();

            // Accept incoming TCP clients
            while (true) {
                Socket clientSocket = tcpServer.accept();
                int clientId = nextClientId++;

                ClientHandler ct = new ClientHandler(clientSocket, clientId);
                clientMap.put(clientId, ct);
                ct.start();

                System.out.println("Client " + clientId + " connected: " + clientSocket.getInetAddress());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to handle buzzes (unchanged)
    public static void handleBuzzes() {
        Set<Integer> seenClients = new HashSet<>();
        Buzz winnerBuzz = null;

        Iterator<Buzz> iterator = buzzQueue.iterator();
        while (iterator.hasNext()) {
            Buzz b = iterator.next();
            if (!seenClients.contains(b.clientId)) {
                seenClients.add(b.clientId);
                if (winnerBuzz == null) {
                    winnerBuzz = b; // The first valid buzz wins
                }
            }
            iterator.remove();
        }

        if (winnerBuzz != null) {
            ClientHandler winner = clientMap.get(winnerBuzz.clientId);
            if (winner != null) {
                winner.sendMessage("ack");
            }
            for (int otherId : seenClients) {
                if (otherId != winnerBuzz.clientId) {
                    ClientHandler other = clientMap.get(otherId);
                    if (other != null) {
                        other.sendMessage("negative-ack");
                    }
                }
            }
            System.out.println("Client " + winnerBuzz.clientId + " won the buzz");
        } else {
            System.out.println("No buzzes received");
        }
    }

    // TCP client thread
    static class ClientHandler extends Thread {
        private Socket socket;
        private int clientId;
        private PrintWriter out;

        public ClientHandler(Socket socket, int clientId) {
            this.socket = socket;
            this.clientId = clientId;
        }

        public void run() {
            try (
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));) {
                out = new PrintWriter(socket.getOutputStream(), true);

                // Send welcome message and then send the current question string
                out.println("WELCOME|ClientID=" + clientId);
                out.println(currentQuestion);

                String msg;
                while ((msg = in.readLine()) != null) {
                    System.out.println("Client " + clientId + " says: " + msg);

                    if (msg.startsWith("ANSWER|")) {
                        String[] parts = msg.split("\\|");
                        if (parts.length >= 3) {
                            // parts[1] is the clientID (can optionally be used for verification)
                            String answerOption = parts[2].trim();
                            // Check if the answer matches the correct answer
                            if (currentQuestion != null && answerOption.equals(currQ.getCorrectAnswer())) {
                                out.println("correct");
                                String nextQuestion = qm.getAndRemoveRandomQuestion();
                                if (nextQuestion != null) {
                                    currentQuestion = nextQuestion;
                                    currQ = qm.loadQuestion(currentQuestion);
                                    out.println(currentQuestion);
                                } else {
                                    out.println("no more questions");
                                }
                            } else {
                                out.println("wrong");
                                String nextQuestion = qm.getAndRemoveRandomQuestion();
                                if (nextQuestion != null) {
                                    currentQuestion = nextQuestion;
                                    currQ = qm.loadQuestion(currentQuestion);
                                    out.println(currentQuestion);
                                } else {
                                    out.println("no more questions");
                                }
                            }
                        } else {
                            out.println("error: invalid ANSWER format");
                        }
                        out.flush();
                    }
                }
            } catch (IOException e) {
                System.out.println("Client " + clientId + " disconnected.");
            }
        }

        public void sendMessage(String msg) {
            if (out != null) {
                out.println(msg);
            } else {
                System.out.println("Output stream not initialized for Client " + clientId);
            }
        }
    }

    static class UDPListener extends Thread {
        private int port;
        private ConcurrentLinkedQueue<Buzz> queue;

        public UDPListener(int port, ConcurrentLinkedQueue<Buzz> queue) {
            this.port = port;
            this.queue = queue;
        }

        public void run() {
            try (DatagramSocket socket = new DatagramSocket(port)) {
                byte[] buffer = new byte[256];
                System.out.println("UDP listener started on port " + port);

                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String msg = new String(packet.getData(), 0, packet.getLength());

                    System.out.println("Received UDP buzz: " + msg);

                    if (msg.startsWith("BUZZ|")) {
                        String[] parts = msg.split("\\|");
                        if (parts.length >= 2) {
                            try {
                                int clientId = Integer.parseInt(parts[1].trim());
                                Buzz buzz = new Buzz(clientId, 0);
                                queue.add(buzz);
                                System.out.println("Added to queue: " + buzz);
                                Server.handleBuzzes();
                            } catch (NumberFormatException e) {
                                System.out.println("Invalid BUZZ format: " + msg);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Buzz object for tracking polls
    static class Buzz {
        public int clientId;
        public int questionNumber;

        public Buzz(int clientId, int questionNumber) {
            this.clientId = clientId;
            this.questionNumber = questionNumber;
        }

        @Override
        public String toString() {
            return "Client " + clientId + " buzzed for question " + questionNumber;
        }
    }
}