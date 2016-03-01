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

package fr.immotronic.ubikit.pems.smartbuilding.impl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import org.json.JSONException;
import org.json.JSONObject;
import org.ubikit.DatabaseProxy;
import org.ubikit.Logger;

import fr.immotronic.ubikit.pems.smartbuilding.AbstractNode;

/**
 * This class manage the Node Model persistence in the app database.
 * 
 * @author Lionel Balme, Immotronic.
 *
 */
final class DatabaseManagerImpl 
{
	private static final String ITEMS_TABLE_STRUCTURE = "CREATE TABLE IF NOT EXISTS node_model_items (" +
			"UID VARCHAR(80) NOT NULL, " +
			"actualUID VARCHAR(80), " +
			"type VARCHAR(50) NOT NULL, " +
			"host VARCHAR(255) NOT NULL, " +
			"pem VARCHAR(255) NOT NULL, " +
			"PRIMARY KEY (UID));";
	
	private static final String ITEMS_INSERT_QUERY = "INSERT INTO node_model_items " +
			"(UID, actualUID, type, host, pem) " +
			"VALUES (?, ?, ?, ?, ?);";
	
	/*private static final String ITEMS_UPDATE_QUERY = "UPDATE node_model_items SET " +
			"type = ?, " 
			"host = ? " +
			"WHERE UID = ?;";
	*/
	private static final String ITEMS_REMOVE_QUERY = "DELETE FROM node_model_items WHERE UID = ?;";
	
	private static final String ITEMS_QUERY_ALL = "SELECT UID, actualUID, type, host, pem FROM node_model_items;";
	
	private static final String CONFIGURATION_UID_TABLE_STRUCTURE = "CREATE TABLE IF NOT EXISTS node_model_CUID (UID INT);";
	private static final String CONFIGURATION_UID_GET = "SELECT UID FROM node_model_CUID;";
	private static final String CONFIGURATION_UID_INIT = "INSERT INTO node_model_CUID (UID) VALUES (0);";
	private static final String CONFIGURATION_UID_UPDATE = "UPDATE node_model_CUID SET UID = ?;";
	
	private final DatabaseProxy dbProxy;
	private final String localHostName;
	
	private final PreparedStatement getItems;
	private final PreparedStatement insertItem;
	//private final PreparedStatement updateItem;
	private final PreparedStatement removeItem;
	private final PreparedStatement getConfigurationUID;
	private final PreparedStatement updateConfigurationUID;
	
	/**
	 * Construct a NodeModelDatabase object. SHOULD be a singleton.
	 * 
	 * @param dbProxy a reference onto the database proxy. This parameter cannot be null.
	 * @param localHostName the name of the local PlaceTouch box. This parameter cannot be null.
	 */
	public DatabaseManagerImpl(DatabaseProxy dbProxy, String localHostName)
	{
		if(dbProxy == null) {
			throw new IllegalArgumentException("dbProxy cannot be null");
		}
		
		if(localHostName == null) {
			throw new IllegalArgumentException("localHostName cannot be null");
		}
		
		this.dbProxy = dbProxy;
		this.localHostName = localHostName;
		
		dbProxy.executeUpdate(ITEMS_TABLE_STRUCTURE);
		dbProxy.executeUpdate(CONFIGURATION_UID_TABLE_STRUCTURE);
		
		getItems = dbProxy.getPreparedStatement(ITEMS_QUERY_ALL);
		insertItem = dbProxy.getPreparedStatement(ITEMS_INSERT_QUERY);
		//updateItem = dbProxy.getPreparedStatement(ITEMS_UPDATE_QUERY);
		removeItem = dbProxy.getPreparedStatement(ITEMS_REMOVE_QUERY);
		getConfigurationUID = dbProxy.getPreparedStatement(CONFIGURATION_UID_GET);
		updateConfigurationUID = dbProxy.getPreparedStatement(CONFIGURATION_UID_UPDATE);
		
		if(getConfigurationUID() == 0) {
			dbProxy.executeUpdate(CONFIGURATION_UID_INIT);
		}
	}
	
	/**
	 * Return the configuration unique identifier of the current Node Model. If getConfigurationUID() return 0, no Node Model has been found. It must be initialized.
	 * 
	 * @return an integer that is the configuration unique identifier of the current Node Model. 
	 */
	public int getConfigurationUID()
	{
		ResultSet rs = dbProxy.executePreparedQuery(getConfigurationUID);
		if(rs != null)
		{
			try 
			{
				if(rs.next())
				{
					return rs.getInt(1);
				}
			} 
			catch (SQLException e) 
			{
				Logger.error(LC.gi(), this, "getConfigurationUID(): Cannot get the Node Model configuration UID.");
			}
		}
		
		return 0;
	}
	
	/**
	 * Update the Node Model configuration unique identifier.
	 * 
	 * @param UID the Node Model configuration unique identifier.
	 * 
	 * @return true if the configuration unique identifier has been successfully updated.
	 */
	public boolean updateConfigurationUID(int UID)
	{
		try 
		{	
			updateConfigurationUID.setInt(1, UID);
			return (dbProxy.executePreparedUpdate(updateConfigurationUID) >= 0);
		} 
		catch (SQLException e) 
		{
			Logger.error(LC.gi(), this, "updateConfigurationUID(): Cannot update the Node Model configuration UID. Query values were UID="+UID, e);
			return false;
		}
	}
	
	/**
	 * Return the list of Node Model items stored in database as JSON objects.
	 *  
	 * @return a collection of JSON objects that represents stored Node Model items.<br> 
	 * Return an empty collection if no items are stored. <br>
	 * Return null if an exception occurs while building the item list.
	 */
	public Collection<JSONObject> getItems()
	{
		Collection<JSONObject> items = new ArrayList<JSONObject>();
		
		ResultSet rs = dbProxy.executePreparedQuery(getItems);
		if(rs != null)
		{
			try 
			{
				while(rs.next())
				{
					JSONObject o = new JSONObject();
					o.put(AbstractNode.Field.UID.name(), rs.getString(1));
					o.put(AbstractNode.Field.ACTUAL_NODE_UID.name(), rs.getString(2));
					o.put(AbstractNode.Field.PROFILE.name(), rs.getString(3));
					o.put(AbstractNode.Field.HOST.name(), rs.getString(4));
					o.put(AbstractNode.Field.PEM.name(), rs.getString(5));
					items.add(o);
				}
			} 
			catch (SQLException e) 
			{
				Logger.error(LC.gi(), this, "getItems(): Cannot get the Node Model item list");
				return null;
			} 
			catch (JSONException e) 
			{
				Logger.error(LC.gi(), this, "getItems(): Cannot construct Node Model item JSON object");
				return null;
			}
		}
		
		return items;
	}
	
	/**
	 * Store a new node in database. Will return true on success, false otherwise.
	 * 
	 * @param item the AbstractNode instance to store in database. If item is null, nothing will be stored, true will be returned.
	 * 
	 * @return true if the node instance has been successfully stored.
	 */
	public boolean addItem(AbstractNode item)
	{
		if(item != null)
		{
			String hostname = null;
			
			try 
			{
				insertItem.setString(1, item.getUID());
				insertItem.setString(2, item.getActualNodeUID());
				insertItem.setString(3, item.getProfile().name());
				
				hostname = item.getHost();
				if(hostname == null) {
					hostname = localHostName;
				}
				insertItem.setString(4, hostname);
				
				insertItem.setString(5, item.getPemLocalID().name());
				
				return (dbProxy.executePreparedUpdate(insertItem) >= 0);
			} 
			catch (SQLException e) 
			{
				Logger.error(LC.gi(), this, "addItem(): Cannot insert the new node. Query values were UID="+item.getUID()+", type="+item.getType().name()+", name="+item.getName()+", host="+hostname, e);
				return false;
			}
		}
		
		return true;
	}
	
	/* *
	 * Update the stored data about a node. Will return true on success, false otherwise.
	 * 
	 * @param item the AbstractNode instance to update in database. If item is null, nothing will be stored, true will be returned.
	 * 
	 * @return true if node data in database have been successfully updated.
	 * /
	public boolean updateItem(AbstractNode item)
	{
		if(item != null)
		{
			String hostname = null;
			
			try 
			{	
				updateItem.setString(1, item.getType().name());
				updateItem.setString(2, item.getName());
				
				hostname = item.getHost();
				if(hostname == null) {
					hostname = localHostName;
				}
				updateItem.setString(3, hostname);
				
				updateItem.setString(4, item.getUID());
				return (dbProxy.executePreparedUpdate(updateItem) >= 0);
			} 
			catch (SQLException e) 
			{
				Logger.logErr(this, "updateItem(): Cannot update node data in database. Query values were UID="+item.getUID()+", type="+item.getType().name()+", name="+item.getName()+", host="+hostname, e);
				return false;
			}
		}
		
		return true;
	}
	*/
	
	/**
	 * Remove node data from database. Will return true on success, false otherwise. If no node matches the given item UID, false will be returned. 
	 * 
	 * @param item the AbstractNode instance to remove from database. If item is null, nothing will be removed, true will be returned.
	 * 
	 * @return true if node data have been successfully removed from database.
	 */
	public boolean removeItem(String itemUID)
	{
		if(itemUID != null)
		{
			try 
			{	
				removeItem.setString(1, itemUID);
				return (dbProxy.executePreparedUpdate(removeItem) >= 0);
			} 
			catch (SQLException e) 
			{
				Logger.error(LC.gi(), this, "removeItem(): Cannot remove node data in database. Query values were UID="+itemUID, e);
			}
		}
		
		return false;
	}
}
