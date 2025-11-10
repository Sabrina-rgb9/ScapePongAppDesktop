package main. java.com.broadcast.client;

import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class SimpleClient extends JFrame {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private JTextArea textArea;
    private JTextField messageField;
    
    public SimpleClient() {
        // Verificar si hay soporte gráfico
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("❌ Entorno sin interfaz gráfica detectado");
            System.out.println("💡 Ejecuta: ./scripts/console-client.sh para modo consola");
            System.exit(1);
        }
        
        initializeUI();
        connectToServer();
    }
    
    private void initializeUI() {
        setTitle("Cliente Broadcast Simple");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 400);
        setLocationRelativeTo(null);
        
        // Área de texto para mensajes
        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setBackground(new Color(240, 240, 240));
        add(new JScrollPane(textArea), BorderLayout.CENTER);
        
        // Panel inferior para enviar mensajes
        JPanel bottomPanel = new JPanel(new BorderLayout());
        
        messageField = new JTextField();
        messageField.addActionListener(e -> sendMessage());
        bottomPanel.add(messageField, BorderLayout.CENTER);
        
        JButton sendButton = new JButton("Enviar");
        sendButton.addActionListener(e -> sendMessage());
        bottomPanel.add(sendButton, BorderLayout.EAST);
        
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    private void connectToServer() {
        try {
            socket = new Socket("localhost", 3000);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            appendMessage("✅ Conectado al servidor en localhost:3000");
            appendMessage("⏰ El servidor enviará 'Hola' automático cada 10 segundos");
            appendMessage("----------------------------------------");
            
            // Hilo para recibir mensajes del servidor
            new Thread(this::receiveMessages).start();
            
        } catch (IOException e) {
            appendMessage("❌ Error conectando al servidor: " + e.getMessage());
            appendMessage("💡 Asegúrate de que el servidor esté ejecutándose");
        }
    }
    
    private void receiveMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                String finalMessage = message;
                SwingUtilities.invokeLater(() -> 
                    appendMessage("📨 " + finalMessage));
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> 
                appendMessage("🔌 Desconectado del servidor"));
        }
    }
    
    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty() && out != null) {
            out.println(message);
            appendMessage("📤 Yo: " + message);
            messageField.setText("");
        }
    }
    
    private void appendMessage(String message) {
        textArea.append(message + "\n");
        textArea.setCaretPosition(textArea.getDocument().getLength());
    }
    
    public static void main(String[] args) {
        // Verificar headless antes de crear la GUI
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("🚨 No se puede abrir interfaz gráfica en este entorno");
            System.out.println("🔧 Ejecuta el cliente en modo consola:");
            System.out.println("   mvn exec:java -Dexec.mainClass=\"com.broadcast.client.ConsoleClient\"");
            return;
        }
        
        SwingUtilities.invokeLater(() -> {
            new SimpleClient().setVisible(true);
        });
    }
}