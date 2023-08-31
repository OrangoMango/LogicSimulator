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
	private boolean toggle, on;

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
			this.on = !this.on;
		}
	}

	public void setToggle(boolean v){
		this.toggle = v;
	}

	public boolean isOn(){
		if (!this.toggle){
			throw new IllegalStateException("Toggle is false");
		} else {
			return this.on;
		}
	}

	public void setRect(Rectangle2D rect){
		this.button = rect;
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
			if (this.toggle){
				gc.drawImage(this.image, 1+(this.on ? 77 : 0), 1, 75, 75, this.button.getMinX(), this.button.getMinY(), this.button.getWidth(), this.button.getHeight());
			} else {
				gc.drawImage(this.image, this.button.getMinX(), this.button.getMinY(), this.button.getWidth(), this.button.getHeight());
			}
			gc.setFill(Color.WHITE);
			gc.setTextAlign(TextAlignment.CENTER);
			gc.fillText(this.text, this.button.getMinX()+this.button.getWidth()/2, this.button.getMaxY()+20);
		}
	}
}