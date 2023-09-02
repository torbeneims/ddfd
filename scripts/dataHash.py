import csv
import hashlib
import sys

def hash_to_int(value):
    # Use hashlib to hash the value and convert it to an integer
    hash_obj = hashlib.sha256(value.encode())
    hash_digest = hash_obj.hexdigest()
    return int(hash_digest, 16)% (2**8)

def hash_csv_file(input_file, separator=','):
    output_file = input_file[:-4] + "_hashed.csv"  # Append "_hashed" to the output file name

    with open(input_file, 'r') as infile, open(output_file, 'w', newline='') as outfile:
        reader = csv.reader(infile, delimiter=separator)
        writer = csv.writer(outfile)

        for row in reader:
            hashed_row = [hash_to_int(value) for value in row]
            writer.writerow(hashed_row)

    print("CSV file hashed and saved to", output_file)

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python hash_csv.py <input_csv_file> [separator]")
        sys.exit(1)

    input_csv_file = sys.argv[1]
    separator = sys.argv[2] if len(sys.argv) > 2 else ','

    hash_csv_file(input_csv_file, separator)
