package com.orangomango.logicsim.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;

public class Light extends Gate{
	public Light(GraphicsContext gc, Rectangle2D rect){
		super(gc, "LIGHT", rect, Color.GRAY);
		this.pins.add(new Gate.Pin(new Rectangle2D(rect.getMaxX(), rect.getMinY(), 15, 15), true));
	}

	public boolean isOn(){
		return this.pins.get(0).isOn();
	}

	@Override
	public void update(){
		this.color = isOn() ? Color.YELLOW : Color.GRAY;
	}
}