package com.orangomango.logicsim.ui;

import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import javafx.scene.text.TextAlignment;

import com.orangomango.logicsim.Util;

public class UiButton{
	private GraphicsContext gc;
	private Rectangle2D button;
	private Runnable onClick;
	private String text;
	private Image image;

	public UiButton(GraphicsContext gc, Image image, String text, Rectangle2D btn, Runnable r){
		this.gc = gc;
		this.button = btn;
		this.onClick = r;
		this.text = text;
		this.image = image;
	}

	public void onClick(double x, double y){
		if (this.button.contains(x, y)){
			this.onClick.run();
		}
	}

	public Rectangle2D getRect(){
		return this.button;
	}

	public void render(){
		if (this.image == null){
			gc.setFill(Color.web("#FFD949"));
			gc.fillRect(this.button.getMinX(), this.button.getMinY(), this.button.getWidth(), this.button.getHeight());
			gc.setFill(Color.BLACK);
			gc.setTextAlign(TextAlignment.CENTER);
			gc.fillText(Util.wrapString(this.text, 5), this.button.getMinX()+this.button.getWidth()/2, this.button.getMinY()+this.button.getHeight()/2);
		} else {
			gc.drawImage(this.image, this.button.getMinX(), this.button.getMinY(), this.button.getWidth(), this.button.getHeight());	
			gc.setFill(Color.WHITE);
			gc.setTextAlign(TextAlignment.CENTER);
			gc.fillText(this.text, this.button.getMinX()+this.button.getWidth()/2, this.button.getMaxY()+20);
		}
	}
}