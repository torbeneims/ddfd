import re
import sys
import json

# Read logs from stdin
logs = sys.stdin.read()

# Split logs into lines
log_lines = logs.strip().split('\n')

# Initialize variables
benchmarks = []
current_benchmark = None

# Define patterns for extracting meta information
benchmark_pattern = re.compile(r"Benchmark (\d+):\s[^$]*?data\/(\w+?)(\d+)kr(\d+)c")
meta_pattern = re.compile(r"(\d+):\s([^]*?data\/(\w+?)(\d+)kr(\d+)c[^]*?)")

# Extract information from log lines
for line in log_lines:
    # Match benchmark line
    match = benchmark_pattern.match(line)
    if match:
        i, dataset, r, c = match.groups()
        r, c, i = map(int, (r, c, i))
        current_benchmark = {
            "meta": {
                "i": i,
                "dataset": dataset,
                "r": r,
                "c": c,
                "fds": None
            }
        }
        benchmarks.append(current_benchmark)
    else:
        fd_count_pattern = re.compile(r"FD Count: (\d+)")
        fd_count_match = fd_count_pattern.match(line)
        if fd_count_match and current_benchmark:
            fd_count = int(fd_count_match.group(1))
            old = current_benchmark["meta"]["fds"]
            if old and old != fd_count:
                print(f"Error, mismatching fd counts in runs (is {old}, new {fd_count})")
                exit(1)
            current_benchmark["meta"]["fds"] = fd_count

# Print extracted information as JSON
print(json.dumps(benchmarks, indent=4))
