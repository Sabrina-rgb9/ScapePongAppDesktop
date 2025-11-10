package com.broadcast.client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ConsoleClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    
    public static void main(String[] args) {
        new ConsoleClient().start();
    }
    
    public void start() {
        try {
            // Conectar al servidor
            socket = new Socket("localhost", 3000);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            System.out.println("✅ Conectado al servidor en localhost:3000");
            System.out.println("⏰ El servidor enviará 'Hola' automático cada 10 segundos");
            System.out.println("💡 Escribe mensajes para enviar al servidor");
            System.out.println("❌ Escribe 'quit' para salir");
            System.out.println("----------------------------------------");
            
            // Hilo para recibir mensajes
            Thread receiverThread = new Thread(this::receiveMessages);
            receiverThread.setDaemon(true);
            receiverThread.start();
            
            // Leer entrada del usuario
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String input = scanner.nextLine();
                if ("quit".equalsIgnoreCase(input)) {
                    break;
                }
                if (!input.trim().isEmpty()) {
                    out.println(input);
                    System.out.println("📤 Yo: " + input);
                }
            }
            
            scanner.close();
            disconnect();
            
        } catch (IOException e) {
            System.out.println("❌ Error conectando al servidor: " + e.getMessage());
            System.out.println("💡 Asegúrate de que el servidor esté ejecutándose");
        }
    }
    
    private void receiveMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("📨 " + message);
            }
        } catch (IOException e) {
            System.out.println("🔌 Desconectado del servidor");
        }
    }
    
    private void disconnect() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}