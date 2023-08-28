# Collect results
# First uncomment the spark python packages in flake.nix, then join nix shell:
#cd ~/dfd/scripts; ../../nix-portable nix develop ..#pyShell

# Usage: cat input.json | python 2line.py [output_file]
cat result2.log | node ../log-analyzer2.js | python ../2line.py a_scalability.png || echo "Didn't create a_scalability"
# Usage: <metadata including fd counts> | python 3line.py <ddfd_file> <hyfd_file> <tane_file> <smartfd_file> [output_file]
cat result7_1.log | python ../hyfd_log_analyzer.py | python3 ../3line.py result3.json result7_1.json result5.json result6_1.json bfhd_runtime_over_r.png || echo "Didn't create bfhd_runtime_over_r"
echo | python3 ../8line.py result8_2.json result8_2.json result10_2.json result9_1.json jlnp_runtime_over_c.png || echo "Didn't create jlnp_runtime_over_c"

cat result13.log | python ../hyfd_log_analyzer.py | python3 ../3line.py result13.json result13.json result13.json result13.json cegi_runtime_over_r.png || echo "Didn't create cegi_runtime_over_r"
echo | python3 ../8line.py result11.json result11.json result11.json result11.json lmoq_runtime_over_c.png || echo "Didn't create lmoq_runtime_over_c"

