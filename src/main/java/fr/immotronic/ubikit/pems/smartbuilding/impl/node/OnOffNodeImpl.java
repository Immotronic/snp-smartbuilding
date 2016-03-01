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

import java.util.ArrayList;
import java.util.Collection;

import org.json.JSONObject;
import org.ubikit.PhysicalEnvironmentItem;
import org.ubikit.event.EventGate;
import org.ubikit.service.PhysicalEnvironmentModelService;

import fr.immotronic.ubikit.pems.enocean.ActuatorProfile;
import fr.immotronic.ubikit.pems.enocean.EnoceanActuatorDevice;
import fr.immotronic.ubikit.pems.enocean.EnoceanSensorAndActuatorDevice;
import fr.immotronic.ubikit.pems.enocean.SensorActuatorProfile;
import fr.immotronic.ubikit.pems.enocean.data.EEPD201xxData;
import fr.immotronic.ubikit.pems.enocean.data.EEPD201xxData.Mode;
import fr.immotronic.ubikit.pems.enocean.event.in.TurnOffActuatorEvent;
import fr.immotronic.ubikit.pems.enocean.event.in.TurnOnActuatorEvent;
import fr.immotronic.ubikit.pems.smartbuilding.AbstractNode;
import fr.immotronic.ubikit.pems.smartbuilding.NodeProfile;
import fr.immotronic.ubikit.pems.smartbuilding.OnOffNode;
import org.ubikit.Logger;
import fr.immotronic.ubikit.pems.smartbuilding.event.in.PowerOffEvent;
import fr.immotronic.ubikit.pems.smartbuilding.event.in.PowerOnEvent;
import fr.immotronic.ubikit.pems.smartbuilding.impl.LC;
import fr.immotronic.ubikit.pems.smartbuilding.impl.PemLauncher;

public class OnOffNodeImpl extends AbstractNode implements OnOffNode, PowerOffEvent.Listener, PowerOnEvent.Listener
{
	private static final String[] supportedCapabilities = { ActuatorProfile.ONOFF_DEVICE.name(), 
															SensorActuatorProfile.EEP_D2_01_00.name(),
															SensorActuatorProfile.EEP_D2_01_06.name(),
															SensorActuatorProfile.EEP_D2_01_11.name()
														  };
	private static final int MAX_CHANNELS_NUMBER = 2;
	private final int channel;
	
	public static Collection<AbstractNode> createInstanceIfAppropriate(PemLocalID pemID, PhysicalEnvironmentItem actualNode, EventGate lowerEventGate, EventGate higherEventGate, PemLauncher pem)
	{
		Collection<AbstractNode> nodes = new ArrayList<AbstractNode>();
		
		if(AbstractNode.isCapabilitiesSupported(pemID, supportedCapabilities, actualNode.getCapabilities()))
		{
			if(!isNodeAlreadyAdded(pem, pemID, actualNode))
			{
				if(LC.debug) {
					Logger.debug(LC.gi(), null, "createInstanceIfAppropriate(): create an On/Off node ("+actualNode.getUID()+", "+actualNode.getPropertyValue(PhysicalEnvironmentItem.Property.CustomName.name())+")");
				}
				
				switch(pemID)
				{
					case ENOCEAN:
						if(actualNode.getType() == Type.SENSOR_AND_ACTUATOR && ((EnoceanSensorAndActuatorDevice) actualNode).getSensorActuatorProfile() == SensorActuatorProfile.EEP_D2_01_11)
						{
							EEPD201xxData data = (EEPD201xxData) actualNode.getValue();
							if(data.getMode() == Mode.RELAY)
							{
								if(LC.debug) {
									Logger.debug(LC.gi(), null, "createInstanceIfAppropriate(): This is a EEP_D2_01_11 (2 channels) configured as RELAY.");
								}
								
								for(int i = 0; i < MAX_CHANNELS_NUMBER; i++)
								{
									nodes.add(new OnOffNodeImpl(pemID, actualNode, i, lowerEventGate, higherEventGate));
								}
							}
						}
						else
						{
							if(LC.debug) {
								Logger.debug(LC.gi(), null, "createInstanceIfAppropriate(): This is a single channel relay.");
							}
							
							nodes.add(new OnOffNodeImpl(pemID, actualNode, -1, lowerEventGate, higherEventGate));
						}
						break;
				}
				
				return nodes;
			}
		}
		
		return null;
	}
	
	private ActuatorProfile ap = null;
	private SensorActuatorProfile sap = null;
	
	private static String makeUID(PemLocalID pem, PhysicalEnvironmentItem actualNode, int channel)
	{
		switch(pem)
		{
			case ENOCEAN:
				if(channel > -1)
				{
					if(actualNode.getType() == Type.SENSOR_AND_ACTUATOR && ((EnoceanSensorAndActuatorDevice) actualNode).getSensorActuatorProfile() == SensorActuatorProfile.EEP_D2_01_11)
					{
						return NodeProfile.ON_OFF+"_"+channel+"_"+actualNode.getUID();
					}
				}
		}
		
		return NodeProfile.ON_OFF+"_"+actualNode.getUID();
	}
	
	private static boolean isNodeAlreadyAdded(PhysicalEnvironmentModelService pem, PemLocalID pemID, PhysicalEnvironmentItem actualNode)
	{
		switch(pemID)
		{
			case ENOCEAN:
				for(int i = -1; i < MAX_CHANNELS_NUMBER; i++)
				{
					if (pem.getItem(makeUID(pemID, actualNode, i)) != null)
						return true;
				}
				return false;
		}
		
		return pem.getItem(makeUID(pemID, actualNode, -1)) != null;
	}
	
	private int getChannelfromUID(PemLocalID pem, PhysicalEnvironmentItem actualNode, String uid)
	{
		switch(pem)
		{
			case ENOCEAN:
				
				if(actualNode.getType() == Type.SENSOR_AND_ACTUATOR && ((EnoceanSensorAndActuatorDevice) actualNode).getSensorActuatorProfile() == SensorActuatorProfile.EEP_D2_01_11)
				{
						return Integer.parseInt(uid.split("_")[2]);
				}
				break;
		}
		return -1;
	}
	
	public OnOffNodeImpl(PemLocalID pem, PhysicalEnvironmentItem actualNode, int channel, EventGate lowerEventGate, EventGate higherEventGate)
	{
		this(makeUID(pem, actualNode, channel), pem, actualNode, lowerEventGate, higherEventGate);
	}
	
	public OnOffNodeImpl(String uid, PemLocalID pem, PhysicalEnvironmentItem actualNode, EventGate lowerEventGate, EventGate higherEventGate)
	{
		super(NodeProfile.ON_OFF, Type.SENSOR_AND_ACTUATOR, null, uid, pem, actualNode, lowerEventGate, higherEventGate);
		
		switch(pem)
		{
			case ENOCEAN:

				switch (actualNode.getType())
				{
					case ACTUATOR:
						EnoceanActuatorDevice enoceanActuatorNode = (EnoceanActuatorDevice) actualNode;
						ap = enoceanActuatorNode.getActuatorProfile();
						break;
					case SENSOR_AND_ACTUATOR:
						EnoceanSensorAndActuatorDevice enoceanSensorActuatorNode = (EnoceanSensorAndActuatorDevice) actualNode;
						sap = enoceanSensorActuatorNode.getSensorActuatorProfile();
						break;
				}
				
				break;
		}
		
		this.channel = getChannelfromUID(pem, actualNode, uid);
		higherEventGate.addListener(this);
	}
	
	
	@Override
	public State getState() 
	{
		if(isLocal()) 
		{
			switch(getPemLocalID())
			{
				case ENOCEAN:
					
					if (ap != null)
					{
						switch(ap)
						{
							case ONOFF_DEVICE: return State.UNKNOWN;
							
							default:
								break;
						}
					}
					
					if (sap != null)
					{
						EEPD201xxData data = null;
						switch(sap)
						{
							case EEP_D2_01_00:
							case EEP_D2_01_06: 
								data = (EEPD201xxData) getActualNodeValue();
								if (data == null) {
									return State.UNKNOWN;
								}
								
								switch (data.getSwitchState())
								{
									case ON : return State.ON;
									case OFF : return State.OFF;
									default : return State.UNKNOWN;
								}
							
							case EEP_D2_01_11:
								data = (EEPD201xxData) getActualNodeValue();
								if (data == null) {
									return State.UNKNOWN;
								}
								
								switch (data.getSwitchState(channel))
								{
									case ON : return State.ON;
									case OFF : return State.OFF;
									default : return State.UNKNOWN;
								}
								
							default:
								break;
						}
					}
					
					break;
			}
		}
		
		return State.UNKNOWN;
	}
	
	@Override
	public void on()
	{
		TurnOnActuatorEvent e = new TurnOnActuatorEvent(getActualNodeUID(), channel);
		lowerEventGate.postEvent(e);
	}
	
	@Override
	public void off()
	{
		TurnOffActuatorEvent e = new TurnOffActuatorEvent(getActualNodeUID(), channel);
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
	public void onEvent(PowerOnEvent event) 
	{
		if(event.getSourceItemUID().equals(getUID()))
		{
			on();
		}
	}

	@Override
	public void onEvent(PowerOffEvent event) 
	{
		if(event.getSourceItemUID().equals(getUID()))
		{
			off();
		}
	}
	
	@Override
	protected void terminate() 
	{
		higherEventGate.removeListener(this);
		super.terminate();
	}
}
