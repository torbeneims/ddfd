import re
import json
import sys

# Read data from stdin
data = sys.stdin.read()

# Define the regex pattern
pattern = r"Benchmark (\d+)[^]*?-t (\d+)[^]*?Task times: LongSummaryStatistics\{(.+)\}[^§]Total time:.*(\d+\.\d+)s"

# Find all matches using the regex pattern
matches = re.findall(pattern, data)

# Organize the extracted information and format the output
results = []
for match in matches:
    benchmark_num, t_value, task_times, total_time = match
    print(match)

    task_times = [int(time) for time in task_times.split(', ')]

    result = {
        'i': int(benchmark_num),
        't': int(t_value),
        'Task Times': task_times,
        'Total Time': float(total_time)
    }
    results.append(result)

# Print the formatted output using JSON
output_json = json.dumps(results, indent=4)
print(output_json)
