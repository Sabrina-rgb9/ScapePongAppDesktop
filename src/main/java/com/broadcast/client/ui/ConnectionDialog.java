package main.java.com.broadcast.client.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class ConnectionDialog extends JDialog {
    private JTextField hostField;
    private JTextField portField;
    private boolean confirmed = false;
    
    public ConnectionDialog(Frame parent) {
        super(parent, "Conectar al Servidor", true);
        initializeUI();
    }
    
    private void initializeUI() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Host
        gbc.gridx = 0; gbc.gridy = 0;
        add(new JLabel("Host:"), gbc);
        
        gbc.gridx = 1; gbc.gridy = 0;
        gbc.weightx = 1.0;
        hostField = new JTextField("192.168.1.100", 15); // Cambiar por IP de Proxmox
        add(hostField, gbc);
        
        // Puerto
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.weightx = 0.0;
        add(new JLabel("Puerto:"), gbc);
        
        gbc.gridx = 1; gbc.gridy = 1;
        gbc.weightx = 1.0;
        portField = new JTextField("8080", 15);
        add(portField, gbc);
        
        // Botones
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        
        JPanel buttonPanel = new JPanel();
        JButton connectButton = new JButton("Conectar");
        connectButton.addActionListener(e -> confirm());
        
        JButton cancelButton = new JButton("Cancelar");
        cancelButton.addActionListener(e -> cancel());
        
        buttonPanel.add(connectButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, gbc);
        
        pack();
        setLocationRelativeTo(getParent());
        getRootPane().setDefaultButton(connectButton);
    }
    
    private void confirm() {
        confirmed = true;
        setVisible(false);
    }
    
    private void cancel() {
        confirmed = false;
        setVisible(false);
    }
    
    public boolean isConfirmed() {
        return confirmed;
    }
    
    public String getHost() {
        return hostField.getText().trim();
    }
    
    public int getPort() {
        try {
            return Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            return 8080;
        }
    }
}