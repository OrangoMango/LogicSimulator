package com.orangomango.logicsim.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

import com.orangomango.logicsim.Util;

public class TriStateBuffer extends Gate{
	public TriStateBuffer(GraphicsContext gc, Rectangle2D rect){
		super(gc, "3SBUFFER", rect, Color.web("#A28585"));
		this.label = "Tri-state buffer";
		this.pins.add(new Pin(new Rectangle2D(rect.getMinX()-7, rect.getMinY()+7, 15, 15), true)); // Input
		this.pins.add(new Pin(new Rectangle2D(rect.getMinX()-7, rect.getMinY()+28, 15, 15), true)); // Input
		this.pins.add(new Pin(new Rectangle2D(rect.getMaxX()-7, rect.getMinY()+15, 15, 15), false)); // Output
	}

	@Override
	public void update(){
		super.update();
		this.pins.get(2).setSignal(this.pins.get(1).isOn(), isPowered());
		this.pins.get(2).setConnected(this.pins.get(0).isOn(), isPowered());
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
		gc.fillText("TRI-STATE\nBUFFER", this.rect.getMinX()+this.rect.getWidth()/2, this.rect.getMinY()+this.rect.getHeight()/2+5);
		gc.restore();
	}
}