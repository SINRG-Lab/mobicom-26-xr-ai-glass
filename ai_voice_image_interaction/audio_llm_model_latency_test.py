import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns

dark_blue    = "#1f77b4"
light_blue   = "#aec7e8"
dark_orange  = "#ff7f0e"
light_orange = "#ffbb78"
green        = "#2ca02c" 

data = pd.read_csv("GPT-4o Total Latency Tests.csv")
data1 = pd.read_csv("GPT-4o-mini Total Latency Tests.csv")
data2 = pd.read_csv("Claude Haiku Total Latency Tests.csv")
data3 = pd.read_csv("Claude Sonnet Total Latency Tests.csv")
data4 = pd.read_csv("Gemini Total Latency Tests.csv")

# Get data
data =  data["total latency (s)"]
data1 =  data1["total latency (s)"]
data2 =  data2["total latency (s)"]
data3 =  data3["total latency (s)"]
data4 =  data4["total latency (s)"]

# Sort the data
sorted_data = np.sort(data)
sorted_data1 = np.sort(data1)
sorted_data2 = np.sort(data2)
sorted_data3 = np.sort(data3)
sorted_data4 = np.sort(data4)

# Calculate CDF
cdf = np.arange(1, len(sorted_data) + 1) / len(sorted_data)
cdf1 = np.arange(1, len(sorted_data1) + 1) / len(sorted_data1)
cdf2 = np.arange(1, len(sorted_data2) + 1) / len(sorted_data2)
cdf3 = np.arange(1, len(sorted_data3) + 1) / len(sorted_data3)
cdf4 = np.arange(1, len(sorted_data4) + 1) / len(sorted_data4)

# Statistical Features
latency_mean = np.mean(data)
latency_median = np.median(data)
latency_std = np.std(data)
latency_var = np.var(data)
# print(f"Latency mean: {latency_mean:.2f}, Latency median: {latency_median:.2f}, Latency standard deviation: {latency_std:.2f}, Latency variance: {latency_var:.2f}")

# Plot CDF
OpenAI_plot = plt.plot(sorted_data, cdf, linewidth=7, color=dark_blue, label="gpt-4o")
plt.scatter(sorted_data, cdf, s=10, color="black")

OpenAI_mini_plot = plt.plot(sorted_data1, cdf1, linewidth=7, color=light_blue, label="gpt-4o-mini")
plt.scatter(sorted_data1, cdf1, s=10, color="black")

Anthropic_plot = plt.plot(sorted_data3, cdf3, linewidth=7, color=dark_orange, label='claude-sonnet-4')
plt.scatter(sorted_data3, cdf3, s=10, color="black")

Anthropic_mini_plot = plt.plot(sorted_data2, cdf2, linewidth=7, color=light_orange, label='claude-haiku-3-5')
plt.scatter(sorted_data2, cdf2, s=10, color="black")

Google_plot = plt.plot(sorted_data4, cdf4, linewidth=7, color=green, label='gemini-2.5-flash')
plt.scatter(sorted_data4, cdf4, s=10, color="black")

print(np.mean(data))
print(np.mean(data1))
print(np.mean(data2))
print(np.mean(data3))
print(np.mean(data4))

plt.xlabel('AI Voice Interaction Latency (s)', fontsize=50)
plt.ylabel('CDF', fontsize=50)
plt.tick_params(axis="both", labelsize="50")
plt.xlim(8, 13)
plt.ylim(0, 1)
plt.grid(True)

plt.legend(fontsize="40", loc="lower right")
plt.subplots_adjust(left=.2,bottom=.2)
plt.show()