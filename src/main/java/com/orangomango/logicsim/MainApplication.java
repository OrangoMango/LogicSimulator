package com.orangomango.logicsim;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.scene.Scene;
import javafx.scene.Cursor;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.GridPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.KeyCode;
import javafx.scene.canvas.*;
import javafx.scene.paint.Color;
import javafx.scene.image.WritableImage;
import javafx.animation.*;
import javafx.util.Duration;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Point2D;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.embed.swing.SwingFXUtils;

import java.util.*;
import java.io.*;
import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;

import org.json.JSONObject;
import org.json.JSONArray;

import com.orangomango.logicsim.ui.*;
import com.orangomango.logicsim.core.*;

public class MainApplication extends Application{
	private static final double WIDTH = 1000;
	private static final double HEIGHT = 800;
	public static final int FPS = 60;
	private static final double TOOLBAR_X = 650;
	private static final double TOOLBAR_Y = 75;

	private SideArea sideArea;
	private File currentFile = null;
	private int selectedId = -1;
	private Point2D mouseMoved = new Point2D(0, 0);
	private List<Gate> gates = new ArrayList<>();
	private List<Wire> wires = new ArrayList<>();
	private Gate.Pin connG;
	private List<Point2D> pinPoints = new ArrayList<>();
	private Gate selectedGate;
	private List<UiButton> buttons = new ArrayList<>();
	private File selectedChipFile;
	private Point2D movePoint, deltaMove = new Point2D(0, 0); // For camera movement
	private double cameraX, cameraY;
	private double cameraScale = 1;
	private boolean rmWire = false, rmGate = false;
	private Gate.Pin rmW;
	private List<Wire> wiresToRemove = new ArrayList<>();
	private List<Gate> gatesToRemove = new ArrayList<>();
	
	@Override
	public void start(Stage stage){
		StackPane pane = new StackPane();
		Canvas canvas = new Canvas(WIDTH, HEIGHT);
		GraphicsContext gc = canvas.getGraphicsContext2D();
		pane.getChildren().add(canvas);

		UiButton saveButton = new UiButton(gc, "SAVE", new Rectangle2D(50, 20, 100, 35), () -> {
			FileChooser fc = new FileChooser();
			fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("LogicSim files", "*.lsim"));
			File file = fc.showSaveDialog(stage);
			if (file != null){
				this.currentFile = file;
				save(file);
			}
		});
		UiButton loadButton = new UiButton(gc, "LOAD", new Rectangle2D(175, 20, 100, 35), () -> {
			FileChooser fc = new FileChooser();
			fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("LogicSim files", "*.lsim"));
			fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("LogicSim chips", "*.lsimc"));
			File file = fc.showOpenDialog(stage);
			List<Gate> gates = new ArrayList<>();
			List<Wire> wires = new ArrayList<>();
			if (file != null){
				int backup = Gate.Pin.PIN_ID;
				Gate.Pin.PIN_ID = 0;
				JSONObject json = load(file, gc, gates, wires);
				if (json == null){
					Gate.Pin.PIN_ID = backup;
					return;
				}
				this.currentFile = file;
				this.gates = gates;
				this.wires = wires;
			}
		});
		UiButton saveChipButton = new UiButton(gc, "SAVE CHIP", new Rectangle2D(300, 20, 150, 35), () -> {
			Alert alert = new Alert(Alert.AlertType.INFORMATION);
			alert.setHeaderText("Create chip");
			alert.setTitle("Create chip");
			GridPane gpane = new GridPane();
			gpane.setPadding(new Insets(5, 5, 5, 5));
			gpane.setHgap(5);
			gpane.setVgap(5);
			Label nameL = new Label("Name: ");
			TextField name = new TextField();
			ColorPicker colorPicker = new ColorPicker(Color.BLUE);
			gpane.add(nameL, 0, 0);
			gpane.add(name, 1, 0);
			gpane.add(colorPicker, 0, 1, 2, 1);
			alert.getDialogPane().setContent(gpane);
			alert.showAndWait();
			FileChooser fc = new FileChooser();
			fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("LogicSim chips", "*.lsimc"));
			File file = fc.showSaveDialog(stage);
			if (file != null){
				this.currentFile = file;
				save(file, name.getText(), colorPicker.getValue());
				Alert info = new Alert(Alert.AlertType.INFORMATION);
				info.setTitle("Saved");
				info.setHeaderText("File saved");
				info.setContentText("File saved successfully");
				info.showAndWait();
			}
		});
		UiButton clearButton = new UiButton(gc, "CLEAR", new Rectangle2D(475, 20, 100, 35), () -> {
			this.wires = new ArrayList<Wire>();
			this.gates = new ArrayList<Gate>();
			Gate.Pin.PIN_ID = 0;
		});
		UiButton rmWireButton = new UiButton(gc, "RM WIRE", new Rectangle2D(600, 20, 75, 35), () -> this.rmWire = true);
		UiButton rmGateButton = new UiButton(gc, "RM GATE", new Rectangle2D(700, 20, 75, 35), () -> this.rmGate = true);
		UiButton exportButton = new UiButton(gc, "EXPORT", new Rectangle2D(800, 20, 75, 35), () ->{
			double minPosX = Double.POSITIVE_INFINITY;
			double maxPosX = Double.NEGATIVE_INFINITY;
			double minPosY = Double.POSITIVE_INFINITY;
			double maxPosY = Double.NEGATIVE_INFINITY;
			for (Gate g : this.gates){
				if (g.getRect().getMinX() < minPosX){
					minPosX = g.getRect().getMinX();
				}
				if (g.getRect().getMaxX() > maxPosX){
					maxPosX = g.getRect().getMaxX();
				}
				if (g.getRect().getMinY() < minPosY){
					minPosY = g.getRect().getMinY();
				}
				if (g.getRect().getMaxY() > maxPosY){
					maxPosY = g.getRect().getMaxY();
				}
			}
			boolean changeX = false;
			boolean changeY = false;
			if (minPosX < 0){
				changeX = true;
				minPosX = -minPosX;
			}
			if (minPosY < 0){
				changeY  = true;
				minPosY = -minPosY;
			}
			int w = (int)(maxPosX+minPosX)+100;
			int h = (int)(maxPosY+minPosY)+100;
			Canvas tempCanvas = new Canvas(w, h);
			if (changeX) tempCanvas.getGraphicsContext2D().translate(minPosX, 0);
			if (changeY) tempCanvas.getGraphicsContext2D().translate(0, minPosY);
			for (Gate g : this.gates){
				g.render(tempCanvas.getGraphicsContext2D());
			}
			for (Wire wire : this.wires){
				wire.render(tempCanvas.getGraphicsContext2D());
			}
			WritableImage image = tempCanvas.snapshot(null, new WritableImage(w, h));
			FileChooser fc = new FileChooser();
			fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));
			File file = fc.showSaveDialog(stage);
			if (file != null){
				RenderedImage ri = SwingFXUtils.fromFXImage(image, null);
				try {
					ImageIO.write(ri, "png", file);
				} catch (IOException ex){
					ex.printStackTrace();
				}
				Alert info = new Alert(Alert.AlertType.INFORMATION);
				info.setTitle("Saved");
				info.setHeaderText("File saved");
				info.setContentText("File saved successfully");
				info.showAndWait();
			}
		});
		this.buttons.add(saveButton);
		this.buttons.add(loadButton);
		this.buttons.add(saveChipButton);
		this.buttons.add(clearButton);
		this.buttons.add(rmWireButton);
		this.buttons.add(rmGateButton);
		this.buttons.add(exportButton);
		
		this.sideArea = new SideArea(gc, new Rectangle2D(950, 250, 50, 75), new Rectangle2D(TOOLBAR_X, 0, 350, 800));
		this.sideArea.addButton("Switch", () -> this.selectedId = 0);
		this.sideArea.addButton("Wire", () -> this.selectedId = 1);
		this.sideArea.addButton("Light", () -> this.selectedId = 2);
		this.sideArea.addButton("NOT", () -> this.selectedId = 3);
		this.sideArea.addButton("AND", () -> this.selectedId = 4);
		this.sideArea.addButton("CHIP", () -> this.selectedId = 5);
		this.sideArea.addButton("DISPLAY7", () -> this.selectedId = 7);

		this.sideArea.startSection();
		for (File file : (new File(System.getProperty("user.dir"), "projects")).listFiles()){
			String nm = file.getName();
			if (nm.endsWith(".lsimc")){
				this.sideArea.addButton(nm.substring(0, nm.lastIndexOf(".")), () -> {
					this.selectedId = 6;
					this.selectedChipFile = file;
				});
			}
		}

		canvas.setFocusTraversable(true);
		canvas.setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.P){
				Util.toggleCircuitPower(this.gates);
			}
		});

		canvas.setOnMousePressed(e -> {
			Point2D clickPoint = getClickPoint(e.getX(), e.getY());
			if (e.getButton() == MouseButton.PRIMARY){
				if (e.getY() < TOOLBAR_Y && (e.getX() < TOOLBAR_X || !this.sideArea.isOpen())){
					for (UiButton ub : this.buttons){
						ub.onClick(e.getX(), e.getY());
					}
				} else {
					if (this.selectedId >= 0){
						Gate g = null;
						boolean loaded = true;
						switch (this.selectedId){
							case 0:
								g = new Switch(gc, new Rectangle2D(clickPoint.getX(), clickPoint.getY(), 50, 50));
								break;
							case 1:
								Gate.Pin found = null;
								for (Gate gt : this.gates){
									Gate.Pin pin = gt.getPin(clickPoint.getX(), clickPoint.getY());
									if (pin != null){
										found = pin;
										break;
									}
								}
								if (found != null && found != this.connG){
									if (this.connG == null){
										this.connG = found;
									} else {
										if (e.isShiftDown() && this.pinPoints.size() > 0){
											Point2D thisPoint = new Point2D(found.getX(), found.getY());
											Point2D ref = this.pinPoints.get(this.pinPoints.size()-1);
											if (Math.abs(thisPoint.getX()-ref.getX()) > Math.abs(thisPoint.getY()-ref.getY())){
												ref = new Point2D(ref.getX(), thisPoint.getY());
											} else {
												ref = new Point2D(thisPoint.getX(), ref.getY());
											}
											this.pinPoints.set(this.pinPoints.size()-1, ref);
										}
										this.wires.add(new Wire(gc, this.connG, found, new ArrayList<Point2D>(this.pinPoints)));
										this.connG = null;
										this.selectedId = -1;
										this.pinPoints.clear();
									}
								} else if (this.connG != null){
									Point2D finalPoint = clickPoint;
									if (e.isControlDown()){
										if (this.pinPoints.size() >= 1){
											this.pinPoints.remove(this.pinPoints.size()-1);
										}
									} else {
										if (e.isShiftDown()){
											Point2D ref = null;
											if (this.pinPoints.size() == 0){
												ref = new Point2D(this.connG.getX(), this.connG.getY());
											} else {
												ref = this.pinPoints.get(this.pinPoints.size()-1);
											}
											if (Math.abs(finalPoint.getX()-ref.getX()) > Math.abs(finalPoint.getY()-ref.getY())){
												finalPoint = new Point2D(finalPoint.getX(), ref.getY());
											} else {
												finalPoint = new Point2D(ref.getX(), finalPoint.getY());
											}
										}
										this.pinPoints.add(finalPoint);
									}
								}
								break;
							case 2:
								g = new Light(gc, new Rectangle2D(clickPoint.getX(), clickPoint.getY(), 50, 50));
								break;
							case 3:
								g = new NotGate(gc, new Rectangle2D(clickPoint.getX(), clickPoint.getY(), 50, 50));
								break;
							case 4:
								g = new AndGate(gc, new Rectangle2D(clickPoint.getX(), clickPoint.getY(), 50, 50));
								break;
							case 5:
								FileChooser fc = new FileChooser();
								fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("LogicSim chips", "*.lsimc"));
								File file = fc.showOpenDialog(stage);
								if (file == null) return;
								g = new Chip(gc, new Rectangle2D(clickPoint.getX(), clickPoint.getY(), 125, 0), file);
								loaded = ((Chip)g).getJSONData() != null;
								break;
							case 6:
								g = new Chip(gc, new Rectangle2D(clickPoint.getX(), clickPoint.getY(), 125, 0), this.selectedChipFile);
								loaded = ((Chip)g).getJSONData() != null;
								break;
							case 7:
								g = new Display7(gc, new Rectangle2D(clickPoint.getX(), clickPoint.getY(), 0, 0));
								break;
						}
						if (g != null){
							if (loaded) this.gates.add(g);
							this.selectedId = -1;
						}
					} else {
						this.sideArea.onClick(e.getX(), e.getY());
						for (Gate g : this.gates){
							Gate.Pin pin = g.getPin(clickPoint.getX(), clickPoint.getY());
							if (pin == null){
								if (this.rmGate && g.getRect().contains(clickPoint.getX(), clickPoint.getY())){
									this.gatesToRemove.add(g);
									this.rmGate = false;
								} else {
									g.onClick(clickPoint.getX(), clickPoint.getY());
								}
							} else {
								if (this.rmWire){
									if (this.rmW == null){
										this.rmW = pin;
									} else {
										Wire foundWire = getWire(this.wires, this.rmW, pin);
										if (foundWire != null){
											this.wiresToRemove.add(foundWire);
										} else {
											System.out.println("No wire found");
										}
										this.rmW = null;
										this.rmWire = false;
									}
								} else {
									this.selectedId = 1;
									this.connG = pin;	
								}
							}
						}
					}
				}
			} else if (e.getButton() == MouseButton.SECONDARY){
				Gate found = null;
				for (Gate g : this.gates){
					if (g.getRect().contains(clickPoint.getX(), clickPoint.getY())){
						found = g;
						break;
					}
				}
				this.selectedGate = found;
				this.rmGate = false;
				this.rmWire = false;
				this.rmW = null;
				if (found == null){
					this.selectedId = -1;
					this.pinPoints.clear();
					this.connG = null;
					this.movePoint = new Point2D(e.getX(), e.getY());
					this.deltaMove = new Point2D(0, 0);
				} else if (found instanceof Chip && e.getClickCount() == 2){
					ContextMenu cm = new ContextMenu();
					MenuItem showChip = new MenuItem("Look inside");
					final Chip chip = (Chip)found;
					showChip.setOnAction(ev -> {
						Alert alert = new Alert(Alert.AlertType.INFORMATION);
						alert.setTitle(chip.getName());
						alert.setHeaderText(chip.getName());
						ChipCanvas cc = new ChipCanvas(chip);
						alert.getDialogPane().setContent(cc.getPane());
						alert.showAndWait();
						cc.destroy();
					});
					cm.getItems().add(showChip);
					cm.show(canvas, e.getScreenX(), e.getScreenY());
				}
			}
		});

		canvas.setOnMouseMoved(e -> {
			this.mouseMoved = new Point2D(e.getX(), e.getY());
		});

		canvas.setOnMouseDragged(e -> {
			if (e.getButton() == MouseButton.SECONDARY){
				if (this.selectedGate != null){
					Point2D clickPoint = getClickPoint(e.getX(), e.getY());
					this.selectedGate.setPos(clickPoint.getX(), clickPoint.getY());
				} else {
					this.deltaMove = new Point2D(e.getX()-this.movePoint.getX(), e.getY()-this.movePoint.getY());
				}
			}
		});

		canvas.setOnMouseReleased(e -> {
			if (e.getButton() == MouseButton.SECONDARY){
				this.selectedGate = null;
				if (this.movePoint != null){
					this.movePoint = null;
					this.cameraX += this.deltaMove.getX();
					this.cameraY += this.deltaMove.getY();
				}
			}
		});

		canvas.setOnScroll(e -> {
			if (e.getDeltaY() != 0){
				this.cameraScale += (e.getDeltaY() > 0 ? 0.05 : -0.05);
			}
		});

		Scene scene = new Scene(pane, WIDTH, HEIGHT);

		Timeline loop = new Timeline(new KeyFrame(Duration.millis(1000.0/FPS), e -> {
			update(gc);
			if (this.rmWire || this.rmGate){
				scene.setCursor(Cursor.CROSSHAIR);
			} else if (this.movePoint != null){
				scene.setCursor(Cursor.MOVE);
			} else if (this.selectedGate != null){
				scene.setCursor(Cursor.HAND);
			} else {
				scene.setCursor(Cursor.DEFAULT);
			}
			stage.setTitle("LogicSim"+(this.currentFile == null ? "" : " - "+this.currentFile.getName()));
		}));
		loop.setCycleCount(Animation.INDEFINITE);
		loop.play();
		
		stage.setResizable(false);
		stage.setScene(scene);
		stage.show();
	}

	private Point2D getClickPoint(double x, double y){
		Point2D clickPoint = new Point2D(x, y);
		clickPoint = clickPoint.subtract(this.cameraX, this.cameraY);
		if (this.movePoint != null){
			clickPoint = clickPoint.subtract(this.deltaMove);
		}
		clickPoint = clickPoint.multiply(1/this.cameraScale);
		return clickPoint;
	}

	private void save(File file){
		save(file, null, null);
	}

	private void save(File file, String chipName, Color color){
		if (!file.getName().endsWith(".lsim") && !file.getName().endsWith(".lsimc")){
			file = new File(file.getName()+".lsim"+(chipName == null ? "" : "c"));
		}
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			JSONObject json = new JSONObject();
			JSONArray data = new JSONArray();
			for (Gate gate : this.gates){
				data.put(gate.getJSON());
			}
			json.put("gates", data);
			data = new JSONArray();
			for (Wire wire : this.wires){
				data.put(wire.getJSON());
			}
			json.put("wires", data);
			if (chipName != null && color != null){
				json.put("chipName", chipName);
				JSONObject cl = new JSONObject();
				cl.put("red", color.getRed());
				cl.put("green", color.getGreen());
				cl.put("blue", color.getBlue());
				json.put("color", cl);
			}
			writer.write(json.toString(4));
			writer.close();
		} catch (IOException ex){
			ex.printStackTrace();
		}
	}

	public static JSONObject load(File file, GraphicsContext gc, List<Gate> tempGates, List<Wire> tempWires){
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			StringBuilder builder = new StringBuilder();
			reader.lines().forEach(builder::append);
			reader.close();
			JSONObject json = new JSONObject(builder.toString());

			int backupId = Gate.Pin.PIN_ID;

			// Load gates
			for (Object o : json.getJSONArray("gates")){
				JSONObject gate = (JSONObject)o;
				String name = gate.getString("name");
				Color color = Color.color(gate.getJSONObject("color").getDouble("red"), gate.getJSONObject("color").getDouble("green"), gate.getJSONObject("color").getDouble("blue"));
				Rectangle2D rect = new Rectangle2D(gate.getJSONObject("rect").getDouble("x"), gate.getJSONObject("rect").getDouble("y"), gate.getJSONObject("rect").getDouble("w"), gate.getJSONObject("rect").getDouble("h"));
				List<Gate.Pin> pins = new ArrayList<>();
				for (Object o2 : gate.getJSONArray("pins")){
					JSONObject pin = (JSONObject)o2;
					Gate.Pin p = new Gate.Pin(pin);
					pins.add(p);
				}
				Gate gt = null;
				int lastPinId = Gate.Pin.PIN_ID; // Save pin id
				boolean lastPinFlag = Gate.Pin.UPDATE_PIN_ID;
				if (name.equals("AND")){
					gt = new AndGate(gc, rect);
				} else if (name.equals("LIGHT")){
					gt = new Light(gc, rect);
				} else if (name.equals("NOT")){
					gt = new NotGate(gc, rect);
				} else if (name.equals("SWITCH")){
					gt = new Switch(gc, rect);
				} else if (name.equals("CHIP")){
					File chipFile = new File(file.getParent(), gate.getString("fileName"));
					if (!chipFile.exists()){
						Alert error = new Alert(Alert.AlertType.ERROR);
						error.setTitle("Missing dependency");
						error.setHeaderText("Missing dependency");
						error.setContentText("The following dependency is missing: "+gate.getString("fileName"));
						error.showAndWait();
						Gate.Pin.PIN_ID = backupId;
						return null;
					}
					gt = new Chip(gc, rect, chipFile);
				} else if (name.equals("DISPLAY7")){
					gt = new Display7(gc, rect);
				}
				Gate.Pin.PIN_ID = lastPinId; // Restore the last pin id
				Gate.Pin.UPDATE_PIN_ID = lastPinFlag;
				gt.setPins(pins);
				tempGates.add(gt);
			}

			// Attach gates' pins
			for (Object o : json.getJSONArray("gates")){
				JSONObject gate = (JSONObject)o;
				for (Object o2 : gate.getJSONArray("pins")){
					JSONObject pin = (JSONObject)o2;
					int pinId = pin.getInt("id");
					Gate.Pin currentPin = getPinById(tempGates, pinId);
					for (Object o3 : pin.getJSONArray("attached")){
						Integer apinId = (Integer)o3;
						Gate.Pin apin = getPinById(tempGates, apinId);
						currentPin.attach(apin);
					}
				}
			}

			// Load wires
			for (Object o : json.getJSONArray("wires")){
				JSONObject wire = (JSONObject)o;
				Gate.Pin p1 = getPinById(tempGates, wire.getInt("pin1"));
				Gate.Pin p2 = getPinById(tempGates, wire.getInt("pin2"));
				List<Point2D> points = new ArrayList<>();
				for (Object o2 : wire.getJSONArray("points")){
					JSONObject p = (JSONObject)o2;
					points.add(new Point2D(p.getDouble("x"), p.getDouble("y")));
				}
				tempWires.add(new Wire(gc, p1, p2, points));
			}

			return json;
		} catch (IOException ex){
			ex.printStackTrace();
			return null;
		}
	}

	private static Gate.Pin getPinById(List<Gate> gates, int id){
		for (Gate g : gates){
			for (Gate.Pin p : g.getPins()){
				if (p.getId() == id){
					return p;
				}
			}
		}
		return null;
	}

	public static Wire getWire(List<Wire> wires, Gate.Pin p1, Gate.Pin p2){
		for (Wire w : wires){
			if ((w.getPin1() == p1 && w.getPin2() == p2) || (w.getPin1() == p2 && w.getPin2() == p1)){
				return w;
			}
		}
		return null;
	}
	
	private void update(GraphicsContext gc){
		gc.clearRect(0, 0, WIDTH, HEIGHT);
		gc.setFill(Color.web("#EDEDED"));
		gc.fillRect(0, 0, WIDTH, HEIGHT);

		gc.save();
		gc.setFill(Color.BLACK);

		gc.fillText("ID:" + Gate.Pin.PIN_ID, 50, 300);

		gc.setGlobalAlpha(0.5);
		gc.fillRect(0, 0, WIDTH, TOOLBAR_Y);
		gc.restore();

		gc.save();
		gc.translate(this.cameraX, this.cameraY);
		if (this.movePoint != null){
			gc.translate(this.deltaMove.getX(), this.deltaMove.getY());
		}
		gc.scale(this.cameraScale, this.cameraScale);

		for (Gate g : this.gates){
			g.update();
			g.render();
		}
		for (Wire w : this.wires){
			w.render();
		}

		if (this.connG != null){
			gc.setStroke(Color.BLACK);
			gc.setLineWidth(3);
			Point2D end = getClickPoint(this.mouseMoved.getX(), this.mouseMoved.getY());
			List<Point2D[]> renderingPoints = Util.getPointsList(new Point2D(this.connG.getX(), this.connG.getY()), end, this.pinPoints);
			for (Point2D[] line : renderingPoints){
				gc.strokeLine(line[0].getX(), line[0].getY(), line[1].getX(), line[1].getY());
			}
		}

		gc.restore();

		// UI
		for (UiButton ub : this.buttons){
			ub.render();
		}
		if (this.selectedId == -1) this.sideArea.render();

		if (this.selectedId >= 0 && this.selectedId != 1){
			gc.save();
			gc.setFill(Color.YELLOW);
			gc.setGlobalAlpha(0.6);
			gc.fillRect(mouseMoved.getX(), mouseMoved.getY(), 50, 50);
			gc.restore();
		}

		// Remove selected gate
		// More than 1 if the user selected multiple gates during a frame update
		for (int i = 0; i < this.gatesToRemove.size(); i++){
			Gate g = this.gatesToRemove.get(i);
			g.destroy(this.wires, this.wiresToRemove);
			this.gates.remove(g);
		}
		this.gatesToRemove.clear();

		// Remove selected wire
		// More than 1 if the user selected multiple wires during a frame update
		for (int i = 0; i < this.wiresToRemove.size(); i++){
			Wire w = this.wiresToRemove.get(i);
			w.destroy();
			this.wires.remove(w);
		}
		this.wiresToRemove.clear();
	}
	
	public static void main(String[] args){
		launch(args);
	}
}
