import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns

data = pd.read_csv("GPT-4o Total Latency Tests.csv")

print(np.mean(data['G2P Latency (s)']))
print(np.mean(data['network latency (s)']))
print(np.mean(data['P2S latency (s)']))
print(np.mean(data['stt latency (s)']))
print(np.mean(data['total llm latency (s)']))
print(np.mean(data['total tts latency (s)']))
print(np.mean(data['stt latency (s)']) / np.mean(data['total latency (s)']))

data = {
    'G2P': data['G2P Latency (s)'],
    'Network': data['network latency (s)'],
    'VAD': data['P2S latency (s)'],
    'STT': data['stt latency (s)'],
    'LLM': data['total llm latency (s)'],
    'TTS': data['total tts latency (s)'],
}
df = pd.DataFrame(data)

# grouped = df.groupby(df.index // 10).median()
grouped = df.groupby(df.index // 60).median()
stds = df.groupby(df.index // 60).std()

grouped_T = grouped.mean().to_frame(name='Median Latency (s)')
stds_T = stds.mean().to_frame(name='Std Latency (s)')

colors = ["#1f77b4", "#ff7f0e", "#2ca02c", "#d62728", "#9467bd"]

ax = grouped_T['Median Latency (s)'].plot(
    kind='bar',
    yerr=stds_T['Std Latency (s)'],            
    color=colors,         
    stacked=False,
    figsize=(28, 16),
    legend=False,
    error_kw=dict(
        lw=4,       
        capsize=12, 
        capthick=4 
    )
)

plt.ylabel('Latency (s)', fontsize=50)
#plt.xlabel('Latency Breakdowns', fontsize=40)
plt.tick_params(axis="both", labelsize=50)
plt.xticks(rotation=0, ha='center')
plt.ylim(0, 7)
plt.grid(True, axis='y')
plt.tight_layout()

# plt.legend(fontsize="20",loc='upper right')
plt.subplots_adjust(left=.2,bottom=.2)
plt.show()