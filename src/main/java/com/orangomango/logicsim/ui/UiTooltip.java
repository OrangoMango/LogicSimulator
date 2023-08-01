package com.orangomango.logicsim.ui;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

public class UiTooltip{
	private GraphicsContext gc;
	private String text;
	private double x, y;

	public UiTooltip(GraphicsContext gc, String text, double x, double y){
		this.gc = gc;
		this.text = text;
		this.x = x;
		this.y = y;
	}

	public void render(){
		gc.save();
		gc.setFill(Color.BLACK);
		gc.setGlobalAlpha(0.6);
		gc.fillRoundRect(this.x, this.y, 95, 20*this.text.split("\n").length, 15, 15);
		gc.setFill(Color.WHITE);
		gc.setTextAlign(TextAlignment.CENTER);
		gc.fillText(this.text, this.x+95/2.0, this.y+45/2.0-5);
		gc.restore();
	}
}