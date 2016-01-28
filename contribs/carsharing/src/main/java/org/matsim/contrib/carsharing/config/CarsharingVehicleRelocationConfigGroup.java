package org.matsim.contrib.carsharing.config;

import org.matsim.core.config.ReflectiveConfigGroup;

public class CarsharingVehicleRelocationConfigGroup extends ReflectiveConfigGroup{

	public static final String GROUP_NAME = "CarsharingVehicleRelocation";

	private String relocationRadius = null;

	private String relocationZonesInputFile = null;

	private String relocationTimesInputFile = null;

	public CarsharingVehicleRelocationConfigGroup() {
		super(GROUP_NAME);
	}
	
	@StringGetter( "relocationRadius" )
	public String getRelocationRadius() {
		return this.relocationRadius;
	}

	@StringSetter( "relocationRadius" )
	public void setRelocationRadius(final String relocationRadius) {
		this.relocationRadius = relocationRadius;
	}

	@StringGetter( "relocationZones" )
	public String getRelocationZones() {
		return this.relocationZonesInputFile;
	}

	@StringSetter( "relocationZones" )
	public void setRelocationZones(final String relocationZonesInputFile) {
		this.relocationZonesInputFile = relocationZonesInputFile;
	}

	@StringGetter( "relocationTimes" )
	public String getRelocationTimes() {
		return this.relocationTimesInputFile;
	}

	@StringSetter( "relocationTimes" )
	public void setRelocationTimes(final String relocationTimesInputFile) {
		this.relocationTimesInputFile = relocationTimesInputFile;
	}
}
