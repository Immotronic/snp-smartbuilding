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

import fr.immotronic.ubikit.pems.enocean.EnoceanSensorDevice;
import fr.immotronic.ubikit.pems.enocean.EnoceanEquipmentProfileV20;
import fr.immotronic.ubikit.pems.enocean.data.EEP0702xxData;
import fr.immotronic.ubikit.pems.enocean.data.EEP070401Data;
import fr.immotronic.ubikit.pems.enocean.data.EEP070402Data;
import fr.immotronic.ubikit.pems.enocean.data.EEP0708xxData;
import fr.immotronic.ubikit.pems.enocean.data.EEP070904Data;
import fr.immotronic.ubikit.pems.enocean.data.EEP0710xxData;
import fr.immotronic.ubikit.pems.smartbuilding.AbstractNode;
import fr.immotronic.ubikit.pems.smartbuilding.NodeProfile;
import fr.immotronic.ubikit.pems.smartbuilding.TemperatureNode;
import fr.immotronic.ubikit.pems.smartbuilding.event.out.TemperatureEvent;
import fr.immotronic.ubikit.pems.smartbuilding.impl.LC;
import fr.immotronic.ubikit.pems.smartbuilding.impl.PemLauncher;

import org.ubikit.Logger;

public final class TemperatureNodeImpl extends AbstractNode implements TemperatureNode, fr.immotronic.ubikit.pems.enocean.event.out.TemperatureEvent.Listener
{
	private static final String[] supportedCapabilities = { EnoceanEquipmentProfileV20.EEP_07_02_01.name(),
															EnoceanEquipmentProfileV20.EEP_07_02_02.name(),
															EnoceanEquipmentProfileV20.EEP_07_02_03.name(),
															EnoceanEquipmentProfileV20.EEP_07_02_04.name(),
															EnoceanEquipmentProfileV20.EEP_07_02_05.name(),
															EnoceanEquipmentProfileV20.EEP_07_02_06.name(),
															EnoceanEquipmentProfileV20.EEP_07_02_07.name(),
															EnoceanEquipmentProfileV20.EEP_07_02_08.name(),
															EnoceanEquipmentProfileV20.EEP_07_02_09.name(),
															EnoceanEquipmentProfileV20.EEP_07_02_0A.name(),
															EnoceanEquipmentProfileV20.EEP_07_02_0B.name(),
															EnoceanEquipmentProfileV20.EEP_07_02_10.name(),
															EnoceanEquipmentProfileV20.EEP_07_02_11.name(),
															EnoceanEquipmentProfileV20.EEP_07_02_12.name(),
															EnoceanEquipmentProfileV20.EEP_07_02_13.name(),
															EnoceanEquipmentProfileV20.EEP_07_02_14.name(),
															EnoceanEquipmentProfileV20.EEP_07_02_15.name(),
															EnoceanEquipmentProfileV20.EEP_07_02_16.name(),
															EnoceanEquipmentProfileV20.EEP_07_02_17.name(),
															EnoceanEquipmentProfileV20.EEP_07_02_18.name(),
															EnoceanEquipmentProfileV20.EEP_07_02_19.name(),
															EnoceanEquipmentProfileV20.EEP_07_02_1A.name(),
															EnoceanEquipmentProfileV20.EEP_07_02_1B.name(),
															EnoceanEquipmentProfileV20.EEP_07_04_01.name(),
															EnoceanEquipmentProfileV20.EEP_07_04_02.name(),
															EnoceanEquipmentProfileV20.EEP_07_08_01.name(),
															EnoceanEquipmentProfileV20.EEP_07_09_04.name(),
															EnoceanEquipmentProfileV20.EEP_07_10_01.name(),
															EnoceanEquipmentProfileV20.EEP_07_10_02.name(),
															EnoceanEquipmentProfileV20.EEP_07_10_03.name(),
															EnoceanEquipmentProfileV20.EEP_07_10_04.name(),
															EnoceanEquipmentProfileV20.EEP_07_10_05.name(),
															EnoceanEquipmentProfileV20.EEP_07_10_06.name(),
															EnoceanEquipmentProfileV20.EEP_07_10_07.name(),
															EnoceanEquipmentProfileV20.EEP_07_10_08.name(),
															EnoceanEquipmentProfileV20.EEP_07_10_09.name(),
															EnoceanEquipmentProfileV20.EEP_07_10_10.name(),
															EnoceanEquipmentProfileV20.EEP_07_10_11.name(),
															EnoceanEquipmentProfileV20.EEP_07_10_12.name(),
															EnoceanEquipmentProfileV20.EEP_07_10_13.name(),
															EnoceanEquipmentProfileV20.EEP_07_10_14.name() };
	
	public static TemperatureNodeImpl createInstanceIfAppropriate(PemLocalID pemID, PhysicalEnvironmentItem actualNode, EventGate lowerEventGate, EventGate higherEventGate, PemLauncher pem)
	{
		if(AbstractNode.isCapabilitiesSupported(pemID, supportedCapabilities, actualNode.getCapabilities()))
		{
			if(pem.getItem(makeUID(actualNode.getUID())) == null)
			{
				if(LC.debug) {
					Logger.debug(LC.gi(), null, "createInstanceIfAppropriate(): create a temperature node ("+actualNode.getUID()+", "+actualNode.getPropertyValue(PhysicalEnvironmentItem.Property.CustomName.name())+")");
				}
				
				return new TemperatureNodeImpl(pemID, actualNode, lowerEventGate, higherEventGate);
			}
		}
		
		return null;
	}
	
	private EnoceanEquipmentProfileV20 eep;
	
	private static String makeUID(String actualNodeUID)
	{
		return NodeProfile.TEMPERATURE+"_"+actualNodeUID;
	}

	public TemperatureNodeImpl(PemLocalID pem, PhysicalEnvironmentItem actualNode, EventGate lowerEventGate, EventGate higherEventGate)
	{
		this(makeUID(actualNode.getUID()), pem, actualNode, lowerEventGate, higherEventGate);
	}
	
	public TemperatureNodeImpl(String uid, PemLocalID pem, PhysicalEnvironmentItem actualNode, EventGate lowerEventGate, EventGate higherEventGate)
	{
		super(NodeProfile.TEMPERATURE, Type.SENSOR, null, uid, pem, actualNode, lowerEventGate, higherEventGate);
		
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
	public float getTemperature() 
	{
		if(isLocal()) 
		{
			switch(getPemLocalID())
			{
				case ENOCEAN:
					
					switch(eep)
					{
						case EEP_07_02_01:
						case EEP_07_02_02:
						case EEP_07_02_03:
						case EEP_07_02_04:
						case EEP_07_02_05:
						case EEP_07_02_06:
						case EEP_07_02_07:
						case EEP_07_02_08:
						case EEP_07_02_09:
						case EEP_07_02_0A:
						case EEP_07_02_0B:
						case EEP_07_02_10:
						case EEP_07_02_11:
						case EEP_07_02_12:
						case EEP_07_02_13:
						case EEP_07_02_14:
						case EEP_07_02_15:
						case EEP_07_02_16:
						case EEP_07_02_17:
						case EEP_07_02_18:
						case EEP_07_02_19:
						case EEP_07_02_1A:
						case EEP_07_02_1B: {
							EEP0702xxData data = (EEP0702xxData) getActualNodeValue();
							if(data == null) {
								return Float.MIN_VALUE;
							}
							return data.getTemperature();
						}
						
						case EEP_07_04_01: {
							EEP070401Data data = (EEP070401Data) getActualNodeValue();
							if(data == null) {
								return Float.MIN_VALUE;
							}
							return data.getTemperature();
						}
						
						case EEP_07_04_02: {
							EEP070402Data data = (EEP070402Data) getActualNodeValue();
							if(data == null) {
								return Float.MIN_VALUE;
							}
							return data.getTemperature();
						}
						
						case EEP_07_08_01: {
							EEP0708xxData data = (EEP0708xxData) getActualNodeValue();
							if(data == null) {
								return Float.MIN_VALUE;
							}
							return data.getTemperature();
						}
						
						case EEP_07_09_04: {
							EEP070904Data data = (EEP070904Data) getActualNodeValue();
							if(data == null) {
								return Float.MIN_VALUE;
							}
							return data.getTemperature();
						}
						
						case EEP_07_10_01:
						case EEP_07_10_02:
						case EEP_07_10_03:
						case EEP_07_10_04:
						case EEP_07_10_05:
						case EEP_07_10_06:
						case EEP_07_10_07:
						case EEP_07_10_08:
						case EEP_07_10_09:
						case EEP_07_10_10:
						case EEP_07_10_11:
						case EEP_07_10_12:
						case EEP_07_10_13:
						case EEP_07_10_14:{
							EEP0710xxData data = (EEP0710xxData) getActualNodeValue();
							if(data == null) {
								return Float.MIN_VALUE;
							}
							return data.getTemperature();
						}
						
						default:
							break;
					}
					break;
			}
		}
		
		return Float.MIN_VALUE;
	}

	@Override
	public void onEvent(fr.immotronic.ubikit.pems.enocean.event.out.TemperatureEvent event) 
	{
		if(event.getSourceItemUID().equals(getActualNodeUID()))
		{
			TemperatureEvent te = new TemperatureEvent(getUID(), event.getTemperature(), event.getDate());
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
