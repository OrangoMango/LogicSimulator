package com.orangomango.logicsim.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;

public class AndGate extends Gate implements DelayedGate{
	private boolean lastValue;

	public AndGate(GraphicsContext gc, Rectangle2D rect){
		super(gc, "AND", rect, Color.BLUE);
		this.pins.add(new Gate.Pin(new Rectangle2D(rect.getMinX()-7, rect.getMinY(), 15, 15), true)); // Input
		this.pins.add(new Gate.Pin(new Rectangle2D(rect.getMinX()-7, rect.getMinY()+20, 15, 15), true)); // Input
		this.pins.add(new Gate.Pin(new Rectangle2D(rect.getMaxX()-7, rect.getMinY(), 15, 15), false)); // Output
	}

	@Override
	public void setLastValue(boolean v){
		this.lastValue = v;
	}

	@Override
	public boolean getLastValue(){
		return this.lastValue;
	}

	@Override
	public void update(){
		super.update();
		applyValue(() -> this.pins.get(0).isOn() && this.pins.get(1).isOn(), v -> this.pins.get(2).setSignal(v, isPowered()));
	}
}