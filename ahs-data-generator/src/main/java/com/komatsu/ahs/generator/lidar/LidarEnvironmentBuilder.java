package com.komatsu.ahs.generator.lidar;

import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;

/**
 * Builds a simple static scene to collide rays against: a flat ground and a few boxes.
 */
public class LidarEnvironmentBuilder {

    public static Node buildDefaultScene() {
        Node root = new Node("lidar-scene");

        // Ground: a large thin box at y = 0
        Geometry ground = new Geometry("ground", new Box(500f, 0.1f, 500f));
        ground.setLocalTranslation(new Vector3f(0, -0.1f, 0));
        ground.updateModelBound();
        root.attachChild(ground);

        // Obstacles: a few boxes at different positions/heights
        root.attachChild(box("ob1", 2f, 2f, 2f, 10f, 1f, 20f));
        root.attachChild(box("ob2", 1f, 4f, 1f, -15f, 2f, 30f));
        root.attachChild(box("ob3", 3f, 1.5f, 3f, 25f, 0.75f, -12f));
        root.attachChild(box("ob4", 5f, 3f, 1f, -30f, 1.5f, -25f));

        root.updateGeometricState();
        return root;
    }

    private static Geometry box(String name, float x, float y, float z, float tx, float ty, float tz) {
        Geometry g = new Geometry(name, new Box(x, y, z));
        g.setLocalTranslation(new Vector3f(tx, ty, tz));
        g.updateModelBound();
        return g;
    }
}
