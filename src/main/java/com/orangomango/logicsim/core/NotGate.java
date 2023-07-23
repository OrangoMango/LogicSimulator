package com.orangomango.logicsim.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;

public class NotGate extends Gate{
	public NotGate(GraphicsContext gc, Rectangle2D rect){
		super(gc, "NOT", rect, Color.ORANGE);
		this.pins.add(new Gate.Pin(new Rectangle2D(rect.getMinX()-15, rect.getMinY(), 15, 15), true)); // Input
		this.pins.add(new Gate.Pin(new Rectangle2D(rect.getMaxX(), rect.getMinY(), 15, 15), false)); // Output
	}

	@Override
	public void update(){
		this.pins.get(1).setSignal(!this.pins.get(0).isOn());
	}
}