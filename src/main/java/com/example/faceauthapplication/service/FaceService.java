package com.example.faceauthapplication.service;

import com.example.faceauthapplication.model.FaceRecord;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Service
public class FaceService {

        private final Path dataDir = Path.of("face-data");
        private final Path imageDir = dataDir.resolve("images");
        private final Path dbFile = dataDir.resolve("faces.json");

        private final ObjectMapper mapper = new ObjectMapper();
        private Map<String, FaceRecord> records = new HashMap<>();

        @PostConstruct
        public void init() throws IOException {
            Files.createDirectories(imageDir);
            if (Files.exists(dbFile)) {
                records = mapper.readValue(dbFile.toFile(), new TypeReference<>() {});
            } else {
                records = new HashMap<>();
                saveDb();
            }
        }

        private synchronized void saveDb() throws IOException {
            mapper.writerWithDefaultPrettyPrinter().writeValue(dbFile.toFile(), records);
        }

        public synchronized List<FaceRecord> listAll() {
            return new ArrayList<>(records.values());
        }

        public synchronized FaceRecord findById(String id) {
            return records.get(id);
        }

        public synchronized FaceRecord findByName(String name) {
            for (FaceRecord r : records.values()) {
                if (r.getName().equalsIgnoreCase(name)) return r;
            }
            return null;
        }

        public synchronized void delete(String id) throws IOException {
            FaceRecord rec = records.remove(id);
            if (rec != null) {
                Files.deleteIfExists(imageDir.resolve(rec.getImageFilename()));
                saveDb();
            }
        }

        public synchronized boolean isDuplicateDescriptor(float[] desc, double threshold) {
            for (FaceRecord r : records.values()) {
                double d = distance(desc, r.getDescriptor());
                if (d <= threshold) return true;
            }
            return false;
        }

        public synchronized String register(String name, float[] descriptor, MultipartFile image, double duplicateThreshold) throws IOException {
            if (findByName(name) != null) {
                throw new IllegalArgumentException("Name already exists");
            }
            if (isDuplicateDescriptor(descriptor, duplicateThreshold)) {
                throw new IllegalArgumentException("Duplicate face detected");
            }
            String filename = UUID.randomUUID().toString() + ".png";
            Files.copy(image.getInputStream(), imageDir.resolve(filename));
            FaceRecord rec = new FaceRecord(name, descriptor, filename);
            records.put(rec.getId(), rec);
            saveDb();
            return rec.getId();
        }

        public synchronized Optional<Map.Entry<FaceRecord, Double>> findBestMatch(float[] descriptor, double threshold) {
            FaceRecord best = null;
            double bestDist = Double.MAX_VALUE;
            for (FaceRecord r : records.values()) {
                double d = distance(descriptor, r.getDescriptor());
                if (d < bestDist) {
                    bestDist = d;
                    best = r;
                }
            }
            if (best != null && bestDist <= threshold) {
                return Optional.of(Map.entry(best, bestDist));
            }
            return Optional.empty();
        }

        private double distance(float[] a, float[] b) {
            double sum = 0;
            for (int i = 0; i < a.length; i++) {
                double diff = a[i] - b[i];
                sum += diff * diff;
            }
            return Math.sqrt(sum);
        }
    }

