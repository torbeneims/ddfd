import sys
from os.path import splitext
from pyspark.sql import SparkSession

def convert_csv_to_json(input_csv, output_json, separator):
    # Create a SparkSession
    spark = SparkSession.builder.appName("CSVtoJSON").getOrCreate()

    # Read the CSV file and create a DataFrame
    df = spark.read.csv(input_csv, header=True, inferSchema=True, sep=separator)

    # Save the DataFrame as a JSON file (overwrite)
    df.write.mode("overwrite").json(output_json)

    # Stop the SparkSession
    spark.stop()

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python script.py input.csv [separator]")
        sys.exit(1)

    input_csv = sys.argv[1]
    output_json = splitext(input_csv)[0] + ".json"

    # Determine the separator (default to , if not provided)
    separator = sys.argv[2] if len(sys.argv) > 2 else ","

    convert_csv_to_json(input_csv, output_json, separator)
