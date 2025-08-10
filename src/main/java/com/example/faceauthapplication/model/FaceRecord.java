package com.example.faceauthapplication.model;

import java.util.UUID;

public class FaceRecord {

        private String id;
        private String name;
        private float[] descriptor;
        private String imageFilename;

        public FaceRecord() {}

        public FaceRecord(String name, float[] descriptor, String imageFilename) {
            this.id = UUID.randomUUID().toString();
            this.name = name;
            this.descriptor = descriptor;
            this.imageFilename = imageFilename;
        }

        // getters & setters
        public String getId(){return id;}
        public void setId(String id){this.id=id;}
        public String getName(){return name;}
        public void setName(String name){this.name=name;}
        public float[] getDescriptor(){return descriptor;}
        public void setDescriptor(float[] descriptor){this.descriptor=descriptor;}
        public String getImageFilename(){return imageFilename;}
        public void setImageFilename(String imageFilename){this.imageFilename=imageFilename;}
    }


