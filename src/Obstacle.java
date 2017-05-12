import java.awt.Rectangle;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.texture.Texture;

public abstract class Obstacle {
	public Structure structure; // shape of the obstacle
	public float vx, vz; // speed in vx and vz
	public float[] hitbox; 
	
	public Obstacle() {
		structure = new Structure(new float[] {1, 1, 1}, new Shape[] {}, new float[][] {});
		vx = 0;
		vz = 0;
		hitbox = new float[] {};
	}
	
	// did the obstacle hit the robot?
	public boolean hit(float[] r) {
		return hitbox[0] < r[0] + r[2] && 
				hitbox[0] + hitbox[2] > r[0] && 
				hitbox[1] < r[1] + r[3] && 
				hitbox[1] + hitbox[3] > r[1] &&
				r[4] < hitbox[4];
	}
	
	public void draw(GL2 gl) {		
		structure.draw(gl);
		update();
	}
	
	protected void update() {
		structure.posX += vx;
		structure.posZ += vz;
		
		if (structure.posX >= 10) {
			structure.posX = -10;
		}
		
		if (structure.posX < -10) {
			structure.posX = 10;
		}
		
		if (structure.posZ >= 10) {
			structure.posZ = -10;
		}
		
		if (structure.posZ < -10) {
			structure.posZ = 10;
		}
	}
}
