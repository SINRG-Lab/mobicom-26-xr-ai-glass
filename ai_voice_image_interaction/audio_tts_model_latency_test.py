import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns

data = pd.read_csv("Audio Total Latency Tests.csv")
data1 = pd.read_csv("Whisper Audio Latency Tests.csv")
data2 = pd.read_csv("Flux Audio Latency Tests.csv")

# Get data
data =  data["total latency (s)"]
data1 =  data1["total latency (s)"]
data2 =  data2["total latency (s)"]

# Sort the data
sorted_data = np.sort(data)
sorted_data1 = np.sort(data1)
sorted_data2 = np.sort(data2)

# Calculate CDF
cdf = np.arange(1, len(sorted_data) + 1) / len(sorted_data)
cdf1 = np.arange(1, len(sorted_data1) + 1) / len(sorted_data1)
cdf2 = np.arange(1, len(sorted_data2) + 1) / len(sorted_data2)

# Statistical Features
latency_mean = np.mean(data)
latency_median = np.median(data)
latency_std = np.std(data)
latency_var = np.var(data)
print(f"Latency mean: {latency_mean:.2f}, Latency median: {latency_median:.2f}, Latency standard deviation: {latency_std:.2f}, Latency variance: {latency_var:.2f}")

# Plot CDF
Deepgram_plot = plt.plot(sorted_data, cdf, linewidth=7, color="black", alpha = .7, label='nova-3/aura-2-en')
#lt.scatter(sorted_data1, cdf1, s=10, color="black")

OpenAI_plot = plt.plot(sorted_data1, cdf1, linewidth=7, color="blue", alpha = .7, label="nova-3/open-ai-tts")
#plt.scatter(sorted_data1, cdf1, s=10, color="black")

Flux_plot = plt.plot(sorted_data2, cdf2, linewidth=7, color="green", alpha = .7, label="flux-general/aura-2-en")
#plt.scatter(sorted_data1, cdf1, s=10, color="black")

plt.xlabel('AI Voice Interaction Latency (s)', fontsize=50)
plt.ylabel('CDF', fontsize=50)
plt.tick_params(axis="both", labelsize="50")
plt.xlim(8, 13)
plt.ylim(0, 1)
plt.grid(True)

plt.legend(fontsize="40", loc="lower right")
plt.subplots_adjust(left=.2,bottom=.2)
plt.show()