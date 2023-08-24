package com.orangomango.logicsim.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

import java.util.*;
import dev.webfx.platform.json.Json;
import dev.webfx.platform.json.JsonObject;
import dev.webfx.platform.file.File;

import com.orangomango.logicsim.MainApplication;
import com.orangomango.logicsim.Util;

public class Chip extends Gate{
	private List<Gate> gates = new ArrayList<>();
	private List<Wire> wires = new ArrayList<>();
	private List<Gate> inputGates = new ArrayList<>();
	private List<Gate> outputGates = new ArrayList<>();
	private List<Pin> inputPins = new ArrayList<>();
	private List<Pin> outputPins = new ArrayList<>();
	private File file;
	private String name;
	private JsonObject json;

	public Chip(GraphicsContext gc, Rectangle2D rect, File file){
		super(gc, "CHIP", rect, Color.BLUE);
		this.file = file;
		this.label = file.getName();

		// Load data
		Pin.UPDATE_PIN_ID = false;
		this.json = MainApplication.load(this.file, this.gc, this.gates, this.wires);
		Pin.UPDATE_PIN_ID = true;
		if (this.json == null) return;

		// Turn on all the subgates
		if (isPowered()){
			Util.schedule(() -> {
				Util.updateGatesPower(this.gates, false);
				Util.updateGatesPower(this.gates, true);
			}, Util.GATE_DELAY);
		}

		this.color = Color.color(this.json.getObject("color").getDouble("red"), this.json.getObject("color").getDouble("green"), this.json.getObject("color").getDouble("blue"));
		this.name = this.json.getString("chipName");

		for (Gate g : this.gates){
			if (g.getName().equals("SWITCH")){
				this.inputGates.add(g);
			} else if (g.getName().equals("LIGHT")){
				this.outputGates.add(g);
			}
		}

		// Sort the gates
		this.inputGates.sort((g1, g2) -> Double.compare(g1.getRect().getMinY()+g1.getRect().getHeight()/2, g2.getRect().getMinY()+g2.getRect().getHeight()/2));
		this.outputGates.sort((g1, g2) -> Double.compare(g1.getRect().getMinY()+g1.getRect().getHeight()/2, g2.getRect().getMinY()+g2.getRect().getHeight()/2));

		this.rect = new Rectangle2D(this.rect.getMinX(), this.rect.getMinY(), this.rect.getWidth(), Math.max(this.inputGates.size(), this.outputGates.size())*20+5);
		double inputOffset = (this.rect.getHeight()-(this.inputGates.size()*15))/(this.inputGates.size()+1);
		for (int i = 0; i < this.inputGates.size(); i++){
			Pin pin = new Pin(this, new Rectangle2D(this.rect.getMinX()-7, this.rect.getMinY()+inputOffset+i*(15+inputOffset), 15, 15), true);
			this.pins.add(pin);
			this.inputPins.add(pin);
		}
		double outputOffset = (this.rect.getHeight()-(this.outputGates.size()*15))/(this.outputGates.size()+1);
		for (int i = 0; i < this.outputGates.size(); i++){
			Pin pin = new Pin(this, new Rectangle2D(this.rect.getMaxX()-7, this.rect.getMinY()+outputOffset+i*(15+outputOffset), 15, 15), false);
			this.pins.add(pin);
			this.outputPins.add(pin);
		}
	}

	public JsonObject getJSONData(){
		return this.json;
	}

	public String getName(){
		return this.name;
	}

	public List<Gate> getGates(){
		return this.gates;
	}

	@Override
	public JsonObject getJSON(){
		JsonObject json = super.getJSON();
		json.set("fileName", this.file.getName());
		return json;
	}

	@Override
	public void setPins(List<Pin> pins){
		super.setPins(pins);
		this.outputPins.clear();
		this.inputPins.clear();
		for (Pin pin : this.pins){
			if (pin.isInput()){
				this.inputPins.add(pin);
			} else {
				this.outputPins.add(pin);
			}
		}
	}

	public void renderInside(GraphicsContext gc){
		for (Gate g : this.gates){
			g.render(gc);
		}
		for (Wire w : this.wires){
			w.render(gc);
		}
	}

	public String getLabel(Pin p){
		if (p.isInput()){
			for (int i = 0; i < this.inputPins.size(); i++){
				Pin pin = this.inputPins.get(i);
				Gate gate = this.inputGates.get(i);
				if (pin == p){
					return gate.getLabel().equals("Switch") ? "Input pin" : gate.getLabel();
				}
			}
			return null;
		} else {
			for (int i = 0; i < this.outputPins.size(); i++){
				Pin pin = this.outputPins.get(i);
				Gate gate = this.outputGates.get(i);
				if (pin == p){
					return gate.getLabel().equals("Light") ? "Output pin" : gate.getLabel();
				}
			}
			return null;
		}
	}

	@Override
	public void update(){
		for (Gate g : this.gates){
			g.update();
		}
		for (int i = 0; i < this.inputPins.size(); i++){
			Pin pin = this.inputPins.get(i);
			((Switch)this.inputGates.get(i)).setOn(pin.isOn());
		}
		for (int i = 0; i < this.outputGates.size(); i++){
			Light l = (Light)this.outputGates.get(i);
			this.outputPins.get(i).setSignal(l.isOn(), isPowered());
			this.outputPins.get(i).setConnected(l.getPins().get(0).isConnected(), isPowered());
		}
		super.update();
	}

	@Override
	protected void renderGate(GraphicsContext gc){
		gc.setFill(this.color);
		gc.fillRoundRect(this.rect.getMinX(), this.rect.getMinY(), this.rect.getWidth(), this.rect.getHeight(), 20, 20);
		gc.save();
		gc.setStroke(Color.BLACK);
		gc.setLineWidth(2);
		gc.strokeRoundRect(this.rect.getMinX(), this.rect.getMinY(), this.rect.getWidth(), this.rect.getHeight(), 20, 20);
		gc.setFill(Util.isDarkColor(this.color) ? Color.WHITE : Color.BLACK);
		gc.setTextAlign(TextAlignment.CENTER);
		gc.fillText(Util.wrapString(this.name, 5), this.rect.getMinX()+this.rect.getWidth()/2, this.rect.getMinY()+this.rect.getHeight()/2+5);
		gc.restore();
	}
}