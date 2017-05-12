import com.jogamp.opengl.GL2;

public class RainDrop {
	public float vx, vy, vz;// velocity
	public float[] pos; 	// position of the rain drop
	public Structure robot;	// the robot
	public Structure drop;	// the shape of the rain drop
	
	public RainDrop(Structure robot) {
		this.robot = robot; // need the robot's location
		
		drop = new Structure(new float[] {0.01f, 0.05f, 0.01f}, new Shape[] {}, new float[][] {}, A4.textures[10]);
		
		vx = vz = 0; // no speed in x and z
		vy = -0.1f;	 // gravity
		
		// random rain drop in a 10 units radious around the robot
		pos = new float[3];
		pos[0] = (float) (-10 + Math.random() * 20) + robot.posX;
		pos[1] = (float) (8 + Math.random() * 8);
		pos[2] = (float) (-10 + Math.random() * 20) + robot.posZ;
		
		drop.posX = pos[0];
		drop.posY = pos[1];
		drop.posZ = pos[2];
	}
	
	public void draw(GL2 gl) {
		drop.draw(gl);
		update();
	}
	
	public void update() {
		drop.posY += vy;
		
		if (drop.posY < 0) {
			// random rain drop in a 10 units radious around the robot
			pos = new float[3];
			pos[0] = (float) (-10 + Math.random() * 20) + robot.posX;
			pos[1] = (float) (8 + Math.random() * 8);
			pos[2] = (float) (-10 + Math.random() * 20) + robot.posZ;
			
			drop.posX = pos[0];
			drop.posY = pos[1];
			drop.posZ = pos[2];
		}
	}
}
