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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ubikit.PhysicalEnvironmentItem;
import org.ubikit.event.EventGate;
import org.ubikit.service.PhysicalEnvironmentModelService;

import fr.immotronic.ubikit.pems.enocean.EnoceanEquipmentProfileV20;
import fr.immotronic.ubikit.pems.enocean.EnoceanSensorAndActuatorDevice;
import fr.immotronic.ubikit.pems.enocean.EnoceanSensorDevice;
import fr.immotronic.ubikit.pems.enocean.SensorActuatorProfile;
import fr.immotronic.ubikit.pems.enocean.data.EEPA51200Data;
import fr.immotronic.ubikit.pems.enocean.data.EEPA512xxData;
import fr.immotronic.ubikit.pems.enocean.data.EEPD201xxData;
import fr.immotronic.ubikit.pems.enocean.event.in.ActuatorUpdateEvent;
import fr.immotronic.ubikit.pems.enocean.event.out.MeteringCounterEvent;
import fr.immotronic.ubikit.pems.smartbuilding.event.out.MeteringEvent.MeasurementUnit;
import fr.immotronic.ubikit.pems.smartbuilding.AbstractNode;
import fr.immotronic.ubikit.pems.smartbuilding.MeteringNode;
import fr.immotronic.ubikit.pems.smartbuilding.NodeProfile;
import fr.immotronic.ubikit.pems.smartbuilding.event.out.MeteringEvent;
import fr.immotronic.ubikit.pems.smartbuilding.impl.LC;
import fr.immotronic.ubikit.pems.smartbuilding.impl.PemLauncher;

import org.ubikit.Logger;

public class MeteringNodeImpl extends AbstractNode implements MeteringNode, MeteringCounterEvent.Listener, fr.immotronic.ubikit.pems.enocean.event.out.MeteringEvent.Listener
{
	private static final String defaultSupportedChannels = "[ 1, 2, 3 ]";
	private static final int MAX_CHANNELS_NUMBER = 16;
	
	private static final String[] supportedCapabilities = { EnoceanEquipmentProfileV20.EEP_A5_12_00.name(),
															EnoceanEquipmentProfileV20.EEP_A5_12_01.name(),
															EnoceanEquipmentProfileV20.EEP_A5_12_02.name(),
															EnoceanEquipmentProfileV20.EEP_A5_12_03.name(),
															SensorActuatorProfile.EEP_D2_01_00.name(),
															SensorActuatorProfile.EEP_D2_01_02.name(),
															SensorActuatorProfile.EEP_D2_01_06.name()
														  };
	
	
	
	public static Collection<AbstractNode> createInstancesIfAppropriate(PemLocalID pemID, PhysicalEnvironmentItem actualNode, EventGate lowerEventGate, EventGate higherEventGate, PemLauncher pem)
	{
		Collection<AbstractNode> nodes = new ArrayList<AbstractNode>();
		if(AbstractNode.isCapabilitiesSupported(pemID, supportedCapabilities, actualNode.getCapabilities()))
		{
			if(!isNodeAlreadyAdded(pem, pemID, actualNode))
			{
				if(LC.debug) {
					Logger.debug(LC.gi(), null, "createInstancesIfAppropriate(): create metering node(s) ("+actualNode.getUID()+", "+actualNode.getPropertyValue(PhysicalEnvironmentItem.Property.CustomName.name())+")");
				}
				
				switch(pemID)
				{
					case ENOCEAN:
						
						if (actualNode.getType() == Type.SENSOR && ((EnoceanSensorDevice) actualNode).getEnoceanEquipmentProfile() == EnoceanEquipmentProfileV20.EEP_A5_12_00)
						{
							// TODO nothing is implemented yet in PEM Enocean for this, we should get from it 
							// the supported channels of the node by reading its properties.
							// String supportedChannels = actualNode.getPropertyValue("supportedChannels");
							String supportedChannels = defaultSupportedChannels;
							if (supportedChannels != null)
							{
								try
								{
									JSONArray channels = new JSONArray(supportedChannels);
									for (int i = 0; i < channels.length(); i++)
									{
										nodes.add(new MeteringNodeImpl(pemID, actualNode, channels.getInt(i), lowerEventGate, higherEventGate));
									}
								}
								catch (JSONException e)
								{
									Logger.error(LC.gi(), null, "Error when reading a JSON property of Metering Node. This should never happen. THis is a bug to fix.");
									return null;
								}
							}
						}
						else
							nodes.add(new MeteringNodeImpl(pemID, actualNode, -1, lowerEventGate, higherEventGate));
						
						break;
				}
				return nodes;
			}
		}
		
		return null;
	}
	
	private static boolean isNodeAlreadyAdded(PhysicalEnvironmentModelService pem, PemLocalID pemID, PhysicalEnvironmentItem actualNode)
	{
		switch(pemID)
		{
			case ENOCEAN:
				
				if (actualNode.getType() == Type.SENSOR && ((EnoceanSensorDevice) actualNode).getEnoceanEquipmentProfile() == EnoceanEquipmentProfileV20.EEP_A5_12_00)
				{
					for (int i = 0; i < MAX_CHANNELS_NUMBER; i++)
					{
						if (pem.getItem(makeUID(pemID, actualNode, i)) != null)
							return true;
					}
					return false;
				}
				break;
		}
		
		return pem.getItem(makeUID(pemID, actualNode, 0)) != null;
	}
	
	private EnoceanEquipmentProfileV20 eep = null;
	private SensorActuatorProfile sap = null;
	private final int channel;
	
	private static String makeUID(PemLocalID pem, PhysicalEnvironmentItem actualNode, int channel)
	{
		switch(pem)
		{
			case ENOCEAN:
				if (actualNode.getType() == Type.SENSOR && ((EnoceanSensorDevice) actualNode).getEnoceanEquipmentProfile() == EnoceanEquipmentProfileV20.EEP_A5_12_00)
				{
					return NodeProfile.METERING+"_"+channel+"_"+actualNode.getUID();
				}
				break;
		}
		
		return NodeProfile.METERING+"_"+actualNode.getUID();
	}
	
	private int getChannelfromUID(PemLocalID pem, PhysicalEnvironmentItem actualNode, String uid)
	{
		switch(pem)
		{
			case ENOCEAN:
				
				if (actualNode.getType() == Type.SENSOR && ((EnoceanSensorDevice) actualNode).getEnoceanEquipmentProfile() == EnoceanEquipmentProfileV20.EEP_A5_12_00)
				{
						return Integer.parseInt(uid.split("_")[1]);
				}
				break;
		}
		return -1;
	}
	
	public String getName()
	{
		if (channel != -1)
			return super.getName()+" - Channel "+channel;
		else
			return super.getName();
	}
	
	public MeteringNodeImpl(PemLocalID pem, PhysicalEnvironmentItem actualNode, int channel, EventGate lowerEventGate, EventGate higherEventGate)
	{
		this(makeUID(pem, actualNode, channel), pem, actualNode, lowerEventGate, higherEventGate);
	}
	
	public MeteringNodeImpl(String uid, PemLocalID pem, PhysicalEnvironmentItem actualNode, EventGate lowerEventGate, EventGate higherEventGate)
	{
		super(NodeProfile.METERING, Type.SENSOR, null, uid, pem, actualNode, lowerEventGate, higherEventGate);
		
		switch(pem)
		{
			case ENOCEAN:
				if (actualNode.getType() == Type.SENSOR)
				{
					EnoceanSensorDevice enoceanNode = (EnoceanSensorDevice) actualNode;
					eep = enoceanNode.getEnoceanEquipmentProfile();
				}
				else if (actualNode.getType() == Type.SENSOR_AND_ACTUATOR)
				{
					EnoceanSensorAndActuatorDevice enoceanSANode = (EnoceanSensorAndActuatorDevice) actualNode;
					sap = enoceanSANode.getSensorActuatorProfile();
				}
				break;
		}
		
		this.channel = getChannelfromUID(pem, actualNode, uid);
		
		lowerEventGate.addListener(this);
	}

	@Override
	public float getInstantValue()
	{
		if(isLocal()) 
		{
			switch(getPemLocalID())
			{
				case ENOCEAN:
					
					if (eep != null)
					{
						switch(eep)
						{
							case EEP_A5_12_00:
								EEPA51200Data eepA51200Data = (EEPA51200Data) getActualNodeValue();
								if(eepA51200Data == null) {
									return Integer.MIN_VALUE;
								}
								return eepA51200Data.getCurrentValue(channel);
							case EEP_A5_12_01:
							case EEP_A5_12_02:
							case EEP_A5_12_03:
								EEPA512xxData eepA512xxData = (EEPA512xxData) getActualNodeValue();
								if(eepA512xxData == null) {
									return Integer.MIN_VALUE;
								}
								return eepA512xxData.getInstantValue();
							default:
								break;
						}
					}
					
					if (sap != null)
					{
						switch(sap)
						{
							case EEP_D2_01_00:
							case EEP_D2_01_02:
							case EEP_D2_01_06:
								return Integer.MIN_VALUE;
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
	public MeasurementUnit getInstantValueUnit()
	{
		if(isLocal()) 
		{
			switch(getPemLocalID())
			{
				case ENOCEAN:
					
					if (eep != null)
					{
						switch(eep)
						{
							case EEP_A5_12_00:
								return MeasurementUnit.UNIT_PER_SECOND;
							case EEP_A5_12_01:
								return MeasurementUnit.WATT;
							case EEP_A5_12_02:
							case EEP_A5_12_03:
								return MeasurementUnit.LITRE_PER_SECOND;
							default:
								break;
						}
					}
					
					if (sap != null)
					{
						switch(sap)
						{
							case EEP_D2_01_00:
							case EEP_D2_01_02:
							case EEP_D2_01_06:
								return MeasurementUnit.UNKNOWN;
							default:
								break;
						}
					}
					
					break;
			}
		}
		
		return MeasurementUnit.UNKNOWN;
	}
	
	@Override
	public float getCumulativeValue()
	{
		if(isLocal()) 
		{
			switch(getPemLocalID())
			{
				case ENOCEAN:
					
					if (eep != null)
					{
						switch(eep)
						{
							case EEP_A5_12_00:
								EEPA51200Data eepA51200Data = (EEPA51200Data) getActualNodeValue();
								if(eepA51200Data == null) {
									return Integer.MIN_VALUE;
								}
								return eepA51200Data.getCumulativeValue(channel);
							case EEP_A5_12_01:
							case EEP_A5_12_02:
							case EEP_A5_12_03:
								EEPA512xxData eepA512xxData = (EEPA512xxData) getActualNodeValue();
								if(eepA512xxData == null) {
									return Integer.MIN_VALUE;
								}
								return eepA512xxData.getCumulativeValue();
							default:
								break;
						}
					}
					
					if (sap != null)
					{
						switch(sap)
						{
							case EEP_D2_01_00:
							case EEP_D2_01_02:
							case EEP_D2_01_06:
								EEPD201xxData eepD201xxData = (EEPD201xxData) getActualNodeValue();
								if(eepD201xxData == null) {
									return Integer.MIN_VALUE;
								}
								return eepD201xxData.getMeasurementValue();
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
	public MeasurementUnit getCumulativeValueUnit()
	{
		if(isLocal()) 
		{
			switch(getPemLocalID())
			{
				case ENOCEAN:
					
					if (eep != null)
					{
						switch(eep)
						{
							case EEP_A5_12_00:
								return MeasurementUnit.UNIT;
							case EEP_A5_12_01:
								return MeasurementUnit.KILOWATT_HOUR;
							case EEP_A5_12_02:
							case EEP_A5_12_03:
								return MeasurementUnit.CUBIC_METRE;
							default:
								break;
						}
					}
					
					if (sap != null)
					{
						switch(sap)
						{
							case EEP_D2_01_00:
							case EEP_D2_01_02:
							case EEP_D2_01_06:
								EEPD201xxData eepD201xxData = (EEPD201xxData) getActualNodeValue();
								if(eepD201xxData == null) {
									return MeasurementUnit.UNKNOWN;
								}
								try	{ return MeasurementUnit.valueOf(eepD201xxData.getMeasurementUnit().name()); } 
								catch (IllegalArgumentException e) { return MeasurementUnit.UNKNOWN; }
							default:
								break;
						}
					}
					
					break;
			}
		}
		
		return MeasurementUnit.UNKNOWN;
	}
	
	@Override
	public MeteringType getMeterReadingType()
	{
		if(isLocal()) 
		{
			switch(getPemLocalID())
			{
				case ENOCEAN:
					
					if (eep != null)
					{
						switch(eep)
						{
							case EEP_A5_12_01:
							case EEP_A5_12_02:
							case EEP_A5_12_03:
								EEPA512xxData data = (EEPA512xxData) getActualNodeValue();
								if(data == null) {
									return null;
								}
								switch (data.getMeterReadingType())
								{
									case Electricity : return MeteringType.Electricity;
									case Gas : return MeteringType.Gas;
									case Water : return MeteringType.Water;
								}
								return null;
							case EEP_A5_12_00:
							default:
								return null;
						}
					}
					
					if (sap != null)
					{
						switch(sap)
						{
							case EEP_D2_01_00:
							case EEP_D2_01_02:
							case EEP_D2_01_06:
								return MeteringType.Electricity;
							default:
								break;
						}
					}
					
					break;
			}
		}
		
		return null;
	}
	
	@Override
	public boolean hasAutoReporting()
	{
		if(isLocal()) 
		{
			switch(getPemLocalID())
			{
				case ENOCEAN:
					
					if (eep != null)
					{
						switch(eep)
						{
							default:
								return true;
						}
					}
					
					if (sap != null)
					{
						switch(sap)
						{
							case EEP_D2_01_00:
							case EEP_D2_01_02:
							case EEP_D2_01_06:
								return false;
							default:
								break;
						}
					}
					
					break;
			}
		}
		
		return true;
	}
	public void requestNewMeasurement()
	{
		if(isLocal()) 
		{
			switch(getPemLocalID())
			{
				case ENOCEAN:
					
					if (sap != null)
					{
						switch(sap)
						{
							case EEP_D2_01_00:
							case EEP_D2_01_02:
							case EEP_D2_01_06:
								ActuatorUpdateEvent e = new ActuatorUpdateEvent(getActualNodeUID());
								lowerEventGate.postEvent(e);
								break;
							default:
								break;
						}
					}
					break;
			}
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
	public void onEvent(MeteringCounterEvent event)
	{
		if(event.getSourceItemUID().equals(getActualNodeUID()))
		{
			if (event.getChannel() == channel)
			{
				MeasurementUnit unit = event.isACumulativeMeasuredValue() ? MeasurementUnit.UNIT : MeasurementUnit.UNIT_PER_SECOND;
				MeteringEvent e = new MeteringEvent(getUID(), event.getCounterValue(), unit, event.getDate());
				higherEventGate.postEvent(e);
			}
		}
	}
	
	@Override
	public void onEvent(fr.immotronic.ubikit.pems.enocean.event.out.MeteringEvent event)
	{
		if(event.getSourceItemUID().equals(getActualNodeUID()))
		{
			MeteringEvent e = new MeteringEvent(getUID(), event.getValue(), MeteringEvent.MeasurementUnit.valueOf(event.getMeasurementUnit().name()), event.getDate());
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