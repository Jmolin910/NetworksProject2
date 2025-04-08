import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public class TCPClient {
    private Socket socket;
    private DataInputStream inStream;
    private DataOutputStream outStream;

    public TCPClient(String serverIP, int serverPort) {
        try {
            socket = new Socket(serverIP, serverPort);
            inStream = new DataInputStream(socket.getInputStream());
            outStream = new DataOutputStream(socket.getOutputStream());
            System.out.println("Connected to the server");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to send a poll message to the server
    public void sendPollMessage(String serverIP, int serverPort, String clientId, int questionNumber) {
        DatagramSocket udpSocket = null;
        try {
            udpSocket = new DatagramSocket();
            String message = "buzz" + " from client " + clientId + " for question " + questionNumber;
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
    public void sendAnswer(String clientId, String selectedOption) {
        try {
            String message = "answer:" + clientId + ":" + selectedOption;
            outStream.writeUTF(message);
            System.out.println("Answer sent: " + message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String receiveMessageTCP() {
        try {
            return inStream.readUTF();
        } catch (IOException e) {
            System.err.println("Error receiving TCP message: " + e.getMessage());
            return null;
        }
    }

    // Close the connection
    public void closeConnection() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}