package system;
import java.io.IOException;
import java.net.InetAddress;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ServidorMestre {

    // Classe interna simples para guardar dados do jogador
    static class Player {
        InetAddress ip;
        int port;
        String name;

        Player(InetAddress ip, int port, String name) {
            this.ip = ip;
            this.port = port;
            this.name = name;
        }
    }

    private static final List<Player> players = new ArrayList<>();
    private static Connection conn;
    private static String secretWord = "";
    private static String secretWordClean = "";

    public static void main(String[] args) {
        try {
            conn = new Connection(9876);
            Scanner console = new Scanner(System.in);

            System.out.println("### SERVIDOR MESTRE ###");
            System.out.print("Mestre, digite a palavra secreta: ");
            secretWord = console.nextLine();
            secretWordClean = removerAcentos(secretWord);

            System.out.println("Aguardando 2 jogadores...");
            while (players.size() < 2) {
                String[] data = conn.receive().split("\\|"); // msg|ip|port
                Player p = new Player(InetAddress.getByName(data[1]), Integer.parseInt(data[2]), data[0]);
                players.add(p);
                System.out.println(p.name + " entrou.");
            }

            System.out.println("Jogo iniciado!");
            broadcast("MSG:O jogo começou! A palavra tem " + secretWord.length() + " letras.");

            int turnIndex = 0;
            boolean gameRunning = true;

            while (gameRunning) {
                Player atual = players.get(turnIndex);

                // Avisa os outros de quem é a vez
                broadcastExcept("MSG:Vez do jogador " + atual.name, atual);

                // Lógica do turno do jogador atual
                boolean turnEnded = false;
                while (!turnEnded) {
                    // Envia Menu para o jogador e pede INPUT
                    String menu = "\n--- SUA VEZ ---\n1. Regras\n2. Perguntar\n3. Chutar\n4. Passar\nEscolha: ";
                    conn.sendTo("INPUT:" + menu, atual.ip, atual.port);

                    // Recebe resposta (O servidor fica parado esperando o jogador atual)
                    String respData = conn.receive().split("\\|")[0];

                    switch (respData) {
                        case "1": // Regras
                            conn.sendTo("MSG:Regras: Faça perguntas de Sim/Não ou tente chutar.", atual.ip, atual.port);
                            break; // Loop continua, não encerra turno
                        case "2": // Perguntar
                            conn.sendTo("INPUT:Digite sua pergunta: ", atual.ip, atual.port);
                            String pergunta = conn.receive().split("\\|")[0];

                            broadcast("MSG:" + atual.name + " perguntou: " + pergunta);
                            String respostaMestre = perguntarAoMestre(console, pergunta);
                            broadcast("MSG:Mestre respondeu: " + respostaMestre);

                            // Após perguntar, pode chutar ou passar
                            conn.sendTo("INPUT:Deseja chutar agora? (S/N): ", atual.ip, atual.port);
                            if (conn.receive().split("\\|")[0].equalsIgnoreCase("S")) {
                                turnEnded = processarChute(atual, console);
                                if (!turnEnded && gameRunning) turnEnded = true; // Se errou, passa a vez
                            } else {
                                turnEnded = true;
                            }
                            break;
                        case "3": // Chutar
                            turnEnded = processarChute(atual, console);
                            break;
                        case "4": // Passar
                            broadcast("MSG:" + atual.name + " passou a vez.");
                            turnEnded = true;
                            break;
                        default:
                            conn.sendTo("MSG:Opção inválida.", atual.ip, atual.port);
                    }

                    // Verifica se alguém ganhou dentro de processarChute
                    if (secretWordClean.equals("ACERTOU_FIM")) {
                        gameRunning = false;
                        turnEnded = true;
                    }
                }
                // Passa para o próximo jogador
                turnIndex = (turnIndex + 1) % players.size();
            }

            broadcast("FIM");
            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean processarChute(Player player, Scanner console) throws IOException {
        conn.sendTo("INPUT:Digite seu chute: ", player.ip, player.port);
        String chute = conn.receive().split("\\|")[0];

        if (removerAcentos(chute).equalsIgnoreCase(secretWordClean)) {
            broadcast("MSG:O jogador " + player.name + " ACERTOU! A palavra era " + secretWord);
            secretWordClean = "ACERTOU_FIM"; // Flag para sair do loop
            return true;
        } else {
            broadcast("MSG:" + player.name + " chutou '" + chute + "' e ERROU.");
            System.out.println("O jogador errou. Escolha a dica (1-Frio, 2-Morno, 3-Quente): ");
            String dica = console.nextLine();
            String textoDica = dica.equals("1") ? "Frio" : dica.equals("2") ? "Morno" : "Quente";
            broadcast("MSG:Dica do Mestre: " + textoDica);
            return true; // Errou, passa a vez
        }
    }

    private static String perguntarAoMestre(Scanner console, String pergunta) {
        System.out.println("Pergunta feita: " + pergunta);
        System.out.println("Responda: 1.Sim 2.Não 3.Talvez 4.Sem Resposta");
        String op = console.nextLine();
        if (op.equals("1")) return "Sim";
        if (op.equals("2")) return "Não";
        if (op.equals("3")) return "Talvez";
        return "Não sei/Não posso responder";
    }

    private static void broadcast(String msg) throws IOException {
        for (Player p : players) conn.sendTo(msg, p.ip, p.port);
    }

    private static void broadcastExcept(String msg, Player except) throws IOException {
        for (Player p : players) {
            if (!p.name.equals(except.name)) conn.sendTo(msg, p.ip, p.port);
        }
    }

    public static String removerAcentos(String str) {
        return Normalizer.normalize(str, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "").replace(" ", "").toLowerCase();
    }
}