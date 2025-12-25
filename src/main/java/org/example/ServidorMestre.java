package org.example;
// ServidorMestre.java
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ServidorMestre {

    private static class PlayerInfo {
        InetAddress address;
        int port;
        String name;

        PlayerInfo(InetAddress address, int port, String name) {
            this.address = address;
            this.port = port;
            this.name = name;
        }
    }

    private static List<PlayerInfo> players = new ArrayList<>();
    private static Connection connection;
    private static final int SERVER_PORT = 9876;
    private static String secretWord = "";
    private static int currentPlayerIndex = 0;

    public static void main(String[] args) {
        try {
            connection = new Connection(SERVER_PORT);
            System.out.println("### JOGO ADIVINHE A PALAVRA ###");
            System.out.println("Servidor Mestre iniciado na porta " + SERVER_PORT);
            System.out.println("Bem-vindo ao Jogo Adivinhe a Palavra.");

            Scanner scanner = new Scanner(System.in);
            System.out.print("Você é o mestre, escolha a palavra a ser adivinhada: ");
            secretWord = scanner.nextLine();

            System.out.println("Aguardando 2 jogadores se conectarem...");

            // Aguarda a conexão de dois jogadores
            while (players.size() < 2) {
                String[] clientData = connection.receive().split("\\|");
                String playerName = clientData[0];
                InetAddress playerAddress = InetAddress.getByName(clientData[1]);
                int playerPort = Integer.parseInt(clientData[2]);

                PlayerInfo newPlayer = new PlayerInfo(playerAddress, playerPort, playerName);
                players.add(newPlayer);
                System.out.println("Jogador '" + playerName + "' conectou-se de " + playerAddress.getHostAddress() + ":" + playerPort);
            }

            System.out.println("Todos os jogadores estão no jogo. A partida vai começar!");
            broadcast("START_GAME|O jogo começou! É a vez de " + players.get(currentPlayerIndex).name);

            // Inicia o turno do primeiro jogador
            sendTurnInfo(players.get(currentPlayerIndex));

            // Loop principal do jogo
            handleGameLoop(scanner);

        } catch (IOException e) {
            System.err.println("Erro de rede no servidor: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    private static void handleGameLoop(Scanner scanner) throws IOException {
        while (true) {
            String[] clientData = connection.receive().split("\\|");
            String message = clientData[0];
            String playerAddress = clientData[1];
            int playerPort = Integer.parseInt(clientData[2]);

            PlayerInfo currentPlayer = getCurrentPlayerByAddress(playerAddress, playerPort);

            // Ignora mensagens de jogadores que não estão na vez
            if (currentPlayer == null || !isCurrentPlayerTurn(currentPlayer)) {
                continue;
            }

            if (message.startsWith("ASK:")) {
                String question = message.substring(4);
                System.out.println("\n" + currentPlayer.name + " fez a seguinte pergunta: " + question);
                broadcast("INFO|" + currentPlayer.name + " perguntou: '" + question + "'");
                handleMasterQuestionMenu(scanner, currentPlayer);
            } else if (message.startsWith("GUESS:")) {
                String guess = message.substring(6);
                handlePlayerGuess(guess, currentPlayer, scanner);
            } else if (message.equals("PASS")) {
                broadcast("INFO|" + currentPlayer.name + " passou a vez.");
                System.out.println(currentPlayer.name + " passou a vez.");
                switchTurn();
            }
        }
    }

    private static void handleMasterQuestionMenu(Scanner scanner, PlayerInfo askingPlayer) throws IOException {
        String choice;
        while (true) {
            System.out.println("\nMenu do jogo - perfil Mestre:");
            System.out.println("1. Sim.");
            System.out.println("2. Não.");
            System.out.println("3. Talvez.");
            System.out.println("4. Não sei.");
            System.out.println("5. Não posso responder.");
            System.out.println("6. Relembrar qual palavra escolhi.");
            System.out.print("Sua escolha: ");
            choice = scanner.nextLine();

            if (choice.equals("6")) {
                System.out.println("A palavra secreta é: " + secretWord);
                continue;
            } else if (Integer.parseInt(choice) >= 1 && Integer.parseInt(choice) <= 5) {
                String[] answers = {"Sim.", "Não.", "Talvez.", "Não sei.", "Não posso responder."};
                String masterAnswer = answers[Integer.parseInt(choice) - 1];
                broadcast("ANSWER|" + masterAnswer);
                System.out.println("Resposta enviada aos jogadores.");

                // Envia menu de adivinhar/passar para o jogador que perguntou
                connection.sendTo("GUESS_OR_PASS_MENU", askingPlayer.address, askingPlayer.port);
                break;
            } else {
                System.out.println("Opção inválida.");
            }
        }
    }

    private static void handlePlayerGuess(String guess, PlayerInfo guessingPlayer, Scanner scanner) throws IOException {
        if (guess.equalsIgnoreCase(secretWord)) {
            broadcast("END_GAME|" + guessingPlayer.name + " acertou! A palavra era '" + secretWord + "'. O jogo terminou.");
            System.out.println("\n" + guessingPlayer.name + " acertou! O jogo terminou.");
            System.exit(0);
        } else {
            System.out.println("\n" + guessingPlayer.name + " errou. A tentativa foi: " + guess);
            broadcast("INFO|" + guessingPlayer.name + " tentou adivinhar, mas errou.");

            String choice;
            while(true) {
                System.out.println("\nMenu do jogo - O jogador está:");
                System.out.println("1. Frio.");
                System.out.println("2. Morno.");
                System.out.println("3. Quente.");
                System.out.println("4. Quentíssimo.");
                System.out.println("5. Relembrar qual palavra escolhi.");
                System.out.print("Sua escolha: ");
                choice = scanner.nextLine();

                if (choice.equals("5")) {
                    System.out.println("A palavra secreta é: " + secretWord);
                    continue;
                } else if (Integer.parseInt(choice) >= 1 && Integer.parseInt(choice) <= 4){
                    String[] hints = {"Frio.", "Morno.", "Quente.", "Quentíssimo."};
                    broadcast("HINT|" + hints[Integer.parseInt(choice) - 1]);
                    break;
                } else {
                    System.out.println("Opção inválida.");
                }
            }
            switchTurn();
        }
    }

    private static void switchTurn() throws IOException {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        PlayerInfo nextPlayer = players.get(currentPlayerIndex);
        broadcast("INFO|\nAgora é a vez de " + nextPlayer.name);
        sendTurnInfo(nextPlayer);
    }

    private static void sendTurnInfo(PlayerInfo player) throws IOException {
        connection.sendTo("YOUR_TURN", player.address, player.port);
    }

    private static void broadcast(String message) throws IOException {
        for (PlayerInfo player : players) {
            connection.sendTo(message, player.address, player.port);
        }
    }

    private static PlayerInfo getCurrentPlayerByAddress(String address, int port) {
        for (PlayerInfo player : players) {
            if (player.address.getHostAddress().equals(address) && player.port == port) {
                return player;
            }
        }
        return null;
    }

    private static boolean isCurrentPlayerTurn(PlayerInfo player) {
        return players.get(currentPlayerIndex).equals(player);
    }
}