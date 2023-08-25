# Collect results
# First uncomment the spark python packages in flake.nix, then join nix shell:
cd ~/dfd/scripts; ../../nix-portable nix develop ..

# Usage: cat input.json | python 2line.py [output_file]
cat result2.log | node ../log-analyzer2.js | python ../2line.py a_scalability.png
# Usage: <metadata including fd counts> | python 3line.py <ddfd_file> <hyfd_file> <tane_file> <smartfd_file> [output_file]
cat result7.log | python ../hyfd_log_analyzer.py | python3 ../3line.py result3.json result7.json result5.json result6.json bfhd_runtime_over_r.png
echo | python3 ../8line.py result8.json result8.json result8.json result8.json jlnp_runtime_over_c.png

