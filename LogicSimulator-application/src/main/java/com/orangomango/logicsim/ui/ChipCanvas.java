package com.orangomango.logicsim.ui;

import javafx.scene.layout.StackPane;
import javafx.scene.canvas.*;
import javafx.scene.paint.Color;
import javafx.animation.*;
import javafx.util.Duration;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseButton;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.Cursor;

import com.orangomango.logicsim.core.Chip;
import com.orangomango.logicsim.core.Gate;
import com.orangomango.logicsim.MainApplication;

public class ChipCanvas{
	private static final int WIDTH = 500;
	private static final int HEIGHT = 400;

	private Chip chip;
	private double scale = 0.5;
	private Timeline loop;
	private double cameraX, cameraY;
	private Point2D movePoint, deltaMove = new Point2D(0, 0);

	public ChipCanvas(Chip c){
		this.chip = c;
	}

	public StackPane getPane(){
		StackPane cpane = new StackPane();
		Canvas canvas = new Canvas(500, 400);
		GraphicsContext gc = canvas.getGraphicsContext2D();
		
		canvas.setOnMousePressed(e -> {
			if (e.getButton() == MouseButton.PRIMARY){
				this.movePoint = new Point2D(e.getX(), e.getY());
				this.deltaMove = new Point2D(0, 0);
			} else if (e.getButton() == MouseButton.SECONDARY){
				Point2D clickPoint = new Point2D(e.getX(), e.getY());
				clickPoint = clickPoint.subtract(this.cameraX, this.cameraY);
				if (this.movePoint != null){
					clickPoint = clickPoint.subtract(this.deltaMove);
				}
				clickPoint = clickPoint.multiply(1/this.scale);
				Gate found = null;
				for (Gate g : this.chip.getGates()){
					if (g.getRect().contains(clickPoint.getX(), clickPoint.getY())){
						found = g;
						break;
					}
				}
				if (found != null && found instanceof Chip){
					ContextMenu cm = new ContextMenu();
					MenuItem showChip = new MenuItem("Look inside");
					final Chip chip = (Chip)found;
					showChip.setOnAction(ev -> {
						ChipCanvas cc = new ChipCanvas(chip);
						MainApplication.createCustomAlert(cc.getPane(), chip.getName(), cc::destroy);
					});
					cm.getItems().add(showChip);
					cm.show(canvas, e.getScreenX(), e.getScreenY());
				}
			}
		});

		canvas.setOnMouseDragged(e -> {
			if (e.getButton() == MouseButton.PRIMARY){
				this.deltaMove = new Point2D(e.getX()-this.movePoint.getX(), e.getY()-this.movePoint.getY());
			}
		});

		canvas.setOnMouseReleased(e -> {
			if (e.getButton() == MouseButton.PRIMARY){
				if (this.movePoint != null){
					this.movePoint = null;
					this.cameraX += this.deltaMove.getX();
					this.cameraY += this.deltaMove.getY();
				}
			}
		});

		canvas.setOnScroll(e -> {
			if (e.getDeltaY() != 0){
				this.scale += (e.getDeltaY() > 0 ? -0.05 : 0.05);
			}
		});

		this.loop = new Timeline(new KeyFrame(Duration.millis(1000.0/MainApplication.FPS), e -> {
			update(gc);
			if (this.movePoint != null){
				canvas.setCursor(Cursor.MOVE);
			} else {
				canvas.setCursor(Cursor.DEFAULT);
			}
		}));
		this.loop.setCycleCount(Animation.INDEFINITE);
		this.loop.play();

		cpane.getChildren().add(canvas);
		return cpane;
	}

	public void destroy(){
		this.loop.stop();
	}

	private void update(GraphicsContext gc){
		gc.clearRect(0, 0, WIDTH, HEIGHT);
		gc.setFill(Color.web("#9595D3"));
		gc.fillRect(0, 0, WIDTH, HEIGHT);
		gc.save();
		gc.translate(this.cameraX, this.cameraY);
		if (this.movePoint != null){
			gc.translate(this.deltaMove.getX(), this.deltaMove.getY());
		}
		gc.scale(this.scale, this.scale);
		this.chip.renderInside(gc);
		gc.restore();
	}
}