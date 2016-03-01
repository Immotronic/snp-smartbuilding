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
import fr.immotronic.ubikit.pems.enocean.data.EEP0710xxData;
import fr.immotronic.ubikit.pems.smartbuilding.AbstractNode;
import fr.immotronic.ubikit.pems.smartbuilding.NodeProfile;
import fr.immotronic.ubikit.pems.smartbuilding.SetPointNode;
import fr.immotronic.ubikit.pems.smartbuilding.event.out.SetPointChangedEvent;
import fr.immotronic.ubikit.pems.smartbuilding.impl.LC;
import fr.immotronic.ubikit.pems.smartbuilding.impl.PemLauncher;

public class SetPointNodeImpl extends AbstractNode implements SetPointNode, fr.immotronic.ubikit.pems.enocean.event.out.SetPointChangedEvent.Listener
{
	private static final float setPointMultiplier = (100f / 255f);
	
	private static final String[] supportedCapabilities = { EnoceanEquipmentProfileV20.EEP_07_10_01.name(),
																EnoceanEquipmentProfileV20.EEP_07_10_02.name(),
																EnoceanEquipmentProfileV20.EEP_07_10_03.name(),
																EnoceanEquipmentProfileV20.EEP_07_10_04.name(),
																EnoceanEquipmentProfileV20.EEP_07_10_05.name(),
																EnoceanEquipmentProfileV20.EEP_07_10_06.name(),
																EnoceanEquipmentProfileV20.EEP_07_10_10.name(),
																EnoceanEquipmentProfileV20.EEP_07_10_11.name(),
																EnoceanEquipmentProfileV20.EEP_07_10_12.name() };
		
	public static SetPointNodeImpl createInstanceIfAppropriate(PemLocalID pemID, PhysicalEnvironmentItem actualNode, EventGate lowerEventGate, EventGate higherEventGate, PemLauncher pem)
	{
		if(AbstractNode.isCapabilitiesSupported(pemID, supportedCapabilities, actualNode.getCapabilities()))
		{
			if(pem.getItem(makeUID(actualNode.getUID())) == null)
			{
				if(LC.debug) {
					Logger.debug(LC.gi(), null, "createInstanceIfAppropriate(): create a set point node ("+actualNode.getUID()+", "+actualNode.getPropertyValue(PhysicalEnvironmentItem.Property.CustomName.name())+")");
				}
				
				return new SetPointNodeImpl(pemID, actualNode, lowerEventGate, higherEventGate);
			}
		}
		
		return null;
	}
		
	private EnoceanEquipmentProfileV20 eep;
		
	private static String makeUID(String actualNodeUID)
	{
		return NodeProfile.SET_POINT+"_"+actualNodeUID;
	}

	public SetPointNodeImpl(PemLocalID pem, PhysicalEnvironmentItem actualNode, EventGate lowerEventGate, EventGate higherEventGate)
	{
		this(makeUID(actualNode.getUID()), pem, actualNode, lowerEventGate, higherEventGate);
	}
		
	public SetPointNodeImpl(String uid, PemLocalID pem, PhysicalEnvironmentItem actualNode, EventGate lowerEventGate, EventGate higherEventGate)
	{
		super(NodeProfile.SET_POINT, Type.SENSOR, null, uid, pem, actualNode, lowerEventGate, higherEventGate);
			
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
	public int getSetPoint()
	{
		if(isLocal()) 
		{
			switch(getPemLocalID())
			{
				case ENOCEAN:
						
					switch(eep)
					{
						case EEP_07_10_01:
						case EEP_07_10_02:
						case EEP_07_10_03:
						case EEP_07_10_04:
						case EEP_07_10_05:
						case EEP_07_10_06:
						case EEP_07_10_10:
						case EEP_07_10_11:
						case EEP_07_10_12:
							EEP0710xxData data = (EEP0710xxData) getActualNodeValue();
							if(data == null) {
								return -1;
							}
							return (int) (data.getSetPoint() * setPointMultiplier);
							
						default:
							break;
					}
					break;
			}
		}
			
		return -1;
	}

	@Override
	public void onEvent(fr.immotronic.ubikit.pems.enocean.event.out.SetPointChangedEvent event) 
	{
		if(event.getSourceItemUID().equals(getActualNodeUID()))
		{
			SetPointChangedEvent te = new SetPointChangedEvent(getUID(), (int) (event.getSetPoint() * setPointMultiplier), event.getDate());
			higherEventGate.postEvent(te);
		}
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
		lowerEventGate.removeListener(this);
		super.terminate();
	}

}
