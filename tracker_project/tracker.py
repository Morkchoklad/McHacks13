import cv2
import numpy as np
import json
import time
import io
import os
from dotenv import load_dotenv
from google import genai
from google.genai import types
from datetime import datetime
import django

# --- Configuration ---
load_dotenv()
API_KEY = os.getenv("GEMINI_API_KEY")
client = genai.Client(api_key=API_KEY)

# --- Django Setup ---
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'tracker_project.settings')
django.setup()
from core.models import ObjectSighting

def process_frame_with_gemini(frame):
    is_success, buffer = cv2.imencode(".jpg", frame)
    if not is_success: return []
    prompt = 'Detect: "keys", "glasses", "phone". Return a JSON list of objects. Each object has "name" and "box_2d" [ymin, xmin, ymax, xmax]. Scale 0-1000.'
    try:
        response = client.models.generate_content(
            model='gemini-2.0-flash',
            contents=[
                prompt,
                types.Part.from_bytes(data=io.BytesIO(buffer).getvalue(), mime_type="image/jpeg")
            ],
            config=types.GenerateContentConfig(response_mime_type='application/json')
        )
        text = response.text.strip()
        if text.startswith("```"):
            text = text.replace("```json", "").replace("```", "").strip()
        return json.loads(text)
    except Exception as e:
        print(f"Gemini Error: {e}")
        return []

def main():
    cap = cv2.VideoCapture(0)
    
    # Ensure media directory exists
    save_dir = os.path.join("media", "sightings")
    os.makedirs(save_dir, exist_ok=True)
    
    last_process_time = 0
    process_interval = 5 
    last_processed_frame_gray = None
    motion_threshold = 1  # Adjust sensitivity (higher = less sensitive)

    print("Tracker running... Press 'q' to quit.")

    while True:
        ret, frame = cap.read()
        if not ret: break
        h, w, _ = frame.shape
        
        # Convert to grayscale and blur for motion detection
        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        gray = cv2.GaussianBlur(gray, (21, 21), 0)

        if time.time() - last_process_time > process_interval:
            should_process = False
            if last_processed_frame_gray is None:
                should_process = True
            else:
                # Calculate difference between current frame and last processed frame
                diff = cv2.absdiff(last_processed_frame_gray, gray)
                mean_diff = np.mean(diff)
                if mean_diff > motion_threshold:
                    should_process = True
                    print(f"Motion detected (Score: {mean_diff:.2f}). Processing...")
                else :
                    print(f"Motion Score: {mean_diff:.2f}")
            if should_process:
                objects = process_frame_with_gemini(frame)
                if isinstance(objects, list):
                    for obj in objects:
                        name = obj.get('name')
                        box = obj.get('box_2d')
                        if box and isinstance(box, list) and len(box) >= 4:
                            # Draw bounding box on a copy of the frame
                            frame_copy = frame.copy()
                            ymin, xmin, ymax, xmax = box[:4]
                            start_point = (int(xmin/1000*w), int(ymin/1000*h))
                            end_point = (int(xmax/1000*w), int(ymax/1000*h))
                            
                            cv2.rectangle(frame_copy, start_point, end_point, (0, 255, 0), 2)
                            cv2.putText(frame_copy, name, (start_point[0], start_point[1]-10), cv2.FONT_HERSHEY_SIMPLEX, 0.9, (0, 255, 0), 2)
                            
                            # Save Image
                            timestamp_str = datetime.now().strftime("%Y%m%d_%H%M%S_%f")
                            filename = f"{name}_{timestamp_str}.jpg"
                            filepath = os.path.join(save_dir, filename)
                            cv2.imwrite(filepath, frame_copy)
                            
                            # Save to SQLite
                            image_url = f"/media/sightings/{filename}"
                            ObjectSighting.objects.create(object_name=name, image_url=image_url)
                            print(f"Logged {name} to Django DB.")
                last_process_time = time.time()
                last_processed_frame_gray = gray
                print('test')
        cv2.imshow('Camera Feed', frame)
        if cv2.waitKey(1) & 0xFF == ord('q'): break
    cap.release()
    cv2.destroyAllWindows()

if __name__ == "__main__":
    main()