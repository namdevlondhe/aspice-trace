# Climate HMI: software requirements (sample)

> Invented sample requirements for demonstrating `aspice-trace`. They do not come from any real project.

Requirements can be written as headings or as table rows. Prose mentions like "see SW-HMI-004" are not definitions.

## Temperature

### SW-HMI-001: Show the set temperature for each zone
The climate screen shall show the driver and passenger set temperatures at all times while the HVAC is on.

### SW-HMI-002: Temperature step and range
A temperature change shall move by the vehicle's step (0.5 °C by default) and stay within the range
reported by the vehicle.

## Air flow

### SW-HMI-003: Fan speed levels
The HMI shall offer fan levels 1 to 6 and send each change to the vehicle within 100 ms.

### SW-HMI-004: Defrost overrides cabin settings
Turning on defrost shall switch A/C on, recirculation off and the fan to maximum.

### SW-HMI-005: Restore settings after defrost
Turning defrost off shall restore the fan speed and A/C state that were active before it (see SW-HMI-004).

## Usability

| ID | Title | Rationale |
|---|---|---|
| SW-HMI-006 | Touch targets of at least 76 dp | Usable with gloves and on rough roads |
| SW-HMI-007 | Day and night theme follows the vehicle | Avoids glare at night |
| SW-HMI-008 | Block text entry while driving | Driver distraction guidelines |
