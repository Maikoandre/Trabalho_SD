package system;
import java.util.Scanner;

public class ClienteJogador {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Uso: java ClienteJogador <IP_do_Servidor>");
            return;
        }

        try (Scanner scanner = new Scanner(System.in)) {
            Connection connection = new Connection(args[0], 9876);

            // Protocolo inicial: enviar nome
            System.out.print("Digite seu nome: ");
            connection.send(scanner.nextLine());
            System.out.println("Conectado! Aguardando o jogo iniciar...");

            boolean rodando = true;
            while (rodando) {
                // O cliente fica travado aqui até o servidor mandar algo (Bloqueante)
                String msg = connection.receive();

                if (msg.startsWith("MSG:")) {
                    // Apenas exibe mensagem
                    System.out.println(msg.substring(4));

                } else if (msg.startsWith("INPUT:")) {
                    // Exibe mensagem e libera teclado para resposta
                    System.out.print(msg.substring(6));
                    String input = scanner.nextLine();
                    connection.send(input);

                } else if (msg.equals("FIM")) {
                    rodando = false;
                }
            }
            connection.close();
        } catch (Exception e) {
            System.out.println("Erro: " + e.getMessage());
        }
    }
}