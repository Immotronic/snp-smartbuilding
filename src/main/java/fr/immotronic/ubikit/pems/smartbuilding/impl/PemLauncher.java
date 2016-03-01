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

// -----
// TODO: 	Update the package name.
//
// 	Note: 
//		fr.immotronic.ubikit.pems should be something like COUNTRY_CODE.Immotronic.ubikit.apps.
//			For instance: fr.immotronic.ubikit.apps
//
// 		APPLICATION_NAME should be replaced with the value you specified in
// 		build.xml  & packaging_info.bnd files.
// -----
package fr.immotronic.ubikit.pems.smartbuilding.impl; // PACKAGE_PREFIX & PEM_NAME must be replaced.

import java.util.ArrayList;
import java.util.Collection;

import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;
import org.ubikit.AbstractPhysicalEnvironmentModel;
import org.ubikit.Logger;
import org.ubikit.PhysicalEnvironmentItem;
import org.ubikit.PhysicalEnvironmentModelInformations;
import org.ubikit.PhysicalEnvironmentModelObserver;
import org.ubikit.event.EventGate;
import org.ubikit.event.EventListener;
import org.ubikit.pem.event.ItemAddedEvent;
import org.ubikit.pem.event.ItemDroppedEvent;
import org.ubikit.pem.event.ItemPropertiesUpdatedEvent;
import org.ubikit.service.PhysicalEnvironmentModelService;

import fr.immotronic.ubikit.pems.smartbuilding.impl.node.IlluminationNodeImpl;
import fr.immotronic.ubikit.pems.smartbuilding.AbstractNode;
import fr.immotronic.ubikit.pems.smartbuilding.NodeProfile;
import fr.immotronic.ubikit.pems.smartbuilding.impl.node.CarbonDioxideNodeImpl;
import fr.immotronic.ubikit.pems.smartbuilding.impl.node.DimmerNodeImpl;
import fr.immotronic.ubikit.pems.smartbuilding.impl.node.HandleNodeImpl;
import fr.immotronic.ubikit.pems.smartbuilding.impl.node.HVACControllerNodeImpl;
import fr.immotronic.ubikit.pems.smartbuilding.impl.node.KeyCardNodeImpl;
import fr.immotronic.ubikit.pems.smartbuilding.impl.node.MeteringNodeImpl;
import fr.immotronic.ubikit.pems.smartbuilding.impl.node.MotorNodeImpl;
import fr.immotronic.ubikit.pems.smartbuilding.impl.node.OnOffNodeImpl;
import fr.immotronic.ubikit.pems.smartbuilding.impl.node.PemLocalID;
import fr.immotronic.ubikit.pems.smartbuilding.impl.node.PresenceNodeImpl;
import fr.immotronic.ubikit.pems.smartbuilding.impl.node.RelativeHumidityNodeImpl;
import fr.immotronic.ubikit.pems.smartbuilding.impl.node.SetPointNodeImpl;
import fr.immotronic.ubikit.pems.smartbuilding.impl.node.SingleContactNodeImpl;
import fr.immotronic.ubikit.pems.smartbuilding.impl.node.SmokeNodeImpl;
import fr.immotronic.ubikit.pems.smartbuilding.impl.node.SwitchGroupNodeImpl;
import fr.immotronic.ubikit.pems.smartbuilding.impl.node.TemperatureNodeImpl;
import fr.immotronic.ubikit.pems.smartbuilding.impl.node.VolatileOrganicCompoundsNodeImpl;

public final class PemLauncher extends AbstractPhysicalEnvironmentModel implements ItemAddedEvent.Listener, ItemDroppedEvent.Listener, ItemPropertiesUpdatedEvent.Listener
{	
	private static final String[] physicalEnvironmentModelUIDs = {"fr.immotronic.ubikit.pems.enocean"};
	private static final int NUMBER_OF_REQUIRED_CONCURRENT_THREADS = 0;
	
	
	//-------SPECIFIC APP PRIVATE MEMBERS--------------------------
	
	private final String localHostname = "placetouch-XXXXX"; // placetouch-XXXXX MUST be replace by the actual PlaceTouch name to enable the 'distributed' feature.
	private int localConfigurationUID = 0; // Then  2,147,483,647 possible UID.
	private PhysicalEnvironmentModelService pemEnocean;
	private DatabaseManagerImpl database;
	
	//-------END OF SPECIFIC APP PRIVATE MEMBERS-------------------
	
	
	
	public PemLauncher(BundleContext bc)
	{
		super(NUMBER_OF_REQUIRED_CONCURRENT_THREADS, bc, physicalEnvironmentModelUIDs);
	}

	@Override
	protected void start() throws Exception
	{
		pemEnocean = getPhysicalEnvironmentModel(physicalEnvironmentModelUIDs[0]);
		database = new DatabaseManagerImpl(getDatabaseConnection(), localHostname);
		
		boolean nodesWereDropped = loadNodeListFromDatabase();
		boolean nodesWereAdded = lookForAdditionalNodesFromPEMs();
		
		if(nodesWereAdded || nodesWereDropped) {
			updateConfigurationUID();
		}
		else 
		{
			if(LC.debug) {
				Logger.debug(LC.gi(), this, "NodeModel(): no need to update DB");
			}
		}
		
		getLowerAbstractionLevelEventGate().addListener(this);
		
		Logger.info(LC.gi(), this, "PEM smartbuilding did started");
	}


	@Override
	protected void stop() 
	{
		// ----- 
		//
		// TODO: 	Implement here the code to execute just before the
		//			model components become invalid.
		//
		// -----
		Logger.info(LC.gi(), this, "PEM smartbuilding did stopped");
	}
	
	@Override
	public PhysicalEnvironmentItem removeItem(String UID)
	{
		PhysicalEnvironmentItem item = getItem(UID);
		getLowerAbstractionLevelEventGate().removeListener((EventListener) item);
		getHigherAbstractionLevelEventGate().removeListener((EventListener) item);
		return super.removeItem(UID);
	}
	
	@Override
	public PhysicalEnvironmentModelInformations getInformations()
	{
		// ----- 
		// OPTIONAL
		//    TODO: Return a adequate object that implements the 
		//			PhysicalEnvironmentModelInformations interface.
		//			Such object SHOULD carry introspection data about
		//			your PEM state. It is a feature that you can give
		//			to PEM clients to help them know what is going on
		//			in your PEM, or get statistical data, or whatever.
		// -----
		
		return null;
	}
	
	@Override
	public void setObserver(PhysicalEnvironmentModelObserver observer)
	{
		// ----- 
		// OPTIONAL
		//    TODO: If you wish to make your PEM able to emit some log
		//			data to PEM clients, you can use the observer object for
		//			that. Keep its reference wherever you want.
		// -----
	}
	
	/**
	 * @return true if local configuration changed => localConfigurationUID MUST be updated.
	 */
	private boolean loadNodeListFromDatabase()
	{
		boolean localConfigurationChange = false;
		localConfigurationUID = database.getConfigurationUID();
		
		if(LC.debug) {
			Logger.debug(LC.gi(), this, "loadNodeListFromDatabase(): current local Configuration UID is "+localConfigurationUID);
		}
		
		Collection<JSONObject> items = database.getItems();
		if(items != null) 
		{
			for(JSONObject item : items) 
			{
				try 
				{
					String uID = item.getString(AbstractNode.Field.UID.name());
					String actualNodeUID = item.getString(AbstractNode.Field.ACTUAL_NODE_UID.name());
					NodeProfile profile = NodeProfile.valueOf(item.getString(AbstractNode.Field.PROFILE.name()));
					String host = item.getString(AbstractNode.Field.HOST.name());
					PemLocalID pemLocalID = PemLocalID.valueOf(item.getString(AbstractNode.Field.PEM.name()));
					
					AbstractNode node = createNodeFromDatabase(profile, uID, actualNodeUID, host, pemLocalID);
					if(node != null) {
						addItem(node);
					}
					else 
					{
						// Node could NOT be created because node is local && the actual node does not exist anymore. See createNodeFromDatabase().
						// Then, this node MUST be removed from database, and the configuration UID MUST be updated.
						database.removeItem(uID);
						localConfigurationChange = true;
					}
				} 
				catch (JSONException e) 
				{
					Logger.error(LC.gi(), this, "loadNodeListFromDatabase(): While loading node list from database, when populate nodes map.",e);
				}
			}
		}
		else 
		{
			// An error occured while getting items from database. 
			// TODO: An error message SHOULD be notified to the user.
		}
		
		return localConfigurationChange;
	}
	
	/**
	 * @return true if local configuration changed => localConfigurationUID MUST be updated.
	 */
	private boolean lookForAdditionalNodesFromPEMs()
	{
		boolean localConfigurationChange = false;
		
		Collection<PhysicalEnvironmentItem> items = pemEnocean.getAllItems();
		for(PhysicalEnvironmentItem item : items) // For each item in the EnOcean PEM
		{
			Collection<AbstractNode> newNodes = createNodeFromPemItem(PemLocalID.ENOCEAN, item);
			if(newNodes != null) 
			{
				// If the item could be used as one or several nodes in the hotel application (i.e. the item profile is supported)
				for(AbstractNode node : newNodes)
				{
					if(database.addItem(node))
					{
						// If node does NOT already exist in database, this is a new node
						addItem(node);
						localConfigurationChange = true;
					}
				}
			}
		}
		
		return localConfigurationChange;
	}
	
	/**
	 * Increment the configurationUID identifier and store it in database.
	 */
	private void updateConfigurationUID()
	{
		localConfigurationUID++;
		database.updateConfigurationUID(localConfigurationUID);
		
		if(LC.debug) {
			Logger.debug(LC.gi(), this, "updateConfigurationUID(): DB UPDATE current local Configuration UID is NOW "+localConfigurationUID);
		}
	}
	
	/**
	 * Create a new node from a PEM item and return it as an AbstractNode instance.
	 * @param pemLocalID the PEM id of the actual node
	 * @param item a reference onto the actual node instance
	 * @return an instance of a subclass of AbstractNode, or null if actual node capabilities are not supported 
	 */
	private Collection<AbstractNode> createNodeFromPemItem(PemLocalID pemLocalID, PhysicalEnvironmentItem item)
	{
		Collection<AbstractNode> newNodes = new ArrayList<AbstractNode>();
		EventGate lowerEventGate = getLowerAbstractionLevelEventGate();
		EventGate higherEventGate = getHigherAbstractionLevelEventGate();
		
		AbstractNode node = null;
		
		node = CarbonDioxideNodeImpl.createInstanceIfAppropriate(pemLocalID, item, lowerEventGate, higherEventGate, this);
		if(node != null) {	
			newNodes.add(node);
		}
		
		node = DimmerNodeImpl.createInstanceIfAppropriate(pemLocalID, item, lowerEventGate, higherEventGate, this);
		if(node != null) {	
			newNodes.add(node);
		}
		
		node = HandleNodeImpl.createInstanceIfAppropriate(pemLocalID, item, lowerEventGate, higherEventGate, this);
		if(node != null) {	
			newNodes.add(node);
		}
		
		node = HVACControllerNodeImpl.createInstanceIfAppropriate(pemLocalID, item, lowerEventGate, higherEventGate, this);
		if(node != null) {	
			newNodes.add(node);
		}
		
		node = IlluminationNodeImpl.createInstanceIfAppropriate(pemLocalID, item, lowerEventGate, higherEventGate, this);
		if(node != null) {	
			newNodes.add(node);
		}
		
		node = KeyCardNodeImpl.createInstanceIfAppropriate(pemLocalID, item, lowerEventGate, higherEventGate, this);
		if(node != null) {	
			newNodes.add(node);
		}
		
		Collection<AbstractNode> nodes = MeteringNodeImpl.createInstancesIfAppropriate(pemLocalID, item, lowerEventGate, higherEventGate, this);
		if (nodes != null) {
			newNodes.addAll(nodes);
		}
		
		node = MotorNodeImpl.createInstanceIfAppropriate(pemLocalID, item, lowerEventGate, higherEventGate, this);
		if(node != null) {	
			newNodes.add(node);
		}
		
		nodes = OnOffNodeImpl.createInstanceIfAppropriate(pemLocalID, item, lowerEventGate, higherEventGate, this);
		if(nodes != null) {	
			newNodes.addAll(nodes);
		}
		
		node = PresenceNodeImpl.createInstanceIfAppropriate(pemLocalID, item, lowerEventGate, higherEventGate, this);
		if(node != null) {	
			newNodes.add(node);
		}
		
		node = RelativeHumidityNodeImpl.createInstanceIfAppropriate(pemLocalID, item, lowerEventGate, higherEventGate, this);
		if(node != null) {	
			newNodes.add(node);
		}
		
		node = SetPointNodeImpl.createInstanceIfAppropriate(pemLocalID, item, lowerEventGate, higherEventGate, this);
		if(node != null) {	
			newNodes.add(node);
		}
		
		node = SingleContactNodeImpl.createInstanceIfAppropriate(pemLocalID, item, lowerEventGate, higherEventGate, this);
		if(node != null) {	
			newNodes.add(node);
		}
		
		node = SmokeNodeImpl.createInstanceIfAppropriate(pemLocalID, item, lowerEventGate, higherEventGate, this);
		if(node != null) {	
			newNodes.add(node);
		}
		
		node = SwitchGroupNodeImpl.createInstanceIfAppropriate(pemLocalID, item, lowerEventGate, higherEventGate, this);
		if(node != null) {	
			newNodes.add(node);
		}
		
		node = TemperatureNodeImpl.createInstanceIfAppropriate(pemLocalID, item, lowerEventGate, higherEventGate, this);
		if(node != null) {	
			newNodes.add(node);
		}
		
		node = VolatileOrganicCompoundsNodeImpl.createInstanceIfAppropriate(pemLocalID, item, lowerEventGate, higherEventGate, this);
		if(node != null) {	
			newNodes.add(node);
		}
		
		if(LC.debug && newNodes.size() == 0) 
		{
			StringBuilder sb = new StringBuilder("createNodeFromPemItem(): node already exists OR ");
			
			for(String s : item.getCapabilities()) {
				sb.append(s).append(" ");
			}
			
			sb.append("capabilities are NOT yet supported. (").append(item.getUID()).append(", ")
				.append(item.getPropertyValue(PhysicalEnvironmentItem.Property.CustomName.name())).append(")");
		
			Logger.debug(LC.gi(), this, sb.toString());
		}
		
		if(newNodes.size() > 0) {
			return newNodes;
		}
		
		return null;
	}
	
	/**
	 * Create a new node from a database entry and return it as an AbstractNode instance.
	 * @param type the node type
	 * @param uid the node unique identifier (at the Hotel app level)
	 * @param uid the actual node unique identifier
	 * @param host the node host
	 * @param pemLocalID the actual node PEM
	 * @return an instance of a subclass of AbstractNode, or null if actual node has been removed from its PEM.
	 */
	private AbstractNode createNodeFromDatabase(NodeProfile profile, String uid, String actualNodeUID, String host, PemLocalID pemLocalID)
	{
		boolean isLocal = host.equals(localHostname);
		EventGate lowerEventGate = getLowerAbstractionLevelEventGate();
		EventGate higherEventGate = getHigherAbstractionLevelEventGate();
		if(isLocal)
		{
			PhysicalEnvironmentItem item = null;
			switch(pemLocalID) 
			{
				case ENOCEAN:
					item = pemEnocean.getItem(actualNodeUID);
					if(item != null)
					{
						switch(profile)
						{
							case CARBON_DIOXIDE:
							
								if(LC.debug) {
									Logger.debug(LC.gi(), this, "createNodeFromDatabase(): instantiate carbon dioxide node ("+uid+", "+item.getPropertyValue("CustomName")+", Location: "+item.getPropertyValue("Location")+")");
								}
								
								return new CarbonDioxideNodeImpl(uid, pemLocalID, item, lowerEventGate, higherEventGate);
								
							case DIMMER:
								
								if(LC.debug) {
									Logger.debug(LC.gi(), this, "createNodeFromDatabase(): instantiate dimmer controler node ("+uid+", "+item.getPropertyValue("CustomName")+", Location: "+item.getPropertyValue("Location")+")");
								}
								
								return new DimmerNodeImpl(uid, pemLocalID, item, lowerEventGate, higherEventGate);
							
							case HANDLE:
								
								if(LC.debug) {
									Logger.debug(LC.gi(), this, "createNodeFromDatabase(): instantiate handle node ("+uid+", "+item.getPropertyValue("CustomName")+", Location: "+item.getPropertyValue("Location")+")");
								}
								
								return new HandleNodeImpl(uid, pemLocalID, item, lowerEventGate, higherEventGate);
								
							
							case HVAC_CONTROLLER:
								
								if(LC.debug) {
									Logger.debug(LC.gi(), this, "createNodeFromDatabase(): instantiate HVAC controller node ("+uid+", "+item.getPropertyValue("CustomName")+", Location: "+item.getPropertyValue("Location")+")");
								}
								
								return new HVACControllerNodeImpl(uid, pemLocalID, item, lowerEventGate, higherEventGate);
								

							case ILLUMINATION:
								
								if(LC.debug) {
									Logger.debug(LC.gi(), this, "createNodeFromDatabase(): instantiate illumination node ("+uid+", "+item.getPropertyValue("CustomName")+", Location: "+item.getPropertyValue("Location")+")");
								}
								
								return new IlluminationNodeImpl(uid, pemLocalID, item, lowerEventGate, higherEventGate);
								
							case KEY_CARD:
								
								if(LC.debug) {
									Logger.debug(LC.gi(), this, "createNodeFromDatabase(): instantiate key card node ("+uid+", "+item.getPropertyValue("CustomName")+", Location: "+item.getPropertyValue("Location")+")");
								}
								
								return new KeyCardNodeImpl(uid, pemLocalID, item, lowerEventGate, higherEventGate);
								
							case METERING:
								
								if(LC.debug) {
									Logger.debug(LC.gi(), this, "createNodeFromDatabase(): instantiate meter reading node ("+uid+", "+item.getPropertyValue("CustomName")+", Location: "+item.getPropertyValue("Location")+")");
								}
								
								return new MeteringNodeImpl(uid, pemLocalID, item, lowerEventGate, higherEventGate);
								
							case MOTOR_CONTROL:
								
								if(LC.debug) {
									Logger.debug(LC.gi(), this, "createNodeFromDatabase(): instantiate motor control node ("+uid+", "+item.getPropertyValue("CustomName")+", Location: "+item.getPropertyValue("Location")+")");
								}
								
								return new MotorNodeImpl(uid, pemLocalID, item, lowerEventGate, higherEventGate);
								
							case ON_OFF:
								
								if(LC.debug) {
									Logger.debug(LC.gi(), this, "createNodeFromDatabase(): instantiate On/Off node ("+uid+", "+item.getPropertyValue("CustomName")+", Location: "+item.getPropertyValue("Location")+")");
								}
								
								return new OnOffNodeImpl(uid, pemLocalID, item, lowerEventGate, higherEventGate);
								
							case PRESENCE:
								
								if(LC.debug) {
									Logger.debug(LC.gi(), this, "createNodeFromDatabase(): instantiate presence node ("+uid+", "+item.getPropertyValue("CustomName")+", Location: "+item.getPropertyValue("Location")+")");
								}
								
								return new PresenceNodeImpl(uid, pemLocalID, item, lowerEventGate, higherEventGate);
							
							case RELATIVE_HUMIDITY:
								
								if(LC.debug) {
									Logger.debug(LC.gi(), this, "createNodeFromDatabase(): instantiate relative humidity node ("+uid+", "+item.getPropertyValue("CustomName")+", Location: "+item.getPropertyValue("Location")+")");
								}
								
								return new RelativeHumidityNodeImpl(uid, pemLocalID, item, lowerEventGate, higherEventGate);
								
							case SET_POINT:
								
								if(LC.debug) {
									Logger.debug(LC.gi(), this, "createNodeFromDatabase(): instantiate set point node ("+uid+", "+item.getPropertyValue("CustomName")+", Location: "+item.getPropertyValue("Location")+")");
								}
								
								return new SetPointNodeImpl(uid, pemLocalID, item, lowerEventGate, higherEventGate);
								
							case SINGLE_CONTACT:
								
								if(LC.debug) {
									Logger.debug(LC.gi(), this, "createNodeFromDatabase(): instantiate single contact node ("+uid+", "+item.getPropertyValue("CustomName")+", Location: "+item.getPropertyValue("Location")+")");
								}
								
								return new SingleContactNodeImpl(uid, pemLocalID, item, lowerEventGate, higherEventGate);
								
							case SMOKE:
								
								if(LC.debug) {
									Logger.debug(LC.gi(), this, "createNodeFromDatabase(): instantiate smoke node ("+uid+", "+item.getPropertyValue("CustomName")+", Location: "+item.getPropertyValue("Location")+")");
								}
								
								return new SmokeNodeImpl(uid, pemLocalID, item, lowerEventGate, higherEventGate);
								
							case SWITCH_GROUP:
								
								if(LC.debug) {
									Logger.debug(LC.gi(), this, "createNodeFromDatabase(): instantiate switch group node ("+uid+", "+item.getPropertyValue("CustomName")+", Location: "+item.getPropertyValue("Location")+")");
								}
								
								return new SwitchGroupNodeImpl(uid, pemLocalID, item, lowerEventGate, higherEventGate);
								
							case TEMPERATURE:
								
								if(LC.debug) {
									Logger.debug(LC.gi(), this, "createNodeFromDatabase(): instantiate temperature node ("+uid+", "+item.getPropertyValue("CustomName")+", Location: "+item.getPropertyValue("Location")+")");
								}
								
								return new TemperatureNodeImpl(uid, pemLocalID, item, lowerEventGate, higherEventGate);
								
							case VOLATILE_ORGANIC_COMPOUNDS:
								
								if(LC.debug) {
									Logger.debug(LC.gi(), this, "createNodeFromDatabase(): instantiate VOC node ("+uid+", "+item.getPropertyValue("CustomName")+", Location: "+item.getPropertyValue("Location")+")");
								}
								
								return new VolatileOrganicCompoundsNodeImpl(uid, pemLocalID, item, lowerEventGate, higherEventGate);
							
							default:
								if(LC.debug) {
									Logger.debug(LC.gi(), this, "createNodeFromDatabase(): profile "+profile+" is NOT yet supported.");
								}
								return null;
						}
					}
					else 
					{ /* actual node has been dropped. Null will be returned.*/
						if(LC.debug) {
							Logger.debug(LC.gi(), this, "createNodeFromDatabase(): node has been dropped ("+uid+").");
						}
					}
					break;
			}
		}
		else 
		{
			// this node is distant
			
			if(LC.debug) {
				Logger.debug(LC.gi(), this, "createNodeFromDatabase(): node is NOT local. This feature is not yet supported.");
			}
		}
		
		return null;
	}

	@Override
	public void onEvent(ItemDroppedEvent event) 
	{
		Logger.debug(LC.gi(), this, "onEvent(ItemDroppedEvent event): pem="+event.getPemUID()+", item="+event.getSourceItemUID());
		PemLocalID pemID = PemLocalID.localIDOf(event.getPemUID());
		if(pemID != null)
		{
			EventGate higherEventGate = getHigherAbstractionLevelEventGate();
			String thisPemID = getUID();
			switch(pemID)
			{
				case ENOCEAN:
					boolean localConfigurationChange = false;
					
					PhysicalEnvironmentItem[] items = getAllItems().toArray(new PhysicalEnvironmentItem[0]);
					for(PhysicalEnvironmentItem item : items)
					{
						AbstractNode node = (AbstractNode) item;
					    if (node.getActualNodeUID().equals(event.getSourceItemUID())) 
					    {
					    	String nodeID = node.getUID();
					    	database.removeItem(nodeID);
					    	
							localConfigurationChange = true;
					    	this.removeItem(nodeID);
					    	
					    	higherEventGate.postEvent(new ItemDroppedEvent(nodeID, thisPemID));
					    	
					    	if(LC.debug) {
					    		Logger.debug(LC.gi(), this, "onEvent(ItemDroppedEvent) "+event.getSourceItemUID()+" was dropped -> "+node.getUID()+" was removed.");
					    	}
					    }
					}
					
					if(localConfigurationChange) {	
						updateConfigurationUID();
					}
					break;
			}
		}			
	}

	@Override
	public void onEvent(ItemAddedEvent event) 
	{
		Logger.debug(LC.gi(), this, "onEvent(ItemAddedEvent event): pem="+event.getPemUID()+", item="+event.getSourceItemUID());
		PemLocalID pemID = PemLocalID.localIDOf(event.getPemUID());
		if(pemID != null)
		{
			EventGate higherEventGate = getHigherAbstractionLevelEventGate();
			String thisPemID = getUID();
			
			switch(pemID)
			{
				case ENOCEAN:
					Collection<AbstractNode> nodes = createNodeFromPemItem(pemID, pemEnocean.getItem(event.getSourceItemUID()));
					if(nodes != null) 
					{
						boolean localConfigurationChange = false;
						// If the item could be used as one or several nodes in the hotel application (i.e. the item profile is supported)
						for(AbstractNode node : nodes)
						{	
							if(database.addItem(node))
							{
								// If node does NOT already exist in database, this is a new node
								addItem(node);
								localConfigurationChange = true;
								
								higherEventGate.postEvent(new ItemAddedEvent(node.getUID(), thisPemID, node.getType(), node.getPropertiesAsJSONObject(), node.getCapabilities(), null));
								
								if(LC.debug) {
						    		Logger.debug(LC.gi(), this, "onEvent(ItemAddedEvent) "+event.getSourceItemUID()+" was added -> "+node.getUID()+" was added.");
						    	}
							}
						}
						
						if(localConfigurationChange) {
							 updateConfigurationUID();
						}
					}
					break;
			}
		}
	}
	
	@Override
	public void onEvent(ItemPropertiesUpdatedEvent event)
	{
		for (PhysicalEnvironmentItem item : getAllItems())
		{
			AbstractNode node = (AbstractNode) item;
			if (node.getActualNodeUID().equals(event.getSourceItemUID()))
			{
				getHigherAbstractionLevelEventGate().postEvent(new ItemPropertiesUpdatedEvent(node.getUID(), event.getPropertiesName()));
			}
		}
	}
}
