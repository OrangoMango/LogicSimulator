package com.orangomango.logicsim.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;

public class Wire{
	private GraphicsContext gc;
	private Gate.Pin pin1, pin2;

	public Wire(GraphicsContext gc, Gate.Pin p1, Gate.Pin p2){
		this.gc = gc;
		this.pin1 = p1;
		this.pin2 = p2;
		this.pin1.attach(this.pin2);
		this.pin2.attach(this.pin1);
	}

	public void render(){
		gc.setStroke((this.pin1.isOn() && !this.pin1.isInput()) || (this.pin2.isOn() && !this.pin2.isInput()) ? Color.GREEN : Color.BLACK);
		gc.setLineWidth(3);
		gc.strokeLine(this.pin1.getX(), this.pin1.getY(), this.pin2.getX(), this.pin2.getY());
	}
}