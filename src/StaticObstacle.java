import com.jogamp.opengl.util.texture.Texture;

public class StaticObstacle extends Obstacle {

	public StaticObstacle(float[] pos, Texture tex) {
		float[] obsScale = new float[] {0.25f, 0.1f, 0.1f};
		structure = new Structure(obsScale, new Shape[] {}, new float[][] {{},{}}, tex);
		structure.posX = pos[0]; 
		structure.posY = 0.1f;
		structure.posZ = pos[1];
		
		hitbox = new float[] {structure.posX - 0.25f, structure.posZ - 0.1f, 0.5f, 0.2f, 0.2f};
		
		vx = 0;
		vz = 0;
	}
	
}
