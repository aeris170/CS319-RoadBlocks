package main;

import java.awt.Color;
import java.awt.Font;
import java.awt.Shape;

import com.doa.engine.DoaCamera;
import com.doa.engine.DoaObject;
import com.doa.engine.graphics.DoaGraphicsContext;
import com.doa.engine.input.DoaKeyboard;
import com.doa.engine.input.DoaMouse;
import com.doa.maths.DoaVectorF;

public class Camera extends DoaObject {

	private static final long serialVersionUID = 1481819429670061069L;

	private static final float KEY_LOOK_SPEED = 4f;
	private static final float MOUSE_LOOK_SPEED = 2f;
	private static final float LOW_PERCENTAGE_FOR_MOUSE_CAMERA = 5;
	private static final float HIGH_PERCENTAGE_FOR_MOUSE_CAMERA = 95;

	private DoaVectorF topLeftBound;
	private DoaVectorF bottomRightBound;

	public Camera(Float x, Float y) {
		super(x, y, DoaObject.STATIC_FRONT);
		topLeftBound = position.clone();
	}

	@Override
	public void tick() {
		if (DoaKeyboard.W || DoaKeyboard.KEY_UP) {
			position.y -= KEY_LOOK_SPEED;
		}
		if (DoaKeyboard.A || DoaKeyboard.KEY_LEFT) {
			position.x -= KEY_LOOK_SPEED;
		}
		if (DoaKeyboard.S || DoaKeyboard.KEY_DOWN) {
			position.y += KEY_LOOK_SPEED;
		}
		if (DoaKeyboard.D || DoaKeyboard.KEY_RIGHT) {
			position.x += KEY_LOOK_SPEED;
		}
		if (DoaMouse.Y < Main.WINDOW_HEIGHT * LOW_PERCENTAGE_FOR_MOUSE_CAMERA / 100) {
			position.y -= MOUSE_LOOK_SPEED;
		}
		if (DoaMouse.X < Main.WINDOW_WIDTH * LOW_PERCENTAGE_FOR_MOUSE_CAMERA / 100) {
			position.x -= MOUSE_LOOK_SPEED;
		}
		if (DoaMouse.Y > Main.WINDOW_HEIGHT * HIGH_PERCENTAGE_FOR_MOUSE_CAMERA / 100) {
			position.y += MOUSE_LOOK_SPEED;
		}
		if (DoaMouse.X > Main.WINDOW_WIDTH * HIGH_PERCENTAGE_FOR_MOUSE_CAMERA / 100) {
			position.x += MOUSE_LOOK_SPEED;
		}
		topLeftBound = new DoaVectorF(Main.WINDOW_WIDTH / 2f + (Main.WINDOW_WIDTH / 2f - (Main.WINDOW_WIDTH / 2f * DoaCamera.getZ())) / DoaCamera.getZ(),
		        Main.WINDOW_HEIGHT / 2f + (Main.WINDOW_HEIGHT / 2f - (Main.WINDOW_HEIGHT / 2f * DoaCamera.getZ())) / DoaCamera.getZ());
		bottomRightBound = new DoaVectorF(Main.WINDOW_WIDTH, Main.WINDOW_HEIGHT).sub(topLeftBound);
		if (position.x < topLeftBound.x) {
			position.x = topLeftBound.x;
		}
		if (position.y < topLeftBound.y) {
			position.y = topLeftBound.y;
		}
		if (position.x > bottomRightBound.x) {
			position.x = bottomRightBound.x;
		}
		if (position.y > bottomRightBound.y) {
			position.y = bottomRightBound.y;
		}
	}

	@Override
	public void render(DoaGraphicsContext g) {
		g.setColor(Color.WHITE);
		g.setFont(new Font("Arial", Font.BOLD, 20));
		g.drawString("Cam Pos: " + position.toString(), 0, 20);
		g.drawString("Cam Top Left Bound: " + topLeftBound.toString(), 0, 40);
		g.drawString("Cam Bottom Right Bound: " + bottomRightBound.toString(), 0, 60);
	}

	@Override
	public Shape getBounds() {
		return null;
	}

}
