package com.orangomango.logicsim.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;

public class Switch extends Gate{
	private boolean on;

	public Switch(GraphicsContext gc, Rectangle2D rect){
		super(gc, "SWITCH", rect, Color.RED);
		this.onClick = () -> setOn(!this.on);
		this.pins.add(new Gate.Pin(new Rectangle2D(rect.getMinX()-15, rect.getMinY(), 15, 15), false));
	}

	@Override
	public void update(){
		// Does nothing
	}

	public void setOn(boolean v){
		this.on = v;
		this.color = this.on ? Color.GREEN : Color.RED;
		this.pins.get(0).setSignal(this.on);
	}
}