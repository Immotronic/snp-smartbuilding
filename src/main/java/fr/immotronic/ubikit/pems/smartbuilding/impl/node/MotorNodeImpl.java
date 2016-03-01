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
import org.ubikit.PhysicalEnvironmentItem;
import org.ubikit.event.EventGate;

import fr.immotronic.ubikit.pems.enocean.ActuatorProfile;
import fr.immotronic.ubikit.pems.enocean.EnoceanSensorAndActuatorDevice;
import fr.immotronic.ubikit.pems.enocean.SensorActuatorProfile;
import fr.immotronic.ubikit.pems.enocean.data.EEPD201xxData;
import fr.immotronic.ubikit.pems.enocean.data.EEPD201xxData.Mode;
import fr.immotronic.ubikit.pems.enocean.event.in.MoveDownBlindOrShutterEvent;
import fr.immotronic.ubikit.pems.enocean.event.in.MoveUpBlindOrShutterEvent;
import fr.immotronic.ubikit.pems.enocean.event.in.StopBlindOrShutterEvent;
import fr.immotronic.ubikit.pems.smartbuilding.AbstractNode;
import fr.immotronic.ubikit.pems.smartbuilding.MotorNode;
import fr.immotronic.ubikit.pems.smartbuilding.NodeProfile;
import org.ubikit.Logger;
import fr.immotronic.ubikit.pems.smartbuilding.event.in.MoveDownEvent;
import fr.immotronic.ubikit.pems.smartbuilding.event.in.MoveUpEvent;
import fr.immotronic.ubikit.pems.smartbuilding.event.in.StopEvent;
import fr.immotronic.ubikit.pems.smartbuilding.impl.LC;
import fr.immotronic.ubikit.pems.smartbuilding.impl.PemLauncher;

public class MotorNodeImpl extends AbstractNode implements MotorNode, MoveDownEvent.Listener, MoveUpEvent.Listener,  StopEvent.Listener
{
	private static final String[] supportedCapabilities = { ActuatorProfile.BLIND_AND_SHUTTER_MOTOR_DEVICE.name(), 
															SensorActuatorProfile.EEP_D2_01_11.name() };
	
	public static MotorNodeImpl createInstanceIfAppropriate(PemLocalID pemID, PhysicalEnvironmentItem actualNode, EventGate lowerEventGate, EventGate higherEventGate, PemLauncher pem)
	{
		if(AbstractNode.isCapabilitiesSupported(pemID, supportedCapabilities, actualNode.getCapabilities()))
		{
			if(LC.debug) {
				Logger.debug(LC.gi(), null, "createInstanceIfAppropriate(): create an motor control node ("+actualNode.getUID()+", "+actualNode.getPropertyValue(PhysicalEnvironmentItem.Property.CustomName.name())+")");
			}
			
			if(pem.getItem(makeUID(actualNode.getUID())) == null)
			{
				if(actualNode.getType() == Type.ACTUATOR)
				{
					return new MotorNodeImpl(pemID, actualNode, lowerEventGate, higherEventGate);
				}
				else if(actualNode.getType() == Type.SENSOR_AND_ACTUATOR && ((EnoceanSensorAndActuatorDevice) actualNode).getSensorActuatorProfile() == SensorActuatorProfile.EEP_D2_01_11)
				{
					EEPD201xxData data = (EEPD201xxData) actualNode.getValue();
					if(data.getMode() == Mode.MOTOR)
					{
						if(LC.debug) {
							Logger.debug(LC.gi(), null, "createInstanceIfAppropriate(): This is a EEP_D2_01_11 configured as MOTOR.");
						}
						
						return new MotorNodeImpl(pemID, actualNode, lowerEventGate, higherEventGate);
					}
				}
			}
		}
		
		return null;
	}
	
	private static String makeUID(String actualNodeUID)
	{
		return NodeProfile.MOTOR_CONTROL+"_"+actualNodeUID;
	}
	
	public MotorNodeImpl(PemLocalID pem, PhysicalEnvironmentItem actualNode, EventGate lowerEventGate, EventGate higherEventGate)
	{
		this(makeUID(actualNode.getUID()), pem, actualNode, lowerEventGate, higherEventGate);
	}
	
	public MotorNodeImpl(String uid, PemLocalID pem, PhysicalEnvironmentItem actualNode, EventGate lowerEventGate, EventGate higherEventGate)
	{
		super(NodeProfile.MOTOR_CONTROL, Type.ACTUATOR, null, uid, pem, actualNode, lowerEventGate, higherEventGate);
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
	public void onEvent(MoveDownEvent event) 
	{
		if(event.getSourceItemUID().equals(getUID()))
		{
			moveDown();
		}
	}

	@Override
	public void onEvent(MoveUpEvent event) 
	{
		if(event.getSourceItemUID().equals(getUID()))
		{
			moveUp();
		}
	}
	
	@Override
	public void onEvent(StopEvent event) 
	{
		if(event.getSourceItemUID().equals(getUID()))
		{
			stop();
		}
	}

	@Override
	public void moveUp() 
	{
		MoveUpBlindOrShutterEvent e = new MoveUpBlindOrShutterEvent(getActualNodeUID());
		lowerEventGate.postEvent(e);
	}

	@Override
	public void moveDown() 
	{
		MoveDownBlindOrShutterEvent e = new MoveDownBlindOrShutterEvent(getActualNodeUID());
		lowerEventGate.postEvent(e);
	}

	@Override
	public void stop() 
	{
		StopBlindOrShutterEvent e = new StopBlindOrShutterEvent(getActualNodeUID());
		lowerEventGate.postEvent(e);
	}
	
	@Override
	protected void terminate() 
	{
		higherEventGate.removeListener(this);
		super.terminate();
	}
}
