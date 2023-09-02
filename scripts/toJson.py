import sys
import csv
import json

def convert_to_json(input_data, separator):
    output = []

    csv_reader = csv.reader(input_data.splitlines(), delimiter=separator)
    for index, row in enumerate(csv_reader):
        record = {'index': index}
        for col_num, value in enumerate(row):
            col_name = 'C' + str(col_num)
            record[col_name] = value
        output.append(record)

    return output

if __name__ == "__main__":
    separator = ',' if len(sys.argv) < 2 else sys.argv[1]
    input_data = sys.stdin.read()
    output_data = convert_to_json(input_data, separator)

    for record in output_data:
        print(json.dumps(record))

