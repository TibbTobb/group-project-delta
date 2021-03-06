# Simulation GUI

## Overview

The application is divided into two panes, the side panel on the left and the
main view on the right.

### Side Panel

This pane contains a tabbed layout, where the current tab can be changed by
clicking the headers at the top of this pane.

#### Properties Tab

Upon selection of an object the "Properties" tab will display the object's
details, such as position and velocity.

#### Network Tab

In the "Network" tab a list of network messages is shown. Along the top of the 
tab are filters for the displayed messages, a tick indicates that that type of
message is to be shown.

The filters are:
-	**Emergency**: all messages tagged as emergency messages.
-	**Data**: all heartbeat data messages, which broadcast a vehicle's current
	state.
-	**Merges**: all messages pertaining to the merging of platoons.
-	**Queries**: all beacon ID question and answer messages.

>	**Note**: the filters will only affect messages received after the filter
>	was changed, previously received messages are not filtered.

New messages will be added to the top of the list.

#### Options Tab

This tab gives the option to reset the simulation and to modify various error
injection options to stress test the current simulated platoon on more realistic
peripherals.

-	**Network delivery modifier**: A value of 0 means that packets should never
	be dropped, and any value larger than that is permitted. A value of 1 will
	give about 95% packet delivery at 1m, 85% at 2m, and 50% at 3m, while a
	value of 2 will give about 90% at 1m, 50% at 1.5m and 25% at 2m.
	
-	**Front proximity enabled**: Enables or disables the front proximity
	sensors for simulated vehicles.
-	**Front proximity standard deviation**: The standard deviation for the front
	proximity sensor's readings.
-	**Front proximity failure rate**: The proportion of the time when the
	proximity sensor will incorrectly report no reading.
	
-	**Beacons emulate Mindstorms**: Whether the beacons should attempt to match
	the LEGO Mindstorms' behaviour.
-	**Beacon distance standard deviation**: The standard deviation for the
	distance measured by the beacons.
-	**Beacon angle standard deviation**: the standard deviation for the angle
	measured by the beacons.
	
-	**Acceleration standard deviation**: The standard deviation for the
	acceleration error.
-	**Speed standard deviation**: The standard deviation for the speed error.
-	**Turn rate standard deviation**: The standard deviation for the turn rate
	error.

### Main View

The main view shows the current simulation state. It can be navigated using
click-and-drag to pan the view, and scrolling to zoom.

New objects can be added by right-clicking in the main view; the status of
existing objects can be acquired by left-clicking on the object and navigating
to the "Network" tab.

The following shortcut keys are available:
-	`<space>`: pause/unpause
-	`;`: step
-	`+`/`=`: reduce time warp factor
-	`-`/`_`: increase time warp factor
-	`0`: reset time warp factor to `1.0`

#### Platoons

Platooning information is indicated by lightly-coloured circles underneath the
cars. The colour of the circle indicates the platoon, all cars with the same
colour are part of the same platoon. If there is a ring around the circle, then
this car is the leader of its platoon.

Hovering over a car will provide information relevant to platooning.

#### Adding a New Car

1.	Right-click in the main view and select "Add object", this will open dialog
2.	Fill in the desired values for:
	-	the car's wheel base (distance between front and rear axles);
	-	the x- and y-coordinate of the car (initially the position clicked); and
	-	the controlling algorithm.
3.	Click "Confirm" to create a car, or "Cancel" at anytime to return to the
	main view without creating a car.
	
>	**Note**: a car with the manual control algorithm does not partake in any
>	communication over the network, and hence will not merge with other
>	platoons.

#### Controlling vehicles

The currently selected vehicle can be controlled using the W, A, S and D keys:
-	`W`: accelerate forward
-	`S`: brake
-	`A`: turn left
-	`D`: turn right

>	**Note**: only the leader of a platoon is likely to be controllable, because
>	the followers will make driving decisions based on their internal state
>	which will override user input.

#### Following vehicles

To follow a vehicle, right-click on it a select the "Follow" option from the
context menu. The view will keep this vehicle centered.

All user input will be directed to the followed vehicle regardless of the
currently selected vehicle.

Drag the view to release the control lock and view tracking.
