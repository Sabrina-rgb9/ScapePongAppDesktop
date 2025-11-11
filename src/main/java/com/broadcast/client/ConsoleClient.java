package com.broadcast.client;

import java.util.Scanner;

public class ConsoleClient {
    private static UtilsWS wsClient;
    
    public static void main(String[] args) {
        new ConsoleClient().start();
    }
    
    public void start() {
        // Conectar al servidor WebSocket - usa puerto 80 (redirección)
        UtilsWS wsClient = UtilsWS.getSharedInstance("ws://ieticloudpro.ieti.cat:3000");        
        System.out.println("🚀 Cliente Consola SpacePong");
        System.out.println("✅ Conectando al servidor...");
        System.out.println("💡 Escribe mensajes para enviar a todos los clientes");
        System.out.println("❌ Escribe 'quit' para salir");
        System.out.println("----------------------------------------");
        
        // Configurar callbacks
        wsClient.onOpen(message -> {
            System.out.println("✅ " + message);
        });
        
        wsClient.onMessage(message -> {
            System.out.println("📨 " + message);
        });
        
        wsClient.onError(message -> {
            System.out.println("❌ " + message);
        });
        
        wsClient.onClose(message -> {
            System.out.println("🔌 " + message);
        });
        
        // Leer entrada del usuario
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String input = scanner.nextLine();
            if ("quit".equalsIgnoreCase(input)) {
                break;
            }
            if (!input.trim().isEmpty()) {
                wsClient.safeSend(input);
            }
        }
        
        scanner.close();
        disconnect();
    }
    
    private void disconnect() {
        if (wsClient != null) {
            wsClient.forceExit();
        }
        System.out.println("👋 Cliente cerrado");
    }
}