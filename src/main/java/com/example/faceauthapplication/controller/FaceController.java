package com.example.faceauthapplication.controller;

import com.example.faceauthapplication.model.FaceRecord;
import com.example.faceauthapplication.service.FaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class FaceController {


        @Autowired
        private FaceService service;

        // threshold: recommended ~0.5-0.6 for face-api descriptors (adjust later)
        private final double MATCH_THRESHOLD = 0.3;
        private final double DUPLICATE_THRESHOLD = 0.5;

        @GetMapping("/list")
        public List<FaceRecord> list() {
            return service.listAll();
        }

        @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<?> register(
                @RequestParam("name") String name,
                @RequestParam("descriptor") String descriptorJson,
                @RequestPart("image") MultipartFile image
        ) throws IOException {
            // descriptorJson is something like "[0.0123, -0.234, ...]"
            float[] descriptor = parseDescriptor(descriptorJson);
            try {
                String id = service.register(name, descriptor, image, DUPLICATE_THRESHOLD);
                return ResponseEntity.ok(Map.of("status","ok","id",id));
            } catch (IllegalArgumentException ex) {
                return ResponseEntity.badRequest().body(Map.of("status","error","message",ex.getMessage()));
            }
        }

        @PostMapping("/match")
        public ResponseEntity<?> match(@RequestBody Map<String,Object> body) {
            Object descObj = body.get("descriptor");
            if (descObj == null) return ResponseEntity.badRequest().body(Map.of("status","error","message","no descriptor"));
            float[] descriptor = parseDescriptorFromObj(descObj);
            var match = service.findBestMatch(descriptor, MATCH_THRESHOLD);
            if (match.isPresent()) {
                var entry = match.get();
                FaceRecord r = entry.getKey();
                double dist = entry.getValue();
                return ResponseEntity.ok(Map.of("status","ok","id",r.getId(),"name",r.getName(),"distance",dist,"image", "/images/"+r.getImageFilename()));
            } else {
                return ResponseEntity.ok(Map.of("status","no-match"));
            }
        }

        @DeleteMapping("/delete/{id}")
        public ResponseEntity<?> delete(@PathVariable String id) throws IOException {
            service.delete(id);
            return ResponseEntity.ok(Map.of("status","ok"));
        }

        private float[] parseDescriptor(String json) {
            // naive parsing, use ObjectMapper in production
            json = json.trim().replaceAll("[\\[\\]]", "");
            String[] parts = json.split(",");
            float[] arr = new float[parts.length];
            for (int i = 0; i < parts.length; i++) arr[i] = Float.parseFloat(parts[i].trim());
            return arr;
        }

        private float[] parseDescriptorFromObj(Object obj) {
            // obj might be a List<Double> from Jackson
            List<?> list = (List<?>)obj;
            float[] arr = new float[list.size()];
            for (int i = 0; i < list.size(); i++) {
                arr[i] = ((Number)list.get(i)).floatValue();
            }
            return arr;
        }
    }

