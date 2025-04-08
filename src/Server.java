import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private static final int TCP_PORT = 55632;
    private static final int UDP_PORT = 55633;

    // map clientId to ClientHandler
    private static Map<Integer, ClientHandler> clientMap = new ConcurrentHashMap<>();
    private static ConcurrentLinkedQueue<Buzz> buzzQueue = new ConcurrentLinkedQueue<>();

    private static int nextClientId = 0;

    public static void main(String[] args) {
        try {
            ServerSocket tcpServer = new ServerSocket(TCP_PORT);
            System.out.println("Server started on TCP port " + TCP_PORT);

            // this starts UDP listener thread
            UDPListener udpListener = new UDPListener(UDP_PORT, buzzQueue);
            udpListener.start();

            // accept sincoming TCP clients
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

    
    public static void handleBuzzesForQuestion(int questionNum) {
        Set<Integer> seenClients = new HashSet<>();
        Buzz winnerBuzz = null;

        Iterator<Buzz> iterator = buzzQueue.iterator();
        while (iterator.hasNext()) {
            Buzz b = iterator.next();

            if (b.questionNumber == questionNum) {
                if (!seenClients.contains(b.clientId)) {
                    seenClients.add(b.clientId);

                    if (winnerBuzz == null) {
                        winnerBuzz = b; // first valid buzz (poll)
                    }
                }
                iterator.remove(); // removes it once processed
            }
        }

        if (winnerBuzz != null) {
            ClientHandler winner = clientMap.get(winnerBuzz.clientId);
            if (winner != null) {
                winner.sendMessage("ACK|Q" + questionNum);
            }

            for (int otherId : seenClients) {
                if (otherId != winnerBuzz.clientId) {
                    ClientHandler other = clientMap.get(otherId);
                    if (other != null) {
                        other.sendMessage("NEG-ACK|Q" + questionNum);
                    }
                }
            }

            System.out.println("Client " + winnerBuzz.clientId + " won the buzz for Q" + questionNum);
        } else {
            System.out.println("No one buzzed for Q" + questionNum);
        }
    }

    // TCP client thread
    static class ClientHandler extends Thread {
        private Socket socket;
        private int clientId;

        public ClientHandler(Socket socket, int clientId) {
            this.socket = socket;
            this.clientId = clientId;
        }

        public void run() {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                out.println("WELCOME|ClientID=" + clientId);

                String msg;
                while ((msg = in.readLine()) != null) {
                    System.out.println("Client " + clientId + " says: " + msg);

                    if (msg.equalsIgnoreCase("PING")) {
                        out.println("PONG");
                    }
                }

            } catch (IOException e) {
                System.out.println("Client " + clientId + " disconnected.");
            }
        }

        public void sendMessage(String msg) {
            try {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println(msg);
            } catch (IOException e) {
                System.out.println("Failed to send message to Client " + clientId);
            }
        }
    }

    // UDP listener thread for polls
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
                        if (parts.length >= 3) {
                            try {
                                int clientId = Integer.parseInt(parts[1]);
                                int questionNum = Integer.parseInt(parts[2]);
                                Buzz buzz = new Buzz(clientId, questionNum);
                                queue.add(buzz);
                                System.out.println("Added to queue: " + buzz);
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

    // buzz object to track client buzzes (polls)
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