package com.orangomango.logicsim.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

import com.orangomango.logicsim.Util;

public class NotGate extends Gate implements DelayedGate{
	private boolean lastValue;

	public NotGate(GraphicsContext gc, Rectangle2D rect){
		super(gc, "NOT", rect, Color.ORANGE);
		this.label = "Not gate";
		this.pins.add(new Pin(new Rectangle2D(rect.getMinX()-7, rect.getMinY()+15, 15, 15), true)); // Input
		this.pins.add(new Pin(new Rectangle2D(rect.getMaxX()-7, rect.getMinY()+15, 15, 15), false)); // Output
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
		applyValue(() -> !this.pins.get(0).isOn(), v -> this.pins.get(1).setSignal(v, isPowered()));
	}

	@Override
	public void renderGate(GraphicsContext gc){
		gc.setFill(this.color);
		gc.fillRoundRect(this.rect.getMinX(), this.rect.getMinY(), this.rect.getWidth(), this.rect.getHeight(), 20, 20);
		gc.save();
		gc.setStroke(Color.BLACK);
		gc.setLineWidth(2);
		gc.strokeRoundRect(this.rect.getMinX(), this.rect.getMinY(), this.rect.getWidth(), this.rect.getHeight(), 20, 20);
		gc.setFill(Util.isDarkColor(this.color) ? Color.WHITE : Color.BLACK);
		gc.setTextAlign(TextAlignment.CENTER);
		gc.fillText("NOT", this.rect.getMinX()+this.rect.getWidth()/2, this.rect.getMinY()+this.rect.getHeight()/2);
		gc.restore();
	}
}