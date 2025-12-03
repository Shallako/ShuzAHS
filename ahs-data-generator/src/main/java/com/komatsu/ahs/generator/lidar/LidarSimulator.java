package com.komatsu.ahs.generator.lidar;

import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.math.Ray;
import com.jme3.scene.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * LidarSimulator casts many rays using JME's collision system against a scene graph
 * and returns the hit points as a simple point cloud (list of positions). This works
 * headlessly and does not require a running JME application.
 */
public class LidarSimulator {
    private final Node sceneRoot;
    private final LidarConfig config;

    public LidarSimulator(Node sceneRoot, LidarConfig config) {
        this.sceneRoot = sceneRoot;
        this.config = config;
    }

    /**
     * Perform a scan from the given vehicle pose.
     * @param vehiclePos world position of the vehicle origin
     * @param vehicleRot world rotation of the vehicle
     * @return list of hit points (world coordinates)
     */
    public List<Vector3f> scan(Vector3f vehiclePos, Quaternion vehicleRot) {
        final List<Vector3f> points = new ArrayList<>();

        // Ensure world bounds are up to date
        sceneRoot.updateGeometricState();

        // Sensor origin in world space
        Vector3f origin = vehiclePos.add(vehicleRot.mult(config.getSensorOffset()));

        // Sweep angles
        float hFov = config.getHorizontalFovDeg() * FastMath.DEG_TO_RAD;
        float vFov = config.getVerticalFovDeg() * FastMath.DEG_TO_RAD;

        int hSamples = Math.max(1, config.getHorizontalSamples());
        int vSamples = Math.max(1, config.getVerticalSamples());

        float hStart = -hFov * 0.5f;
        float vStart = -vFov * 0.5f;

        for (int vi = 0; vi < vSamples; vi++) {
            float vT = vSamples == 1 ? 0f : (float) vi / (float) (vSamples - 1);
            float pitch = vStart + vT * vFov; // rotation around X

            for (int hi = 0; hi < hSamples; hi++) {
                float hT = hSamples == 1 ? 0f : (float) hi / (float) (hSamples - 1);
                float yaw = hStart + hT * hFov; // rotation around Y

                // Local direction (forward along +Z), apply yaw/pitch and then vehicle rotation
                Quaternion dirRot = new Quaternion().fromAngles(pitch, yaw, 0f);
                Vector3f dir = vehicleRot.mult(dirRot.mult(Vector3f.UNIT_Z)).normalizeLocal();

                Ray ray = new Ray(origin, dir);
                ray.setLimit(config.getMaxRange());

                CollisionResults results = new CollisionResults();
                sceneRoot.collideWith(ray, results);
                if (results.size() > 0) {
                    CollisionResult closest = results.getClosestCollision();
                    points.add(closest.getContactPoint().clone());
                }
            }
        }

        return points;
    }
}
