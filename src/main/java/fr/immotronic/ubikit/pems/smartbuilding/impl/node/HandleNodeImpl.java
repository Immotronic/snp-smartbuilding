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
import fr.immotronic.ubikit.pems.enocean.data.EEP051000Data;
import fr.immotronic.ubikit.pems.smartbuilding.AbstractNode;
import fr.immotronic.ubikit.pems.smartbuilding.HandleNode;
import fr.immotronic.ubikit.pems.smartbuilding.NodeProfile;
import fr.immotronic.ubikit.pems.smartbuilding.event.out.WindowHandleClosedEvent;
import fr.immotronic.ubikit.pems.smartbuilding.event.out.WindowHandleOpenedHorizontallyEvent;
import fr.immotronic.ubikit.pems.smartbuilding.event.out.WindowHandleOpenedVerticallyEvent;
import fr.immotronic.ubikit.pems.smartbuilding.impl.LC;
import fr.immotronic.ubikit.pems.smartbuilding.impl.PemLauncher;

public class HandleNodeImpl extends AbstractNode implements HandleNode, fr.immotronic.ubikit.pems.enocean.event.out.WindowHandleClosedEvent.Listener, fr.immotronic.ubikit.pems.enocean.event.out.WindowHandleOpenedHorizontallyEvent.Listener, fr.immotronic.ubikit.pems.enocean.event.out.WindowHandleOpenedVerticallyEvent.Listener
{
	private static final String[] supportedCapabilities = { EnoceanEquipmentProfileV20.EEP_05_10_00.name()};
	
	public static HandleNodeImpl createInstanceIfAppropriate(PemLocalID pemID, PhysicalEnvironmentItem actualNode, EventGate lowerEventGate, EventGate higherEventGate, PemLauncher pem)
	{
		if(AbstractNode.isCapabilitiesSupported(pemID, supportedCapabilities, actualNode.getCapabilities()))
		{
			if(pem.getItem(makeUID(actualNode.getUID())) == null)
			{
				if(LC.debug) {
					Logger.debug(LC.gi(), null, "createInstanceIfAppropriate(): create a handle node ("+actualNode.getUID()+", "+actualNode.getPropertyValue(PhysicalEnvironmentItem.Property.CustomName.name())+")");
				}
				
				return new HandleNodeImpl(pemID, actualNode, lowerEventGate, higherEventGate);
			}
		}
		
		return null;
	}
	
	private EnoceanEquipmentProfileV20 eep;
	
	private static String makeUID(String actualNodeUID)
	{
		return NodeProfile.HANDLE+"_"+actualNodeUID;
	}
	
	public HandleNodeImpl(PemLocalID pem, PhysicalEnvironmentItem actualNode, EventGate lowerEventGate, EventGate higherEventGate)
	{
		this(makeUID(actualNode.getUID()), pem, actualNode, lowerEventGate, higherEventGate);
	}
	
	public HandleNodeImpl(String uid, PemLocalID pem, PhysicalEnvironmentItem actualNode, EventGate lowerEventGate, EventGate higherEventGate)
	{
		super(NodeProfile.HANDLE, Type.SENSOR, null, uid, pem, actualNode, lowerEventGate, higherEventGate);
		
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
	public Position getPosition() 
	{
		if(isLocal()) 
		{
			switch(getPemLocalID())
			{
				case ENOCEAN:
					
					switch(eep)
					{
						case EEP_05_10_00: {
							EEP051000Data data = (EEP051000Data) getActualNodeValue();
							if(data == null) {
								return Position.UNKNOWN;
							}
							switch(data.getHandleState())
							{
								case UNKNOWN: return Position.UNKNOWN;
								case CLOSED: return Position.CLOSED;
								case OPEN_HORIZONTAL: return Position.OPENED_HORIZONTAL;
								case OPEN_VERTICAL: return Position.OPENED_VERTICAL;
							}}
						
						default:
							break;
					}
					break;
			}
		}
		
		return Position.UNKNOWN;
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
	public void onEvent(fr.immotronic.ubikit.pems.enocean.event.out.WindowHandleClosedEvent event) 
	{
		if(event.getSourceItemUID().equals(getActualNodeUID()))
		{
			WindowHandleClosedEvent e = new WindowHandleClosedEvent(getUID(), event.getDate());
			higherEventGate.postEvent(e);
		}
	}

	@Override
	public void onEvent(fr.immotronic.ubikit.pems.enocean.event.out.WindowHandleOpenedHorizontallyEvent event) 
	{
		if(event.getSourceItemUID().equals(getActualNodeUID()))
		{
			WindowHandleOpenedHorizontallyEvent e = new WindowHandleOpenedHorizontallyEvent(getUID(), event.getDate());
			higherEventGate.postEvent(e);
		}
	}
	
	@Override
	public void onEvent(fr.immotronic.ubikit.pems.enocean.event.out.WindowHandleOpenedVerticallyEvent event) 
	{
		if(event.getSourceItemUID().equals(getActualNodeUID()))
		{
			WindowHandleOpenedVerticallyEvent e = new WindowHandleOpenedVerticallyEvent(getUID(), event.getDate());
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
