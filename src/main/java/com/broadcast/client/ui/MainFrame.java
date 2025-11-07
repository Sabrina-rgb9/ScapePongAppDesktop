package main.java.com.broadcast.client.ui


import main.java.com.broadcast.client.ConnectionManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class MainFrame extends JFrame {
    private ConnectionDialog connectionManager;
    private JTextArea textArea;
    private JTextField messageField;
    private JButton sendButton;
    private JLabel statusLabel;
    
    public MainFrame() {
        initializeUI();
        connectionManager = new ConnectionDialog();
        connectionManager.setMessageConsumer(this::appendMessage);
    }
    
    private void initializeUI() {
        setTitle("Cliente Broadcast Desktop");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);
        
        // Panel principal
        setLayout(new BorderLayout());
        
        // Área de texto para mensajes
        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setBackground(new Color(240, 240, 240));
        JScrollPane scrollPane = new JScrollPane(textArea);
        add(scrollPane, BorderLayout.CENTER);
        
        // Panel inferior
        JPanel bottomPanel = new JPanel(new BorderLayout());
        
        // Campo de mensaje
        messageField = new JTextField();
        messageField.addActionListener(e -> sendMessage());
        bottomPanel.add(messageField, BorderLayout.CENTER);
        
        // Botón enviar
        sendButton = new JButton("Enviar");
        sendButton.addActionListener(e -> sendMessage());
        bottomPanel.add(sendButton, BorderLayout.EAST);
        
        // Barra de estado
        statusLabel = new JLabel("Desconectado");
        statusLabel.setBorder(BorderFactory.createLoweredBevelBorder());
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);
        
        add(bottomPanel, BorderLayout.SOUTH);
        
        // Menú
        createMenuBar();
        
        updateUIState();
    }
    
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        JMenu connectionMenu = new JMenu("Conexión");
        
        JMenuItem connectItem = new JMenuItem("Conectar");
        connectItem.addActionListener(e -> showConnectionDialog());
        
        JMenuItem disconnectItem = new JMenuItem("Desconectar");
        disconnectItem.addActionListener(e -> disconnect());
        
        JMenuItem exitItem = new JMenuItem("Salir");
        exitItem.addActionListener(e -> System.exit(0));
        
        connectionMenu.add(connectItem);
        connectionMenu.add(disconnectItem);
        connectionMenu.addSeparator();
        connectionMenu.add(exitItem);
        
        menuBar.add(connectionMenu);
        setJMenuBar(menuBar);
    }
    
    private void showConnectionDialog() {
        ConnectionDialog dialog = new ConnectionDialog(this);
        dialog.setVisible(true);
        
        if (dialog.isConfirmed()) {
            String host = dialog.getHost();
            int port = dialog.getPort();
            
            appendMessage("[SISTEMA] Conectando a " + host + ":" + port + "...");
            
            boolean success = connectionManager.connect(host, port);
            if (success) {
                appendMessage("[SISTEMA] ✅ Conectado al servidor");
                updateUIState();
            } else {
                appendMessage("[SISTEMA] ❌ Error al conectar");
                JOptionPane.showMessageDialog(this, 
                    "Error al conectar con el servidor", 
                    "Error de conexión", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty() && connectionManager.isConnected()) {
            connectionManager.sendMessage(message);
            appendMessage("Yo: " + message);
            messageField.setText("");
        }
    }
    
    private void disconnect() {
        connectionManager.disconnect();
        appendMessage("[SISTEMA] Desconectado del servidor");
        updateUIState();
    }
    
    private void appendMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            textArea.append(message + "\n");
            textArea.setCaretPosition(textArea.getDocument().getLength());
            statusLabel.setText(connectionManager.getConnectionInfo());
        });
    }
    
    private void updateUIState() {
        boolean connected = connectionManager.isConnected();
        sendButton.setEnabled(connected);
        messageField.setEnabled(connected);
        statusLabel.setText(connectionManager.getConnectionInfo());
    }
}