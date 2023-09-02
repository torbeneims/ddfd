import json
import sys
import re

def combine_json_arrays(concatenated_json):
    try:
        # Use regex to extract JSON arrays
        json_arrays = re.split(r'\][\w\n]*\[', concatenated_json)

        if len(json_arrays) != 2:
            return "Error: Expected exactly 2 JSON arrays."

        array1 = json.loads(json_arrays[0] + ']')
        array2 = json.loads('[' + json_arrays[1])

        combined_array = array1 + array2

        combined_json = json.dumps(combined_array, indent=2)
        return combined_json
    except json.JSONDecodeError:
        return "Error: Invalid JSON input."

if __name__ == "__main__":
    try:
        # Read the concatenated JSON arrays from standard input
        concatenated_input = sys.stdin.read().strip()

        combined_json = combine_json_arrays(concatenated_input)
        print(combined_json)
    except KeyboardInterrupt:
        print("Process interrupted.")
