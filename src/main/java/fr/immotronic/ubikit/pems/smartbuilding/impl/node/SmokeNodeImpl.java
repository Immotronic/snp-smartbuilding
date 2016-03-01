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

import fr.immotronic.ubikit.pems.enocean.EnoceanEquipmentProfileV20;
import fr.immotronic.ubikit.pems.enocean.EnoceanSensorDevice;
import fr.immotronic.ubikit.pems.enocean.data.Eltako_FRW_ws_SmokeAlarmData;
import fr.immotronic.ubikit.pems.smartbuilding.AbstractNode;
import fr.immotronic.ubikit.pems.smartbuilding.NodeProfile;
import fr.immotronic.ubikit.pems.smartbuilding.SmokeNode;
import fr.immotronic.ubikit.pems.smartbuilding.event.out.SmokeDetectedEvent;
import fr.immotronic.ubikit.pems.smartbuilding.event.out.SmokeNoLongerDetectedEvent;
import fr.immotronic.ubikit.pems.smartbuilding.impl.LC;
import fr.immotronic.ubikit.pems.smartbuilding.impl.PemLauncher;

public class SmokeNodeImpl extends AbstractNode implements SmokeNode, fr.immotronic.ubikit.pems.enocean.event.out.SmokeAlarmOnEvent.Listener, fr.immotronic.ubikit.pems.enocean.event.out.SmokeAlarmOffEvent.Listener
{
	private static final String[] supportedCapabilities = { EnoceanEquipmentProfileV20.ELTAKO_FRW_WS_SMOKE_ALARM.name()};
	
	public static SmokeNodeImpl createInstanceIfAppropriate(PemLocalID pemID, PhysicalEnvironmentItem actualNode, EventGate lowerEventGate, EventGate higherEventGate, PemLauncher pem)
	{
		if(AbstractNode.isCapabilitiesSupported(pemID, supportedCapabilities, actualNode.getCapabilities()))
		{
			if(pem.getItem(makeUID(actualNode.getUID())) == null)
			{
				if(LC.debug) {
					Logger.debug(LC.gi(), null, "createInstanceIfAppropriate(): create a smoke node ("+actualNode.getUID()+", "+actualNode.getPropertyValue(PhysicalEnvironmentItem.Property.CustomName.name())+")");
				}
				
				return new SmokeNodeImpl(pemID, actualNode, lowerEventGate, higherEventGate);
			}
		}
		
		return null;
	}
	
	private EnoceanEquipmentProfileV20 eep;
	
	private static String makeUID(String actualNodeUID)
	{
		return NodeProfile.SMOKE+"_"+actualNodeUID;
	}
	
	public SmokeNodeImpl(PemLocalID pem, PhysicalEnvironmentItem actualNode, EventGate lowerEventGate, EventGate higherEventGate)
	{
		this(makeUID(actualNode.getUID()), pem, actualNode, lowerEventGate, higherEventGate);
	}
	
	public SmokeNodeImpl(String uid, PemLocalID pem, PhysicalEnvironmentItem actualNode, EventGate lowerEventGate, EventGate higherEventGate)
	{
		super(NodeProfile.SMOKE, Type.SENSOR, null, uid, pem, actualNode, lowerEventGate, higherEventGate);
		
		switch(pem)
		{
			case ENOCEAN:
				EnoceanSensorDevice enoceanNode = (EnoceanSensorDevice) actualNode;
				eep = enoceanNode.getEnoceanEquipmentProfile();
				break;
		}
		
		lowerEventGate.addListener(this);
	}
	
	
	@Override
	public Smoke getSmokeDetection()
	{
		if(isLocal()) 
		{
			switch(getPemLocalID())
			{
				case ENOCEAN:
					
					switch(eep)
					{
						case ELTAKO_FRW_WS_SMOKE_ALARM: {
							Eltako_FRW_ws_SmokeAlarmData data = (Eltako_FRW_ws_SmokeAlarmData) getActualNodeValue();
							if(data == null) {
								return Smoke.UNKNOWN;
							}
							switch(data.getSmokeAlarmStatus())
							{
								case UNKNOWN: return Smoke.UNKNOWN;
								case ON: return Smoke.YES;
								case OFF: return Smoke.NO;
							}}
						
						default:
							break;
					}
					break;
			}
		}
		
		return Smoke.UNKNOWN;
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
	public void onEvent(fr.immotronic.ubikit.pems.enocean.event.out.SmokeAlarmOffEvent event) 
	{
		if(event.getSourceItemUID().equals(getActualNodeUID()))
		{
			SmokeNoLongerDetectedEvent e = new SmokeNoLongerDetectedEvent(getUID(), event.getDate());
			higherEventGate.postEvent(e);
		}
	}

	@Override
	public void onEvent(fr.immotronic.ubikit.pems.enocean.event.out.SmokeAlarmOnEvent event) 
	{
		if(event.getSourceItemUID().equals(getActualNodeUID()))
		{
			SmokeDetectedEvent e = new SmokeDetectedEvent(getUID(), event.getDate());
			higherEventGate.postEvent(e);
		}
	}
	
	@Override
	protected void terminate() 
	{
		lowerEventGate.removeListener(this);
		super.terminate();
	}

}
