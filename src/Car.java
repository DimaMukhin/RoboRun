
public class Car extends Obstacle {

	private Structure frontLeftWheel;
	private Structure frontRightWheel;
	private Structure rearLeftWheel;
	private Structure rearRightWheel;
	
	public Car(float[] pos) {
		constructCar();
		
		hitbox = new float[] {structure.posX - 0.25f, structure.posZ - 0.1f, 0.5f, 0.2f, 0.15f};
		
		vx = -0.01f;
		
		structure.posX = pos[0];
		structure.posY = 0.1f;
		structure.posZ = pos[1];
	}

	private void constructCar() {
		float[] bodyShape = new float[] {0.25f, 0.05f, 0.1f};
		float[] wheelShape = new float[] {0.05f, 0.05f, 0.01f};
		
		structure = new Structure(bodyShape, new Shape[] {}, new float[][] {});
		
		frontLeftWheel = new Structure(wheelShape, new Shape[] {}, new float[][] {});
		frontRightWheel = new Structure(wheelShape, new Shape[] {}, new float[][] {});
		rearLeftWheel = new Structure(wheelShape, new Shape[] {}, new float[][] {});
		rearRightWheel = new Structure(wheelShape, new Shape[] {}, new float[][] {});
		
		structure.add(frontLeftWheel, new float[] {0.15f, -0.05f, -0.1f});
		structure.add(frontRightWheel, new float[] {0.15f, -0.05f, 0.1f});
		structure.add(rearLeftWheel, new float[] {-0.15f, -0.05f, -0.1f});
		structure.add(rearRightWheel, new float[] {-0.15f, -0.05f, 0.1f});
	}
	
	public void update() {
		super.update();
		
		hitbox = new float[] {structure.posX - 0.25f, structure.posZ - 0.1f, 0.5f, 0.2f, 0.15f};
		
		// make the wheels spin!
		frontLeftWheel.rotateZ += 1;
		frontRightWheel.rotateZ += 1;
		rearLeftWheel.rotateZ += 1;
		rearRightWheel.rotateZ += 1;
		
		if (frontLeftWheel.rotateZ >= 360) {
			frontLeftWheel.rotateZ = 0;
			frontRightWheel.rotateZ = 0;
			rearLeftWheel.rotateZ = 0;
			rearRightWheel.rotateZ = 0;
		}
	}
}
