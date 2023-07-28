package com.orangomango.logicsim.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;

public class Light extends Gate{
	private boolean lastValue;
	private Image image;

	public Light(GraphicsContext gc, Rectangle2D rect){
		super(gc, "LIGHT", rect, null);
		this.image = new Image(getClass().getResourceAsStream("/light.png"));
		this.pins.add(new Pin(new Rectangle2D(rect.getMinX()-7, rect.getMinY()+7, 15, 15), true));
		this.label = "Light";
	}

	public boolean isOn(){
		return this.pins.get(0).isOn();
	}

	@Override
	public void update(){
		super.update();
		if (this.lastValue != isOn()){
			this.lastValue = isOn();
		}
	}

	@Override
	public void renderGate(GraphicsContext gc){
		gc.drawImage(this.image, 1+(isOn() ? 52 : 0), 1, 50, 50, this.rect.getMinX(), this.rect.getMinY(), this.rect.getWidth(), this.rect.getHeight());
	}
}