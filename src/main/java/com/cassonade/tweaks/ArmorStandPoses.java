package com.cassonade.tweaks;

import java.util.List;

import net.minecraft.core.Rotations;
import net.minecraft.world.entity.decoration.ArmorStand;

/** The poses of Bedrock Edition's armor stands, in its order, the first being a stand's default. */
public final class ArmorStandPoses {
	private record Pose(Rotations head, Rotations body, Rotations leftArm, Rotations rightArm, Rotations leftLeg, Rotations rightLeg) {
		boolean isOn(ArmorStand stand) {
			return head.equals(stand.getHeadPose()) && body.equals(stand.getBodyPose())
					&& leftArm.equals(stand.getLeftArmPose()) && rightArm.equals(stand.getRightArmPose())
					&& leftLeg.equals(stand.getLeftLegPose()) && rightLeg.equals(stand.getRightLegPose());
		}

		void applyTo(ArmorStand stand) {
			stand.setHeadPose(head);
			stand.setBodyPose(body);
			stand.setLeftArmPose(leftArm);
			stand.setRightArmPose(rightArm);
			stand.setLeftLegPose(leftLeg);
			stand.setRightLegPose(rightLeg);
		}
	}

	private static final List<Pose> POSES = List.of(
			pose(0, 0, 0, 0, 0, 0, -10, 0, -10, -15, 0, 10, -1, 0, -1, 1, 0, 1), // Default
			pose(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), // No pose
			pose(15, 0, 0, 0, 0, 0, -30, 15, 15, -30, -15, -15, 0, 0, 0, 0, 0, 0), // Solemn
			pose(-5, 0, 0, 0, 0, 0, 10, 0, -5, -60, 20, -10, -3, -3, -3, 3, 3, 3), // Athena
			pose(-15, 0, 0, 0, 0, 0, 20, 0, -10, -110, 50, 0, 5, -3, -3, -5, 3, 3), // Brandish
			pose(-15, 0, 0, 0, 0, 0, -110, 35, 0, -110, -35, 0, 5, -3, -3, -5, 3, 3), // Honor
			pose(-15, 0, 0, 0, 0, 0, -110, -35, 0, -110, 35, 0, 5, -3, -3, -5, 3, 3), // Entertain
			pose(0, 0, 0, 0, 0, 0, 10, 0, -5, -70, -40, 0, -1, 0, -1, 1, 0, 1), // Salute
			pose(16, 20, 0, 0, 0, 0, 4, 8, 237, 246, 0, 89, -14, 0, -6, 8, 20, 4), // Riposte
			pose(-10, 0, -5, 0, 0, 0, -105, 0, 0, -100, 0, 0, 7, 0, 0, -46, 0, 0), // Zombie
			pose(-5, 18, 0, 0, 22, 0, 8, 0, -114, 0, 84, 111, -111, 55, 0, 0, 0, 0), // Cancan A
			pose(-10, -20, 0, 0, -18, 0, 0, 0, -112, 8, 90, 111, 0, 0, 0, -111, -55, 0), // Cancan B
			pose(-4, 67, 0, 0, 8, 0, 16, 32, -8, -99, 63, 0, 0, -75, -8, 4, 63, 8)); // Hero

	private ArmorStandPoses() {
	}

	private static Pose pose(float... angles) {
		return new Pose(new Rotations(angles[0], angles[1], angles[2]), new Rotations(angles[3], angles[4], angles[5]),
				new Rotations(angles[6], angles[7], angles[8]), new Rotations(angles[9], angles[10], angles[11]),
				new Rotations(angles[12], angles[13], angles[14]), new Rotations(angles[15], angles[16], angles[17]));
	}

	/** The next pose, read from the stand itself so that it needs no saving: one set by commands goes back to the default. */
	public static void next(ArmorStand stand) {
		for (int i = 0; i < POSES.size(); i++) {
			if (POSES.get(i).isOn(stand)) {
				POSES.get((i + 1) % POSES.size()).applyTo(stand);
				return;
			}
		}
		POSES.getFirst().applyTo(stand);
	}
}
