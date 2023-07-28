package com.orangomango.logicsim.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;

public class Switch extends Gate{
	private boolean on;
	private Image image;

	public Switch(GraphicsContext gc, Rectangle2D rect){
		super(gc, "SWITCH", rect, null);
		this.image = new Image(getClass().getResourceAsStream("/switch.png"));
		this.onClick = e -> setOn(!this.on);
		this.pins.add(new Pin(new Rectangle2D(rect.getMaxX()-7, rect.getMinY()+7, 15, 15), false));
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
	public void renderGate(GraphicsContext gc){
		gc.drawImage(this.image, 1+(this.on ? 52 : 0), 1, 50, 50, this.rect.getMinX(), this.rect.getMinY(), this.rect.getWidth(), this.rect.getHeight());
	}
}