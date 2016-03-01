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

import fr.immotronic.ubikit.pems.enocean.ActuatorProfile;
import fr.immotronic.ubikit.pems.enocean.EnoceanActuatorDevice;
import fr.immotronic.ubikit.pems.enocean.EnoceanSensorAndActuatorDevice;
import fr.immotronic.ubikit.pems.enocean.SensorActuatorProfile;
import fr.immotronic.ubikit.pems.enocean.data.EEPD201xxData;
import fr.immotronic.ubikit.pems.enocean.event.in.SetOutputLevelEvent;
import fr.immotronic.ubikit.pems.smartbuilding.impl.PemLauncher;
import fr.immotronic.ubikit.pems.smartbuilding.AbstractNode;
import fr.immotronic.ubikit.pems.smartbuilding.DimmerNode;
import fr.immotronic.ubikit.pems.smartbuilding.NodeProfile;
import fr.immotronic.ubikit.pems.smartbuilding.impl.LC;
import fr.immotronic.ubikit.pems.smartbuilding.event.in.SetBrightnessEvent;

public class DimmerNodeImpl extends AbstractNode implements DimmerNode, SetBrightnessEvent.Listener
{
	private static final String[] supportedCapabilities = { ActuatorProfile.RH_DEVICE.name(),
															SensorActuatorProfile.EEP_D2_01_02.name()
														  };
	
	public static DimmerNodeImpl createInstanceIfAppropriate(PemLocalID pemID, PhysicalEnvironmentItem actualNode, EventGate lowerEventGate, EventGate higherEventGate, PemLauncher pem)
	{
		if(AbstractNode.isCapabilitiesSupported(pemID, supportedCapabilities, actualNode.getCapabilities()))
		{
			if(pem.getItem(makeUID(actualNode.getUID())) == null)
			{
				if(LC.debug) {
					Logger.debug(LC.gi(), null, "createInstanceIfAppropriate(): create an dimmer control node ("+actualNode.getUID()+", "+actualNode.getPropertyValue(PhysicalEnvironmentItem.Property.CustomName.name())+")");
				}
				
				return new DimmerNodeImpl(pemID, actualNode, lowerEventGate, higherEventGate);
			}
		}
		
		return null;
	}
	
	private static String makeUID(String actualNodeUID)
	{
		return NodeProfile.DIMMER+"_"+actualNodeUID;
	}
	
	private ActuatorProfile ap = null;
	private SensorActuatorProfile sap = null;
	
	public DimmerNodeImpl(PemLocalID pem, PhysicalEnvironmentItem actualNode, EventGate lowerEventGate, EventGate higherEventGate)
	{
		this(makeUID(actualNode.getUID()), pem, actualNode, lowerEventGate, higherEventGate);
	}
	
	public DimmerNodeImpl(String uid, PemLocalID pem, PhysicalEnvironmentItem actualNode, EventGate lowerEventGate, EventGate higherEventGate)
	{
		super(NodeProfile.DIMMER, Type.ACTUATOR, null, uid, pem, actualNode, lowerEventGate, higherEventGate);
		
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
		
		higherEventGate.addListener(this);
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
	public void onEvent(SetBrightnessEvent event) 
	{
		if(event.getSourceItemUID().equals(getUID()))
		{
			setBrightness(event.getBrightness());
		}
	}

	@Override
	public void setBrightness(int brightness) 
	{
		SetOutputLevelEvent e = new SetOutputLevelEvent(getActualNodeUID(), brightness);
		lowerEventGate.postEvent(e);
	}
	
	@Override
	public int getBrightness()
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
							case ONOFF_DEVICE: return Integer.MIN_VALUE;
							
							default:
								break;
						}
					}
					
					if (sap != null)
					{
						switch(sap)
						{
							case EEP_D2_01_02:
								EEPD201xxData data = (EEPD201xxData) getActualNodeValue();
								if (data == null)
									return Integer.MIN_VALUE;
								
								return data.getDimmerValue();							
							default:
								break;
						}
					}
					break;
			}
		}
		return Integer.MIN_VALUE;
	}
	
	@Override
	protected void terminate() 
	{
		higherEventGate.removeListener(this);
		super.terminate();
	}
}
