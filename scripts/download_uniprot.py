import requests
import csv
import sys
import re

# Define the URL for the UniprotKB search API
api_url = "https://rest.uniprot.org/uniprotkb/search?query=human&format=tsv"

if len(sys.argv) != 3:
    print(f"Usage: python {sys.argv[0]} <num rows in 1000> <num cols>")
    sys.exit(1)

# Define the number of rows and columns to retrieve
num_rows = int(sys.argv[1])*1000
num_cols = int(sys.argv[2])

# Define the number of rows to retrieve per request
rows_per_request = 500

# Define the file path to save the downloaded data
output_file = f"uniprot{int(num_rows/1000)}kr{num_cols}c.csv"

# Names & Taxonomy Fields
names_taxonomy_fields = [
    "accession",
    "id",
    "gene_names",
    "gene_primary",
    "gene_synonym",
    "gene_oln",
    "gene_orf",
    "organism_name",
    "organism_id",
    "protein_name",
    "xref_proteomes",
    "lineage",
    "lineage_ids",
    "virus_hosts"
]

# Sequences Fields
sequences_fields = [
    "cc_alternative_products",
    "ft_var_seq",
    "error_gmodel_pred",
    "fragment",
    "organelle",
    "length",
    "mass",
    "cc_mass_spectrometry",
    "ft_variant",
    "ft_non_cons",
    "ft_non_std",
    "ft_non_ter",
    "cc_polymorphism",
    "cc_rna_editing",
    "sequence",
    "cc_sequence_caution",
    "ft_conflict",
    "ft_unsure",
    "sequence_version"
]

# Function Fields
function_fields = [
    "absorption",
    "ft_act_site",
    "cc_activity_regulation",
    "ft_binding",
    "cc_catalytic_activity",
    "cc_cofactor",
    "ft_dna_bind",
    "ec",
    "cc_function",
    "kinetics",
    "cc_pathway",
    "ph_dependence",
    "redox_potential",
    "rhea",
    "ft_site",
    "temp_dependence"
]

misc = [
    "annotation_score",
    "cc_caution",
    "comment_count",
    "feature_count",
    "keywordid",
    "keyword",
    #"<does not exist>",
    "cc_miscellaneous",
    "protein_existence",
    "reviewed",
    "tools",
    "uniparc_id"
]

columns = names_taxonomy_fields + sequences_fields + function_fields + misc

if len(columns) < num_cols:
    print(f"Insufficient fields! This program can only download up to {len(columns)} columns ({num_cols} requested)")
    sys.exit(1)
    # uniprot has more....

def download_uniprot_data(api_url, output_path, rows_per_request, columns):
    with open(output_path, "w", newline="") as csv_file:
        csv_writer = csv.writer(csv_file)
        csv_writer.writerow(columns)  # Write the header

        cursor = None
        remaining_rows = num_rows

        link = None
        while remaining_rows > 0:
            params = {
                "cursor": cursor,
                "fields": ",".join(columns),
                "size": min(rows_per_request, remaining_rows)
            }
            response = requests.get(api_url, params=params)

            if response.status_code == 200:
                data_lines = response.text.strip().split("\n")
                data = [line.split("\t") for line in data_lines[1:]]  # Skip header row

                for row in data:
                    csv_writer.writerow(row)
                
                link_header = response.headers.get("Link")
                if link_header:
                    next_link = [link.strip() for link in link_header.split(",") if 'rel="next"' in link]
                    if next_link:
                        cursor = re.search(r"cursor=([^&]+)", next_link[0])
                        if cursor:
                            cursor = cursor.group(1)
                remaining_rows -= rows_per_request
                print(f"{remaining_rows} rows remaining")
            else:
                print(f"Failed to download data. Status code: {response.text}")
                break

# Download and save UniprotKB search results as a CSV file
download_uniprot_data(api_url, output_file, rows_per_request, columns)
