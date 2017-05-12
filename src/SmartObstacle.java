// this one follows the robot! if it gets you stuck you can simply jump over it

public class SmartObstacle extends Obstacle {
	Structure robot;
	
	public SmartObstacle(Structure robot, float[] pos) {
		this.robot = robot; // need to know where the robot is located
		structure = new Structure("resources/cone.obj", new Shape[] {}, new float[][] {});
		structure.uniScale = 0.1f;
		structure.posX = pos[0];
		structure.posY = 0.0f;
		structure.posZ = pos[1];
		hitbox = new float[] {structure.posX - 0.12f, structure.posZ - 0.12f, 0.25f, 0.25f, 0.26f};
	}
	
	public void update() {
		super.update();
		
		// algorithm that makes the obstacle follow the robot
		float dx = robot.posX - structure.posX;
		float dz = robot.posZ - structure.posZ;
		float distance = (float) Math.sqrt(Math.pow(dx, 2) + 
				Math.pow(dz, 2));
		
		hitbox = new float[] {structure.posX - 0.12f, structure.posZ - 0.12f, 0.25f, 0.25f, 0.26f};
		
		
		
		vx = 1/distance * dx * 0.01f;
		vz = 1/distance * dz * 0.01f;
	}
}
