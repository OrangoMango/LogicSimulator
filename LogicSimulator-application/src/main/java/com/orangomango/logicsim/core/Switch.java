package com.orangomango.logicsim.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;

import dev.webfx.platform.resource.Resource;

public class Switch extends Gate{
	private boolean on;
	private Image image;

	public Switch(GraphicsContext gc, Rectangle2D rect, boolean hasPins){
		super(gc, "SWITCH", rect, null);
		this.image = new Image(Resource.toUrl("/images/switch.png", Switch.class));
		this.onClick = () -> setOn(!this.on);
		if (hasPins) this.pins.add(new Pin(this, new Rectangle2D(rect.getMaxX()-7, rect.getMinY()+7, 15, 15), false));
		this.label = "Switch";
	}

	public void setOn(boolean v){
		this.on = v;
	}

	@Override
	public void update(){
		super.update();
		this.pins.get(0).setSignal(this.on, isPowered());
	}

	@Override
	protected void renderGate(GraphicsContext gc){
		gc.drawImage(this.image, 1+(this.on ? 52 : 0), 1, 50, 50, this.rect.getMinX(), this.rect.getMinY(), this.rect.getWidth(), this.rect.getHeight());
	}
}