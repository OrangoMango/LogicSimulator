package com.orangomango.logicsim;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.input.MouseButton;
import javafx.scene.canvas.*;
import javafx.scene.paint.Color;
import javafx.animation.*;
import javafx.util.Duration;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Point2D;

import java.util.*;

import com.orangomango.logicsim.ui.*;
import com.orangomango.logicsim.core.*;

public class MainApplication extends Application{
	private static final double WIDTH = 1000;
	private static final double HEIGHT = 800;
	private static final int FPS = 40;

	private SideArea sideArea;
	private int selectedId = -1;
	private Point2D mouseMoved = new Point2D(0, 0);
	private List<Gate> gates = new ArrayList<>();
	private List<Wire> wires = new ArrayList<>();
	private Gate.Pin connG;
	
	@Override
	public void start(Stage stage){
		stage.setTitle("LogicSim");
		StackPane pane = new StackPane();
		Canvas canvas = new Canvas(WIDTH, HEIGHT);
		GraphicsContext gc = canvas.getGraphicsContext2D();
		pane.getChildren().add(canvas);
		
		this.sideArea = new SideArea(gc, new Rectangle2D(950, 250, 50, 75), new Rectangle2D(650, 0, 350, 800));
		this.sideArea.addButton(() -> this.selectedId = 0);
		this.sideArea.addButton(() -> this.selectedId = 1);
		this.sideArea.addButton(() -> this.selectedId = 2);
		this.sideArea.addButton(() -> this.selectedId = 3);
		this.sideArea.addButton(() -> this.selectedId = 4);

		canvas.setOnMousePressed(e -> {
			if (e.getButton() == MouseButton.PRIMARY){
				if (this.selectedId >= 0){
					Gate g = null;
					switch (this.selectedId){
						case 0:
							g = new Switch(gc, new Rectangle2D(e.getX(), e.getY(), 50, 50));
							break;
						case 1:
							Gate.Pin found = null;
							for (Gate gt : this.gates){
								Gate.Pin pin = gt.getPin(e.getX(), e.getY());
								if (pin != null){
									found = pin;
									break;
								}
							}
							if (found != null){
								if (this.connG == null){
									this.connG = found;
								} else {
									this.wires.add(new Wire(gc, this.connG, found));
									this.connG = null;
									this.selectedId = -1;
								}
							} else {
								this.selectedId = -1;
							}
							break;
						case 2:
							g = new Light(gc, new Rectangle2D(e.getX(), e.getY(), 50, 50));
							break;
						case 3:
							g = new NotGate(gc, new Rectangle2D(e.getX(), e.getY(), 50, 50));
							break;
						case 4:
							g = new AndGate(gc, new Rectangle2D(e.getX(), e.getY(), 50, 50));
							break;
					}
					if (g != null){
						this.gates.add(g);
						this.selectedId = -1;
					}
				} else {
					this.sideArea.onClick(e.getX(), e.getY());
					for (Gate g : this.gates){
						Gate.Pin pin = g.getPin(e.getX(), e.getY());
						if (pin == null){
							g.onClick(e.getX(), e.getY());
						} else {
							this.selectedId = 1;
							this.connG = pin;
						}
					}
				}
			}
		});

		canvas.setOnMouseMoved(e -> {
			this.mouseMoved = new Point2D(e.getX(), e.getY());
		});

		Timeline loop = new Timeline(new KeyFrame(Duration.millis(1000.0/FPS), e -> update(gc)));
		loop.setCycleCount(Animation.INDEFINITE);
		loop.play();
		
		stage.setScene(new Scene(pane, WIDTH, HEIGHT));
		stage.show();
	}
	
	private void update(GraphicsContext gc){
		gc.clearRect(0, 0, WIDTH, HEIGHT);
		gc.setFill(Color.web("#EDEDED"));
		gc.fillRect(0, 0, WIDTH, HEIGHT);

		for (Gate g : this.gates){
			g.render();
		}
		for (Wire w : this.wires){
			w.render();
		}

		// UI
		this.sideArea.render();
	}
	
	public static void main(String[] args){
		launch(args);
	}
}
