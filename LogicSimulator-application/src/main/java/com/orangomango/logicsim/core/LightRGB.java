package com.orangomango.logicsim.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Rectangle2D;

public class LightRGB extends Light{
	public LightRGB(GraphicsContext gc, Rectangle2D rect, boolean hasPins){
		super(gc, rect, hasPins);
		this.label = "RGB Light";
		if (hasPins){
			this.pins.clear(); // Reset pins
			this.pins.add(new Pin(this, new Rectangle2D(rect.getMinX()-7, rect.getMinY()+3, 10, 10), true));
			this.pins.add(new Pin(this, new Rectangle2D(rect.getMinX()-7, rect.getMinY()+21, 10, 10), true));
			this.pins.add(new Pin(this, new Rectangle2D(rect.getMinX()-7, rect.getMinY()+39, 10, 10), true));
		}
	}

	private int getRGB(){
		int rgb = 0;
		if (this.pins.get(0).isOn()) rgb |= 4;
		if (this.pins.get(1).isOn()) rgb |= 2;
		if (this.pins.get(2).isOn()) rgb |= 1;
		return rgb;
	}

	@Override
	public boolean isOn(){
		return getRGB() != 0;
	}

	@Override
	protected void renderGate(GraphicsContext gc){
		int index = getRGB()+1;
		gc.drawImage(this.image, 1+(52*index), 1, 50, 50, this.rect.getMinX(), this.rect.getMinY(), this.rect.getWidth(), this.rect.getHeight());
	}
}