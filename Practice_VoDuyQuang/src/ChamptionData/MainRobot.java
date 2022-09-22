package ChamptionData;

import java.util.Scanner;

public class MainRobot {
	
	

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		Robot robo = null;
		System.out.println("Hello welcome to the robot moving demostation program");
		System.out.println("Please initalize the possiton of the robot following instuction");
		System.out.println("PLACE 0,0,direction");
		System.out.println("To exit the program type EXIT\n");
		String input = scanner.nextLine();
		int flag = 0; //determine if function PLACE called or not
		String regexCommand = "(PLACE|MOVE|LEFT|RIGHT|REPORT)";
		String regex = "PLACE [0-4],[0-4],(NORTH|SOUTH|WEST|EAST)"; //determine if user enter the correct format
		while (true) {
			
			
		   		
			
			
           if( !input.contains("EXIT"))
           {
        	   if (input.contains("PLACE")) {
                   
        		   if (input.matches(regex)) {
                   	
        			   
        			   //First split to remove PLACE command
        			   
        			   
        			   String removePlace = input.substring(input.lastIndexOf(" ") + 1);
        			   
        			   //Second split to take x,y,direction as an intialize for the Robot
        			   
        			   String segments[] = removePlace.split(",");
        			   
        			   Integer xCor = Integer.valueOf(segments[0]);
        			   Integer yCor = Integer.valueOf(segments[1]);
        			   String dirCor = segments[2];
        			   
                   	
	             
	                   	
	                   robo = new Robot(xCor,yCor, dirCor);
	                   flag = 1;
	                   //Inital the object
	                   System.out.println("Initialize  the position of robot successful");
	                   System.out.println("The robot is at coordinate (" + xCor +","+ yCor + ") heading " + dirCor);
	                   
                   }  
        		   else if (!input.matches(regex)) {
        			   System.out.println("Your command does not follow the correct format. \nPlease try again.");
        		   }
        	   }

        	   
               else if (!input.contains("PLACE") && flag == 0){
                	
                	
                	System.out.println("Please enter command PLACE first before executing others command\n");
               }
               else if (!input.matches(regexCommand) && flag == 1) {
            	   System.out.println("Only 5 commands PLACE, MOVE, LEFT, RIGHT, REPORT are allowing");
               }
        	   
               else if (input.equals("MOVE")) {
            	   int out = robo.moveRobot(1);
            	   if (out != 0) {
            		   System.out.println("The robot moves 1 step to the " + robo.getDirection()); 
            	   }
            	   
               }
               else if (input.equals("LEFT")) {
            	   robo.moveRobot(2);
            	   System.out.println("The robot turns left now it's heading " + robo.getDirection());
               }
               else if (input.equals("RIGHT")) {
            	   robo.moveRobot(3);
            	   System.out.println("The robot turns right now it's heading " + robo.getDirection());
               }
               else if (input.equals("REPORT")) {
            	   robo.printReport();
               }
        	   
               input = scanner.nextLine();
           }
           else if (input.contains("EXIT"))
           {
                System.out.println("The program is exiting !!!");
                System.exit(1);
           }
		}
		
		
		
		
		
		
	}
}
