package com.orangomango.logicsim.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;

import com.orangomango.logicsim.MainApplication;

public class Bus extends Gate{
	private boolean on = false;

	public Bus(GraphicsContext gc, Rectangle2D rect, MainApplication app){
		super(gc, "BUS", rect, Color.GRAY);
		this.label = "Bus";
		this.labelDown = this.rect.getWidth() <= this.rect.getHeight();
		this.onClick = e -> {
			Point2D clickPoint = app.getClickPoint(e.getX(), e.getY());
			if (this.rect.getWidth() > this.rect.getHeight()){
				this.pins.add(new Pin(new Rectangle2D(clickPoint.getX()-7.5, this.rect.getMinY()+this.rect.getHeight()/2-7.5, 15, 15), e.isShiftDown()));
			} else {
				this.pins.add(new Pin(new Rectangle2D(this.rect.getMinX()+this.rect.getWidth()/2-7.5, clickPoint.getY()-7.5, 15, 15), e.isShiftDown()));
			}
		};
	}

	@Override
	public void update(){
		super.update();
		this.pins.stream().filter(p -> p.isInput() && p.getAttachedPins().size() > 0 && p.isConnected()).forEach(p -> this.on = p.isOn());
		long puttingOn = this.pins.stream().filter(p -> p.isInput() && p.getAttachedPins().size() > 0 && p.isConnected() && p.isOn()).count();
		long puttingOff = this.pins.stream().filter(p -> p.isInput() && p.getAttachedPins().size() > 0 && p.isConnected() && !p.isOn()).count();
		if (puttingOn > 0 && puttingOff > 0){
			// Unstable state where multiple pins are trying to put data on the bus
			this.color = Color.ORANGE;
			this.on = false;
		} else {
			this.color = this.on ? Color.web("#B2FE73") : Color.GRAY;
		}
		this.pins.stream().filter(p -> !p.isInput()).forEach(p -> p.setSignal(this.on, isPowered()));
	}
}