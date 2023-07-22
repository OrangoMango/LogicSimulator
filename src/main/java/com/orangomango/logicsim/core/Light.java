package com.orangomango.logicsim.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;

public class Light extends Gate{
	public Light(GraphicsContext gc, Rectangle2D rect){
		super(gc, rect, Color.YELLOW);
		this.pins.add(new Gate.Pin(new Rectangle2D(rect.getMaxX()-15, rect.getMinY(), 15, 15)));
	}

	@Override
	public void render(){
		gc.setFill(this.pins.get(0).isOn() ? this.color : Color.GRAY);
		gc.fillRect(this.rect.getMinX(), this.rect.getMinY(), this.rect.getWidth(), this.rect.getHeight());
		renderPins(this.gc);
	}
}