import sys
import json
import matplotlib.pyplot as plt

def plot_metadata():
    # Read additional metadata from stdin
    metadata_input = sys.stdin.read().strip()
    try:
        metadata = json.loads(metadata_input)
    except json.JSONDecodeError:
        print("Error decoding JSON metadata from stdin")
        sys.exit(1)

    # Extracting metadata for FDs
    metadata_r_values = [entry["meta"]["r"]*1000 for entry in metadata]
    metadata_fds = [entry["meta"]["fds"] for entry in metadata]

    # Plotting metadata FDs on a new axis
    ax2 = plt.gca().twinx()
    ax2.plot(metadata_r_values, metadata_fds, 'o-', color='tab:orange', alpha=0.5, label="FDs")
    ax2.set_ylabel("Number of FDs")
    ax2.tick_params(axis='y', labelcolor='tab:orange')

def process_input_file(file_path):
    with open(file_path, "r") as file:
        file_data = file.read().strip()
        try:
            parsed_data = json.loads(file_data)
            return parsed_data["results"]
        except json.JSONDecodeError:
            print(f"Error decoding JSON data from: {file_path}")
            return []

# Process input files from command line arguments
if len(sys.argv) <5:
    print("Usage: python3 script.py <ddfd_file> <hyfd_file> <tane_file> <smartfd_file> [output_file]")
    sys.exit(1)

ddfd_file = sys.argv[1]
hyfd_file = sys.argv[2]
tane_file = sys.argv[3]
smartfd_file = sys.argv[4]

output = sys.argv[5] if len(sys.argv) >= 6 else "bfhd_runtime_over_r.png"

# Algorithm friendly names
algorithm_names = {
    "ddfd": "DDFD",
    "hyfd": "HyFD",
    "tane": "Distributed TANE (improved)",
    "smartfd": "SmartFD"
}

# Organize and filter data based on command type
data_files = {
    "ddfd": ddfd_file,
    "hyfd": hyfd_file,
    "tane": tane_file,
    "smartfd": smartfd_file
}

mean_runtimes = {}
stddevs = {}
r_values = {}

for command_type, file_path in data_files.items():
    results = process_input_file(file_path)
    filtered_results = [result for result in results if command_type in result["command"]]
    mean_runtimes[command_type] = [result["mean"] for result in filtered_results]
    stddevs[command_type] = [result["stddev"] for result in filtered_results]
    r_values[command_type] = [int(result["parameters"]["r"]) * 1000 for result in filtered_results]

# Create the line graph with error bars for each command type
for command_type, _ in data_files.items():
    plt.errorbar(r_values[command_type], mean_runtimes[command_type], yerr=stddevs[command_type], fmt='o-', label=f"{algorithm_names[command_type]}")

#plot_metadata()

plt.xlabel("# Rows (Log Scale)")
plt.ylabel("Mean Runtime [s]")
plt.title("Runtime on NCVoter with 17 columns")
plt.legend()
plt.grid(True)

# Set log scale for x-axis
plt.xscale('log')
plt.yscale('log')

# Set x-axis ticks for 1k, 10k, 100k, and 1M
plt.xticks([1000, 10000, 100000, 1000000], ["1k", "10k", "100k", "1M"])

# Save the graph as a PNG image
plt.savefig(output)

# Show the plot
plt.show()
