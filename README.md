# ![icon](icon.png) LogicSimulator
This is a simple logic gate simulator made in Java using the JavaFX framework.

# Features
## Building blocks
* Switches (input)
* Lights (output)
* AND gate
* NOT gate
* Bus
* Tri-state buffer
* 7 Segment display
* Circuits embedding using chips
## Simulator features
* Save/Load projects to a readable JSON format
* Save projects as a chip with your custom name and color
* Clear workspace
* Delete gate/wire
* Connect multiple buses together
* Gate label
* Connect/Disconnect pins from the circuit
* Turn global power on/off
* Export circuit to a PNG image

# Keyboard usage help
* Use `P` to toggle global power on/off
* Use `DELETE` to delete selected gates from the circuit
* Use `Shift` + `DELETE` to delete the wires of the selected gates in the circuit
* Use `Z` and `X` to decrement/increment buses amount when placing a bus
* Use `R` to align selected buses
* Use `F1` to show/hide pin IDs
* Use `Shift` when:
	* placing a wire to align it
	* to place an input pin instead of an output one onto the bus
	* to move pins that are on a bus
* Use `Control` when:
	* placing a wire to delete the previous point
	* to define a new selection without unselecting the previous one
* Use `Alt` to place a wirepoint on the position of the nearest wirepoint on the circuit

# Mouse usage help
* Use the scroll wheel to scale the canvas
* Use `Right click`:
	* on pins to enter wire-placing mode
	* to place the chosen block from the menu on the right
	* to toggle a switch on/off
	* to delete a gate if you clicked on the `RM GATE` button
	* to place a pin on a bus (use `Shift` to toggle between an input/output pin)
	* to resize a bus horizontally or vertically
	* to connect buses together if you clicked on the `CONNECT BUS` button
	* to delete a wire if you clicked on the `RM WIRE` button
	* to move a pin of the bus if the `Shift` key is pressed too
	* to define a selection area or if you click on the background to unselect the current selection
* Use `Left click`:
	* to interrupt an action like creating a bus, deleting a gate or a wire
	* to move the camera
	* to move selected gates or selected wire points
	* to open the gate properties menu where you can change the label, remove a pin (if you clicked on a bus pin), and so on

# Simulator screenshots and exports
![export](example.png)  
![sc1](sc1.png)  
![sc2](sc2.png)