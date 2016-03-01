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

package fr.immotronic.ubikit.pems.smartbuilding.impl.node;

import org.json.JSONObject;
import org.ubikit.Logger;
import org.ubikit.PhysicalEnvironmentItem;
import org.ubikit.event.EventGate;

import fr.immotronic.ubikit.pems.enocean.EnoceanSensorAndActuatorDevice;
import fr.immotronic.ubikit.pems.enocean.SensorActuatorProfile;
import fr.immotronic.ubikit.pems.enocean.actuator.HVACDevice;
import fr.immotronic.ubikit.pems.enocean.data.EEP07201011Data;
import fr.immotronic.ubikit.pems.enocean.data.EEP07201011Data.FanSpeed;
import fr.immotronic.ubikit.pems.enocean.data.EEP07201011Data.VanePosition;
import fr.immotronic.ubikit.pems.enocean.event.in.GenericHVACInterfaceControlEvent;
import fr.immotronic.ubikit.pems.enocean.event.in.TemperatureAndSetPointEvent;
import fr.immotronic.ubikit.pems.smartbuilding.NodeProfile;
import fr.immotronic.ubikit.pems.smartbuilding.AbstractNode;
import fr.immotronic.ubikit.pems.smartbuilding.HVACControllerNode;
import fr.immotronic.ubikit.pems.smartbuilding.impl.LC;
import fr.immotronic.ubikit.pems.smartbuilding.impl.PemLauncher;

/**
 * TODO add incoming events for IntesisBoxController (GenericHVACommand and Set point events)
 * @author kevinplanchet
 */

public class HVACControllerNodeImpl extends AbstractNode implements HVACControllerNode
{
	
	private static final String[] supportedCapabilities = { SensorActuatorProfile.INTESIS_BOX_DK_RC_ENO_1i1iC_DEVICE.name() };
	
	public static HVACControllerNodeImpl createInstanceIfAppropriate(PemLocalID pemID, PhysicalEnvironmentItem actualNode, EventGate lowerEventGate, EventGate higherEventGate, PemLauncher pem)
	{
		if(AbstractNode.isCapabilitiesSupported(pemID, supportedCapabilities, actualNode.getCapabilities()))
		{
			if(pem.getItem(makeUID(actualNode.getUID())) == null)
			{
				if(LC.debug) {
					Logger.debug(LC.gi(), null, "createInstanceIfAppropriate(): create a HVAC controller node ("+actualNode.getUID()+", "+actualNode.getPropertyValue(PhysicalEnvironmentItem.Property.CustomName.name())+")");
				}
				
				return new HVACControllerNodeImpl(pemID, actualNode, lowerEventGate, higherEventGate);
			}
		}
		
		return null;
	}
	
	private SensorActuatorProfile enoceanProfile;
	private HVACDevice enoceanHVACDevice;
	
	private static String makeUID(String actualNodeUID)
	{
		return NodeProfile.HVAC_CONTROLLER+"_"+actualNodeUID;
	}
	
	public HVACControllerNodeImpl(PemLocalID pem, PhysicalEnvironmentItem actualNode, EventGate lowerEventGate, EventGate higherEventGate)
	{
		this(makeUID(actualNode.getUID()), pem, actualNode, lowerEventGate, higherEventGate);
	}
	
	public HVACControllerNodeImpl(String uid, PemLocalID pem, PhysicalEnvironmentItem actualNode, EventGate lowerEventGate, EventGate higherEventGate)
	{
		super(NodeProfile.HVAC_CONTROLLER, Type.SENSOR_AND_ACTUATOR, null, uid, pem, actualNode, lowerEventGate, higherEventGate);
		
		switch(pem)
		{
			case ENOCEAN:
				EnoceanSensorAndActuatorDevice enoceanNode = (EnoceanSensorAndActuatorDevice) actualNode;
				enoceanProfile = enoceanNode.getSensorActuatorProfile();
				enoceanHVACDevice = (HVACDevice) actualNode;
				break;
		}

		//lowerEventGate.addListener(this);
	}
	
	@Override
	public void setSetPointTemperature(float setPoint) throws IllegalArgumentException
	{
		if(setPoint < 0 || setPoint > 40) {
			throw new IllegalArgumentException("setPoint="+setPoint+". It MUST BE in the interval [0..40].");
		}
		
		switch(getPemLocalID())
		{
			case ENOCEAN:
				switch(enoceanProfile)
				{
					case INTESIS_BOX_DK_RC_ENO_1i1iC_DEVICE:
						TemperatureAndSetPointEvent e = new TemperatureAndSetPointEvent(getActualNodeUID(), setPoint, Float.MIN_VALUE);
						lowerEventGate.postEvent(e);
						break;
				}
				break;
				
			default: 
				break;
		}
	}
	
	@Override
	public void setAmbientTemperature(float currentAmbientTemperature) throws IllegalArgumentException
	{
		if(currentAmbientTemperature < 0 || currentAmbientTemperature > 40) {
			throw new IllegalArgumentException("currentAmbientTemperature="+currentAmbientTemperature+". It MUST BE in the interval [0..40].");
		}
		
		switch(getPemLocalID())
		{
			case ENOCEAN:
				switch(enoceanProfile)
				{
					case INTESIS_BOX_DK_RC_ENO_1i1iC_DEVICE:
						TemperatureAndSetPointEvent e = new TemperatureAndSetPointEvent(getActualNodeUID(), Float.MIN_VALUE, currentAmbientTemperature);
						lowerEventGate.postEvent(e);
						break;
				}
				break;
				
			default: 
				break;
		}
	}

	@Override
	public boolean setMode(Mode mode)
	{
		switch(getPemLocalID())
		{
			case ENOCEAN:
				switch(enoceanProfile)
				{
					case INTESIS_BOX_DK_RC_ENO_1i1iC_DEVICE:
						if(mode != null)
						{
							EEP07201011Data.Mode m = null;
							try
							{
								m = EEP07201011Data.Mode.valueOf(mode.name());
								postCommand(m, EEP07201011Data.VanePosition.NO_ACTION, EEP07201011Data.FanSpeed.NO_ACTION, EEP07201011Data.OnOffStatus.ON);
							}
							catch(IllegalArgumentException e)
							{ }
						}
						
						return false;
				}
				break;
				
			default:
				break;
		}
		
		return false;
	}
	
	@Override
	public void turnOff()
	{
		switch(getPemLocalID())
		{
			case ENOCEAN:
				switch(enoceanProfile)
				{
					case INTESIS_BOX_DK_RC_ENO_1i1iC_DEVICE:
						postCommand(EEP07201011Data.Mode.NO_ACTION, EEP07201011Data.VanePosition.NO_ACTION, EEP07201011Data.FanSpeed.NO_ACTION, EEP07201011Data.OnOffStatus.OFF);
						break;
				}
				break;
				
			default: 
				break;
		}
		
	}
	
	@Override
	public void turnOn()
	{
		switch(getPemLocalID())
		{
			case ENOCEAN:
				switch(enoceanProfile)
				{
					case INTESIS_BOX_DK_RC_ENO_1i1iC_DEVICE:
						postCommand(EEP07201011Data.Mode.NO_ACTION, EEP07201011Data.VanePosition.NO_ACTION, EEP07201011Data.FanSpeed.NO_ACTION, EEP07201011Data.OnOffStatus.ON);
						break;
				}
				break;
				
			default: 
				break;
		}
		
	}
	
	@Override
	public boolean isModeAvailable(Mode mode) 
	{
		switch(getPemLocalID())
		{
			case ENOCEAN:
				switch(enoceanProfile)
				{
					case INTESIS_BOX_DK_RC_ENO_1i1iC_DEVICE:
						EEP07201011Data.Mode m = EEP07201011Data.Mode.valueOf(mode.name());
						return enoceanHVACDevice.isModeSupported(m);
				}
				break;
				
			default: 
				break;
		}
		
		return false;
	}

	@Override
	public Mode getCurrentMode() 
	{
		switch(getPemLocalID())
		{
			case ENOCEAN:
				switch(enoceanProfile)
				{
					case INTESIS_BOX_DK_RC_ENO_1i1iC_DEVICE:
						EEP07201011Data data = (EEP07201011Data) getActualNodeValue();
						switch(data.getMode())
						{
							case AUTO:
							case HEAT:
							case COOL:
							case FAN_ONLY:
							case DRY:
								return Mode.valueOf(data.getMode().name());
							default:
								break;
						}
						break;
				}
				break;
				
			default: 
				break;
		}
		
		return Mode.UNKNOWN;
	}

	@Override
	public int setFanSpeed(int fanSpeed) throws IllegalArgumentException
	{
		if(fanSpeed < 0 || fanSpeed > 100) {
			throw new IllegalArgumentException("fanSpeed="+fanSpeed+". It MUST BE in the interval [0..100].");
		}
		
		switch(getPemLocalID())
		{
			case ENOCEAN:
				switch(enoceanProfile)
				{
					case INTESIS_BOX_DK_RC_ENO_1i1iC_DEVICE:
						if(fanSpeed < 33)
						{
							enoceanHVACDevice.setFanSpeed(FanSpeed.SPEED_1);
							return 33;
						} 
						else if(fanSpeed < 66) 
						{
							enoceanHVACDevice.setFanSpeed(FanSpeed.SPEED_2);
							return 66;
						}
						else
						{
							enoceanHVACDevice.setFanSpeed(FanSpeed.SPEED_3);
							return 100;
						}
				}
				break;
				
			default: 
				break;
		}
		
		return UNKNOWN_FAN_SPEED;
	}

	@Override
	public int getFanSpeed() 
	{	
		switch(getPemLocalID())
		{
			case ENOCEAN:
				switch(enoceanProfile)
				{
					case INTESIS_BOX_DK_RC_ENO_1i1iC_DEVICE:
						EEP07201011Data data = (EEP07201011Data) getActualNodeValue();
						switch(data.getFanSpeed())
						{
							case SPEED_1:
								return 33;
							case SPEED_2:
								return 66;
							case SPEED_3:
								return 100;
							default:
								return UNKNOWN_FAN_SPEED;
						} 
				}
				break;
				
			default: 
				break;
		}
		
		return UNKNOWN_FAN_SPEED;
	}

	@Override
	public int setVanePosition(int vanePosition) throws IllegalArgumentException
	{
		if((vanePosition < 0 || vanePosition > 100) && (vanePosition != VANE_SWING_MODE)) {
			throw new IllegalArgumentException("vanePosition="+vanePosition+". It MUST BE in the interval [0..90].");
		}
		
		switch(getPemLocalID())
		{
			case ENOCEAN:
				switch(enoceanProfile)
				{
					case INTESIS_BOX_DK_RC_ENO_1i1iC_DEVICE:
						if(vanePosition == VANE_SWING_MODE)
						{
							enoceanHVACDevice.setVanePosition(VanePosition.VERTICAL_SWING);
							return VANE_SWING_MODE;
						}
						else if(vanePosition < 11)
						{
							enoceanHVACDevice.setVanePosition(VanePosition.HORIZONTAL);
							return 0;
						}
						else if(vanePosition < 33)
						{
							enoceanHVACDevice.setVanePosition(VanePosition.POSITION_2);
							return 22;
						} 
						else if(vanePosition < 55)
						{
							enoceanHVACDevice.setVanePosition(VanePosition.POSITION_3);
							return 44;
						}
						else if(vanePosition < 77)
						{
							enoceanHVACDevice.setVanePosition(VanePosition.POSITION_4);
							return 66;
						}
						else
						{
							enoceanHVACDevice.setVanePosition(VanePosition.VERTICAL);
							return 90;
						}
				}
		}
		
		return UNKNOWN_VANE_POSITION;
	}

	@Override
	public int getVanePosition() 
	{
		switch(getPemLocalID())
		{
			case ENOCEAN:
				switch(enoceanProfile)
				{
					case INTESIS_BOX_DK_RC_ENO_1i1iC_DEVICE:
						EEP07201011Data data = (EEP07201011Data) getActualNodeValue();
						switch(data.getVanePosition())
						{
							case HORIZONTAL:
								return 0;
							case POSITION_2:
								return 22;
							case POSITION_3:
								return 44;
							case POSITION_4:
								return 66;
							case VERTICAL:
								return 90;
							case VERTICAL_SWING:
								return VANE_SWING_MODE;
							default:
								break;
						}
				}
		}
		
		return UNKNOWN_VANE_POSITION;
	}

	@Override
	public boolean isVaneSwingModeAvailable() 
	{
		switch(getPemLocalID())
		{
			case ENOCEAN:
				switch(enoceanProfile)
				{
					case INTESIS_BOX_DK_RC_ENO_1i1iC_DEVICE:
						return true;
				}
		}
		
		return false;
	}

	@Override
	public State getUnitState() 
	{
		switch(getPemLocalID())
		{
			case ENOCEAN:
				switch(enoceanProfile)
				{
					case INTESIS_BOX_DK_RC_ENO_1i1iC_DEVICE:
						EEP07201011Data data = (EEP07201011Data) getActualNodeValue();
						switch(data.getOnOffStatus())
						{
							case ON:
								return State.ON;
							case OFF:
								return State.OFF;
							default:
								break;
						}
				}
		}
		
		return State.UNKNOWN;
	}
	
	private void postCommand(EEP07201011Data.Mode mode, EEP07201011Data.VanePosition vanePosition, EEP07201011Data.FanSpeed fanSpeed, EEP07201011Data.OnOffStatus onOffStatus)
	{
		GenericHVACInterfaceControlEvent e = new GenericHVACInterfaceControlEvent(getActualNodeUID(), mode, vanePosition, fanSpeed, EEP07201011Data.RoomOccupancy.OCCUPIED, onOffStatus);
		lowerEventGate.postEvent(e);
	}
	
	@Override
	public Object getValue() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JSONObject getValueAsJSON() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	protected void terminate() 
	{
		//higherEventGate.removeListener(this);
		super.terminate();
	}
}
