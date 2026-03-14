import os, cv2, re, easyocr, base64
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from openai import OpenAI
from ultralytics import YOLO

def numerical_sort(value):
    numbers = re.findall(r'\d+', value)
    return int(numbers[0]) if numbers else -1

# model = YOLO("yolov5su.pt")
client = os.getenv("OPENAI_API_KEY")

image_folder = "Cyan Voltage Tests"

voltage = []

for filename in sorted(os.listdir(image_folder), key=numerical_sort):
    # reader = easyocr.Reader(['en'])

    image_path = os.path.join(image_folder, filename)
    print(image_path)
    image = cv2.imread(image_path)

    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    gray = cv2.convertScaleAbs(gray, alpha=2.0, beta=0)
    blur = cv2.GaussianBlur(gray, (3, 3), 0)

    thresh = cv2.adaptiveThreshold(
        blur, 255,
        cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
        cv2.THRESH_BINARY_INV,
        21, 8
    )

    kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (2, 2))
    clean = cv2.dilate(thresh, kernel, iterations=1)
    clean = cv2.erode(clean, kernel, iterations=1)

    h, w = clean.shape
    cropped = clean[int(h * .2):int(h * .8), int(w * .08):int(w * .9)]

    processed_path = "processed.png"
    cv2.imwrite(processed_path, image)

    # results = reader.readtext(cropped, detail=0)
    # print(results, "\n")

    with open(processed_path, "rb") as f:
        image_bytes = f.read()
    image_base64 = base64.b64encode(image_bytes).decode("utf-8")

    prompt = f"Get the number being read on this multimeter, just include the reading in your response and nothing else."
    response = client.chat.completions.create(
        model="gpt-4o",
            messages=[{
                "role":"user",
                    "content": [
                        {"type": "text", "text": prompt},
                        {"type": "image_url", "image_url": {
                            "url": f"data:image/png;base64,{image_base64}"}
                        }
                    ]
                }],
            max_tokens=120
        )
    print(response.choices[0].message.content.strip(), "\n")

    voltage_reading = float(response.choices[0].message.content.strip())
    voltage.append(voltage_reading)

pattern = re.compile(r"voltage=(\d+)V")

df = pd.DataFrame(voltage)

df.to_csv("Cyan Voltage Tests.csv", index=False)

print("Extracted", len(df), "V")
print(df.head())