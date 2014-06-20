/*
 * Copyright 2012, United States Geological Survey or
 * third-party contributors as indicated by the @author tags.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/  >.
 *
 */
package asl.metadata;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeedVolume {
	private static final Logger logger = LoggerFactory
			.getLogger(asl.metadata.SeedVolume.class);

	private Blockette volumeInfo = null;
	private NetworkKey networkKey = null;

	private ArrayList<Blockette> stationLocators;
	private Hashtable<StationKey, StationData> stations;

	// constructor(s)
	public SeedVolume() {
		stations = new Hashtable<StationKey, StationData>();
		stationLocators = new ArrayList<Blockette>();
	}

	public SeedVolume(Blockette volumeInfo) {
		this.volumeInfo = volumeInfo;
		try {
			this.networkKey = new NetworkKey(volumeInfo);
			// System.out.format("== new SeedVolume() networkKey=[%s]\n",
			// networkKey);
		} catch (WrongBlocketteException e) {
			// System.out.format("== SeedVolume Error: WrongBlocketteException:%s\n",
			// e.getMessage() );
			logger.error("== WrongBlocketteException:", e);
		}
		stations = new Hashtable<StationKey, StationData>();
		stationLocators = new ArrayList<Blockette>();
	}

	// stations
	public void addStation(StationKey key, StationData data) {
		stations.put(key, data);
	}

	public boolean hasStation(StationKey key) {
		return stations.containsKey(key);
	}

	public StationData getStation(StationKey key) {
		// for (StationKey ky : stations.keySet()){
		// System.out.format("== Got StationKey=[%s]\n", ky);
		// }
		return stations.get(key);
	}

	// volume info
	public void setVolumeInfo(Blockette volumeInfo) {
		this.volumeInfo = volumeInfo;
	}

	public Blockette getVolumeInfo() {
		return this.volumeInfo;
	}

	public NetworkKey getNetworkKey() {
		return this.networkKey;
	}

	// station locators (list of stations in seed volume)
	public void addStationLocator(Blockette stationLocator) {
		stationLocators.add(stationLocator);
	}

	public ArrayList<Blockette> getStationLocators() {
		return stationLocators;
	}

	public void printStations() {
		TreeSet<StationKey> keys = new TreeSet<StationKey>();
		keys.addAll(stations.keySet());
		for (StationKey key : keys) {
			System.out.format("     == SeedVolume: Stn = [%s]\n", key);
		}
	}

	public List<Station> getStationList() {
		ArrayList<Station> stns = new ArrayList<Station>();
		TreeSet<StationKey> keys = new TreeSet<StationKey>();
		keys.addAll(stations.keySet());

		for (StationKey key : keys) {
			stns.add(new Station(key.getNetwork(), key.getName()));
		}
		return stns;
	}

}
