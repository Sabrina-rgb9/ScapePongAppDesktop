package com.spacepong.desktop;

import java.util.ArrayList;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class UtilsViews {

    public static StackPane parentContainer = new StackPane();
    public static ArrayList<Object> controllers = new ArrayList<>();

    // Add viewStart to the container
    public static void addStartView() {
        try {
            System.out.println("📁 Cargando viewStart.fxml...");
            // ✅ USAR LA CLASE ACTUAL PARA CARGAR RECURSOS
            FXMLLoader loader = new FXMLLoader(UtilsViews.class.getResource("/assets/viewStart.fxml"));
            Pane view = loader.load();
            ObservableList<Node> children = parentContainer.getChildren();

            view.setId("viewStart");
            view.setVisible(true);
            view.setManaged(true);

            children.add(view);
            controllers.add(loader.getController());
            System.out.println("✅ viewStart.fxml cargado correctamente");
        } catch (Exception e) {
            System.err.println("❌ Error cargando viewStart.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Add viewWait to the container
    public static void addWaitView() {
        try {
            System.out.println("📁 Cargando viewWait.fxml...");
            
            // ✅ DIFERENTES FORMAS DE CARGAR EL ARCHIVO - PRUEBA ESTAS OPCIONES:
            
            // Opción 1: Usar getResourceAsStream (más confiable)
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(UtilsViews.class.getResource("/assets/viewWait.fxml"));
            Pane view = loader.load();
            
            // Opción 2: Alternativa si la anterior falla
            // FXMLLoader loader = new FXMLLoader(getClass().getResource("/assets/viewWait.fxml"));
            // Pane view = loader.load();
            
            ObservableList<Node> children = parentContainer.getChildren();

            view.setId("viewWait");
            view.setVisible(false);
            view.setManaged(false);

            children.add(view);
            controllers.add(loader.getController());
            System.out.println("✅ viewWait.fxml cargado correctamente");
            
        } catch (Exception e) {
            System.err.println("❌ Error cargando viewWait.fxml: " + e.getMessage());
            e.printStackTrace();
            
            // ✅ MOSTRAR MÁS INFORMACIÓN DE DEBUG
            System.err.println("🔍 Debug info:");
            System.err.println("  - Ruta intentada: /assets/viewWait.fxml");
            System.err.println("  - ClassLoader: " + UtilsViews.class.getClassLoader());
            
            // Verificar si el recurso existe
            try {
                java.net.URL resourceUrl = UtilsViews.class.getResource("/assets/viewWait.fxml");
                if (resourceUrl == null) {
                    System.err.println("  ❌ El recurso NO existe en el classpath");
                } else {
                    System.err.println("  ✅ El recurso SÍ existe: " + resourceUrl);
                }
            } catch (Exception ex) {
                System.err.println("  ❌ Error verificando recurso: " + ex.getMessage());
            }
        }
    }

    // Get CtrlWait controller
    public static CtrlWait getWaitController() {
        int index = 0;
        for (Node n : parentContainer.getChildren()) {
            if (n.getId() != null && n.getId().equals("viewWait")) {
                return (CtrlWait) controllers.get(index);
            }
            index++;
        }
        return null;
    }

    // Get CtrlStart controller
    public static ctrlStart getStartController() {
        int index = 0;
        for (Node n : parentContainer.getChildren()) {
            if (n.getId() != null && n.getId().equals("viewStart")) {
                return (ctrlStart) controllers.get(index);
            }
            index++;
        }
        return null;
    }

    // Show only ViewWait (hide others)
    public static void showWaitView() {
        ArrayList<Node> list = new ArrayList<>();
        list.addAll(parentContainer.getChildrenUnmodifiable());

        boolean waitViewFound = false;
        
        for (Node n : list) {
            if (n.getId() != null && n.getId().equals("viewWait")) {
                n.setVisible(true);
                n.setManaged(true);
                waitViewFound = true;
            } else {
                n.setVisible(false);
                n.setManaged(false);
            }
        }
        
        if (!waitViewFound) {
            System.err.println("⚠️ viewWait no encontrada al intentar mostrarla");
        }
        
        parentContainer.requestFocus();
    }

    // Animate transition to ViewWait
    public static void showWaitViewWithAnimation() {
        ArrayList<Node> list = new ArrayList<>();
        list.addAll(parentContainer.getChildrenUnmodifiable());

        // Get current view
        Node curView = null;
        for (Node n : list) {
            if (n.isVisible()) {
                curView = n;
            }
        }

        // Get WaitView
        Node waitView = null;
        for (Node n : list) {
            if (n.getId() != null && n.getId().equals("viewWait")) {
                waitView = n;
            }
        }

        if (waitView == null) {
            System.err.println("❌ viewWait no encontrada para animación");
            showWaitView();
            return;
        }

        // Set WaitView visible
        waitView.setVisible(true);
        waitView.setManaged(true);

        // Animation from right to left
        double width = parentContainer.getScene() != null ? parentContainer.getScene().getWidth() : 800;
        
        if (curView != null) {
            curView.translateXProperty().set(0);
            KeyValue kvCurrent = new KeyValue(curView.translateXProperty(), -width, Interpolator.EASE_BOTH);
            KeyFrame kfCurrent = new KeyFrame(Duration.seconds(0.4), kvCurrent);
            Timeline timelineCurrent = new Timeline(kfCurrent);
            timelineCurrent.play();
        }

        waitView.translateXProperty().set(width);
        KeyValue kvWait = new KeyValue(waitView.translateXProperty(), 0, Interpolator.EASE_BOTH);
        KeyFrame kfWait = new KeyFrame(Duration.seconds(0.4), kvWait);
        Timeline timelineWait = new Timeline(kfWait);
        
        timelineWait.setOnFinished(t -> {
            for (Node n : list) {
                if (n.getId() == null || !n.getId().equals("viewWait")) {
                    n.setVisible(false);
                    n.setManaged(false);
                }
                n.translateXProperty().set(0);
            }
        });
        timelineWait.play();

        parentContainer.requestFocus();
    }

    // Show only ViewStart (hide others)
    public static void showStartView() {
        ArrayList<Node> list = new ArrayList<>();
        list.addAll(parentContainer.getChildrenUnmodifiable());

        for (Node n : list) {
            if (n.getId() != null && n.getId().equals("viewStart")) {
                n.setVisible(true);
                n.setManaged(true);
            } else {
                n.setVisible(false);
                n.setManaged(false);
            }
        }
        parentContainer.requestFocus();
    }

    // Animate transition to ViewStart
    public static void showStartViewWithAnimation() {
        ArrayList<Node> list = new ArrayList<>();
        list.addAll(parentContainer.getChildrenUnmodifiable());

        // Get current view
        Node curView = null;
        for (Node n : list) {
            if (n.isVisible()) {
                curView = n;
            }
        }

        // If already on StartView, do nothing
        if (curView != null && curView.getId() != null && curView.getId().equals("viewStart")) {
            return;
        }

        // Get StartView
        Node startView = null;
        for (Node n : list) {
            if (n.getId() != null && n.getId().equals("viewStart")) {
                startView = n;
            }
        }

        if (startView == null) {
            System.err.println("❌ viewStart no encontrada para animación");
            showStartView();
            return;
        }

        // Set StartView visible
        startView.setVisible(true);
        startView.setManaged(true);

        // Animation from right to left
        double width = parentContainer.getScene() != null ? parentContainer.getScene().getWidth() : 800;
        
        if (curView != null) {
            curView.translateXProperty().set(0);
            KeyValue kvCurrent = new KeyValue(curView.translateXProperty(), -width, Interpolator.EASE_BOTH);
            KeyFrame kfCurrent = new KeyFrame(Duration.seconds(0.4), kvCurrent);
            Timeline timelineCurrent = new Timeline(kfCurrent);
            timelineCurrent.play();
        }

        startView.translateXProperty().set(width);
        KeyValue kvStart = new KeyValue(startView.translateXProperty(), 0, Interpolator.EASE_BOTH);
        KeyFrame kfStart = new KeyFrame(Duration.seconds(0.4), kvStart);
        Timeline timelineStart = new Timeline(kfStart);
        
        timelineStart.setOnFinished(t -> {
            for (Node n : list) {
                if (n.getId() == null || !n.getId().equals("viewStart")) {
                    n.setVisible(false);
                    n.setManaged(false);
                }
                n.translateXProperty().set(0);
            }
        });
        timelineStart.play();

        parentContainer.requestFocus();
    }

    // Simple method to initialize the view system
    public static void initialize() {
        try {
            parentContainer.setStyle("-fx-font: 14 arial;");
            addStartView();
            addWaitView(); // ✅ Esto ya no lanza excepción si falla
            
            // Verificar qué vistas se cargaron
            System.out.println("📊 Vistas cargadas:");
            for (Node n : parentContainer.getChildren()) {
                System.out.println("  - " + n.getId() + " (visible: " + n.isVisible() + ")");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error en initialize: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Get the parent container for the scene
    public static StackPane getParentContainer() {
        return parentContainer;
    }
}