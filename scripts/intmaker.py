import sys
import csv
csv.field_size_limit(sys.maxsize)

def build_hash_maps(data):
    hash_maps = {}
    for row in data:
        for col_idx, value in enumerate(row):
            if col_idx not in hash_maps:
                hash_maps[col_idx] = {}
            if value not in hash_maps[col_idx]:
                hash_maps[col_idx][value] = len(hash_maps[col_idx])
    return hash_maps

def replace_with_index(data, hash_maps):
    for row_idx, row in enumerate(data):
        for col_idx, value in enumerate(row):
            data[row_idx][col_idx] = hash_maps[col_idx][value]

def main(delimiter):
    data = []
    header = None

    input_csv = csv.reader(sys.stdin, delimiter=delimiter, quoting=csv.QUOTE_MINIMAL)
    for row in input_csv:
        if header is None:
            header = row
        else:
            data.append(row)

    hash_maps = build_hash_maps(data)
    replace_with_index(data, hash_maps)

    output_csv = csv.writer(sys.stdout, delimiter=delimiter, quoting=csv.QUOTE_MINIMAL)
    output_csv.writerow(header)
    for row in data:
        output_csv.writerow(row)

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python script.py <delimiter>")
        sys.exit(1)

    delimiter = sys.argv[1]
    main(delimiter)
