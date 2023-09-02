import sys
import json
import matplotlib.pyplot as plt
from itertools import groupby

# Read data from stdin
data = json.load(sys.stdin)

attributes = ["max", "sum"]

t_values = [entry["t"] for entry in data]
max_values = [entry["max"] for entry in data]
total_values = [entry["sum"] for entry in data]
values = {a: [entry[a] for entry in data] for a in attributes}

#[plt.plot(t_values, values[a], label=a) for a in attributes]

print(data)

data = sorted(data, key=lambda x: x["count"])
groups = groupby(data, lambda x: x["count"])
for key, group in groups:
    group = [x for x in group]
    group = sorted(group, key=lambda x: x["t"])
    xval = [x["t"] for x in group]
    yval = [x["total"] for x in group]
    yval2 = [x["max"]/1000 for x in group]
    yval3 = [x["sum"]/1000 for x in group]
    print("total", yval)
    print("max  ", yval2)
    plt.plot(xval, yval, label=f"{key} (total)")
    plt.plot(xval, yval2, label=f"{key} (max)")
    plt.plot(xval, yval3, label=f"{key} (sum)")

plt.xlabel("number of threads")
plt.ylabel("runtime [s]")
plt.title("Runtime over number of jobs")
plt.legend()
plt.grid(True)

# Save the plot as a PNG image
plt.savefig("plot.png")

# Display the plot
plt.show()
