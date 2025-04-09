import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public class TCPClient {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String clientId;
    private String serverIP;
    private int serverPort;

    public TCPClient(String serverIP, int serverPort) {

        this.serverIP = serverIP;
        this.serverPort = serverPort;
        try {
            socket = new Socket(serverIP, serverPort);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("Sent connection to the server");

            // Read welcome message from the server
            String welcomeMessage = in.readLine();
            System.out.println("Received welcome message: " + welcomeMessage);

            // Parse the welcome message to extract the client ID
            if (welcomeMessage != null && welcomeMessage.startsWith("WELCOME|ClientID=")) {
                clientId = welcomeMessage.split("=")[1];
                System.out.println("Assigned Client ID: " + clientId);
            } else {
                System.out.println("Unexpected welcome message format: " + welcomeMessage);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to get the actual client ID
    public String getClientId() {
        return clientId;
    }

    // Method to send a poll message to the server
    public void sendPollMessage(String ClientId) {
        DatagramSocket udpSocket = null;
        try {
            udpSocket = new DatagramSocket();
            String message = "BUZZ|" + clientId;
            byte[] buffer = message.getBytes();

            InetAddress serverAddress = InetAddress.getByName(serverIP);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, serverPort);

            udpSocket.send(packet);
            System.out.println("Poll message sent using UDP: " + message);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (udpSocket != null) {
                udpSocket.close();
            }
        }
    }

    // Method to send an answer to the server
    public void sendAnswer(String selectedOption) {
        try {
            String message = "ANSWER|" + clientId + "|" + selectedOption;
            System.out.println("Sending answer: " + message);
            out.println(message);
            System.out.println("Answer sent: " + message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String receiveMessageTCP() {
        try {
            String msg = in.readLine();
            return msg;
        } catch (IOException e) {
            System.err.println("Error receiving TCP message: " + e.getMessage());
            return null;
        }
    }

    // Close the connection
    public void closeConnection() {
        try {
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void run() {
        try (DatagramSocket socket = new DatagramSocket(55632)) {
            byte[] buffer = new byte[256];
            System.out.println("UDP listener started on port " + 55632);

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String msg = new String(packet.getData(), 0, packet.getLength());

                System.out.println("Received : " + msg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}