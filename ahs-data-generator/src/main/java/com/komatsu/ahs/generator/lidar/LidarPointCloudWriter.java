package com.komatsu.ahs.generator.lidar;

import com.jme3.math.Vector3f;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class LidarPointCloudWriter {

    /**
     * Write points to a CSV file with header: x,y,z
     */
    public static void writeCsv(List<Vector3f> points, File file) throws IOException {
        // Create parent directories if missing
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
            w.write("x,y,z\n");
            for (Vector3f p : points) {
                w.write(p.x + "," + p.y + "," + p.z + "\n");
            }
        }
    }
}
