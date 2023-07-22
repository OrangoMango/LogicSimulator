package com.orangomango.logicsim.ui;

import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;

public class UiButton{
	private GraphicsContext gc;
	private Rectangle2D button;
	private Runnable onClick;

	public UiButton(GraphicsContext gc, Rectangle2D btn, Runnable r){
		this.gc = gc;
		this.button = btn;
		this.onClick = r;
	}

	public void onClick(double x, double y){
		if (this.button.contains(x, y)){
			this.onClick.run();
		}
	}

	public void render(){
		gc.setFill(Color.web("#FFD949"));
		gc.fillRect(this.button.getMinX(), this.button.getMinY(), this.button.getWidth(), this.button.getHeight());
	}
}