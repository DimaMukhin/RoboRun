import java.util.LinkedList;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.texture.Texture;

public class Structure extends Shape {
	// this array can include other structures...
	public LinkedList<Shape> contents;
	private LinkedList<float[]> positions;
	
	float[] center;	// center of rotation
	float uniScale; // uniform scale
	float posZ;		// position in the Z axis
	float posY;		// position in the Y axis
	float posX;		// position in the X axis
	float rotateX; 	// rotation about the X axis 
	float rotateY; 	// rotation about the Y axis
	float rotateZ; 	// rotation about the Z axis
	Texture tex;
	
	public Structure(float[] scaleS, Shape[] contents, float[][] positions) {
		super(scaleS);
		init(contents, positions);
	}
	
	public Structure(float[] scaleS, Shape[] contents, float[][] positions, Texture tex) {
		super(scaleS, tex);
		this.tex = tex;
		init(contents, positions);
	}
	
	public Structure(String filename, Shape[] contents, float[][] positions) {
		super(filename);
		init(contents, positions);
	}
	
	private void init(Shape[] contents, float[][] positions) {
		center = new float[] {0, 0, 0};
		uniScale = 1;
		posX = 0;
		posY = 0;
		posZ = 0;
		rotateX = 0;
		rotateY = 0;
		rotateZ = 0;
		
		this.contents = new LinkedList<Shape>();
		this.positions = new LinkedList<float[]>();
		
		for (int i = 0; i < contents.length; i++) {
			this.contents.add(contents[i]);
		}
		
		for (int i = 0; i < positions.length; i++) {
			this.positions.add(positions[i]);
		}
	}

	public void add(float[] shapeS, float[] position) {
		Shape addShape;
		if (tex != null) {
			addShape = new Shape(shapeS, tex);
		} else {
			addShape = new Shape(shapeS);
		}
		contents.add(addShape);
		positions.add(position);
	}
	
	public void add(Shape shape, float[] position) {
		contents.add(shape);
		positions.add(position);
	}
	
	public void add(Structure struct, float[] position) {
		contents.add(struct);
		positions.add(position);
	}
	
	public void draw(GL2 gl) {
		gl.glPushMatrix();
		gl.glTranslatef(posX, posY, posZ); // translate into world position
		gl.glTranslatef(center[0], center[1], center[2]); // translate back to original position
		gl.glRotatef(rotateZ, 0, 0, 1); // rotate z
		gl.glRotatef(rotateY, 0, 1, 0); // rotate y
		gl.glRotatef(rotateX, 1, 0, 0); // rotate x
		gl.glScalef(uniScale, uniScale, uniScale);
		gl.glTranslatef(-center[0], -center[1], -center[2]); // translate shape to center
		
		super.draw(gl);
		for (int i = 0; i < contents.size(); i++) {
			gl.glPushMatrix();
			gl.glTranslatef(positions.get(i)[0], positions.get(i)[1], positions.get(i)[2]);
			contents.get(i).draw(gl);
			gl.glPopMatrix();
		}
		
		gl.glPopMatrix();
	}
}