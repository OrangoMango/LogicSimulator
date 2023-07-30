package com.orangomango.logicsim.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;

import java.util.function.Predicate;

public class Bus extends Gate{
	private boolean on = false;

	public Bus(GraphicsContext gc, Rectangle2D rect){
		super(gc, "BUS", rect, Color.GRAY);
		this.label = "Bus";
		this.labelDown = this.rect.getWidth() <= this.rect.getHeight();
	}

	public boolean isOn(){
		return this.on;
	}

	public void setOn(boolean v){
		this.on = v;
	}

	public boolean isOnBorder(double x, double y){
		if (this.rect.contains(x, y)){
			if (this.rect.getWidth() > this.rect.getHeight()){
				return this.rect.getMaxX()-x <= 15;
			} else {
				return this.rect.getMaxY()-y <= 15;
			}
		}
		return false;
	}

	public void resize(double x, double y){
		if (this.rect.getWidth() > this.rect.getHeight()){
			double diff = x-this.rect.getMinX();
			if (diff < 100){
				return;
			}
			double maxPinWidth = this.pins.stream().mapToDouble(p -> p.getX()).max().orElse(0);
			if (diff < maxPinWidth+15-this.rect.getMinX()){
				return;
			}
			this.rect = new Rectangle2D(this.rect.getMinX(), this.rect.getMinY(), diff, this.rect.getHeight());
		} else {
			double diff = y-this.rect.getMinY();
			if (diff < 100){
				return;
			}
			double maxPinHeight = this.pins.stream().mapToDouble(p -> p.getY()).max().orElse(0);
			if (diff < maxPinHeight+15-this.rect.getMinY()){
				return;
			}
			this.rect = new Rectangle2D(this.rect.getMinX(), this.rect.getMinY(), this.rect.getWidth(), diff);
		}
	}

	public void setRect(Rectangle2D rect){
		if (this.rect.getWidth() > this.rect.getHeight()){
			for (Pin p : this.pins){
				p.setRect(new Rectangle2D((p.getRect().getMinX()-this.rect.getMinX())/this.rect.getWidth()*rect.getWidth()+rect.getMinX(), rect.getMinY()+rect.getHeight()/2-7.5, p.getRect().getWidth(), p.getRect().getHeight()));
			}
		} else {
			for (Pin p : this.pins){
				p.setRect(new Rectangle2D(rect.getMinX()+rect.getWidth()/2-7.5, (p.getRect().getMinY()-this.rect.getMinY())/this.rect.getHeight()*rect.getHeight()+rect.getMinY(), p.getRect().getWidth(), p.getRect().getHeight()));
			}
		}
		this.rect = rect;
	}

	@Override
	public void update(){
		super.update();
		Predicate<Pin> acceptablePins = p -> p.isInput() && p.getAttachedPins().size() > 0 && p.isConnected();
		this.pins.stream().filter(acceptablePins).forEach(p -> setOn(p.isOn()));
		long puttingOn = this.pins.stream().filter(acceptablePins.and(p -> p.isOn())).count();
		long puttingOff = this.pins.stream().filter(acceptablePins.and(p -> !p.isOn())).count();
		if (puttingOn > 0 && puttingOff > 0){
			// Unstable state where multiple pins are trying to put different data on the bus
			this.color = Color.ORANGE;
			setOn(false);
		} else {
			if (puttingOn == 0) setOn(false);
			this.color = this.on ? Color.web("#B2FE73") : Color.GRAY;
		}
		this.pins.stream().filter(p -> !p.isInput()).forEach(p -> p.setSignal(isOn(), isPowered()));
	}
}