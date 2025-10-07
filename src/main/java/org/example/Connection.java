package org.example;

// Connection.java
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class Connection {
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private int serverPort;

    // Construtor para o Cliente
    public Connection(String serverIp, int serverPort) throws IOException {
        this.socket = new DatagramSocket();
        this.serverAddress = InetAddress.getByName(serverIp);
        this.serverPort = serverPort;
    }

    // Construtor para o Servidor
    public Connection(int port) throws SocketException {
        this.socket = new DatagramSocket(port);
    }

    public void send(String message) throws IOException {
        byte[] buffer = message.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, serverPort);
        socket.send(packet);
    }

    public void sendTo(String message, InetAddress address, int port) throws IOException {
        byte[] buffer = message.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
        socket.send(packet);
    }

    public String receive() throws IOException {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        // Retorna os dados junto com o endereço e a porta do remetente
        return new String(packet.getData(), 0, packet.getLength()) + "|" + packet.getAddress().getHostAddress() + "|" + packet.getPort();
    }

    public void close() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}