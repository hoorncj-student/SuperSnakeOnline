package edu.rosehulman.supersnakeonline;

import android.graphics.Color;

public class Opponent {
	
	private String name;
	private int snakeColor;
	private long score;
	
	public Opponent (String name, int color) {
		this.name = name;
		this.snakeColor = color;
		this.score = 0;
	}
	
	public String getName() {
		return name;
	}
	public int getSnakeColor() {
		return snakeColor;
	}
	public long getScore() {
		return score;
	}

}
