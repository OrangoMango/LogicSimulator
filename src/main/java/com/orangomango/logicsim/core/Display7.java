package com.orangomango.logicsim.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

public class Display7 extends Gate{
	private Image image;
	private DisplayPiece[] pieces = new DisplayPiece[7];

	private static class DisplayPiece{
		private GraphicsContext gc;
		private Image image;
		private double x, y;

		public DisplayPiece(GraphicsContext gc, Image image, double x, double y){
			this.gc = gc;
			this.image = image;
			this.x = x;
			this.y = y;
		}

		public void render(){
			gc.drawImage(this.image, this.x, this.y, this.image.getWidth(), this.image.getHeight());
		}

		public void move(double x, double y){
			this.x += x;
			this.y += y;
		}
	}

	public Display7(GraphicsContext gc, Rectangle2D rect){
		super(gc, "DISPLAY7", rect, Color.GRAY);
		this.rect = new Rectangle2D(this.rect.getMinX(), this.rect.getMinY(), 70, 150);
		this.image = new Image(getClass().getResourceAsStream("/display.png"));
		for (int i = 0; i < 7; i++){
			this.pins.add(new Gate.Pin(new Rectangle2D(rect.getMinX()-15, rect.getMinY()+15*i, 15, 15), false));
		}

		this.pieces[0] = new DisplayPiece(gc, new Image(getClass().getResourceAsStream("/display-h.png")), this.rect.getMinX()+6*2, this.rect.getMinY()+5*2);
		this.pieces[1] = new DisplayPiece(gc, new Image(getClass().getResourceAsStream("/display-v.png")), this.rect.getMinX()+26*2, this.rect.getMinY()+11*2);
		this.pieces[2] = new DisplayPiece(gc, new Image(getClass().getResourceAsStream("/display-v.png")), this.rect.getMinX()+26*2, this.rect.getMinY()+39*2);
		this.pieces[3] = new DisplayPiece(gc, new Image(getClass().getResourceAsStream("/display-h.png")), this.rect.getMinX()+6*2, this.rect.getMinY()+63*2);
		this.pieces[4] = new DisplayPiece(gc, new Image(getClass().getResourceAsStream("/display-v.png")), this.rect.getMinX()+3*2, this.rect.getMinY()+39*2);
		this.pieces[5] = new DisplayPiece(gc, new Image(getClass().getResourceAsStream("/display-v.png")), this.rect.getMinX()+3*2, this.rect.getMinY()+11*2);
		this.pieces[6] = new DisplayPiece(gc, new Image(getClass().getResourceAsStream("/display-h.png")), this.rect.getMinX()+6*2, this.rect.getMinY()+34*2);
	}

	@Override
	public void setPos(double x, double y){
		double deltaX = x-this.rect.getMinX();
		double deltaY = y-this.rect.getMinY();
		super.setPos(x, y);
		for (int i = 0; i < 7; i++){
			this.pieces[i].move(deltaX, deltaY);
		}
	}

	@Override
	public void update(){
		// Does nothing
	}

	@Override
	public void render(GraphicsContext gc){
		super.render(gc);
		gc.drawImage(this.image, this.rect.getMinX(), this.rect.getMinY(), this.rect.getWidth(), this.rect.getHeight());
		for (int i = 0; i < 7; i++){
			if (this.pins.get(i).isOn()) this.pieces[i].render();
		}
	}
}