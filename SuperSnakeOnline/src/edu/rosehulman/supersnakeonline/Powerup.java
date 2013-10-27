package edu.rosehulman.supersnakeonline;

import edu.rosehulman.supersnakeonline.SnakeGameView.Coordinate;

public class Powerup {
	private Coordinate c;
	private PowerupType t;
	
	public Coordinate getCoord() {
		return this.c;
	}
	public PowerupType getPowerup() {
		return this.t;
	}
	public void setCoord(Coordinate c) {
		this.c = c;
	}
	public void setPowerup(PowerupType t) {
		this.t = t;
	}
}