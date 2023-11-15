package client;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("Enter command: ");
                String command = scanner.nextLine();

                switch (command) {
                    case "hello":
                        System.out.println("Hello world!");
                        break;
                    case "exit":
                        System.out.println("Exiting...");
                        System.exit(0);
                    default:
                        System.out.println("Unknown command: " + command);
                }
            }
        }
    }
}