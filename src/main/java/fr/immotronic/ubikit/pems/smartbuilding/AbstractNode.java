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

package fr.immotronic.ubikit.pems.smartbuilding;

import org.json.JSONObject;
import org.ubikit.AbstractPhysicalEnvironmentItem;
import org.ubikit.PhysicalEnvironmentItem;
import org.ubikit.event.EventGate;

import fr.immotronic.ubikit.pems.smartbuilding.impl.node.PemLocalID;

/**
 * Base class to implement concrete node.
 * 
 * @author Lionel Balme, Immotronic.
 *
 */
public abstract class AbstractNode extends AbstractPhysicalEnvironmentItem
{
	private final NodeProfile profile;
	private final String host; // The name of the PlaceTouch box associated to the actual node. Worth NULL if node is local.
	//private final String name; // The node name. Should be the same as the actual node name.
	private final PemLocalID pemLocalID; // The actual node PEM name.
	
	public static enum Field {
		PROFILE,
		HOST,
		UID,
		ACTUAL_NODE_UID,
		PEM
	}
	
	/**
	 * A reference to a valid EventGate object. Allow the node to subscribe to PEM events and send events to PEMs.
	 */
	protected final EventGate lowerEventGate;
	
	/**
	 * A reference to a valid EventGate object. Allow the node to interact with Hotel functions apps.
	 */
	protected final EventGate higherEventGate;
	
	/**
	 * A reference onto the actual node
	 */
	private PhysicalEnvironmentItem actualNode;
	
	private final Object actualNodeSynchronization;
	
	/**
	 * Construct a AbstractNode object.
	 * 
	 * @param type The node type. This parameter cannot be null.
	 * @param host The node host name. This is the name of the PlaceTouch box associated to the actual node. It MUST worth null if this node is local.
	 * @param uid The node unique identifier (at the Hotel app level). This parameter cannot be null.
	 * @param pemLocalID the actual node PEM.
	 * @param actualNode a reference onto the actual node. MUST be null for distant nodes. MUST NOT be null for local nodes.
	 * @param eventGate The EventGate object through which interact with the actual node.
	 */
	protected AbstractNode(NodeProfile profile, Type type, String host, String uid, PemLocalID pemLocalID, PhysicalEnvironmentItem actualNode, EventGate lowerEventGate, EventGate higherEventGate)
	{
		super(uid, type, false, null);
		
		if(profile == null) {
			throw new IllegalArgumentException("profile cannot be null");
		}
		
		if(uid == null) {
			throw new IllegalArgumentException("uid cannot be null");
		}
		
		if(pemLocalID == null) {
			throw new IllegalArgumentException("pemLocalID cannot be null");
		}
		
		if(host == null && actualNode == null) {
			throw new IllegalArgumentException("if node is local (host == null) then actualNode cannot be null");
		}
		
		if(lowerEventGate == null) {
			throw new IllegalArgumentException("lowerEventGate cannot be null");
		}
		
		if(higherEventGate == null) {
			throw new IllegalArgumentException("higherEventGate cannot be null");
		}
		
		actualNodeSynchronization = new Object();
		
		this.profile = profile;
		this.host = host;
		this.pemLocalID = pemLocalID;
		this.actualNode = actualNode;
		this.higherEventGate = higherEventGate;
		this.lowerEventGate = lowerEventGate;
	}
	
	
	/**
	 * Indicate if this node is a local one or a distant one.
	 * @return true if this node is local, return false if this node is distant.
	 */
	protected boolean isLocal()
	{
		return host == null;
	}
	
	/**
	 * Return the node host name. Worth NULL if this node is local.
	 * @return the node host name or null if this node is local.
	 */
	public String getHost()
	{
		return host;
	}
	
	/* *
	 * Return a reference onto the PEM that host the actual node. Will return null if node is a distant one.
	 * @return a reference onto a PEM.
	 * /
	protected PhysicalEnvironmentModelService getPem()
	{
		return pem;
	}*/
	
	/**
	 * Return the actual node UID.
	 * @return the actual node UID.
	 */
	public String getActualNodeUID()
	{
		synchronized(actualNodeSynchronization)
		{
			if(actualNode == null) {
				return null;
			}
			
			return actualNode.getUID();
		}
	}
	
	/**
	 * Return the actual node value.
	 * @return the actual node value.
	 */
	public Object getActualNodeValue()
	{
		synchronized(actualNodeSynchronization)
		{
			if(actualNode == null) {
				return null;
			}
			
			return actualNode.getValue();
		}
	}
	
	@Override
	public String getPropertyValue(String propertyName) 
	{
		return actualNode.getPropertyValue(propertyName);
	}
	
	@Override
	public JSONObject getPropertiesAsJSONObject()
	{
		return actualNode.getPropertiesAsJSONObject();
	}
	
	/**
	 * Return the node name.
	 * @return the node name.
	 */
	public String getName()
	{
		return actualNode.getPropertyValue(PhysicalEnvironmentItem.Property.CustomName.name());
	}
	
	/**
	 * Return the node location.
	 * @return the node location.
	 */
	public String getLocation()
	{
		return actualNode.getPropertyValue(PhysicalEnvironmentItem.Property.Location.name());
	}
	
	/**
	 * Return the node profile.
	 * @return the node profile.
	 */
	public NodeProfile getProfile()
	{
		return profile;
	}
	
	/**
	 * Return the pem name of the node. Will return null for a distant node and a regular value otherwise.
	 * @return the pem name or null if the actual node is distant.
	 */
	public PemLocalID getPemLocalID()
	{
		return pemLocalID;
	}
	
	@Override
	protected void propertiesHaveBeenUpdated(String[] propertiesName)
	{
		
	}
	
	public static boolean isCapabilitiesSupported(PemLocalID pem, String[] supportedCapabilities, String[] capabilities)
	{
		if(supportedCapabilities == null || supportedCapabilities.length < 1) {
			throw new IllegalArgumentException("supportedCapabilities cannot be null and MUST contain 1 element at least.");
		}
		
		switch(pem)
		{
			case ENOCEAN:
				if(capabilities.length == 1)
				{
					String capability = capabilities[0];
					for(String supportedCapability : supportedCapabilities)
					{
						if(supportedCapability.equals(capability))
						{
							return true;
						}
					}
				}
				break;
		}
		
		return false;
	}
	
	@Override
	protected void terminate() 
	{
		synchronized(actualNodeSynchronization)
		{
			actualNode = null;
		}
	}
}
