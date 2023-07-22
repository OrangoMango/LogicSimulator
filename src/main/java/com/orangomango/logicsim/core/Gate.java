package com.orangomango.logicsim.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;

import java.util.*;

public class Gate{
	protected GraphicsContext gc;
	protected Rectangle2D rect;
	protected Color color;
	protected Runnable onClick;
	protected List<Pin> pins = new ArrayList<>();

	public static class Pin{
		private Rectangle2D rect;
		private boolean on;
		private List<Pin> attached = new ArrayList<>();

		public Pin(Rectangle2D r){
			this.rect = r;
		}

		public double getX(){
			return (this.rect.getMinX()+this.rect.getMaxX())/2;
		}

		public double getY(){
			return (this.rect.getMinY()+this.rect.getMaxY())/2;
		}

		public boolean contains(double x, double y){
			return this.rect.contains(x, y);
		}

		public void attach(Pin o){
			this.attached.add(o);
		}

		private boolean hasOnPin(){
			for (Pin p : this.attached){
				if (p.isOn()){
					return true;
				}
			}
			return false;
		}

		public void toggleOn(){
			this.on = !this.on;
			if (this.on){
				for (Pin p : this.attached){
					p.on = true;
				}
			} else {
				for (Pin p : this.attached){
					if (!p.hasOnPin()){
						p.on = false;	
					}
				}
			}
		}

		public boolean isOn(){
			return this.on;
		}

		public void render(GraphicsContext gc){
			gc.setFill(this.on ? Color.GREEN : Color.BLACK);
			gc.fillOval(this.rect.getMinX(), this.rect.getMinY(), this.rect.getWidth(), this.rect.getHeight());
		}
	}

	public Gate(GraphicsContext gc, Rectangle2D rect, Color color){
		this.gc = gc;
		this.rect = rect;
		this.color = color;
	}

	public void onClick(double x, double y){
		if (onClick != null && this.rect.contains(x, y)){
			this.onClick.run();
		}
	}

	public Pin getPin(double x, double y){
		for (Pin pin : this.pins){
			if (pin.contains(x, y)){
				return pin;
			}
		}
		return null;
	}

	protected void renderPins(GraphicsContext gc){
		for (Pin pin : pins){
			pin.render(gc);
		}
	}

	public void render(){
		gc.setFill(this.color);
		gc.fillRect(this.rect.getMinX(), this.rect.getMinY(), this.rect.getWidth(), this.rect.getHeight());
		renderPins(this.gc);
	}
}