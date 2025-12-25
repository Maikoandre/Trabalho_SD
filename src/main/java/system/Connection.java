package system;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Connection {
    private DatagramSocket socket;
    private InetAddress address;
    private int port;
    private boolean isServer;

    // Construtor Cliente
    public Connection(String serverIp, int serverPort) throws IOException {
        this.socket = new DatagramSocket();
        this.address = InetAddress.getByName(serverIp);
        this.port = serverPort;
        this.isServer = false;
    }

    // Construtor Servidor
    public Connection(int port) throws IOException {
        this.socket = new DatagramSocket(port);
        this.isServer = true;
    }

    public void send(String text) throws IOException {
        sendTo(text, this.address, this.port);
    }

    public void sendTo(String text, InetAddress ip, int port) throws IOException {
        byte[] buffer = text.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, ip, port);
        socket.send(packet);
    }

    public String receive() throws IOException {
        byte[] buffer = new byte[2048];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        String msg = new String(packet.getData(), 0, packet.getLength());

        // Se for servidor, anexa IP e Porta para saber quem mandou
        if (isServer) {
            return msg + "|" + packet.getAddress().getHostAddress() + "|" + packet.getPort();
        }
        return msg;
    }

    public void close() {
        if (socket != null && !socket.isClosed()) socket.close();
    }
}