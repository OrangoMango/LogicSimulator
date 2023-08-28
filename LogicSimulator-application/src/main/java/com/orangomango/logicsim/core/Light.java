package com.orangomango.logicsim.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;

import dev.webfx.platform.resource.Resource;

public class Light extends Gate{
	private Image image;

	public Light(GraphicsContext gc, Rectangle2D rect, boolean hasPins){
		super(gc, "LIGHT", rect, null);
		this.image = new Image(Resource.toUrl("/images/light.png", Light.class));
		if (hasPins) this.pins.add(new Pin(this, new Rectangle2D(rect.getMinX()-7, rect.getMinY()+7, 15, 15), true));
		this.label = "Light";
	}

	public boolean isOn(){
		return this.pins.get(0).isOn();
	}

	@Override
	protected void renderGate(GraphicsContext gc){
		gc.drawImage(this.image, 1+(isOn() ? 52 : 0), 1, 50, 50, this.rect.getMinX(), this.rect.getMinY(), this.rect.getWidth(), this.rect.getHeight());
	}
}