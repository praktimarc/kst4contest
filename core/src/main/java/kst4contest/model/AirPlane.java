package kst4contest.model;

public class AirPlane {

	String apCallSign, apSizeCategory;
	String potencialDescriptionAsWord;

	public String getPotencialDescriptionAsWord() {
		if (this.getPotential() <=50) {
			return "small AP";
		} else if (this.getPotential() <=75 && this.getPotential() > 50) {
			return "big AP";
		} else if (this.getPotential() > 75) {
			return "very big AP";
		}


		return potencialDescriptionAsWord;
	}

	public void setPotencialDescriptionAsWord(String potencialDescriptionAsWord) {
		this.potencialDescriptionAsWord = potencialDescriptionAsWord;
	}

	int distanceKm, potential, arrivingDurationMinutes;
	public String getApCallSign() {
		return apCallSign;
	}
	public void setApCallSign(String apCallSign) {
		this.apCallSign = apCallSign;
	}
	public String getApSizeCategory() {
		return apSizeCategory;
	}
	public void setApSizeCategory(String apSizeCategory) {
		this.apSizeCategory = apSizeCategory;
	}
	public int getDistanceKm() {
		return distanceKm;
	}
	public void setDistanceKm(int distanceKm) {
		this.distanceKm = distanceKm;
	}
	public int getPotential() {
		return potential;
	}
	public void setPotential(int potential) {
		this.potential = potential;
	}
	public int getArrivingDurationMinutes() {
		return arrivingDurationMinutes;
	}
	public void setArrivingDurationMinutes(int arrivingDurationMinutes) {
		this.arrivingDurationMinutes = arrivingDurationMinutes;
	}
	
	@Override
	public String toString() {
	
		String toStringString = "\n";
		
		toStringString += this.apCallSign + ", category: " + this.apSizeCategory + ", distance: " + this.getDistanceKm() + ", potential: " + this.potential + ", distance: " + this.getDistanceKm() + ", duration " + this.arrivingDurationMinutes + "" ;
		
		return toStringString;
	}
	
}
