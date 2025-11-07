package main.java.com.broadcast.client;

import java.io.*;
import java.net.*;
import java.util.function.Consumer;

public class ConnectionManager {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Consumer<String> messageConsumer;
    private boolean connected = false;
    private String serverHost;
    private int serverPort;
    
    public boolean connect(String host, int port) {
        this.serverHost = host;
        this.serverPort = port;
        
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            connected = true;
            
            // Iniciar hilo para recibir mensajes
            startMessageReceiver();
            return true;
            
        } catch (IOException e) {
            System.err.println("Error conectando al servidor: " + e.getMessage());
            connected = false;
            return false;
        }
    }
    
    private void startMessageReceiver() {
        Thread receiverThread = new Thread(() -> {
            try {
                String message;
                while (connected && (message = in.readLine()) != null) {
                    if (messageConsumer != null) {
                        messageConsumer.accept(message);
                    }
                }
            } catch (IOException e) {
                if (connected) {
                    System.err.println("Error recibiendo mensajes: " + e.getMessage());
                }
            } finally {
                disconnect();
            }
        });
        receiverThread.setDaemon(true);
        receiverThread.start();
    }
    
    public void sendMessage(String message) {
        if (connected && out != null) {
            out.println(message);
        }
    }
    
    public void disconnect() {
        connected = false;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Error cerrando conexión: " + e.getMessage());
        }
        
        if (messageConsumer != null) {
            messageConsumer.accept("[SISTEMA] Desconectado del servidor");
        }
    }
    
    public void setMessageConsumer(Consumer<String> messageConsumer) {
        this.messageConsumer = messageConsumer;
    }
    
    public boolean isConnected() {
        return connected;
    }
    
    public String getConnectionInfo() {
        return connected ? 
            String.format("Conectado a %s:%d", serverHost, serverPort) : 
            "Desconectado";
    }
}