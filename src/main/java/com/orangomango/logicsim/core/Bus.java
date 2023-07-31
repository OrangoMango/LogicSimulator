package com.orangomango.logicsim.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.json.JSONObject;
import org.json.JSONArray;

public class Bus extends Gate{
	private boolean on = false;
	private List<Bus> connections = new ArrayList<>();
	private int id;

	private static int BUS_ID = 0;

	public Bus(GraphicsContext gc, Rectangle2D rect, int id){
		super(gc, "BUS", rect, Color.GRAY);
		this.id = id;
		this.label = "Bus #"+this.id;
		this.labelDown = this.rect.getWidth() <= this.rect.getHeight();
	}

	public Bus(GraphicsContext gc, Rectangle2D rect){
		this(gc, rect, BUS_ID++);
	}

	public int getId(){
		return this.id;
	}

	public void connectBus(Bus bus){
		if (!this.connections.contains(bus)){
			this.connections.add(bus);
			bus.connections.add(this);
		}
	}

	public void clearConnections(){
		for (Bus bus : this.connections){
			bus.connections.remove(this);
		}
		this.connections.clear();
	}

	@Override
	public JSONObject getJSON(){
		JSONObject json = super.getJSON();
		json.put("id", this.id);
		JSONArray ids = new JSONArray();
		for (Bus bus : this.connections){
			ids.put(bus.getId());
		}
		json.put("connections", ids);
		return json;
	}


	@Override
	public void destroy(List<Wire> wires, List<Wire> wiresToRemove){
		super.destroy(wires, wiresToRemove);
		for (Bus bus : this.connections){
			bus.connections.remove(this);
		}
	}

	public boolean isOn(){
		return this.on;
	}

	public boolean isOnBorder(double x, double y){
		if (this.rect.contains(x, y)){
			if (this.rect.getWidth() > this.rect.getHeight()){
				return this.rect.getMaxX()-x <= 15;
			} else {
				return this.rect.getMaxY()-y <= 15;
			}
		}
		return false;
	}

	public void resize(double x, double y){
		if (this.rect.getWidth() > this.rect.getHeight()){
			double diff = x-this.rect.getMinX();
			if (diff < 100){
				return;
			}
			double maxPinWidth = this.pins.stream().mapToDouble(p -> p.getX()).max().orElse(0);
			if (diff < maxPinWidth+15-this.rect.getMinX()){
				return;
			}
			this.rect = new Rectangle2D(this.rect.getMinX(), this.rect.getMinY(), diff, this.rect.getHeight());
		} else {
			double diff = y-this.rect.getMinY();
			if (diff < 100){
				return;
			}
			double maxPinHeight = this.pins.stream().mapToDouble(p -> p.getY()).max().orElse(0);
			if (diff < maxPinHeight+15-this.rect.getMinY()){
				return;
			}
			this.rect = new Rectangle2D(this.rect.getMinX(), this.rect.getMinY(), this.rect.getWidth(), diff);
		}
	}

	public void setRect(Rectangle2D rect){
		if (this.rect.getWidth() > this.rect.getHeight()){
			for (Pin p : this.pins){
				p.setRect(new Rectangle2D((p.getRect().getMinX()-this.rect.getMinX())/this.rect.getWidth()*rect.getWidth()+rect.getMinX(), rect.getMinY()+rect.getHeight()/2-7.5, p.getRect().getWidth(), p.getRect().getHeight()));
			}
		} else {
			for (Pin p : this.pins){
				p.setRect(new Rectangle2D(rect.getMinX()+rect.getWidth()/2-7.5, (p.getRect().getMinY()-this.rect.getMinY())/this.rect.getHeight()*rect.getHeight()+rect.getMinY(), p.getRect().getWidth(), p.getRect().getHeight()));
			}
		}
		this.rect = rect;
	}

	private Stream<Pin> buildStream(){
		Stream<Pin> stream = this.pins.stream();
		for (Bus bus : this.connections){
			stream = Stream.concat(stream, bus.pins.stream());
		}
		return stream;
	}

	@Override
	public void update(){
		super.update();
		Predicate<Pin> acceptablePins = p -> p.isInput() && p.getAttachedPins().size() > 0 && p.isConnected();
		buildStream().filter(acceptablePins).forEach(p -> this.on = p.isOn());
		long puttingOn = buildStream().filter(acceptablePins.and(p -> p.isOn())).count();
		long puttingOff = buildStream().filter(acceptablePins.and(p -> !p.isOn())).count();
		if (puttingOn > 0 && puttingOff > 0){
			// Unstable state where multiple pins are trying to put different data onto the bus
			this.color = Color.ORANGE;
			this.on = false;
		} else {
			if (puttingOn == 0) this.on = false;
			this.color = this.on ? Color.web("#B2FE73") : Color.GRAY;
		}
		buildStream().filter(p -> !p.isInput()).forEach(p -> p.setSignal(isOn(), isPowered()));
	}
}