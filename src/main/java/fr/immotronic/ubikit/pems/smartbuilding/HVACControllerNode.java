/*
 * Copyright (c) Immotronic, 2013
 *
 * Contributors:
 *
 *  	Lionel Balme (lbalme@immotronic.fr)
 *  	Kevin Planchet (kplanchet@immotronic.fr)
 *
 * This file is part of ubikit-core, a component of the UBIKIT project.
 *
 * This software is a computer program whose purpose is to host third-
 * parties applications that make use of sensor and actuator networks.
 *
 * This software is governed by the CeCILL-C license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL-C
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * As a counterpart to the access to the source code and  rights to copy,
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-C license and that you accept its terms.
 *
 * CeCILL-C licence is fully compliant with the GNU Lesser GPL v2 and v3.
 *
 */

package fr.immotronic.ubikit.pems.smartbuilding;

import org.ubikit.PhysicalEnvironmentItem;

public interface HVACControllerNode extends PhysicalEnvironmentItem
{
	public enum Mode {
		AUTO,
		HEAT,
		COOL,
		FAN_ONLY,
		DRY,
		UNKNOWN
	}
	
	public enum State {
		ON,
		OFF,
		UNKNOWN
	}
	
	public static final int UNKNOWN_FAN_SPEED = -1;
	public static final int UNKNOWN_VANE_POSITION = -1;
	public static final int VANE_SWING_MODE = -2;
	
	/**
	 * Set a new set point temperature in °C to the unit. Valid range goes from 0°C to 40°C.
	 * 
	 * @param setPoint a temperature in °C between 0°C to 40°C.
	 */
	public void setSetPointTemperature(float setPoint) throws IllegalArgumentException;
	
	/**
	 * Provide the current ambient temperature (in °C) to the unit. If this option is available on the unit.
	 * @param currentTemperature the current ambient temperature in °C.
	 */
	public void setAmbientTemperature(float currentAmbientTemperature) throws IllegalArgumentException;
	
	/**
	 * This method return true if the specified mode is available. It return false otherwise.
	 * 
	 * @param mode a mode to test.
	 * @return true if the specified mode exist in the actual unit, false otherwise. 
	 */
	public boolean isModeAvailable(Mode mode);
	
	/**
	 * Set the unit mode. This method return true if the required mode is available. It return false otherwise.
	 * 
	 * @param mode a mode.
	 * @return true if the specified mode could has been applied, false if the specified mode was not available. 
	 */
	public boolean setMode(Mode mode);
	
	/**
	 * Return the current mode of the unit.
	 */
	public Mode getCurrentMode();
	
	/**
	 * Set the fan speed of the unit in percent. The actual fan speed of the unit may not be exactly 
	 * as specified, but will be as close as possible. This method return the new actual fan speed.  
	 * 
	 * @param fanSpeed Fan speed in percent. Valid range goes from 0% to 100%.
	 * @return the actual fan speed in percent, between 0% to 100%.
	 */
	public int setFanSpeed(int fanSpeed) throws IllegalArgumentException;
	
	/**
	 * Return the current fan speed of the unit. 
	 * @return a fan speed between 0% to 100%. Return HVACControllerNode.UNKNOWN_FAN_SPEED if the 
	 * 			current fan speed could not be determined.
	 */
	public int getFanSpeed();
	
	/**
	 * Set the vane position of the unit in degrees, from 0° (horizontal) to 90° (vertical), 
	 * or HVACControllerNode.VANE_SWING_MODE. The actual vane position of the unit may not be 
	 * exactly as specified, but will be as close as possible. This method return the new actual 
	 * vane position.
	 * 
	 * @param vanePosition The vane position from 0° (horizontal) to 90° (vertical), or 
	 * 			HVACControllerNode.VANE_SWING_MODE, or HVACControllerNode.UNKNOWN_VANE_POSITION.
	 * 
	 * @return the actual vane position in degrees, from 0° (horizontal) to 90° (vertical), or 
	 * 			HVACControllerNode.VANE_SWING_MODE, or HVACControllerNode.UNKNOWN_VANE_POSITION.
	 */
	public int setVanePosition(int vanePosition) throws IllegalArgumentException;
	
	/**
	 * Return the current position of the vane of the unit. 
	 * @return a position between 0° (horizontal) to 90° (vertical), or 
	 * 			HVACControllerNode.VANE_SWING_MODE, or HVACControllerNode.UNKNOWN_VANE_POSITION.
	 */
	public int getVanePosition();
	
	/**
	 * Return true if the actual unit support a swing mode for its vane, return false otherwise.
	 */
	public boolean isVaneSwingModeAvailable();
	
	/**
	 * Set the unit on.
	 */
	public void turnOn();
	
	/**
	 * Set the unit off.
	 */
	public void turnOff();
	
	/**
	 * Return the On/Off state of the unit.
	 */
	public State getUnitState();
}
