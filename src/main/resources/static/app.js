// app.js
const video = document.getElementById('video');
const status = document.getElementById('status');

// model path: either host models under /models/ or use CDN models - here we assume you copy models to /models/
//const MODEL_URL = '/models';

async function setupCamera() {
    try {
        const stream = await navigator.mediaDevices.getUserMedia({video: true});
        video.srcObject = stream;
        // Wait for video metadata to load so width & height are available
        video.addEventListener('loadedmetadata', () => {
            faceapi.matchDimensions(canvas, {
                width: video.videoWidth,
                height: video.videoHeight
            });
        });
        await new Promise(r => video.onloadedmetadata = r);
    } catch (err) {
        alert('Camera error: ' + err);
    }
}
// load models

    async function loadModels(){
        // load tinyFaceDetector + face Landmarks + face recognition model
        const modelUrl = '/models';
        await faceapi.nets.tinyFaceDetector.loadFromUri(modelUrl);
        await faceapi.nets.faceLandmark68TinyNet.loadFromUri(modelUrl);
        await faceapi.nets.faceRecognitionNet.loadFromUri(modelUrl);

}

function show(msg){ status.innerText = msg; }

async function captureDescriptor(){
    // detect single face and compute descriptor
    const options = new faceapi.TinyFaceDetectorOptions({ inputSize: 224, scoreThreshold: 0.5 });
    const result = await faceapi.detectSingleFace(video, options).withFaceLandmarks(true).withFaceDescriptor();
    if (!result) throw new Error('No face detected');
    return result.descriptor; // Float32Array
}

function float32ArrayToJson(desc){
    return JSON.stringify(Array.from(desc));
}

async function registerFace(){
    const name = document.getElementById('name').value.trim();
    if (!name) { alert('Enter name'); return; }

    show('Capturing face...');
    try {
        const descriptor = await captureDescriptor();
        show('Sending to server...');
        // capture image frame as blob
        const canvas = faceapi.createCanvasFromMedia(video);
        faceapi.matchDimensions(canvas, { width: video.videoWidth, height: video.videoHeight });
        canvas.getContext('2d').drawImage(video,0,0,video.videoWidth,video.videoHeight);
        const blob = await new Promise(res => canvas.toBlob(res,'image/png'));

        // build multipart form data
        const fd = new FormData();
        fd.append('name', name);
        fd.append('descriptor', float32ArrayToJson(descriptor));
        fd.append('image', blob, 'capture.png');

        const resp = await fetch('/api/register', { method: 'POST', body: fd });
        const j = await resp.json();
        if (resp.ok) {
            show('Registered OK: ' + j.id);
        } else {
            show('Register failed: ' + j.message);
        }
    } catch (err) {
        show('Error: ' + err.message);
    }
}

async function matchFace(){
    show('Capturing face for match...');
    try {
        const descriptor = await captureDescriptor();
        show('Sending for match...');
        const resp = await fetch('/api/match', {
            method: 'POST',
            headers: {'Content-Type':'application/json'},
            body: JSON.stringify({ descriptor: Array.from(descriptor) })
        });
        const j = await resp.json();
        if (j.status==='ok') {
            show(`Matched: ${j.name} (distance ${j.distance.toFixed(4)})`);
            // redirect on success (example)
            // window.location.href = '/welcome?user=' + encodeURIComponent(j.name);
        } else {
            show('No match found');
            alert('Access denied: no match');
        }
    } catch (err) {
        show('Error: ' + err.message);
    }
}

document.getElementById('registerBtn').addEventListener('click', registerFace);
document.getElementById('matchBtn').addEventListener('click', matchFace);

(async ()=>{
    show('Loading models...');
    await loadModels();
    show('Starting camera...');
    await setupCamera();
    show('Ready');
})();
