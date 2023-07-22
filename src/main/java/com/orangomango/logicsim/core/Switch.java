package com.orangomango.logicsim.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;

public class Switch extends Gate{
	private boolean on;

	public Switch(GraphicsContext gc, Rectangle2D rect){
		super(gc, rect, Color.RED);
		this.onClick = () -> {
			this.on = !this.on;
			this.color = this.on ? Color.GREEN : Color.RED;
			this.pins.get(0).setSignal(!this.pins.get(0).isOn());
		};
		this.pins.add(new Gate.Pin(new Rectangle2D(rect.getMinX(), rect.getMinY(), 15, 15), false));
	}
}