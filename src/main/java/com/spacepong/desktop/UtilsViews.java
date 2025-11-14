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
    public static void addStartView() throws Exception {
        FXMLLoader loader = new FXMLLoader(UtilsViews.class.getResource("/assets/viewStart.fxml"));
        Pane view = loader.load();
        ObservableList<Node> children = parentContainer.getChildren();

        view.setId("viewStart");
        view.setVisible(true);
        view.setManaged(true);

        children.add(view);
        controllers.add(loader.getController());
    }

    // Get CtrlStart controller
    public static ctrlStart getStartController() {
        int index = 0;
        for (Node n : parentContainer.getChildren()) {
            if (n.getId().equals("viewStart")) {
                return (ctrlStart) controllers.get(index);
            }
            index++;
        }
        return null;
    }

    // Check if ViewStart is active
    public static boolean isStartViewActive() {
        for (Node n : parentContainer.getChildren()) {
            if (n.isVisible() && n.getId().equals("viewStart")) {
                return true;
            }
        }
        return false;
    }

    // Show only ViewStart (hide others if any)
    public static void showStartView() {
        ArrayList<Node> list = new ArrayList<>();
        list.addAll(parentContainer.getChildrenUnmodifiable());

        for (Node n : list) {
            if (n.getId().equals("viewStart")) {
                n.setVisible(true);
                n.setManaged(true);
            } else {
                n.setVisible(false);
                n.setManaged(false);
            }
        }

        // Remove focus from buttons
        parentContainer.requestFocus();
    }

    // Animate transition to ViewStart (useful if you add more views later)
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
        if (curView != null && curView.getId().equals("viewStart")) {
            return;
        }

        // Get StartView
        Node startView = null;
        for (Node n : list) {
            if (n.getId().equals("viewStart")) {
                startView = n;
            }
        }

        if (startView == null) {
            showStartView();
            return;
        }

        // Set StartView visible
        startView.setVisible(true);
        startView.setManaged(true);

        // Animation from right to left
        double width = parentContainer.getScene().getWidth();
        
        if (curView != null) {
            // Animate current view out to the left
            curView.translateXProperty().set(0);
            KeyValue kvCurrent = new KeyValue(curView.translateXProperty(), -width, Interpolator.EASE_BOTH);
            KeyFrame kfCurrent = new KeyFrame(Duration.seconds(0.4), kvCurrent);
            Timeline timelineCurrent = new Timeline(kfCurrent);
            timelineCurrent.play();
        }

        // Animate StartView in from the right
        startView.translateXProperty().set(width);
        KeyValue kvStart = new KeyValue(startView.translateXProperty(), 0, Interpolator.EASE_BOTH);
        KeyFrame kfStart = new KeyFrame(Duration.seconds(0.4), kvStart);
        Timeline timelineStart = new Timeline(kfStart);
        
        timelineStart.setOnFinished(t -> {
            // Hide other views and reset translations
            for (Node n : list) {
                if (!n.getId().equals("viewStart")) {
                    n.setVisible(false);
                    n.setManaged(false);
                }
                n.translateXProperty().set(0);
            }
        });
        timelineStart.play();

        // Remove focus from buttons
        parentContainer.requestFocus();
    }

    // Simple method to initialize the view system
    public static void initialize() throws Exception {
        parentContainer.setStyle("-fx-font: 14 arial;");
        addStartView();
    }

    // Get the parent container for the scene
    public static StackPane getParentContainer() {
        return parentContainer;
    }
}