import os, cv2, re, easyocr, base64
import matplotlib.pyplot as plt
import numpy as np
from openai import OpenAI
from ultralytics import YOLO

def numerical_sort(value):
    numbers = re.findall(r'\d+', value)
    return int(numbers[0]) if numbers else -1

model = YOLO("yolov5su.pt")
client = os.getenv("OPENAI_API_KEY")

image_folder = "Smart Glasses Voice AI Power Tests"

i = 10

power = []
time = []

power_mean = []

for filename in sorted(os.listdir(image_folder), key=numerical_sort):
    reader = easyocr.Reader(['en'])

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
    cv2.imwrite(processed_path, cropped)

    # results = reader.readtext(cropped, detail=0)
    # print(results, "\n")

    with open(processed_path, "rb") as f:
        image_bytes = f.read()
    image_base64 = base64.b64encode(image_bytes).decode("utf-8")

    prompt = f"Get the current from this text originating from a power supply reading, just include the number and nothing else in your response"
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

    current_reading = float(response.choices[0].message.content.strip())
    if current_reading > 1:
        current_reading = current_reading / 1000
        print(current_reading)
    power_reading = 5 * current_reading

    if current_reading < .1:
        power_mean.append(power_reading)

    power.append(power_reading)
    time.append(i)

    i += 1

mean_power = np.mean(power_mean)

start_time = time[0]
end_time = time[-1]

first_times = list(range(int(start_time) - 10, int(start_time)))
last_times = list(range(int(end_time) + 1, int(end_time) + 11))

first_powers = [mean_power] * 10
last_powers = [mean_power] * 10

time_extended = first_times + time + last_times
power_extended = first_powers + power + last_powers

time_extended, power_extended = zip(*sorted(zip(time_extended, power_extended)))

plt.plot(time_extended, power_extended, linewidth=7, color="blue")
plt.scatter(time_extended, power_extended, s=10, color="black")
plt.xlabel('Time (s)', fontsize=60)
plt.ylabel('Power (W)', fontsize=60)
plt.xlim(0, 50)
plt.tick_params(axis="both", labelsize="50")
plt.grid(True)
plt.show()