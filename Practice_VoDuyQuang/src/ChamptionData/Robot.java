package ChamptionData;

import java.util.Scanner;

public class Robot {

	int x;
	int y;
	String direction;
	
	
	
	//constructor
	public Robot(int x, int y, String direction) {
	    this.x = x;
	    this.y = y;
	    this.direction = direction;
	}
	
	
	public int getX() {
		return x;
	}


	public void setX(int x) {
		this.x = x;
	}


	public int getY() {
		return y;
	}


	public void setY(int y) {
		this.y = y;
	}


	public String getDirection() {
		return direction;
	}


	public void setDirection(String direction) {
		this.direction = direction;
	}


	public void printReport() {
		System.out.println( this.x + "," + this.y + "," + this.direction);
	}
	
	public int checkOutOfDimesion() {
		if (y == 0 && direction.equals("SOUTH")) {
			return 0;
		}
		else if (x == 0 && direction.equals("WEST")) {
			return 0;
		}
		else if (x == 4 && direction.equals("EAST")) {
			return 0;
		}
		else if (y == 4 && direction.equals("NORTH")) {
			return 0;
		}
		
		
		
		
		return 1;
		
	}

	public int moveRobot(int dir){

	    switch (dir) {
	        case 1: //MOVE
	        	if (checkOutOfDimesion() == 0) {
	        		System.out.println("The robot can not go out the dimension");
	        		return 0;
	        	}
	        	if (direction.equals("NORTH")) {
	        		if (y < 4) {
	        			y++;
	        		}	        		
	        	}
	        	
	        	else if (direction.equals("SOUTH")) {
	        		if (y > 0) {
	        			y--;
	        		}	        		
	        	}
	        	
	        	else if (direction.equals("WEST")) {
	        		if (x > 0) {
	        			x--;
	        		}
	        	}
	        	
	        	else if (direction.equals("EAST") ) {
	        		if (x < 4) {
	        			x++;
	        		}	        		
	        	}
	        	break;

	        case 2: //LEFT
	        	if (direction.equals("NORTH")) {
	        		direction = "WEST";
	        	}
	        	else if (direction.equals("WEST")) {
	        		direction = "SOUTH";
	        	}
	        	else if (direction.equals("SOUTH")) {
	        		direction = "EAST";
	        	}
	        	else if (direction.equals("EAST")) {
	        		direction = "NORTH";
	        	}
	        	break;
	        case 3: //RIGHT
	        	if (direction.equals("NORTH")) {
	        		direction = "EAST";
	        	}
	        	else if (direction.equals("EAST")) {
	        		direction = "SOUTH";
	        	}
	        	else if (direction.equals("SOUTH")) {
	        		direction = "WEST";
	        	}
	        	else if (direction.equals("WEST")) {
	        		direction = "NORTH";
	        	}

	        	break;
	        default: System.out.print("invalid move");
	    }
		return 1;
	}

	
	

	
}
