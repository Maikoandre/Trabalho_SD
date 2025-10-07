package org.example;

// ClienteJogador.java
import java.io.IOException;
import java.util.Scanner;

public class ClienteJogador {

    private static Connection connection;
    private static boolean myTurn = false;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Uso: java ClienteJogador <IP_do_Servidor>");
            return;
        }

        String serverIp = args[0];
        int serverPort = 9876;

        try {
            connection = new Connection(serverIp, serverPort);
            Scanner scanner = new Scanner(System.in);

            System.out.print("Digite seu nome para entrar no jogo: ");
            String playerName = scanner.nextLine();
            connection.send(playerName);

            System.out.println("Bem-vindo, " + playerName + "! Aguardando o início do jogo...");

            // Thread para ouvir mensagens do servidor
            Thread listenerThread = new Thread(() -> {
                try {
                    while (true) {
                        String serverResponse = connection.receive().split("\\|")[0];
                        handleServerMessage(serverResponse);
                    }
                } catch (IOException e) {
                    System.out.println("\nConexão com o servidor perdida.");
                }
            });
            listenerThread.start();

            // Loop para entrada do jogador
            while (true) {
                if (myTurn) {
                    displayPlayerMenu();
                    String choice = scanner.nextLine();
                    handlePlayerAction(choice, scanner);
                }
                // Pequena pausa para não sobrecarregar a CPU
                Thread.sleep(100);
            }

        } catch (IOException e) {
            System.err.println("Erro de conexão com o servidor: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    private static void handleServerMessage(String message) {
        if (message.startsWith("START_GAME")) {
            System.out.println(message.split("\\|")[0]);
        } else if (message.equals("YOUR_TURN")) {
            System.out.println("\n>>> É A SUA VEZ! <<<");
            myTurn = true;
        } else if (message.startsWith("ANSWER")) {
            System.out.println("Resposta do Mestre: " + message.split("\\|")[0]);
        } else if (message.startsWith("HINT")) {
            System.out.println("Dica do Mestre sobre o palpite: " + message.split("\\|")[0]);
        } else if (message.equals("GUESS_OR_PASS_MENU")) {
            // Este menu aparece depois de uma pergunta
        } else if (message.startsWith("INFO")) {
            System.out.println("Info: " + message.split("\\|")[0]);
        } else if (message.startsWith("END_GAME")) {
            System.out.println("\n### FIM DE JOGO ###");
            System.out.println(message.split("\\|")[0]);
            System.exit(0);
        }
    }

    private static void displayPlayerMenu() {
        System.out.println("\nMenu do jogo:");
        System.out.println("1. Ver as regras do jogo.");
        System.out.println("2. Fazer uma pergunta ao mestre.");
        System.out.println("3. Tentar adivinhar a palavra.");
        System.out.println("4. Passar a vez.");
        System.out.print("Sua escolha: ");
    }

    private static void handlePlayerAction(String choice, Scanner scanner) throws IOException {
        switch (choice) {
            case "1":
                System.out.println("\n--- REGRAS ---");
                System.out.println("O objetivo é adivinhar a palavra secreta do mestre.");
                System.out.println("Você pode fazer perguntas de 'sim' ou 'não' para obter dicas.");
                System.out.println("Quando tiver um palpite, pode tentar adivinhar a palavra.");
                System.out.println("Se errar, o mestre dará uma dica de quão perto você está.");
                // O menu principal será exibido novamente
                break;
            case "2":
                System.out.print("Digite sua pergunta: ");
                String question = scanner.nextLine();
                connection.send("ASK:" + question);
                // Após a pergunta, o jogador deve esperar a resposta e depois decidir se adivinha ou passa.
                waitForGuessOrPass(scanner);
                myTurn = false;
                break;
            case "3":
                System.out.print("Digite seu palpite para a palavra: ");
                String guess = scanner.nextLine();
                connection.send("GUESS:" + guess);
                myTurn = false;
                break;
            case "4":
                connection.send("PASS");
                myTurn = false;
                break;
            default:
                System.out.println("Opção inválida. Tente novamente.");
                break;
        }
    }

    private static void waitForGuessOrPass(Scanner scanner) throws IOException {
        System.out.println("\nApós a resposta do mestre, você pode:");
        System.out.println("1. Tentar adivinhar a palavra.");
        System.out.println("2. Passar a vez.");
        System.out.print("Sua escolha: ");
        String choice = scanner.nextLine();

        if (choice.equals("1")) {
            System.out.print("Digite seu palpite para a palavra: ");
            String guess = scanner.nextLine();
            connection.send("GUESS:" + guess);
        } else {
            connection.send("PASS");
        }
    }
}