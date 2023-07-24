package com.orangomango.logicsim.ui;

import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;

import java.util.*;

public class SideArea{
	private GraphicsContext gc;
	private Rectangle2D closedButton, area;
	private boolean opened = false;
	private List<UiButton> buttons = new ArrayList<>();
	private int amount = 0;
	private double extraY;

	public SideArea(GraphicsContext gc, Rectangle2D closedBtn, Rectangle2D area){
		this.gc = gc;
		this.closedButton = closedBtn;
		this.area = area;
	}

	public void addButton(String text, Runnable r){
		final double distance = 10;
		final int maxRow = (int)Math.floor(area.getWidth()/(75+distance));
		Point2D pos = new Point2D(distance+(this.amount%maxRow)*(75+distance), distance+(this.amount/maxRow)*(75+distance)+this.extraY);
		pos = pos.add(area.getMinX(), area.getMinY());
		UiButton ub = new UiButton(this.gc, text, new Rectangle2D(pos.getX(), pos.getY(), 75, 75), r);
		this.buttons.add(ub);
		this.amount++;
	}

	public void startSection(){
		this.extraY = this.buttons.get(this.buttons.size()-1).getRect().getMaxY()+35;
		this.amount = 0;
	}

	public boolean isOpen(){
		return this.opened;
	}

	public void onClick(double x, double y){
		if (this.opened){
			if (this.closedButton.contains(x+this.area.getWidth(), y)){
				this.opened = false;
			} else {
				for (UiButton ub : this.buttons){
					ub.onClick(x, y);
				}
			}
		} else {
			if (this.closedButton.contains(x, y)){
				this.opened = true;
			}
		}
	}

	public void render(){
		gc.save();
		if (this.opened){
			gc.translate(-this.area.getWidth(), 0);
		}
		gc.setFill(Color.GRAY);
		gc.fillRect(this.closedButton.getMinX(), this.closedButton.getMinY(), this.closedButton.getWidth(), this.closedButton.getHeight());
		gc.restore();
		if (this.opened){
			gc.save();
			gc.setGlobalAlpha(0.6);
			gc.setFill(Color.web("#C9B9B9"));
			gc.fillRect(this.area.getMinX(), this.area.getMinY(), this.area.getWidth(), this.area.getHeight());
			gc.restore();
			for (UiButton ub : this.buttons){
				ub.render();
			}
		}
	}
}